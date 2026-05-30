package com.cabinet.cabinet.unit;

import com.cabinet.entity.FileRecord;
import com.cabinet.exception.FileTooLargeException;
import com.cabinet.exception.InvalidFileException;
import com.cabinet.exception.StorageException;
import com.cabinet.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "storageDir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "maxSize", "1");
    }

    // Verifies a valid file is persisted at {storageDir}/{username}/{fileName}.zip.
    @Test
    void saveFile_validInput_writesArchiveToDisk() {
        byte[] content = "zip-bytes".getBytes();

        fileService.saveFile("alice", "docs", content);

        Path savedPath = tempDir.resolve("alice").resolve("docs.zip");
        assertTrue(Files.exists(savedPath));
        assertArrayEquals(content, assertDoesNotThrow(() -> Files.readAllBytes(savedPath)));
    }

    // Verifies invalid path input is rejected and surfaced as a storage failure.
    @Test
    void saveFile_invalidPathSegment_throwsStorageException() {
        StorageException exception = assertThrows(
                StorageException.class,
                () -> fileService.saveFile("../alice", "docs", "content".getBytes())
        );

        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    // Verifies reading an existing archive returns the exact persisted bytes.
    @Test
    void readFile_existingArchive_returnsBytes() {
        byte[] content = "archive-content".getBytes();
        fileService.saveFile("bob", "backup", content);

        byte[] loaded = fileService.readFile("bob", "backup");

        assertArrayEquals(content, loaded);
    }

    // Verifies missing archive reads are surfaced as a storage failure.
    @Test
    void readFile_missingArchive_throwsStorageException() {
        assertThrows(StorageException.class, () -> fileService.readFile("charlie", "missing"));
    }

    // Verifies deleting an existing archive removes it from disk.
    @Test
    void deleteFile_existingArchive_removesArchive() {
        fileService.saveFile("dana", "project", "bytes".getBytes());
        Path path = tempDir.resolve("dana").resolve("project.zip");
        assertTrue(Files.exists(path));

        fileService.deleteFile("dana", "project");

        assertFalse(Files.exists(path));
    }

    // Verifies null upload bytes are rejected before any size arithmetic.
    @Test
    void validateSizeLimit_nullBytes_throwsInvalidFileException() {
        assertThrows(
                InvalidFileException.class,
                () -> fileService.validateSizeLimit(List.of(), "archive", null)
        );
    }

    // Verifies a payload under the configured size limit is accepted.
    @Test
    void validateSizeLimit_underConfiguredLimit_doesNotThrow() {
        byte[] content = new byte[512 * 1024]; // 0.5 MB

        assertDoesNotThrow(() -> fileService.validateSizeLimit(List.of(), "small", content));
    }

    // Verifies total stored bytes plus new upload exceeding max size is rejected.
    @Test
    void validateSizeLimit_exceedsConfiguredLimit_throwsFileTooLargeException() {
        FileRecord existing = new FileRecord();
        existing.setName("existing");
        existing.setSizeBytes(800 * 1024); // 0.8 MB

        byte[] newUpload = new byte[300 * 1024]; // 0.3 MB

        assertThrows(
                FileTooLargeException.class,
                () -> fileService.validateSizeLimit(List.of(existing), "new-file", newUpload)
        );
    }
}

