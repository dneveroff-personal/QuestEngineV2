package dn.questenginev2.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.TeamNotFoundException;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.entity.*;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestProgressServiceImplTest {

  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private QuestRepository questRepository;
  @Mock private QuestRegistrationRepository questRegistrationRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private QuestAuthorRepository questAuthorRepository;
  @Mock private UserService userService;
  @Mock private Authentication authentication;

  @InjectMocks private QuestProgressServiceImpl questProgressService;

  private User authorUser;
  private User adminUser;
  private User playerUser;
  private Quest runningQuest;
  private Quest draftQuest;
  private Quest finishedQuest;
  private Team team1;
  private Team team2;
  private TeamMember teamMember;
  private QuestRegistration approvedRegistration;
  private QuestRegistration pendingRegistration;
  private QuestRegistration rejectedRegistration;

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

    Instant questStartTime = Instant.parse("2024-01-01T10:00:00Z");

    runningQuest =
        Quest.builder()
            .id(1L)
            .title("Running Quest")
            .description("Test Description")
            .status(QuestStatus.RUNNING)
            .maximumTeams(100)
            .startTime(questStartTime)
            .createdAt(Instant.now())
            .build();

    draftQuest =
        Quest.builder()
            .id(2L)
            .title("Draft Quest")
            .description("Test Description")
            .status(QuestStatus.DRAFT)
            .maximumTeams(100)
            .createdAt(Instant.now())
            .build();

    finishedQuest =
        Quest.builder()
            .id(3L)
            .title("Finished Quest")
            .description("Test Description")
            .status(QuestStatus.FINISHED)
            .maximumTeams(100)
            .createdAt(Instant.now())
            .build();

    team1 = new Team();
    team1.setId(1L);
    team1.setName("Team 1");

    team2 = new Team();
    team2.setId(2L);
    team2.setName("Team 2");

    teamMember = new TeamMember();
    teamMember.setId(1L);
    teamMember.setUser(playerUser);
    teamMember.setTeam(team1);
    teamMember.setRole(TeamRole.CAPTAIN);

    approvedRegistration =
        QuestRegistration.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(RegistrationStatus.APPROVED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    pendingRegistration =
        QuestRegistration.builder()
            .id(2L)
            .quest(runningQuest)
            .team(team1)
            .status(RegistrationStatus.PENDING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    rejectedRegistration =
        QuestRegistration.builder()
            .id(3L)
            .quest(runningQuest)
            .team(team1)
            .status(RegistrationStatus.REJECTED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  // ────── CREATE PROGRESS ────────────────────────────────────────────────────────

  @Test
  void createProgress_returnsProgress_whenRegistrationApproved() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration));
    when(questProgressRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(false);

    QuestProgress savedProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(savedProgress);

    QuestProgressResponse response = questProgressService.createProgress(1L, 1L);

    assertThat(response).isNotNull();
    assertThat(response.getTeamName()).isEqualTo("Team 1");
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.WAITING);
    assertThat(response.getStartedAt()).isEqualTo(runningQuest.getStartTime());

    verify(questProgressRepository).save(any(QuestProgress.class));
  }

  @Test
  void createProgress_throwsForbiddenOperationException_whenRegistrationPending() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.createProgress(1L, 1L))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("не подтверждена");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void createProgress_throwsForbiddenOperationException_whenRegistrationRejected() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.createProgress(1L, 1L))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("не подтверждена");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void createProgress_throwsIllegalArgumentException_whenDuplicateProgress() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration));
    when(questProgressRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(true);

    assertThatThrownBy(() -> questProgressService.createProgress(1L, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("уже существует");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void createProgress_throwsForbiddenOperationException_whenQuestNotRunning() {
    when(questRepository.findById(2L)).thenReturn(Optional.of(draftQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));

    assertThatThrownBy(() -> questProgressService.createProgress(2L, 1L))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("RUNNING");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void createProgress_throwsIllegalArgumentException_whenQuestNotFound() {
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.createProgress(999L, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void createProgress_throwsTeamNotFoundException_whenTeamNotFound() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.createProgress(1L, 999L))
        .isInstanceOf(TeamNotFoundException.class)
        .hasMessageContaining("Команда не найдена");

    verify(questProgressRepository, never()).save(any());
  }

  // ────── ENTER QUEST ───────────────────────────────────────────────────────────

  @Test
  void enterQuest_changesStatusToRunning_whenProgressIsWaiting() {
    QuestProgress waitingProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(teamMemberRepository.findByUser(playerUser)).thenReturn(Optional.of(teamMember));
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(waitingProgress));

    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(runningProgress);

    QuestProgressResponse response = questProgressService.enterQuest(1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.RUNNING);
    assertThat(response.getStartedAt()).isEqualTo(runningQuest.getStartTime());

    verify(questProgressRepository).save(any(QuestProgress.class));
  }

  @Test
  void enterQuest_throwsForbiddenOperationException_whenProgressNotWaiting() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(teamMemberRepository.findByUser(playerUser)).thenReturn(Optional.of(teamMember));
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(runningProgress));

    assertThatThrownBy(() -> questProgressService.enterQuest(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("WAITING");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void enterQuest_throwsIllegalArgumentException_whenProgressNotFound() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(teamMemberRepository.findByUser(playerUser)).thenReturn(Optional.of(teamMember));
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.enterQuest(1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Прогресс не найден");

    verify(questProgressRepository, never()).save(any());
  }

  // ────── GET PROGRESS ──────────────────────────────────────────────────────────

  @Test
  void getProgress_returnsProgress_whenExists() {
    QuestProgress progress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L)).thenReturn(Optional.of(progress));

    QuestProgressResponse response = questProgressService.getProgress(1L, 1L);

    assertThat(response).isNotNull();
    assertThat(response.getTeamName()).isEqualTo("Team 1");
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.WAITING);
  }

  @Test
  void getProgress_throwsIllegalArgumentException_whenNotFound() {
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.getProgress(1L, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Прогресс не найден");
  }

  // ────── GET ALL BY QUEST ──────────────────────────────────────────────────────

  @Test
  void getAllByQuest_returnsProgresses_whenQuestExists() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));

    QuestProgress progress1 =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    QuestProgress progress2 =
        QuestProgress.builder()
            .id(2L)
            .quest(runningQuest)
            .team(team2)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(questProgressRepository.findByQuestId(1L)).thenReturn(List.of(progress1, progress2));

    List<QuestProgressResponse> responses = questProgressService.getAllByQuest(1L);

    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).getTeamName()).isEqualTo("Team 1");
    assertThat(responses.get(1).getTeamName()).isEqualTo("Team 2");
  }

  @Test
  void getAllByQuest_throwsIllegalArgumentException_whenQuestNotFound() {
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.getAllByQuest(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");
  }

  // ────── FINISH PROGRESS ───────────────────────────────────────────────────────

  @Test
  void finishProgress_changesStatusToFinished_whenRunning() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, QuestProgressStatus.RUNNING))
        .thenReturn(Optional.of(runningProgress));

    QuestProgress finishedProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.FINISHED)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(finishedProgress);

    QuestProgressResponse response = questProgressService.finishProgress(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.FINISHED);
    assertThat(response.getFinishedAt()).isNotNull();

    verify(questProgressRepository).save(any(QuestProgress.class));
  }

  @Test
  void finishProgress_throwsIllegalArgumentException_whenProgressNotFound() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, QuestProgressStatus.RUNNING))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.finishProgress(1L, 1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Прогресс не найден");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void finishProgress_throwsForbiddenOperationException_whenNotAuthor() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 3L)).thenReturn(false);
    when(questProgressRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, QuestProgressStatus.RUNNING))
        .thenReturn(Optional.of(runningProgress));

    assertThatThrownBy(() -> questProgressService.finishProgress(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Автору квеста");

    verify(questProgressRepository, never()).save(any());
  }

  // ────── SET DNF ───────────────────────────────────────────────────────────────

  @Test
  void setDnf_changesStatusToDnf_whenRunning() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(runningProgress));

    QuestProgress dnfProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.DNF)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(dnfProgress);

    QuestProgressResponse response = questProgressService.setDnf(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.DNF);
    assertThat(response.getFinishedAt()).isNotNull();

    verify(questProgressRepository).save(any(QuestProgress.class));
  }

  @Test
  void setDnf_throwsForbiddenOperationException_whenFinished() {
    QuestProgress finishedProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.FINISHED)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(finishedProgress));

    assertThatThrownBy(() -> questProgressService.setDnf(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("завершённый прогресс");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void setDnf_throwsForbiddenOperationException_whenAlreadyDnf() {
    QuestProgress dnfProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.DNF)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(dnfProgress));

    assertThatThrownBy(() -> questProgressService.setDnf(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("завершённый прогресс");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void setDnf_throwsForbiddenOperationException_whenNotAuthor() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 3L)).thenReturn(false);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(runningProgress));

    assertThatThrownBy(() -> questProgressService.setDnf(1L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Автору квеста");

    verify(questProgressRepository, never()).save(any());
  }

  @Test
  void setDnf_throwsIllegalArgumentException_whenProgressNotFound() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questProgressService.setDnf(1L, 1L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Прогресс не найден");

    verify(questProgressRepository, never()).save(any());
  }

  // ────── STARTED AT ────────────────────────────────────────────────────────────

  @Test
  void createProgress_setsStartedAtToQuestStartTime() {
    Instant questStartTime = Instant.parse("2024-01-01T10:00:00Z");
    Quest questWithStartTime =
        Quest.builder()
            .id(1L)
            .title("Quest with start time")
            .description("Test")
            .status(QuestStatus.RUNNING)
            .maximumTeams(100)
            .startTime(questStartTime)
            .createdAt(Instant.now())
            .build();

    when(questRepository.findById(1L)).thenReturn(Optional.of(questWithStartTime));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration));
    when(questProgressRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(false);

    QuestProgress savedProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(questWithStartTime)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(questWithStartTime.getStartTime())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(savedProgress);

    QuestProgressResponse response = questProgressService.createProgress(1L, 1L);

    assertThat(response.getStartedAt()).isEqualTo(questStartTime);
  }

  @Test
  void enterQuest_doesNotChangeStartedAt() {
    Instant questStartTime = Instant.parse("2024-01-01T10:00:00Z");
    Quest questWithStartTime =
        Quest.builder()
            .id(1L)
            .title("Quest with start time")
            .description("Test")
            .status(QuestStatus.RUNNING)
            .maximumTeams(100)
            .startTime(questStartTime)
            .createdAt(Instant.now())
            .build();

    QuestProgress waitingProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(questWithStartTime)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(questStartTime)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    when(teamMemberRepository.findByUser(playerUser)).thenReturn(Optional.of(teamMember));
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(waitingProgress));

    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(questWithStartTime)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(questStartTime)
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(runningProgress);

    QuestProgressResponse response = questProgressService.enterQuest(1L, authentication);

    assertThat(response.getStartedAt()).isEqualTo(questStartTime);
  }

  // ────── ONE QUEST + ONE TEAM = ONE PROGRESS ───────────────────────────────────

  @Test
  void createProgress_throwsIllegalArgumentException_whenProgressAlreadyExists() {
    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration));
    when(questProgressRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(true);

    assertThatThrownBy(() -> questProgressService.createProgress(1L, 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("уже существует");

    verify(questProgressRepository, never()).save(any());
  }

  // ────── ONE TEAM + MULTIPLE QUESTS = MULTIPLE PROGRESSES ─────────────────────

  @Test
  void createProgress_allowsMultipleProgressesForDifferentQuests() {
    Quest quest2 =
        Quest.builder()
            .id(2L)
            .title("Quest 2")
            .description("Test")
            .status(QuestStatus.RUNNING)
            .maximumTeams(100)
            .startTime(Instant.parse("2024-01-01T10:00:00Z"))
            .createdAt(Instant.now())
            .build();

    QuestRegistration approvedRegistration2 =
        QuestRegistration.builder()
            .id(4L)
            .quest(quest2)
            .team(team1)
            .status(RegistrationStatus.APPROVED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(questRepository.findById(2L)).thenReturn(Optional.of(quest2));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team1));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration));
    when(questRegistrationRepository.findByQuestIdAndTeamIdAndStatus(
            2L, 1L, RegistrationStatus.APPROVED))
        .thenReturn(Optional.of(approvedRegistration2));
    when(questProgressRepository.existsByQuestIdAndTeamId(1L, 1L)).thenReturn(false);
    when(questProgressRepository.existsByQuestIdAndTeamId(2L, 1L)).thenReturn(false);

    QuestProgress progress1 =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    QuestProgress progress2 =
        QuestProgress.builder()
            .id(2L)
            .quest(quest2)
            .team(team1)
            .status(QuestProgressStatus.WAITING)
            .startedAt(quest2.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(questProgressRepository.save(any(QuestProgress.class)))
        .thenAnswer(
            invocation -> {
              QuestProgress arg = invocation.getArgument(0);
              if (arg.getQuest().getId().equals(1L)) {
                return progress1;
              } else if (arg.getQuest().getId().equals(2L)) {
                return progress2;
              }
              return null;
            });

    QuestProgressResponse response1 = questProgressService.createProgress(1L, 1L);
    QuestProgressResponse response2 = questProgressService.createProgress(2L, 1L);

    assertThat(response1).isNotNull();
    assertThat(response1.getTeamName()).isEqualTo("Team 1");

    assertThat(response2).isNotNull();
    assertThat(response2.getTeamName()).isEqualTo("Team 1");

    verify(questProgressRepository, times(2)).save(any(QuestProgress.class));
  }

  // ────── ADMIN CAN FINISH/DNF ──────────────────────────────────────────────────

  @Test
  void finishProgress_succeeds_whenUserIsAdmin() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(adminUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 2L)).thenReturn(false);
    when(questProgressRepository.findByQuestIdAndTeamIdAndStatus(
            1L, 1L, QuestProgressStatus.RUNNING))
        .thenReturn(Optional.of(runningProgress));

    QuestProgress finishedProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.FINISHED)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(finishedProgress);

    QuestProgressResponse response = questProgressService.finishProgress(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.FINISHED);

    verify(questProgressRepository).save(any(QuestProgress.class));
  }

  @Test
  void setDnf_succeeds_whenUserIsAdmin() {
    QuestProgress runningProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.RUNNING)
            .startedAt(runningQuest.getStartTime())
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(adminUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 2L)).thenReturn(false);
    when(questProgressRepository.findByQuestIdAndTeamId(1L, 1L))
        .thenReturn(Optional.of(runningProgress));

    QuestProgress dnfProgress =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(team1)
            .status(QuestProgressStatus.DNF)
            .startedAt(runningQuest.getStartTime())
            .finishedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    when(questProgressRepository.save(any(QuestProgress.class))).thenReturn(dnfProgress);

    QuestProgressResponse response = questProgressService.setDnf(1L, 1L, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(QuestProgressStatus.DNF);

    verify(questProgressRepository).save(any(QuestProgress.class));
  }
}
