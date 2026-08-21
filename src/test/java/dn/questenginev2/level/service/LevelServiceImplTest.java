package dn.questenginev2.level.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class LevelServiceImplTest {

  @Mock private LevelRepository levelRepository;

  @Mock private QuestService questService;

  @Mock private UserService userService;

  @Mock private Authentication authentication;

  @InjectMocks private LevelServiceImpl levelService;

  private User authorUser;
  private User adminUser;
  private User playerUser;
  private Quest testQuest;

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
  }

  @Test
  void createLevel_createsLevel_whenUserIsAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questService.validateQuestExist(1L)).thenReturn(testQuest);
    when(levelRepository.findMaxOrderIndex(1L)).thenReturn(0);

    CreateLevelRequest request = new CreateLevelRequest("Level 1", "Level content", 300, 1);

    Level savedLevel =
        Level.builder()
            .id(1L)
            .quest(testQuest)
            .title("Level 1")
            .orderIndex(1)
            .content("Level content")
            .timeoutSeconds(300)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(levelRepository.save(any(Level.class))).thenReturn(savedLevel);

    LevelResponse response = levelService.createLevel(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getQuestId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("Level 1");
    assertThat(response.getOrderIndex()).isEqualTo(1);
    assertThat(response.getContent()).isEqualTo("Level content");
    assertThat(response.getTimeoutSeconds()).isEqualTo(300);

    verify(levelRepository).save(any(Level.class));
  }

  @Test
  void createLevel_createsLevel_whenUserIsAdmin() {
    when(userService.getCurrentUser(authentication)).thenReturn(adminUser);
    when(questService.validateQuestExist(1L)).thenReturn(testQuest);
    when(levelRepository.findMaxOrderIndex(1L)).thenReturn(0);

    CreateLevelRequest request =
        new CreateLevelRequest("Admin Level", "Admin level content", 600, 1);

    Level savedLevel =
        Level.builder()
            .id(2L)
            .quest(testQuest)
            .title("Admin Level")
            .orderIndex(1)
            .content("Admin level content")
            .timeoutSeconds(600)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    when(levelRepository.save(any(Level.class))).thenReturn(savedLevel);

    LevelResponse response = levelService.createLevel(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("Admin Level");
    assertThat(response.getTimeoutSeconds()).isEqualTo(600);
    verify(levelRepository).save(any(Level.class));
  }

  @Test
  void createLevel_throwsForbiddenOperationException_whenUserIsPlayer() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    doThrow(
            new ForbiddenOperationException(
                "Доступ к редактированию квестов имеют только AUTHOR или ADMIN"))
        .when(questService)
        .validateAuthorOrAdmin(playerUser);

    CreateLevelRequest request =
        new CreateLevelRequest("Player Level", "Player level content", 100, 1);

    assertThatThrownBy(() -> levelService.createLevel(1L, request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("AUTHOR или ADMIN");

    verify(questService, never()).validateQuestExist(any());
    verify(levelRepository, never()).save(any());
  }

  @Test
  void createLevel_throwsIllegalArgumentException_whenQuestDoesNotExist() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(questService.validateQuestExist(999L))
        .thenThrow(new IllegalArgumentException("Квест не найден: 999"));

    CreateLevelRequest request = new CreateLevelRequest("Level 1", "Level content", 200, 1);

    assertThatThrownBy(() -> levelService.createLevel(999L, request, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Квест не найден");

    verify(levelRepository, never()).save(any());
  }
}
