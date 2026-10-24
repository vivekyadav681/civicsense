package com.example.civicsense.repository;

import com.example.civicsense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by {@link com.example.civicsense.security.UserDetailsServiceImpl} for authentication. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
