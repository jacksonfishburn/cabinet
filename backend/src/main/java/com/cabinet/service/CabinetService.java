package com.cabinet.service;

import com.cabinet.model.FileRecord;
import com.cabinet.storage.MetadataStore;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CabinetService {
    private MetadataStore metadataStore;

    @Value("${cabinet.storage-dir}")
    private String storageDir;

    @Value("${cabinet.max-size-mb}")
    private String maxSize;

    public FileRecord insert(String name, byte[] bytes) {
        validateSizeLimit(bytes);
        String md5 = computeMd5(bytes);
        FileRecord record = createRecord(bytes.length, md5);
        saveFile(name, bytes);
        metadataStore.save(name, record);
        return record;
    }

    public byte[] grab(String name) {
        return new byte[0];
    }

    public Map<String, FileRecord> peek() {
        return metadataStore.findAll();
    }

    public void delete(String name) {

    }

    private void validateSizeLimit(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Archive bytes cannot be null");
        }

        long maxBytes = Long.parseLong(maxSize) * 1024L * 1024L;
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "Archive is too large: " + bytes.length + " bytes exceeds the limit of " + maxBytes + " bytes"
            );
        }
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

    private FileRecord createRecord(int length, String md5) {
        return new FileRecord(length, md5, Instant.now(), Instant.now());
    }

    private void saveFile(String name, byte[] bytes) {
        try {
            Path storagePath = Paths.get(storageDir);
            Files.createDirectories(storagePath);

            Path archivePath = storagePath.resolve(name + ".zip");
            Files.write(
                    archivePath,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save archive for " + name, e);
        }
    }
}
