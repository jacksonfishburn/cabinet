package com.cabinet.service;

import com.cabinet.entity.FileRecord;
import com.cabinet.entity.User;
import com.cabinet.exception.ItemNotFoundException;
import com.cabinet.exception.StorageException;
import com.cabinet.model.InsertResponse;
import com.cabinet.repository.FileRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.List;

@Service
public class CabinetService {
    private final FileRecordRepository repository;
    private final FileService fileService;

    public CabinetService(FileRecordRepository repository, FileService fileService) {
        this.repository = repository;
        this.fileService = fileService;
    }

    @Transactional
    public InsertResponse insert(User user, String fileName, byte[] bytes) {
        List<FileRecord> userRecords = repository.findByUserId(user.getId());

        fileService.validateSizeLimit(userRecords, fileName, bytes);
        String md5 = computeMd5(bytes);

        FileRecord existing = userRecords.stream()
                .filter(r -> r.getName().equals(fileName))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return updateFile(user, fileName, bytes, md5, existing);
        }

        FileRecord fileRecord = createRecord(user, fileName, bytes.length, md5);
        repository.save(fileRecord);
        fileService.saveFile(user.getUsername(), fileName, bytes);
        return new InsertResponse(fileName, bytes.length, md5);
    }

    @Transactional 
    public byte[] grab(User user, String fileName) {
        if (repository.findByUserIdAndName(user.getId(), fileName) == null) {
            throw new ItemNotFoundException(fileName);
        }
        return fileService.readFile(user.getUsername(), fileName);
    }

    public List<FileRecord> peek(User user) {
        return repository.findByUserId(user.getId());
    }

    @Transactional 
    public void delete(User user, String fileName) {
        if (repository.findByUserIdAndName(user.getId(), fileName) == null) {
            throw new ItemNotFoundException(fileName);
        }
        repository.deleteByUserIdAndName(user.getId(), fileName);
        fileService.deleteFile(user.getUsername(), fileName);
    }

    private String computeMd5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 digest algorithm is not available", e);
        }
    }

    private FileRecord createRecord(User user, String name, int size, String md5) {
        return new FileRecord(user, name, size, md5);
    }

    @Transactional
    private InsertResponse updateFile(User user, String fileName, byte[] bytes, String md5, FileRecord existing) {
        if (md5.equals(existing.getMd5())) {
            existing.setUpdatedAt(Instant.now());
            repository.save(existing);
            return new InsertResponse(fileName, existing.getSizeBytes(), existing.getMd5());
        }

        existing.setMd5(md5);
        existing.setSizeBytes(bytes.length);
        existing.setUpdatedAt(Instant.now());
        repository.save(existing);
        fileService.saveFile(user.getUsername(), fileName, bytes);

        return new InsertResponse(fileName, bytes.length, md5);
    }
}
