package com.example.civicsense.entity;

import com.example.civicsense.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Represents an application user.
 * Implements {@link UserDetails} so Spring Security can directly use this entity
 * without a separate DTO conversion step.
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name of the user. */
    @Column(nullable = false, length = 120)
    private String name;

    /** Unique email used as the login username. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-hashed password. Never stored in plain text. */
    @Column(nullable = false)
    private String password;

    /** Role controlling access to API endpoints. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    // ── UserDetails contract ─────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects the "ROLE_" prefix for hasRole() checks.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Email is used as the unique username for authentication. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return true; }
}
