package com.cabinet.service;

import com.cabinet.entity.Cabinet;
import com.cabinet.entity.CabinetMember;
import com.cabinet.entity.User;
import com.cabinet.exception.CabinetNotFoundException;
import com.cabinet.repository.CabinetMemberRepository;
import com.cabinet.repository.CabinetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabinetManagementService {

    private final CabinetRepository cabinetRepository;
    private final CabinetMemberRepository cabinetMemberRepository;

    public CabinetManagementService(CabinetRepository cabinetRepository, CabinetMemberRepository cabinetMemberRepository) {
        this.cabinetRepository = cabinetRepository;
        this.cabinetMemberRepository = cabinetMemberRepository;
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
}