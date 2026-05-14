package com.cabinet.service;

import com.cabinet.model.FileRecord;
import com.cabinet.storage.MetadataStore;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
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
    private final MetadataStore metadataStore;

    @Value("${cabinet.storage-dir}")
    private String storageDir;

    @Value("${cabinet.max-size-mb}")
    private String maxSize;

    public CabinetService(MetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    public FileRecord insert(String name, byte[] bytes) {
        validateSizeLimit(name, bytes);
        String md5 = computeMd5(bytes);
        FileRecord record = createRecord(name, bytes.length, md5);
        saveFile(name, bytes);
        metadataStore.save(name, record);
        return record;
    }

    public byte[] grab(String name) {
        metadataStore.find(name)
                .orElseThrow(() -> new IllegalArgumentException("No archive found for " + name));

        Path path = Paths.get(storageDir, name + ".zip");
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read archive for " + name, e);
        }
    }

    public Map<String, FileRecord> peek() {
        return metadataStore.findAll();
    }

    public void delete(String name) {
        Path path = Paths.get(storageDir, name + ".zip");
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete archive for " + name, e);
        }
        metadataStore.delete(name);
    }

    private void validateSizeLimit(String name, byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Archive bytes cannot be null");
        }

        long maxBytes = Long.parseLong(maxSize) * 1024L * 1024L;

        long currentTotal = metadataStore.findAll().values().stream()
                .filter(r -> !r.name().equals(name))
                .mapToLong(FileRecord::sizeBytes)
                .sum();

        if (currentTotal + bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "Archive would exceed cabinet limit: adding " + bytes.length +
                            " bytes to " + currentTotal + " bytes exceeds " + maxBytes + " bytes"
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

    private FileRecord createRecord(String name, int length, String md5) {
        return new FileRecord(name, length, md5, Instant.now(), Instant.now());
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
