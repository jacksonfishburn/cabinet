package com.cabinet.service;

import com.cabinet.model.FileRecord;
import com.cabinet.storage.MetadataStore;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public class CabinetService {
    private MetadataStore metadataStore;

    @Value("${cabinet.storage-dir}")
    private String storageDir;

    @Value("${cabinet.max-size-mb}")
    private String maxSize;

    public FileRecord insert(String name, byte[] data) {
        return new FileRecord(data.length, "md5", null, null);
    }

    public byte[] grab(String name) {
        return new byte[0];
    }

    public Map<String, FileRecord> peek() {
        return metadataStore.findAll();
    }

    public void delete(String name) {

    }
}
