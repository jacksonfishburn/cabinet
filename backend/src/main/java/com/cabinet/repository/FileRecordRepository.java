package com.cabinet.repository;

import com.cabinet.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    List<FileRecord> findByUserId(Long userId);
    Optional<FileRecord> findByUserIdAndName(Long userId, String name);
    void deleteByUserIdAndName(Long userId, String name);
}