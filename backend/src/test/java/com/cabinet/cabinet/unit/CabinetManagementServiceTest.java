package com.cabinet.cabinet.unit;

import com.cabinet.entity.Cabinet;
import com.cabinet.entity.CabinetMember;
import com.cabinet.entity.InviteCode;
import com.cabinet.entity.User;
import com.cabinet.exception.CabinetNotFoundException;
import com.cabinet.exception.InvalidCodeException;
import com.cabinet.repository.CabinetMemberRepository;
import com.cabinet.repository.CabinetRepository;
import com.cabinet.repository.InviteCodeRepository;
import com.cabinet.service.CabinetManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class CabinetManagementServiceTest {

    @Mock
    private CabinetRepository cabinetRepository;

    @Mock
    private CabinetMemberRepository cabinetMemberRepository;

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private CabinetManagementService cabinetManagementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cabinetManagementService, "expirationMs", 3_600_000L);
    }

    // Verifies createCabinet persists the cabinet first, then creates the member with the saved cabinet ID.
    @Test
    void createCabinet_savesCabinetBeforeMemberAndUsesCorrectRelations() {
        User user = new User("alice", "hash");
        user.setId(11L);

        when(cabinetRepository.save(any(Cabinet.class))).thenAnswer(invocation -> {
            Cabinet cabinet = invocation.getArgument(0);
            cabinet.setName("personal");
            ReflectionTestUtils.setField(cabinet, "id", 101L);
            return cabinet;
        });
        when(cabinetMemberRepository.save(any(CabinetMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cabinet cabinet = cabinetManagementService.createCabinet(user, "personal");

        assertNotNull(cabinet.getId());
        assertEquals(101L, cabinet.getId());
        assertFalse(cabinet.isDefault());

        ArgumentCaptor<CabinetMember> memberCaptor = ArgumentCaptor.forClass(CabinetMember.class);
        InOrder inOrder = inOrder(cabinetRepository, cabinetMemberRepository);
        inOrder.verify(cabinetRepository).save(any(Cabinet.class));
        inOrder.verify(cabinetMemberRepository).save(memberCaptor.capture());

        CabinetMember savedMember = memberCaptor.getValue();
        assertNotNull(savedMember.getCabinet().getId());
        assertEquals(101L, savedMember.getCabinet().getId());
        assertEquals(user, savedMember.getUser());
    }

    // Verifies the default cabinet lookup returns the matching cabinet for the user.
    @Test
    void getDefaultCabinet_returnsMatchingCabinet() {
        User user = new User("bob", "hash");
        user.setId(22L);
        Cabinet defaultCabinet = new Cabinet("default", true);
        ReflectionTestUtils.setField(defaultCabinet, "id", 202L);

        when(cabinetRepository.findByMembers_UserIdAndIsDefaultTrue(22L)).thenReturn(Optional.of(defaultCabinet));

        Cabinet result = cabinetManagementService.getDefaultCabinet(user);

        assertEquals(defaultCabinet, result);
    }

    // Verifies the default cabinet lookup throws when the user does not have one.
    @Test
    void getDefaultCabinet_missingCabinet_throwsCabinetNotFoundException() {
        User user = new User("carol", "hash");
        user.setId(33L);

        when(cabinetRepository.findByMembers_UserIdAndIsDefaultTrue(33L)).thenReturn(Optional.empty());

        assertThrows(CabinetNotFoundException.class, () -> cabinetManagementService.getDefaultCabinet(user));
    }

    // Verifies generated invite codes use the configured expiration duration.
    @Test
    void generateInviteCode_setsExpirationUsingConfiguredDuration() {
        Cabinet cabinet = new Cabinet("shared", false);
        ReflectionTestUtils.setField(cabinet, "id", 303L);
        Instant before = Instant.now();

        when(inviteCodeRepository.findByCode(any(String.class))).thenReturn(Optional.empty());
        when(inviteCodeRepository.save(any(InviteCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String code = cabinetManagementService.generateInviteCode(cabinet);

        ArgumentCaptor<InviteCode> inviteCaptor = ArgumentCaptor.forClass(InviteCode.class);
        verify(inviteCodeRepository).save(inviteCaptor.capture());

        InviteCode savedInvite = inviteCaptor.getValue();
        assertEquals(code, savedInvite.getCode());
        assertEquals(cabinet, savedInvite.getCabinet());
        assertNotNull(savedInvite.getCreatedAt());
        assertNotNull(savedInvite.getExpiresAt());
        assertFalse(savedInvite.getExpiresAt().isBefore(before.plusMillis(3_600_000L)));
        assertFalse(savedInvite.getExpiresAt().isAfter(before.plusMillis(3_600_000L).plusSeconds(2)));
    }

    // Verifies expired invite codes are rejected and do not create memberships.
    @Test
    void joinCabinet_expiredCode_throwsInvalidCodeException() {
        User user = new User("dave", "hash");
        user.setId(44L);
        Cabinet cabinet = new Cabinet("team", false);
        ReflectionTestUtils.setField(cabinet, "id", 404L);
        InviteCode inviteCode = new InviteCode(cabinet, "ABCDE", 3_600_000L);
        ReflectionTestUtils.setField(inviteCode, "expiresAt", Instant.now().minusSeconds(5));

        when(inviteCodeRepository.findByCode("ABCDE")).thenReturn(Optional.of(inviteCode));

        assertThrows(InvalidCodeException.class, () -> cabinetManagementService.joinCabinet(user, "ABCDE"));

        verify(cabinetMemberRepository, never()).save(any(CabinetMember.class));
        verify(inviteCodeRepository, never()).save(inviteCode);
    }

    // Verifies codes already marked used are rejected and do not create memberships.
    @Test
    void joinCabinet_usedCode_throwsInvalidCodeException() {
        User user = new User("erin", "hash");
        user.setId(55L);
        Cabinet cabinet = new Cabinet("team", false);
        ReflectionTestUtils.setField(cabinet, "id", 505L);
        InviteCode inviteCode = new InviteCode(cabinet, "FGHIJ", 3_600_000L);
        inviteCode.markUsed();

        when(inviteCodeRepository.findByCode("FGHIJ")).thenReturn(Optional.of(inviteCode));

        assertThrows(InvalidCodeException.class, () -> cabinetManagementService.joinCabinet(user, "FGHIJ"));

        verify(cabinetMemberRepository, never()).save(any(CabinetMember.class));
        verify(inviteCodeRepository, never()).save(inviteCode);
    }

    // Verifies a valid invite code adds the user to the cabinet and marks the code used.
    @Test
    void joinCabinet_validCode_addsMemberAndMarksInviteUsed() {
        User user = new User("frank", "hash");
        user.setId(66L);
        Cabinet cabinet = new Cabinet("project", false);
        ReflectionTestUtils.setField(cabinet, "id", 606L);
        InviteCode inviteCode = new InviteCode(cabinet, "KLMNO", 3_600_000L);

        when(inviteCodeRepository.findByCode("KLMNO")).thenReturn(Optional.of(inviteCode));
        when(cabinetMemberRepository.save(any(CabinetMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inviteCodeRepository.save(any(InviteCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cabinet result = cabinetManagementService.joinCabinet(user, "KLMNO");

        assertEquals(cabinet, result);
        ArgumentCaptor<CabinetMember> memberCaptor = ArgumentCaptor.forClass(CabinetMember.class);
        verify(cabinetMemberRepository).save(memberCaptor.capture());
        assertEquals(cabinet, memberCaptor.getValue().getCabinet());
        assertEquals(user, memberCaptor.getValue().getUser());
        verify(inviteCodeRepository).save(inviteCode);
        assertTrue(inviteCode.isUsed());
    }
}


