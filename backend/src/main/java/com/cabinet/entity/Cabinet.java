package com.cabinet.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cabinets")
public class Cabinet {

    protected Cabinet() {}

    public Cabinet(String name, boolean isPersonal) {
        this.name = name;
        this.isPersonal = isPersonal;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean isPersonal;

    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InviteCode> inviteCodes = new ArrayList<>();

    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileRecord> fileRecords = new ArrayList<>();

    @OneToMany(mappedBy = "cabinet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CabinetMember> members = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPersonal() {
        return isPersonal;
    }

    public void setPersonal(boolean personal) {
        isPersonal = personal;
    }

    public List<InviteCode> getInviteCodes() {
        return inviteCodes;
    }

    public void setInviteCodes(List<InviteCode> inviteCodes) {
        this.inviteCodes = inviteCodes;
    }

    public List<FileRecord> getFileRecords() {
        return fileRecords;
    }

    public void setFileRecords(List<FileRecord> fileRecords) {
        this.fileRecords = fileRecords;
    }

    public List<CabinetMember> getMembers() {
        return members;
    }

    public void setMembers(List<CabinetMember> members) {
        this.members = members;
    }
}
