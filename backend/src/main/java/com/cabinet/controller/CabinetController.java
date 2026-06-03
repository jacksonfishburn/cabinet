package com.cabinet.controller;

import com.cabinet.entity.FileRecord;
import com.cabinet.entity.User;
import com.cabinet.model.InsertResponse;
import com.cabinet.service.CabinetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{cabinet}/peek")
    public List<FileRecord> peek(@PathVariable Long cabinet) {
        User user = getCurrentUser();
        return cabinetService.peek(user, cabinet);
    }

    @GetMapping("/{cabinet}/{name}")
    public ResponseEntity<byte[]> grab(@PathVariable Long cabinet, @PathVariable String name) {
        User user = getCurrentUser();
        byte[] bytes = cabinetService.grab(user, cabinet, name);
        return ResponseEntity.ok()
                .header("Content-Type", "application/zip")
                .body(bytes);
    }

    @PostMapping("/{cabinet}/{name}")
    public InsertResponse insert(@PathVariable Long cabinet, @PathVariable String name, @RequestBody byte[] bytes) {
        User user = getCurrentUser();
        return cabinetService.insert(user, cabinet, name, bytes);
    }

    @DeleteMapping("/{cabinet}/{name}")
    public ResponseEntity<Void> delete(@PathVariable Long cabinet, @PathVariable String name) {
        User user = getCurrentUser();
        cabinetService.delete(user, cabinet, name);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        return (User) auth.getPrincipal();
    }
}
