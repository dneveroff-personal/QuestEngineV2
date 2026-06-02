package dn.questenginev2.team.service;

import dn.questenginev2.common.exceptions.*;
import dn.questenginev2.team.dto.*;
import dn.questenginev2.team.entity.*;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamJoinRequestRepository joinRequestRepository;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TeamServiceImpl teamService;

    private User testUser;
    private CreateTeamRequest createTeamRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPublicName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");

        createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("Test Team");
    }

    @Test
    void createTeam_createsTeam_whenNameIsUnique() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.existsByName("Test Team")).thenReturn(false);
        when(teamMemberRepository.existsByUser(testUser)).thenReturn(false);
        
        Team savedTeam = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        // Act
        TeamResponse response = teamService.createTeam(createTeamRequest, authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Team");
        assertThat(response.getCaptainName()).isEqualTo("Test User");
        assertThat(response.getCreatedAt()).isNotNull();

        verify(teamRepository).existsByName("Test Team");
        verify(teamRepository).save(any(Team.class));
        verify(userService).findByUsername("testuser");
    }

    @Test
    void createTeam_throwsTeamAlreadyExistsException_whenNameAlreadyExists() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.existsByName("Test Team")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(createTeamRequest, authentication))
                .isInstanceOf(TeamAlreadyExistsException.class)
                .hasMessage("Team with name Test Team already exists");

        verify(teamRepository).existsByName("Test Team");
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_throwsRuntimeException_whenUserNotFound() {
        // Arrange
        when(authentication.getName()).thenReturn("nonexistent");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(createTeamRequest, authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пользователь не найден: nonexistent");

        verify(teamRepository, never()).existsByName(anyString());
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createJoinRequest_createsRequest_whenUserNotInTeamAndNoExistingRequest() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamMemberRepository.existsByUser(testUser)).thenReturn(false);
        when(joinRequestRepository.existsByTeamAndUserAndType(any(Team.class), eq(testUser), eq(JoinRequestType.JOIN_REQUEST))).thenReturn(false);

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        // Act
        Boolean result = teamService.createJoinRequest(authentication, 1L, null);

        // Assert
        assertThat(result).isTrue();
        verify(joinRequestRepository).save(any(TeamJoinRequest.class));
    }

    @Test
    void createJoinRequest_throwsUserAlreadyInTeamException_whenUserAlreadyInTeam() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamMemberRepository.existsByUser(testUser)).thenReturn(true);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).build()));

        // Act & Assert
        assertThatThrownBy(() -> teamService.createJoinRequest(authentication, 1L, null))
                .isInstanceOf(UserAlreadyInTeamException.class)
                .hasMessage("User already member of a team");

        verify(joinRequestRepository, never()).save(any(TeamJoinRequest.class));
    }

    @Test
    void getJoinRequests_returnsRequests_whenUserIsCaptain() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.findByCaptain(testUser)).thenReturn(Optional.of(team));

        User requester = new User();
        requester.setId(2L);
        requester.setUsername("requester");
        requester.setPublicName("Requester User");

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(1L)
                .user(requester)
                .team(team)
                .type(JoinRequestType.JOIN_REQUEST)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findByTeamAndType(team, JoinRequestType.JOIN_REQUEST)).thenReturn(Collections.singletonList(joinRequest));

        // Act
        List<TeamJoinResponse> response = teamService.getJoinRequests(authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserName()).isEqualTo("Requester User");
    }

    @Test
    void getJoinRequests_returnsEmptyList_whenUserIsNotCaptainAndHasNoInvites() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.findByCaptain(testUser)).thenReturn(Optional.empty());
        when(joinRequestRepository.findByUserAndType(testUser, JoinRequestType.CAPTAIN_INVITE)).thenReturn(Collections.emptyList());

        // Act
        List<TeamJoinResponse> response = teamService.getJoinRequests(authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    void rejectRequest_rejectsRequest_whenUserIsCaptain() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();

        User requester = new User();
        requester.setId(2L);
        requester.setUsername("requester");
        requester.setPublicName("Requester User");

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(1L)
                .user(requester)
                .team(team)
                .type(JoinRequestType.JOIN_REQUEST)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(joinRequest));

        // Act
        Boolean result = teamService.rejectRequest(1L, authentication);

        // Assert
        assertThat(result).isTrue();
        verify(joinRequestRepository).delete(joinRequest);
    }

    @Test
    void rejectRequest_throwsRuntimeException_whenUserIsNotCaptain() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setPublicName("Other User");

        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(otherUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(1L)
                .user(testUser)
                .team(team)
                .type(JoinRequestType.JOIN_REQUEST)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(joinRequest));

        // Act & Assert
        assertThatThrownBy(() -> teamService.rejectRequest(1L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only captain can reject");

        verify(joinRequestRepository, never()).delete(any(TeamJoinRequest.class));
    }

    @Test
    void rejectRequest_throwsRequestNotFoundException_whenRequestNotFound() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(joinRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> teamService.rejectRequest(999L, authentication))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessage("Request not found");

        verify(joinRequestRepository, never()).delete(any(TeamJoinRequest.class));
    }

    @Test
    void createJoinRequest_createsInvite_whenCaptainInvitesUser() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        User invitedUser = new User();
        invitedUser.setId(2L);
        invitedUser.setUsername("inviteduser");
        invitedUser.setPublicName("Invited User");
        when(userService.findByUsername("inviteduser")).thenReturn(Optional.of(invitedUser));

        when(teamMemberRepository.existsByUser(invitedUser)).thenReturn(false);
        when(joinRequestRepository.existsByTeamAndUserAndType(team, invitedUser, JoinRequestType.CAPTAIN_INVITE)).thenReturn(false);

        // Act
        Boolean result = teamService.createJoinRequest(authentication, 1L, "inviteduser");

        // Assert
        assertThat(result).isTrue();
        verify(joinRequestRepository).save(any(TeamJoinRequest.class));
    }

    @Test
    void createJoinRequest_throwsAccessDeniedException_whenNonCaptainTriesToInvite() {
        // Arrange
        User captainUser = new User();
        captainUser.setId(2L);
        captainUser.setUsername("captain");
        captainUser.setPublicName("Captain User");

        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(captainUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        // Act & Assert
        assertThatThrownBy(() -> teamService.createJoinRequest(authentication, 1L, "inviteduser"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only captain can invite users");

        verify(joinRequestRepository, never()).save(any(TeamJoinRequest.class));
    }

    @Test
    void createJoinRequest_throwsUserNotFoundException_whenInvitedUserNotFound() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> teamService.createJoinRequest(authentication, 1L, "nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Приглашаемый пользователь не найден: nonexistent");

        verify(joinRequestRepository, never()).save(any(TeamJoinRequest.class));
    }

    @Test
    void getJoinRequests_returnsInvites_whenUserIsNotCaptain() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.findByCaptain(testUser)).thenReturn(Optional.empty());

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest invite = TeamJoinRequest.builder()
                .id(1L)
                .team(team)
                .user(testUser)
                .type(JoinRequestType.CAPTAIN_INVITE)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findByUserAndType(testUser, JoinRequestType.CAPTAIN_INVITE)).thenReturn(Collections.singletonList(invite));

        // Act
        List<TeamJoinResponse> response = teamService.getJoinRequests(authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getType()).isEqualTo(JoinRequestType.CAPTAIN_INVITE);
    }

    @Test
    void approveRequest_approvesJoinRequest_whenUserIsCaptain() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();

        User requester = new User();
        requester.setId(2L);
        requester.setUsername("requester");
        requester.setPublicName("Requester User");

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(1L)
                .user(requester)
                .team(team)
                .type(JoinRequestType.JOIN_REQUEST)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(joinRequest));

        // Act
        Boolean result = teamService.approveRequest(1L, authentication);

        // Assert
        assertThat(result).isTrue();
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(joinRequestRepository).delete(joinRequest);
    }

    @Test
    void approveRequest_approvesInvite_whenUserIsInvited() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest invite = TeamJoinRequest.builder()
                .id(1L)
                .team(team)
                .user(testUser)
                .type(JoinRequestType.CAPTAIN_INVITE)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(invite));

        // Act
        Boolean result = teamService.approveRequest(1L, authentication);

        // Assert
        assertThat(result).isTrue();
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(joinRequestRepository).delete(invite);
    }

    @Test
    void approveRequest_throwsAccessDeniedException_whenNonCaptainApprovesJoinRequest() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setPublicName("Other User");

        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(otherUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest joinRequest = TeamJoinRequest.builder()
                .id(1L)
                .user(testUser)
                .team(team)
                .type(JoinRequestType.JOIN_REQUEST)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(joinRequest));

        // Act & Assert
        assertThatThrownBy(() -> teamService.approveRequest(1L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only captain can approve");

        verify(teamMemberRepository, never()).save(any(TeamMember.class));
        verify(joinRequestRepository, never()).delete(any(TeamJoinRequest.class));
    }

    @Test
    void approveRequest_throwsAccessDeniedException_whenNonInvitedUserApprovesInvite() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setPublicName("Other User");

        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(otherUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest invite = TeamJoinRequest.builder()
                .id(1L)
                .team(team)
                .user(otherUser)
                .type(JoinRequestType.CAPTAIN_INVITE)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> teamService.approveRequest(1L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only invited user can accept");

        verify(teamMemberRepository, never()).save(any(TeamMember.class));
        verify(joinRequestRepository, never()).delete(any(TeamJoinRequest.class));
    }

    @Test
    void rejectRequest_rejectsInvite_whenUserIsInvited() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest invite = TeamJoinRequest.builder()
                .id(1L)
                .team(team)
                .user(testUser)
                .type(JoinRequestType.CAPTAIN_INVITE)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(invite));

        // Act
        Boolean result = teamService.rejectRequest(1L, authentication);

        // Assert
        assertThat(result).isTrue();
        verify(joinRequestRepository).delete(invite);
    }

    @Test
    void rejectRequest_throwsAccessDeniedException_whenNonInvitedUserRejectsInvite() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setPublicName("Other User");

        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Team team = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(otherUser)
                .createdAt(Instant.now())
                .build();

        TeamJoinRequest invite = TeamJoinRequest.builder()
                .id(1L)
                .team(team)
                .user(otherUser)
                .type(JoinRequestType.CAPTAIN_INVITE)
                .createdAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(invite));

        // Act & Assert
        assertThatThrownBy(() -> teamService.rejectRequest(1L, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only invited user can reject");

        verify(joinRequestRepository, never()).delete(any(TeamJoinRequest.class));
    }
}
