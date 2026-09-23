package dn.questenginev2.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.*;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private TeamJoinRequestRepository joinRequestRepository;
  @Mock private UserService userService;
  @Mock private Authentication authentication;
  @InjectMocks private TeamServiceImpl teamService;

  private User testUser;
  private CreateTeamRequest createTeamRequest;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setPublicName("Test User");
    testUser.setEmail("test@example.com");
    testUser.setRole(UserRole.PLAYER);
    createTeamRequest = new CreateTeamRequest("Test Team");
  }

  @Test
  void createTeam_createsTeam_whenValidRequest() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    when(teamRepository.existsByName("Test Team")).thenReturn(false);
    when(teamMemberRepository.existsByUser(testUser)).thenReturn(false);

    Team savedTeam =
        Team.builder().id(1L).name("Test Team").captain(testUser).createdAt(Instant.now()).build();
    when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
    when(teamMemberRepository.findAllByTeam(savedTeam)).thenReturn(Collections.emptyList());

    TeamResponse response = teamService.createTeam(createTeamRequest, authentication);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("Test Team");
    assertThat(response.captainUsername()).isEqualTo("testuser");
    assertThat(response.captainDisplayName()).isEqualTo("Test User");
    assertThat(response.createdAt()).isNotNull();

    verify(teamRepository).existsByName("Test Team");
    verify(teamRepository).save(any(Team.class));
    verify(userService).getCurrentUser(authentication);
  }

  @Test
  void createTeam_throwsTeamAlreadyExistsException_whenNameAlreadyExists() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    when(teamRepository.existsByName("Test Team")).thenReturn(true);

    assertThatThrownBy(() -> teamService.createTeam(createTeamRequest, authentication))
        .isInstanceOf(TeamAlreadyExistsException.class);

    verify(teamRepository, never()).save(any());
  }

  @Test
  void renameTeam_updatesName_whenCallerIsCaptain() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    Team team =
        Team.builder().id(1L).name("Old Name").captain(testUser).createdAt(Instant.now()).build();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.existsByName("New Name")).thenReturn(false);
    when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
    when(teamMemberRepository.findAllByTeam(team)).thenReturn(Collections.emptyList());

    TeamResponse response =
        teamService.renameTeam(1L, new CreateTeamRequest("New Name"), authentication);

    assertThat(response.name()).isEqualTo("New Name");
    assertThat(response.id()).isEqualTo(1L);
    verify(teamRepository).save(team);
  }

  @Test
  void renameTeam_isIdempotent_whenNameUnchanged() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    Team team =
        Team.builder().id(1L).name("Same").captain(testUser).createdAt(Instant.now()).build();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findAllByTeam(team)).thenReturn(Collections.emptyList());

    TeamResponse response =
        teamService.renameTeam(1L, new CreateTeamRequest("Same"), authentication);

    assertThat(response.name()).isEqualTo("Same");
    verify(teamRepository, never()).existsByName(any());
    verify(teamRepository, never()).save(any());
  }

  @Test
  void renameTeam_throwsTeamAlreadyExists_whenNameTaken() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    Team team =
        Team.builder().id(1L).name("Old").captain(testUser).createdAt(Instant.now()).build();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.existsByName("Taken")).thenReturn(true);

    assertThatThrownBy(
            () -> teamService.renameTeam(1L, new CreateTeamRequest("Taken"), authentication))
        .isInstanceOf(TeamAlreadyExistsException.class);

    verify(teamRepository, never()).save(any());
  }

  @Test
  void renameTeam_throwsForbidden_whenCallerIsNotCaptain() {
    User other = new User();
    other.setId(99L);
    other.setUsername("other");
    other.setRole(UserRole.PLAYER);
    when(userService.getCurrentUser(authentication)).thenReturn(other);
    Team team =
        Team.builder().id(1L).name("Old").captain(testUser).createdAt(Instant.now()).build();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(
            () -> teamService.renameTeam(1L, new CreateTeamRequest("New"), authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("капитан");

    verify(teamRepository, never()).save(any());
  }

  @Test
  void renameTeam_throwsNotFound_whenTeamMissing() {
    when(userService.getCurrentUser(authentication)).thenReturn(testUser);
    when(teamRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> teamService.renameTeam(999L, new CreateTeamRequest("X"), authentication))
        .isInstanceOf(TeamNotFoundException.class);
  }
}
