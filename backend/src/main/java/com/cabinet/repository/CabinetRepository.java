package com.cabinet.repository;

import com.cabinet.entity.Cabinet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CabinetRepository extends JpaRepository<Cabinet, Long> {
    Optional<Cabinet> findByName(String name);
    Optional<Cabinet> findByMembers_UserIdAndIsDefaultTrue(Long userId);
    boolean existsByIdAndMembers_UserId(Long cabinetId, Long userId);
}