package com.aomanagement.repository;

import com.aomanagement.entity.Role;
import com.aomanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    List<User> findByRoleOrderByLastNameAscFirstNameAsc(Role role);
}
