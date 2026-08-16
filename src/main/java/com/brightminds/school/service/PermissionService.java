package com.brightminds.school.service;

import com.brightminds.school.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

// Backs the @PreAuthorize("@perm.has('MODULE:ACTION')") checks across the controllers.
// A user's effective permissions are the union of the permissions granted to every role
// (system or custom) they hold, via the roles/permissions tables editable under
// Users & Roles — so changing a role's permissions there now actually changes what that
// role's users can do, instead of that screen being purely decorative.
@Component("perm")
@RequiredArgsConstructor
public class PermissionService {

    private final RoleRepository roleRepo;

    public boolean has(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        Set<String> roleNames = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());

        // SUPER_ADMIN is an unremovable safety net: it always has full access regardless
        // of whatever permissions happen to be configured for that role, so a misconfigured
        // or emptied-out SUPER_ADMIN role can never lock every admin out of the system.
        if (roleNames.contains("SUPER_ADMIN")) return true;

        return roleRepo.findByNameIn(roleNames).stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName().equalsIgnoreCase(permission));
    }
}
