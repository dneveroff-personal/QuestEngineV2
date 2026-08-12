package dn.questenginev2.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.dto.QuestRegisterResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.RegistrationStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class QuestRegistrationServiceImplTest {

  @Mock private QuestRegistrationRepository questRegistrationRepository;
  @Mock private QuestRepository questRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private QuestAuthorRepository questAuthorRepository;
  @Mock private UserService userService;
  @Mock private Authentication authentication;

  @InjectMocks private QuestRegistrationServiceImpl questRegistrationService;

  private User authorUser;
  private User adminUser;
  private User playerUser;
  private User captainUser;
  private Quest quest;
  private Team team;
  private TeamMember teamMember;

  @BeforeEach
  void setUp() {
    authorUser = new User();
    authorUser.setId(1L);
    authorUser.setUsername("author");
    authorUser.setRole(UserRole.AUTHOR);

    adminUser = new User();
    adminUser.setId(2L);
    adminUser.setUsername("admin");
    adminUser.setRole(UserRole.ADMIN);

    playerUser = new User();
    playerUser.setId(3L);
    playerUser.setUsername("player");
    playerUser.setRole(UserRole.PLAYER);

    captainUser = new User();
    captainUser.setId(4L);
    captainUser.setUsername("captain");
    captainUser.setRole(UserRole.PLAYER);

    quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(dn.questenginev2.quest.entity.QuestType.TEAM)
            .status(QuestStatus.REGISTRATION)
            .maximumTeams(100)
            .createdAt(Instant.now())
            .build();

    team = new Team();
    team.setId(1L);
    team.setName("Test Team");

    teamMember = new TeamMember();
    teamMember.setId(1L);
    teamMember.setUser(captainUser);
    teamMember.setTeam(team);
    teamMember.setRole(TeamRole.CAPTAIN);
  }

  @Test
  void registerTeam_returnsRegistration_whenUserIsCaptain() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByUserAndTeam(captainUser, team))
        .thenReturn(Optional.of(teamMember));
    when(questRegistrationRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(false);

    QuestRegistration savedRegistration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.save(any(QuestRegistration.class)))
        .thenReturn(savedRegistration);

    QuestRegisterResponse response = questRegistrationService.registerTeam(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getQuestId()).isEqualTo(1L);
    assertThat(response.getTeamId()).isEqualTo(1L);
    assertThat(response.getTeamName()).isEqualTo("Test Team");
    assertThat(response.getStatus()).isEqualTo(RegistrationStatus.PENDING);

    verify(questRegistrationRepository).save(any(QuestRegistration.class));
  }

  @Test
  void registerTeam_throwsForbiddenOperationException_whenUserIsNotCaptain() {
    User memberUser = new User();
    memberUser.setId(5L);
    memberUser.setUsername("member");
    memberUser.setRole(UserRole.PLAYER);

    TeamMember memberTeamMember = new TeamMember();
    memberTeamMember.setUser(memberUser);
    memberTeamMember.setTeam(team);
    memberTeamMember.setRole(TeamRole.MEMBER);

    when(userService.getCurrentUser(authentication)).thenReturn(memberUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByUserAndTeam(memberUser, team))
        .thenReturn(Optional.of(memberTeamMember));

    assertThatThrownBy(() -> questRegistrationService.registerTeam(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("капитан");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void registerTeam_throwsIllegalArgumentException_whenQuestNotFound() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questRegistrationService.registerTeam(999L, 1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void registerTeam_throwsForbiddenOperationException_whenQuestIsFinished() {
    Quest finishedQuest =
        Quest.builder()
            .id(1L)
            .title("Finished Quest")
            .status(QuestStatus.FINISHED)
            .maximumTeams(100)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(finishedQuest));

    assertThatThrownBy(() -> questRegistrationService.registerTeam(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("завершённый квест");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void registerTeam_throwsIllegalArgumentException_whenTeamNotFound() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questRegistrationService.registerTeam(1L, 999L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Команда не найдена");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void registerTeam_throwsIllegalArgumentException_whenDuplicateRegistration() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamMemberRepository.findByUserAndTeam(captainUser, team))
        .thenReturn(Optional.of(teamMember));
    when(questRegistrationRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(true);

    assertThatThrownBy(() -> questRegistrationService.registerTeam(1L, 1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("уже подала заявку");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void findAll_returnsRegistrations_whenQuestExists() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByQuestId(1L))
        .thenReturn(Collections.singletonList(registration));

    List<QuestRegisterResponse> responses = questRegistrationService.findAll(1L);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getQuestId()).isEqualTo(1L);
    assertThat(responses.get(0).getTeamId()).isEqualTo(1L);
    assertThat(responses.get(0).getTeamName()).isEqualTo("Test Team");
  }

  @Test
  void unregisterTeam_returnsRegistration_whenPending() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(teamMemberRepository.findByUser(captainUser)).thenReturn(Optional.of(teamMember));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(registration));

    QuestRegisterResponse response = questRegistrationService.unregisterTeam(1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RegistrationStatus.PENDING);

    verify(questRegistrationRepository).delete(registration);
  }

  @Test
  void unregisterTeam_throwsForbiddenOperationException_whenNotPending() {
    when(userService.getCurrentUser(authentication)).thenReturn(captainUser);
    when(teamMemberRepository.findByUser(captainUser)).thenReturn(Optional.of(teamMember));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.APPROVED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(() -> questRegistrationService.unregisterTeam(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("PENDING");

    verify(questRegistrationRepository, never()).delete(any());
  }

  @Test
  void approveTeam_returnsApprovedRegistration_whenAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(registration));
    when(questRegistrationRepository.countByQuestIdAndStatus(1L, RegistrationStatus.APPROVED))
        .thenReturn(0L);

    QuestRegistration approvedRegistration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.APPROVED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.save(any(QuestRegistration.class)))
        .thenReturn(approvedRegistration);

    QuestRegisterResponse response = questRegistrationService.approveTeam(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RegistrationStatus.APPROVED);

    verify(questRegistrationRepository).save(any(QuestRegistration.class));
  }

  @Test
  void approveTeam_throwsForbiddenOperationException_whenNotAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 3L)).thenReturn(false);

    assertThatThrownBy(() -> questRegistrationService.approveTeam(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Автор квеста");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void approveTeam_throwsForbiddenOperationException_whenLimitReached() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(registration));
    when(questRegistrationRepository.countByQuestIdAndStatus(1L, RegistrationStatus.APPROVED))
        .thenReturn(100L);

    assertThatThrownBy(() -> questRegistrationService.approveTeam(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("лимит команд");

    verify(questRegistrationRepository, never()).save(any());
  }

  @Test
  void rejectTeam_returnsRejectedRegistration_whenAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    QuestRegistration registration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.findByTeamIdAndStatus(1L, RegistrationStatus.PENDING))
        .thenReturn(Collections.singletonList(registration));

    QuestRegistration rejectedRegistration =
        QuestRegistration.builder()
            .id(1L)
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.REJECTED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(questRegistrationRepository.save(any(QuestRegistration.class)))
        .thenReturn(rejectedRegistration);

    QuestRegisterResponse response = questRegistrationService.rejectTeam(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RegistrationStatus.REJECTED);

    verify(questRegistrationRepository).save(any(QuestRegistration.class));
  }

  @Test
  void rejectTeam_throwsIllegalArgumentException_whenNoPendingRegistration() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(questRegistrationRepository.findByTeamIdAndStatus(1L, RegistrationStatus.PENDING))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> questRegistrationService.rejectTeam(1L, 1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Активная заявка не найдена");

    verify(questRegistrationRepository, never()).save(any());
  }
}
