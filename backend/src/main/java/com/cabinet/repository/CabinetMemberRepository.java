package com.cabinet.repository;

import com.cabinet.entity.CabinetMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CabinetMemberRepository extends JpaRepository<CabinetMember, Long> {
    List<CabinetMember> findByCabinetId(Long cabinetId);
    List<CabinetMember> findByUserId(Long userId);
    boolean existsByCabinetIdAndUserId(Long cabinetId, Long userId);
    Optional<CabinetMember> findByCabinetIdAndUserId(Long cabinetId, Long userId);
}