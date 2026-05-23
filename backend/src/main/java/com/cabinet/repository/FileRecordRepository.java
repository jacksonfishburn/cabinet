package com.cabinet.repository;

import com.cabinet.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    List<FileRecord> findByUserId(Long userId);
    void deleteByUserIdAndName(Long userId, String name);
}