package dn.questenginev2.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.common.exceptions.*;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamFilterRequest;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
}
