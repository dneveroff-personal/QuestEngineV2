package dn.questenginev2.hint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.service.QuestService;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class HintServiceImplTest {

  @Mock private HintRepository hintRepository;

  @Mock private LevelRepository levelRepository;

  @Mock private QuestService questService;

  @Mock private UserService userService;

  @Mock private Authentication authentication;

  @InjectMocks private HintServiceImpl hintService;

  private User authorUser;
  private User adminUser;
  private User playerUser;
  private Quest testQuest;
  private Level testLevel;

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

    testQuest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

    testLevel =
        Level.builder()
            .id(1L)
            .quest(testQuest)
            .title("Level 1")
            .orderIndex(1)
            .content("Level content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  @Test
  void createHint_createsHint_whenUserIsAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(levelRepository.findById(1L)).thenReturn(java.util.Optional.of(testLevel));

    CreateHintRequest request = new CreateHintRequest();
    request.setOrderIndex(1);
    request.setDelaySeconds(30);
    request.setContent("Hint content");

    Hint savedHint =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(1)
            .delaySeconds(30)
            .content("Hint content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(hintRepository.save(any(Hint.class))).thenReturn(savedHint);

    HintResponse response = hintService.createHint(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getLevelId()).isEqualTo(1L);
    assertThat(response.getOrderIndex()).isEqualTo(1);
    assertThat(response.getDelaySeconds()).isEqualTo(30);
    assertThat(response.getContent()).isEqualTo("Hint content");

    verify(hintRepository).save(any(Hint.class));
  }

  @Test
  void createHint_throwsForbiddenOperationException_whenUserIsPlayer() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    doThrow(
            new ForbiddenOperationException(
                "Доступ к редактированию квестов имеют только AUTHOR или ADMIN"))
        .when(questService)
        .validateAuthorOrAdmin(playerUser);

    CreateHintRequest request = new CreateHintRequest();
    request.setOrderIndex(1);
    request.setDelaySeconds(30);
    request.setContent("Hint content");

    assertThatThrownBy(() -> hintService.createHint(1L, request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("AUTHOR или ADMIN");

    verify(levelRepository, never()).findById(any());
    verify(hintRepository, never()).save(any());
  }

  @Test
  void createHint_throwsIllegalArgumentException_whenLevelDoesNotExist() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(levelRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    CreateHintRequest request = new CreateHintRequest();
    request.setOrderIndex(1);
    request.setDelaySeconds(30);
    request.setContent("Hint content");

    assertThatThrownBy(() -> hintService.createHint(999L, request, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Уровень не найден");

    verify(hintRepository, never()).save(any());
  }

  @Test
  void getHintsByLevelId_returnsHints_whenLevelExists() {
    when(levelRepository.findById(1L)).thenReturn(java.util.Optional.of(testLevel));

    Hint hint1 =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(1)
            .delaySeconds(30)
            .content("Hint 1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    Hint hint2 =
        Hint.builder()
            .id(2L)
            .level(testLevel)
            .orderIndex(2)
            .delaySeconds(60)
            .content("Hint 2")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(hintRepository.findByLevelIdOrderByOrderIndex(1L)).thenReturn(List.of(hint1, hint2));

    List<HintResponse> responses = hintService.getHintsByLevelId(1L);

    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).getOrderIndex()).isEqualTo(1);
    assertThat(responses.get(1).getOrderIndex()).isEqualTo(2);
  }

  @Test
  void getHintById_returnsHint_whenHintExists() {
    Hint hint =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(1)
            .delaySeconds(30)
            .content("Hint content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(hintRepository.findById(1L)).thenReturn(java.util.Optional.of(hint));

    HintResponse response = hintService.getHintById(1L);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getContent()).isEqualTo("Hint content");
  }

  @Test
  void getHintById_throwsIllegalArgumentException_whenHintDoesNotExist() {
    when(hintRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> hintService.getHintById(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Подсказка не найдена");
  }

  @Test
  void updateHint_updatesHint_whenUserIsAuthor() {
    Hint hint =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(1)
            .delaySeconds(30)
            .content("Old content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(hintRepository.findById(1L)).thenReturn(java.util.Optional.of(hint));

    CreateHintRequest request = new CreateHintRequest();
    request.setOrderIndex(2);
    request.setDelaySeconds(45);
    request.setContent("New content");

    Hint updatedHint =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(2)
            .delaySeconds(45)
            .content("New content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(hintRepository.save(any(Hint.class))).thenReturn(updatedHint);

    HintResponse response = hintService.updateHint(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getOrderIndex()).isEqualTo(2);
    assertThat(response.getDelaySeconds()).isEqualTo(45);
    assertThat(response.getContent()).isEqualTo("New content");

    verify(hintRepository).save(any(Hint.class));
  }

  @Test
  void deleteHint_deletesHint_whenUserIsAuthor() {
    Hint hint =
        Hint.builder()
            .id(1L)
            .level(testLevel)
            .orderIndex(1)
            .delaySeconds(30)
            .content("Hint content")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(hintRepository.findById(1L)).thenReturn(java.util.Optional.of(hint));

    hintService.deleteHint(1L, authentication);

    verify(questService).validateAuthorOrAdmin(authorUser);
    verify(questService).validateQuestAuthor(authorUser, testLevel.getQuest().getId());
    verify(hintRepository).delete(hint);
  }

  @Test
  void deleteHint_throwsIllegalArgumentException_whenHintDoesNotExist() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(hintRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> hintService.deleteHint(999L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Подсказка не найдена");

    verify(hintRepository, never()).delete(any());
  }
}
