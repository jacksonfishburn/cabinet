package com.cabinet.storage;

import com.cabinet.model.FileRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    }

    public void delete(String name) {

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
