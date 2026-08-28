package com.brightminds.school.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// Everything backup-related that isn't HTTP concerns lives here, shared by BackupController
// (manual downloads + restore) and the nightly scheduled job below — one place that knows how
// to actually talk to pg_dump/psql, instead of that logic being duplicated between "someone
// clicked a button" and "the clock said 2am".
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    @Value("${spring.datasource.password}")
    private String datasourcePassword;
    @Value("${app.backup.pg-dump-path}")
    private String pgDumpPath;
    @Value("${app.backup.psql-path}")
    private String psqlPath;
    @Value("${app.uploads.dir}")
    private String uploadsDir;
    @Value("${app.backup.dir:./backups}")
    private String backupDir;
    @Value("${app.backup.retention:14}")
    private int retention;

    private static final Pattern JDBC_URL = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");

    public record DbTarget(String host, String port, String name) {
    }

    public DbTarget parseDbTarget() {
        Matcher m = JDBC_URL.matcher(datasourceUrl);
        if (!m.matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not parse datasource URL for backup: " + datasourceUrl);
        }
        return new DbTarget(m.group(1), m.group(2) != null ? m.group(2) : "5432", m.group(3));
    }

    // --clean --if-exists makes the dump self-restoring: every object is dropped (if it
    // exists) immediately before being recreated, so running it back through psql cleanly
    // overwrites a database that already has the app's schema/data in it — not just an empty
    // one. Without this, restoring into anything but a brand-new database fails outright with
    // "relation already exists" on the very first CREATE TABLE.
    public byte[] runPgDump(DbTarget target) {
        Path tmp;
        try {
            tmp = Files.createTempFile("school-backup-", ".sql");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not create temp file for backup", e);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpPath, "-h", target.host(), "-p", target.port(), "-U", datasourceUsername,
                    "--no-owner", "--no-privileges", "--clean", "--if-exists",
                    "-F", "p", "-f", tmp.toString(), target.name());
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String log = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "pg_dump timed out after 120s");
            }
            if (proc.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "pg_dump failed (exit " + proc.exitValue() + "): " + log);
            }
            return Files.readAllBytes(tmp);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not run pg_dump (is it installed and on PATH? override with PG_DUMP_PATH): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup interrupted", e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
        }
    }

    // The one backup meant for disaster recovery: the full database (schema + every row) AND
    // every uploaded file bundled into a single archive — restoring only the database would
    // leave every image/document reference in the app pointing at files that no longer exist.
    public byte[] buildFullBackupZip() {
        byte[] sql = runPgDump(parseDbTarget());
        Path uploads = Paths.get(uploadsDir);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buf)) {
            zip.putNextEntry(new ZipEntry("database.sql"));
            zip.write(sql);
            zip.closeEntry();
            if (Files.isDirectory(uploads)) {
                try (Stream<Path> files = Files.walk(uploads)) {
                    for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                        zip.putNextEntry(new ZipEntry("uploads/" + uploads.relativize(f).toString().replace('\\', '/')));
                        Files.copy(f, zip);
                        zip.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not build backup archive: " + e.getMessage(), e);
        }
        return buf.toByteArray();
    }

    public record RestoreResult(int filesRestored) {
    }

    public RestoreResult restoreFromZip(InputStream zipInput) {
        DbTarget target = parseDbTarget();
        byte[] sqlToRestore = null;
        int filesRestored = 0;
        Path uploads = Paths.get(uploadsDir);
        try (ZipInputStream zip = new ZipInputStream(zipInput)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (entry.getName().equals("database.sql")) {
                    sqlToRestore = zip.readAllBytes();
                } else if (entry.getName().startsWith("uploads/")) {
                    String relative = entry.getName().substring("uploads/".length());
                    if (relative.isBlank() || relative.contains("..")) continue;
                    Path dest = uploads.resolve(relative).normalize();
                    if (!dest.startsWith(uploads.normalize())) continue;
                    Files.createDirectories(dest.getParent());
                    Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
                    filesRestored++;
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded backup archive: " + e.getMessage(), e);
        }
        if (sqlToRestore == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This doesn't look like a full backup archive — no database.sql found inside it.");
        }

        Path tmp;
        try {
            tmp = Files.createTempFile("school-restore-", ".sql");
            Files.write(tmp, sqlToRestore);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not stage the restore file", e);
        }
        try {
            // ON_ERROR_STOP=1 so a genuine problem (version mismatch, a table still locked by
            // a live connection, etc.) surfaces immediately as a failed restore instead of
            // psql quietly limping through and reporting success over a half-restored database.
            ProcessBuilder pb = new ProcessBuilder(
                    psqlPath, "-h", target.host(), "-p", target.port(), "-U", datasourceUsername,
                    "-v", "ON_ERROR_STOP=1", "-f", tmp.toString(), target.name());
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String log = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = proc.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Restore timed out after 5 minutes");
            }
            if (proc.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "psql reported errors during restore (exit " + proc.exitValue() + "):\n" + log);
            }
            return new RestoreResult(filesRestored);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not run psql (is it installed and on PATH? override with PSQL_PATH): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Restore interrupted", e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
        }
    }

    // ─── On-disk scheduled/safety backups ──────────────────────────────────────

    private Path backupDirPath() {
        Path dir = Paths.get(backupDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not create backup directory " + dir, e);
        }
        return dir;
    }

    // `prefix` distinguishes what triggered the snapshot ("scheduled" for the nightly job,
    // "pre-restore" for the automatic safety net right before a restore) so an admin browsing
    // the list later can tell why each one exists.
    public Path saveSnapshot(String prefix) {
        byte[] zip = buildFullBackupZip();
        Path dir = backupDirPath();
        String filename = prefix + "-" + LocalDateTime.now().format(STAMP) + ".zip";
        Path dest = dir.resolve(filename);
        try {
            Files.write(dest, zip);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save backup snapshot to " + dest, e);
        }
        return dest;
    }

    public record SnapshotInfo(String filename, long sizeBytes, String createdAt) {
    }

    public List<SnapshotInfo> listSnapshots() {
        Path dir = backupDirPath();
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(f -> f.toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .map(f -> new SnapshotInfo(
                            f.getFileName().toString(),
                            sizeSafe(f),
                            java.time.Instant.ofEpochMilli(lastModifiedSafe(f)).toString()))
                    .toList();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not list backups in " + dir, e);
        }
    }

    private long lastModifiedSafe(Path f) {
        try { return Files.getLastModifiedTime(f).toMillis(); } catch (IOException e) { return 0; }
    }

    private long sizeSafe(Path f) {
        try { return Files.size(f); } catch (IOException e) { return 0; }
    }

    // Only ever resolves a bare filename inside the backup directory — never a path a caller
    // could use to walk out of it (see BackupController, which rejects anything containing a
    // path separator before this is even called, but this is the actual enforcement point).
    public Path resolveSnapshot(String filename) {
        Path dir = backupDirPath();
        Path resolved = dir.resolve(filename).normalize();
        if (!resolved.startsWith(dir.normalize()) || !Files.exists(resolved)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup not found: " + filename);
        }
        return resolved;
    }

    // Deletes the oldest scheduled/pre-restore snapshots beyond the configured retention count
    // so this directory doesn't grow forever — each backup contains every uploaded file, so
    // these add up fast on a school with a few years of photos and documents.
    private void pruneOldSnapshots() {
        List<SnapshotInfo> all = listSnapshots();
        if (all.size() <= retention) return;
        Path dir = backupDirPath();
        all.stream().skip(retention).forEach(s -> {
            try {
                Files.deleteIfExists(dir.resolve(s.filename()));
                log.info("Pruned old backup snapshot: {}", s.filename());
            } catch (IOException e) {
                log.warn("Could not prune old backup snapshot {}", s.filename(), e);
            }
        });
    }

    // Runs once a day at 02:00 server time — quiet hours for a school system, and well clear
    // of the late-fee sweep (see FeeController) which runs at 01:00.
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        try {
            Path saved = saveSnapshot("scheduled");
            log.info("Scheduled backup saved: {}", saved);
            pruneOldSnapshots();
        } catch (Exception e) {
            // A failed nightly backup must never crash the app or block anything else — it
            // just means tonight's snapshot didn't happen, which is exactly why relying on
            // manual downloads alone was worth automating away in the first place.
            log.error("Scheduled backup failed", e);
        }
    }
}
