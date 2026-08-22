package dn.questenginev2.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
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
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class QuestServiceImplTest {

  @Mock private QuestAuthorRepository questAuthorRepository;

  @Mock private QuestRepository questRepository;

  @Mock private QuestProgressRepository questProgressRepository;

  @Mock private LevelRepository levelRepository;

  @Mock private CodeRepository codeRepository;

  @Mock private UserService userService;

  @Mock private Authentication authentication;

  @InjectMocks private QuestServiceImpl questService;

  private User authorUser;
  private User adminUser;
  private User playerUser;

  @BeforeEach
  void setUp() {
    authorUser = new User();
    authorUser.setId(1L);
    authorUser.setUsername("author");
    authorUser.setPublicName("Author User");
    authorUser.setEmail("author@example.com");
    authorUser.setPasswordHash("hashedPassword");
    authorUser.setRole(UserRole.AUTHOR);
    authorUser.setCreatedAt(Instant.now());

    adminUser = new User();
    adminUser.setId(2L);
    adminUser.setUsername("admin");
    adminUser.setPublicName("Admin User");
    adminUser.setEmail("admin@example.com");
    adminUser.setPasswordHash("hashedAdminPassword");
    adminUser.setRole(UserRole.ADMIN);
    adminUser.setCreatedAt(Instant.now());

    playerUser = new User();
    playerUser.setId(3L);
    playerUser.setUsername("player");
    playerUser.setPublicName("Player User");
    playerUser.setEmail("player@example.com");
    playerUser.setPasswordHash("hashedPlayerPassword");
    playerUser.setRole(UserRole.PLAYER);
    playerUser.setCreatedAt(Instant.now());
  }

  @Test
  void createQuest_createsQuest_whenUserIsAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);

    CreateQuestRequest request =
        new CreateQuestRequest("Test Quest", "Test Description", QuestType.TEAM, null, null);

    Quest savedQuest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(questRepository.save(any(Quest.class))).thenReturn(savedQuest);

    QuestAuthor savedAuthor =
        QuestAuthor.builder()
            .id(1L)
            .quest(savedQuest)
            .user(authorUser)
            .createdAt(Instant.now())
            .build();
    when(questAuthorRepository.save(any(QuestAuthor.class))).thenReturn(savedAuthor);

    QuestResponse response = questService.createQuest(request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("Test Quest");
    assertThat(response.getDescription()).isEqualTo("Test Description");
    assertThat(response.getType()).isEqualTo(QuestType.TEAM);
    assertThat(response.getStatus()).isEqualTo(QuestStatus.DRAFT);

    verify(questRepository).save(any(Quest.class));
    verify(questAuthorRepository).save(any(QuestAuthor.class));
  }

  @Test
  void createQuest_createsQuest_whenUserIsAdmin() {
    when(userService.getCurrentUser(authentication)).thenReturn(adminUser);

    CreateQuestRequest request =
        new CreateQuestRequest("Admin Quest", "Admin Description", QuestType.SINGLE, null, null);

    Quest savedQuest =
        Quest.builder()
            .id(1L)
            .title("Admin Quest")
            .description("Admin Description")
            .type(QuestType.SINGLE)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(questRepository.save(any(Quest.class))).thenReturn(savedQuest);

    QuestAuthor savedAuthor =
        QuestAuthor.builder()
            .id(1L)
            .quest(savedQuest)
            .user(adminUser)
            .createdAt(Instant.now())
            .build();
    when(questAuthorRepository.save(any(QuestAuthor.class))).thenReturn(savedAuthor);

    QuestResponse response = questService.createQuest(request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("Admin Quest");
    verify(questRepository).save(any(Quest.class));
    verify(questAuthorRepository).save(any(QuestAuthor.class));
  }

  @Test
  void createQuest_throwsForbiddenOperationException_whenUserIsPlayer() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);

    CreateQuestRequest request =
        new CreateQuestRequest("Player Quest", "Player Description", null, null, null);

    assertThatThrownBy(() -> questService.createQuest(request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("AUTHOR или ADMIN");

    verify(questRepository, never()).save(any());
    verify(questAuthorRepository, never()).save(any());
  }

  @Test
  void getQuestById_returnsQuest_whenQuestExists() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.REGISTRATION)
            .createdAt(Instant.now())
            .build();
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));

    QuestResponse response = questService.getQuestById(1L);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("Test Quest");
    assertThat(response.getStatus()).isEqualTo(QuestStatus.REGISTRATION);
    verify(questRepository).findById(1L);
  }

  @Test
  void getQuestById_throwsIllegalArgumentException_whenQuestDoesNotExist() {
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questService.getQuestById(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(questRepository).findById(999L);
  }

  @Test
  void updateQuest_updatesQuest_whenUserIsAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questAuthorRepository.existsByQuestIdAndUserId(any(Long.class), eq(authorUser.getId())))
        .thenReturn(true);

    Quest existingQuest =
        Quest.builder()
            .id(1L)
            .title("Old Title")
            .description("Old Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(questRepository.findById(1L)).thenReturn(Optional.of(existingQuest));

    Quest updatedQuest =
        Quest.builder()
            .id(1L)
            .title("New Title")
            .description("New Description")
            .type(QuestType.SINGLE)
            .status(QuestStatus.DRAFT)
            .createdAt(existingQuest.getCreatedAt())
            .build();
    when(questRepository.save(any(Quest.class))).thenReturn(updatedQuest);

    CreateQuestRequest request =
        new CreateQuestRequest("New Title", "New Description", QuestType.SINGLE, null, null);

    QuestResponse response = questService.updateQuest(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("New Title");
    assertThat(response.getDescription()).isEqualTo("New Description");
    assertThat(response.getType()).isEqualTo(QuestType.SINGLE);
    verify(questRepository).save(any(Quest.class));
  }

  @Test
  void updateQuest_throwsForbiddenOperationException_whenUserIsPlayer() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);

    Quest existingQuest =
        Quest.builder()
            .id(1L)
            .title("Old Title")
            .description("Old Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(questRepository.findById(1L)).thenReturn(Optional.of(existingQuest));
    when(questAuthorRepository.existsByQuestIdAndUserId(any(Long.class), eq(playerUser.getId())))
        .thenReturn(false);

    CreateQuestRequest request = new CreateQuestRequest("New Title", null, null, null, null);

    assertThatThrownBy(() -> questService.updateQuest(1L, request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Авторы");

    verify(questRepository).findById(1L);
    verify(questRepository, never()).save(any());
  }

  @Test
  void delete_deletesQuest_whenUserIsAuthor() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, 1L)).thenReturn(true);

    questService.delete(1L, authentication);

    verify(userService).getCurrentUser(authentication);
    verify(questAuthorRepository).existsByQuestIdAndUserId(1L, 1L);
    verify(questRepository).findById(1L);
    verify(questRepository).delete(quest);
  }

  @Test
  void delete_throwsIllegalArgumentException_whenQuestDoesNotExist() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questService.delete(999L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(questRepository).findById(999L);
    verify(questRepository, never()).delete(any(Quest.class));
  }

  @Test
  void validateQuestExist_returnsQuest_whenQuestExists() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));

    Quest result = questService.validateQuestExist(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    verify(questRepository).findById(1L);
  }

  @Test
  void validateQuestExist_throwsIllegalArgumentException_whenQuestDoesNotExist() {
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> questService.validateQuestExist(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(questRepository).findById(999L);
  }

  // ────── publishQuest ───────────────────────────────────────────────────────────

  @Test
  void publishQuest_transitionsToRegistration_whenDraftHasValidLevels() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    Level levelWithTimeout =
        Level.builder().id(10L).quest(quest).title("L1").orderIndex(1).timeoutSeconds(600).build();
    Level levelWithCode = Level.builder().id(11L).quest(quest).title("L2").orderIndex(2).build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L))
        .thenReturn(List.of(levelWithTimeout, levelWithCode));
    when(codeRepository.existsByLevelId(10L)).thenReturn(false);
    when(codeRepository.existsByLevelId(11L)).thenReturn(true);
    when(questRepository.save(any(Quest.class))).thenAnswer(inv -> inv.getArgument(0));

    QuestResponse response = questService.publishQuest(1L, authentication);

    assertThat(response.getStatus()).isEqualTo(QuestStatus.REGISTRATION);
    verify(questRepository).save(quest);
  }

  @Test
  void publishQuest_throwsForbiddenOperationException_whenQuestNotDraft() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.REGISTRATION)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);

    assertThatThrownBy(() -> questService.publishQuest(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("REGISTRATION");

    verify(questRepository, never()).save(any());
  }

  @Test
  void publishQuest_throwsForbiddenOperationException_whenNoLevels() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L)).thenReturn(List.of());

    assertThatThrownBy(() -> questService.publishQuest(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("без уровней");

    verify(questRepository, never()).save(any());
  }

  @Test
  void publishQuest_throwsForbiddenOperationException_whenLevelIsAnomalous() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();
    Level anomalousLevel =
        Level.builder().id(10L).quest(quest).title("Broken Level").orderIndex(1).build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L)).thenReturn(List.of(anomalousLevel));
    when(codeRepository.existsByLevelId(10L)).thenReturn(false);

    assertThatThrownBy(() -> questService.publishQuest(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Broken Level")
        .hasMessageContaining("непроходим");

    verify(questRepository, never()).save(any());
  }

  // ────── finishQuest ───────────────────────────────────────────────────────────

  @Test
  void finishQuest_transitionsToFinished_andMarksUnfinishedProgressesAsDnf() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.RUNNING)
            .createdAt(Instant.now())
            .build();
    QuestProgress runningProgress =
        QuestProgress.builder().id(100L).quest(quest).status(QuestProgressStatus.RUNNING).build();
    QuestProgress finishedProgress =
        QuestProgress.builder().id(101L).quest(quest).status(QuestProgressStatus.FINISHED).build();
    QuestProgress waitingProgress =
        QuestProgress.builder().id(102L).quest(quest).status(QuestProgressStatus.WAITING).build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);
    when(questRepository.save(any(Quest.class))).thenAnswer(inv -> inv.getArgument(0));
    when(questProgressRepository.findByQuestId(1L))
        .thenReturn(List.of(runningProgress, finishedProgress, waitingProgress));

    QuestResponse response = questService.finishQuest(1L, authentication);

    assertThat(response.getStatus()).isEqualTo(QuestStatus.FINISHED);
    assertThat(runningProgress.getStatus()).isEqualTo(QuestProgressStatus.DNF);
    assertThat(waitingProgress.getStatus()).isEqualTo(QuestProgressStatus.DNF);
    assertThat(finishedProgress.getStatus()).isEqualTo(QuestProgressStatus.FINISHED);
    verify(questProgressRepository)
        .saveAll(List.of(runningProgress, finishedProgress, waitingProgress));
  }

  @Test
  void finishQuest_throwsForbiddenOperationException_whenQuestNotRunning() {
    Quest quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questRepository.findById(1L)).thenReturn(Optional.of(quest));
    when(questAuthorRepository.existsByQuestIdAndUserId(1L, authorUser.getId())).thenReturn(true);

    assertThatThrownBy(() -> questService.finishQuest(1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("DRAFT");

    verify(questRepository, never()).save(any());
    verify(questProgressRepository, never()).findByQuestId(any());
  }
}
