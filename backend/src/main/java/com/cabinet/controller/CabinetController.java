package com.cabinet.controller;

import com.cabinet.model.FileRecord;
import com.cabinet.service.CabinetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CabinetController {
    private final CabinetService cabinetService;

    public CabinetController(CabinetService cabinetService) {
        this.cabinetService = cabinetService;
    }

    @GetMapping("/ping")
    public boolean heathCheck() {
        return true;
    }

    @GetMapping("/peek")
    public Map<String, FileRecord> peek() {
        return cabinetService.peek();
    }

    @GetMapping("/{name}")
    public ResponseEntity<byte[]> grab(@PathVariable String name) {
        byte[] bytes = cabinetService.grab(name);
        return ResponseEntity.ok()
                .header("Content-Type", "application/zip")
                .body(bytes);
    }

    @PostMapping("/{name}")
    public FileRecord insert(@PathVariable String name, @RequestBody byte[] bytes) {
        return cabinetService.insert(name, bytes);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        cabinetService.delete(name);
        return ResponseEntity.noContent().build();
    }
}
