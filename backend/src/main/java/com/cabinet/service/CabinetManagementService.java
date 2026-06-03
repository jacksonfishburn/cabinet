package com.cabinet.service;

import com.cabinet.entity.Cabinet;
import com.cabinet.entity.CabinetMember;
import com.cabinet.entity.InviteCode;
import com.cabinet.entity.User;
import com.cabinet.exception.CabinetNotFoundException;
import com.cabinet.exception.InvalidCodeException;
import com.cabinet.repository.CabinetMemberRepository;
import com.cabinet.repository.CabinetRepository;
import com.cabinet.repository.InviteCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.annotation.IncompleteAnnotationException;
import java.security.SecureRandom;
import java.util.List;

@Service
public class CabinetManagementService {

    private final CabinetRepository cabinetRepository;
    private final CabinetMemberRepository cabinetMemberRepository;
    private final InviteCodeRepository inviteCodeRepository;


    public CabinetManagementService(CabinetRepository cabinetRepository,
                                    CabinetMemberRepository cabinetMemberRepository,
                                    InviteCodeRepository inviteCodeRepository
    ) {
        this.cabinetRepository = cabinetRepository;
        this.cabinetMemberRepository = cabinetMemberRepository;
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public Cabinet createCabinet(User user, String name) {
        Cabinet cabinet = new Cabinet(name, false);
        CabinetMember member = new CabinetMember(cabinet, user);

        cabinetMemberRepository.save(member);
        return cabinetRepository.save(cabinet);
    }

    public Cabinet getDefaultCabinet(User user) {
        return cabinetRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(CabinetNotFoundException::new);
    }

    public List<Cabinet> getCabinets(User user) {
        return cabinetMemberRepository.findByUserId(user.getId())
                .stream()
                .map(CabinetMember::getCabinet)
                .toList();
    }

    public String generateInviteCode(Cabinet cabinet) {
        String code = generateRandomCode();
        while (inviteCodeRepository.findByCode(code).isPresent()) {
            code = generateRandomCode();
        }
        InviteCode inviteCode = new InviteCode(cabinet, code);
        return inviteCodeRepository.save(inviteCode).getCode();
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public Cabinet joinCabinet(User user, String code) {
        InviteCode inviteCode = inviteCodeRepository.findByCode(code)
                .orElseThrow(InvalidCodeException::new);
        if (inviteCode.isUsed() || inviteCode.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new InvalidCodeException("Invite code is expired or already used");
        }
        Cabinet cabinet = inviteCode.getCabinet();

        CabinetMember member = new CabinetMember(cabinet, user);
        cabinetMemberRepository.save(member);
        inviteCode.markUsed();
        inviteCodeRepository.save(inviteCode);

        return cabinet;
    }
}