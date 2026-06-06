package com.cabinet.service;

import com.cabinet.entity.Cabinet;
import com.cabinet.entity.CabinetMember;
import com.cabinet.entity.FileRecord;
import com.cabinet.entity.User;
import com.cabinet.exception.CabinetNotFoundException;
import com.cabinet.exception.ItemNotFoundException;
import com.cabinet.model.CabinetInfo;
import com.cabinet.model.InsertResponse;
import com.cabinet.model.ListCabinetsResponse;
import com.cabinet.repository.CabinetRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CabinetService {
    private final CabinetRepository repository;
    private final FileService fileService;
    private final CabinetManagementService cabinetManagementService;

    public CabinetService(CabinetRepository repository,
                          FileService fileService,
                          CabinetManagementService cabinetManagementService) {
        this.repository = repository;
        this.fileService = fileService;
        this.cabinetManagementService = cabinetManagementService;
    }

    @Transactional
    @CacheEvict(value = "peek", key = "#cabinetId")
    public InsertResponse insert(User user, Long cabinetId, String fileName, byte[] bytes) {
        Cabinet cabinet = findCabinetAndVerifyMembership(cabinetId, user);
        List<FileRecord> cabinetRecords = cabinet.getFileRecords();

        fileService.validateSizeLimit(cabinetRecords, fileName, bytes);
        String md5 = computeMd5(bytes);

        FileRecord existing = getFileRecord(cabinet, fileName)
                .orElse(null);

        if (existing != null) {
            InsertResponse response = updateFile(cabinet.getId().toString(), fileName, bytes, md5, existing);
            repository.save(cabinet);
            return response;
        }

        FileRecord fileRecord = createRecord(cabinet, fileName, bytes.length, md5);
        cabinetRecords.add(fileRecord);
        repository.save(cabinet);
        fileService.saveFile(cabinet.getId().toString(), fileName, bytes);
        return new InsertResponse(fileName, bytes.length, md5);
    }

    @Transactional 
    public byte[] grab(User user, Long cabinetId, String fileName) {
        Cabinet cabinet = findCabinetAndVerifyMembership(cabinetId, user);

        getFileRecord(cabinet, fileName)
                .orElseThrow(() -> new ItemNotFoundException(fileName));

        return fileService.readFile(cabinet.getId().toString(), fileName);
    }

    public void peekVerifyMember(User user, Long cabinetId) {
        findCabinetAndVerifyMembership(cabinetId, user);
    }

    @Cacheable(value = "peek", key = "#cabinetId")
    public List<FileRecord> peek(User user, Long cabinetId) {
        Cabinet cabinet = findCabinetAndVerifyMembership(cabinetId, user);
        return cabinet.getFileRecords();
    }

    @Transactional
    @CacheEvict(value = "peek", key = "#cabinetId")
    public void delete(User user, Long cabinetId, String fileName) {
        Cabinet cabinet = findCabinetAndVerifyMembership(cabinetId, user);
        List<FileRecord> userRecords = cabinet.getFileRecords();

        FileRecord existing = getFileRecord(cabinet, fileName)
                .orElseThrow(() -> new ItemNotFoundException(fileName));

        userRecords.remove(existing);
        repository.save(cabinet);
        fileService.deleteFile(cabinet.getId().toString(), fileName);
    }

    public ListCabinetsResponse listCabinets(User user) {
        List<Cabinet> cabinets = cabinetManagementService.getCabinets(user);
        List<CabinetInfo> cabinetInfos = cabinets.stream()
                .map(c -> new CabinetInfo(c.getId(), c.getName()))
                .toList();
        return new ListCabinetsResponse(cabinetInfos);
    }

    public CabinetInfo createCabinet(User user, String name) {
        Cabinet cabinet = cabinetManagementService.createCabinet(user, name);
        return new CabinetInfo(cabinet.getId(), name);
    }

    public String generateInviteCode(User user, Long cabinetId) {
        Cabinet cabinet = findCabinetAndVerifyMembership(cabinetId, user);
        return cabinetManagementService.generateInviteCode(cabinet);
    }

    @Transactional
    public CabinetInfo join(User user, String code) {
        Cabinet cabinet = cabinetManagementService.joinCabinet(user, code);
        return new CabinetInfo(cabinet.getId(), cabinet.getName());
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

    private FileRecord createRecord(Cabinet cabinet, String name, int size, String md5) {
        return new FileRecord(cabinet, name, size, md5);
    }

    private InsertResponse updateFile(String cabinetId, String fileName,
                                      byte[] bytes, String md5,
                                      FileRecord existing) {
        if (md5.equals(existing.getMd5())) {
            existing.setUpdatedAt(Instant.now());
            return new InsertResponse(fileName, existing.getSizeBytes(), existing.getMd5());
        }

        existing.setMd5(md5);
        existing.setSizeBytes(bytes.length);
        existing.setUpdatedAt(Instant.now());
        fileService.saveFile(cabinetId, fileName, bytes);

        return new InsertResponse(fileName, bytes.length, md5);
    }

    private Cabinet findCabinetAndVerifyMembership(Long id, User user) {
        Cabinet cabinet = repository.findById(id)
                .orElseThrow(CabinetNotFoundException::new);

        List<CabinetMember> members = cabinet.getMembers();
        boolean isMember = members.stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()));
        if (!isMember) {
            throw new CabinetNotFoundException();
        }

        return cabinet;
    }

    private Optional<FileRecord> getFileRecord(Cabinet cabinet, String fileName) {
        return cabinet.getFileRecords().stream()
                .filter(r -> r.getName().equals(fileName))
                .findFirst();
    }
}