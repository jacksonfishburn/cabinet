package com.cabinet.service;

import com.cabinet.entity.FileRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class FileService {

    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @Value("${cabinet.storage-dir}")
    private String storageDir;

    @Value("${cabinet.max-size-mb}")
    private String maxSize;

    public void saveFile(String username, String fileName, byte[] bytes) {
        try {
            String safeUsername = sanitizePathSegment(username, "username");
            String safeFileName = sanitizePathSegment(fileName, "fileName");

            Path storagePath = Paths.get(storageDir, safeUsername);
            Files.createDirectories(storagePath);

            Path archivePath = storagePath.resolve(safeFileName + ".zip");
            Files.write(
                    archivePath,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save archive for " + fileName, e);
        }
    }

    public void validateSizeLimit(List<FileRecord> records, String name, byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Archive bytes cannot be null");
        }

        long maxBytes = Long.parseLong(maxSize) * 1024L * 1024L;

        FileRecord existingRecord = records.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (existingRecord != null) {
            maxBytes += existingRecord.getSizeBytes();
        }

        long totalBytes = records.stream().mapToLong(FileRecord::getSizeBytes).sum();

        if (totalBytes + bytes.length > maxBytes) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                    "An archive that big wont fit in the cabinet.");
        }
    }

     public byte[] readFile(String username, String fileName) {
         try {
             String safeUsername = sanitizePathSegment(username, "username");
             String safeFileName = sanitizePathSegment(fileName, "fileName");

             Path archivePath = Paths.get(storageDir, safeUsername, safeFileName + ".zip");
             return Files.readAllBytes(archivePath);
         } catch (Exception e) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No archive found for: " + fileName);
         }
     }

     public void deleteFile(String username, String fileName) {
         try {
             String safeUsername = sanitizePathSegment(username, "username");
             String safeFileName = sanitizePathSegment(fileName, "fileName");

             Path path = Paths.get(storageDir, safeUsername, safeFileName + ".zip");
             Files.deleteIfExists(path);
         } catch (Exception e) {
             throw new RuntimeException("Failed to delete archive for " + fileName, e);
         }
     }

    private String sanitizePathSegment(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        if (!SAFE_PATH_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
        return value;
    }
}
