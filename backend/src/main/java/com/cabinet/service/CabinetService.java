package com.cabinet.service;

import com.cabinet.entity.FileRecord;
import com.cabinet.entity.User;
import com.cabinet.model.InsertResponse;
import com.cabinet.repository.FileRecordRepository;
import org.springframework.stereotype.Service;

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

    public InsertResponse insert(User user, String fileName, byte[] bytes) {
        List<FileRecord> userRecords = repository.findByUserId(user.getId());

        fileService.validateSizeLimit(userRecords, fileName, bytes);
        String md5 = computeMd5(bytes);

        FileRecord existing = userRecords.stream()
                .filter(r -> r.getName().equals(fileName))
                .findFirst()
                .orElse(null);

        return saveOrUpdate(user, fileName, bytes, md5, existing);
    }

    public byte[] grab(User user, String fileName) {
        return fileService.readFile(user.getUsername(), fileName);
    }

    public List<FileRecord> peek(User user) {
        return repository.findByUserId(user.getId());
    }

    public void delete(User user, String name) {
        fileService.deleteFile(user.getUsername(), name);
        repository.deleteByUserIdAndName(user.getId(), name);
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

    private InsertResponse saveOrUpdate(User user, String fileName, byte[] bytes, String md5, FileRecord existing) {
        if (existing != null) {
            if (md5.equals(existing.getMd5())) {
                existing.setUpdatedAt(Instant.now());
                repository.save(existing);
                return new InsertResponse(fileName, existing.getSizeBytes(), existing.getMd5());
            }

            fileService.saveFile(user.getUsername(), fileName, bytes);
            existing.setMd5(md5);
            existing.setSizeBytes(bytes.length);
            existing.setUpdatedAt(Instant.now());
            try {
                repository.save(existing);
            } catch (RuntimeException e) {
                try
                { fileService.deleteFile(user.getUsername(), fileName);
                } catch (Exception ignore) {}
                throw e;
            }
            return new InsertResponse(fileName, bytes.length, md5);
        }

        FileRecord fileRecord = createRecord(user, fileName, bytes.length, md5);
        fileService.saveFile(user.getUsername(), fileName, bytes);
        try {
            repository.save(fileRecord);
        } catch (RuntimeException e) {
            try { fileService.deleteFile(user.getUsername(), fileName); } catch (Exception ignore) {}
            throw e;
        }
        return new InsertResponse(fileName, bytes.length, md5);
    }
}
