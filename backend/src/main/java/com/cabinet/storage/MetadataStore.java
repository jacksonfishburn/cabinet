package com.cabinet.storage;

import com.cabinet.model.FileRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MetadataStore {
    private Map<String, FileRecord> data;

    @Value("${cabinet.storage-dir}")
    private String storageDir;

    @PostConstruct
    public void load() {
        File metadataFile = new File(storageDir, "cabinet-meta.json");

        if (!metadataFile.exists()) {
            data = new HashMap<>();
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            data = mapper.readValue(metadataFile, new TypeReference<Map<String, FileRecord>>() {});
        } catch (Exception e) {
            data = new HashMap<>();
        }
    }

    public void save(String name, FileRecord record) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(name, record);
        flush();
    }

    public void delete(String name) {
        if (data != null) {
            data.remove(name);
            flush();
        }
    }

    public Optional<FileRecord> find(String name) {
        if (data == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.get(name));
    }

    public Map<String, FileRecord> findAll() {
        if (data == null) {
            return new HashMap<>();
        }
        return data;
    }

    private void flush() {
        try {
            File dir = new File(storageDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new RuntimeException("Unable to create storage directory: " + storageDir);
                }
            }

            File metadataFile = new File(storageDir, "cabinet-meta.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();

            Map<String, FileRecord> toWrite = (data == null) ? new HashMap<>() : data;
            mapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile, toWrite);
        } catch (Exception e) {
            throw new RuntimeException("Failed to flush metadata to disk", e);
        }
    }
}
