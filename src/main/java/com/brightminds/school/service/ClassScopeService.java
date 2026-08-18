package com.brightminds.school.service;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.SchoolClass;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

// A teacher (TEACHER/CLASS_TEACHER, and not also holding an admin-tier role) is restricted
// to the class(es) they're the class teacher of — the same "my classes" concept already used
// for attendance/marks — across every class-scoped resource: homework, report card remarks,
// pupil records, attendance registers, timetable and marks. Admin-tier roles and any other
// role (accountant, librarian, etc.) are unrestricted here; their own module permission is
// what already gates them via @PreAuthorize.
@Component
@RequiredArgsConstructor
public class ClassScopeService {

    private static final Set<String> ADMIN_TIER = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_HEAD_TEACHER", "ROLE_DEPUTY_HEAD");
    private static final Set<String> TEACHER_TIER = Set.of("ROLE_TEACHER", "ROLE_CLASS_TEACHER");

    private final AppUserRepository userRepo;
    private final SchoolClassRepository classRepo;

    /**
     * Null means "unrestricted" (admin-tier, or a role this scoping doesn't apply to).
     * A non-null (possibly empty) set means the caller must filter results to these class IDs.
     */
    public Set<java.util.UUID> restrictedClassIds(Authentication auth) {
        if (auth == null) return java.util.Set.of();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
        if (authorities.stream().anyMatch(ADMIN_TIER::contains)) return null;
        if (authorities.stream().noneMatch(TEACHER_TIER::contains)) return null;

        AppUser user = userRepo.findByEmail(auth.getName()).orElse(null);
        if (user == null) return Set.of();
        return classRepo.findByClassTeacherUserId(user.getId()).stream()
                .map(SchoolClass::getId)
                .collect(Collectors.toSet());
    }

    public boolean isRestricted(Authentication auth) {
        return restrictedClassIds(auth) != null;
    }

    /** Throws if the caller is restricted and classId isn't (or is null and so can't be) in their scope. */
    public void assertInScope(Set<java.util.UUID> scope, java.util.UUID classId) {
        if (scope == null) return; // unrestricted
        if (classId == null || !scope.contains(classId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You're only permitted to act on classes you teach.");
        }
    }
}
