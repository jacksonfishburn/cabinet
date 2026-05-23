package com.cabinet.service;

import com.cabinet.entity.FileRecord;
import com.cabinet.entity.User;
import com.cabinet.model.InsertResponse;
import com.cabinet.repository.FileRecordRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

        FileRecord fileRecord = createRecord(user, fileName, bytes.length, md5);
        repository.save(fileRecord);
        fileService.saveFile(user.getUsername(), fileName, bytes);
        return new InsertResponse(fileName, bytes.length, md5);
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
}
