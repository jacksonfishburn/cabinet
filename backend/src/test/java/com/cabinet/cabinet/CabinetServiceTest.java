package com.cabinet.cabinet;

import com.cabinet.model.FileRecord;
import com.cabinet.service.CabinetService;
import com.cabinet.storage.MetadataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CabinetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void insert_savesFileAndMetadata_andReturnsMatchingRecord() throws Exception {
        MetadataStore store = newStore();
        CabinetService service = newService(store, "10");
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        FileRecord record = service.insert("greeting", bytes);

        assertEquals("greeting", record.name());
        assertEquals(bytes.length, record.sizeBytes());
        assertEquals("5d41402abc4b2a76b9719d911017c592", record.md5());
        assertNotNull(record.createdAt());
        assertNotNull(record.updatedAt());

        Path filePath = tempDir.resolve("greeting.zip");
        assertTrue(Files.exists(filePath));
        assertArrayEquals(bytes, Files.readAllBytes(filePath));
        assertEquals(record, store.find("greeting").orElseThrow());
    }

    @Test
    void insert_sameNameTwice_overwritesStoredFileAndMetadata() throws Exception {
        MetadataStore store = newStore();
        CabinetService service = newService(store, "10");
        byte[] first = "first version".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second version".getBytes(StandardCharsets.UTF_8);

        service.insert("same-name", first);
        FileRecord secondRecord = service.insert("same-name", second);

        assertEquals(1, store.findAll().size());
        assertEquals(secondRecord, store.find("same-name").orElseThrow());
        assertArrayEquals(second, Files.readAllBytes(tempDir.resolve("same-name.zip")));
        assertEquals(second.length, secondRecord.sizeBytes());
    }

    @Test
    void insert_rejectsWhenSizeLimitWouldBeExceeded_andDoesNotWriteAnything() {
        MetadataStore store = newStore();
        CabinetService service = newService(store, "0");
        byte[] bytes = "too big".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> service.insert("oversized", bytes));
        assertTrue(store.findAll().isEmpty());
        assertFalse(Files.exists(tempDir.resolve("oversized.zip")));
    }

    @Test
    void grab_returnsBytesPreviouslySavedToDisk() {
        MetadataStore store = newStore();
        CabinetService service = newService(store, "10");
        byte[] bytes = "fetch me".getBytes(StandardCharsets.UTF_8);

        service.insert("download", bytes);

        assertArrayEquals(bytes, service.grab("download"));
    }

    @Test
    void delete_removesBothFileAndMetadataEntry() {
        MetadataStore store = newStore();
        CabinetService service = newService(store, "10");
        byte[] bytes = "remove me".getBytes(StandardCharsets.UTF_8);

        service.insert("to-delete", bytes);
        service.delete("to-delete");

        assertFalse(Files.exists(tempDir.resolve("to-delete.zip")));
        assertTrue(store.find("to-delete").isEmpty());
        assertTrue(store.findAll().isEmpty());
    }

    private MetadataStore newStore() {
        MetadataStore store = new MetadataStore();
        setField(store, "storageDir", tempDir.toString());
        store.load();
        return store;
    }

    private CabinetService newService(MetadataStore store, String maxSizeMb) {
        CabinetService service = new CabinetService(store);
        setField(service, "storageDir", tempDir.toString());
        setField(service, "maxSize", maxSizeMb);
        return service;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
