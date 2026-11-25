package com.aomanagement.repository;

import com.aomanagement.entity.ApplicationVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationVersionRepository extends JpaRepository<ApplicationVersion, Long> {
    Optional<ApplicationVersion> findTopByOrderByBuildDateDesc();
}
