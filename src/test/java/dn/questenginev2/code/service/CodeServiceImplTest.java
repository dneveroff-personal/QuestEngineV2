package dn.questenginev2.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
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
class CodeServiceImplTest {

  @Mock private CodeRepository codeRepository;

  @Mock private LevelRepository levelRepository;

  @Mock private QuestService questService;

  @Mock private UserService userService;

  @Mock private Authentication authentication;

  @InjectMocks private CodeServiceImpl codeService;

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
  void createCode_createsCode_whenUserIsAuthor() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(levelRepository.findById(1L)).thenReturn(java.util.Optional.of(testLevel));
    when(codeRepository.existsByCodeValue("CODE123")).thenReturn(false);

    CreateCodeRequest request = new CreateCodeRequest();
    request.setValue("CODE123");
    request.setType(CodeType.MAIN);
    request.setPoints(100);

    Code savedCode =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("CODE123")
            .type(CodeType.MAIN)
            .points(100)
            .createdAt(Instant.now())
            .build();
    when(codeRepository.save(any(Code.class))).thenReturn(savedCode);

    CodeResponse response = codeService.createCode(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getLevelId()).isEqualTo(1L);
    assertThat(response.getValue()).isEqualTo("CODE123");
    assertThat(response.getType()).isEqualTo(CodeType.MAIN);
    assertThat(response.getPoints()).isEqualTo(100);

    verify(codeRepository).save(any(Code.class));
  }

  @Test
  void createCode_throwsIllegalArgumentException_whenCodeValueAlreadyExists() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(levelRepository.findById(1L)).thenReturn(java.util.Optional.of(testLevel));
    when(codeRepository.existsByCodeValue("CODE123")).thenReturn(true);

    CreateCodeRequest request = new CreateCodeRequest();
    request.setValue("CODE123");
    request.setType(CodeType.MAIN);
    request.setPoints(100);

    assertThatThrownBy(() -> codeService.createCode(1L, request, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Код уже существует");

    verify(codeRepository, never()).save(any());
  }

  @Test
  void createCode_throwsForbiddenOperationException_whenUserIsPlayer() {
    when(userService.getCurrentUser(authentication)).thenReturn(playerUser);
    doThrow(
            new ForbiddenOperationException(
                "Доступ к редактированию квестов имеют только AUTHOR или ADMIN"))
        .when(questService)
        .validateAuthorOrAdmin(playerUser);

    CreateCodeRequest request = new CreateCodeRequest();
    request.setValue("CODE123");
    request.setType(CodeType.MAIN);
    request.setPoints(100);

    assertThatThrownBy(() -> codeService.createCode(1L, request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("AUTHOR или ADMIN");

    verify(levelRepository, never()).findById(any());
    verify(codeRepository, never()).save(any());
  }

  @Test
  void getCodesByLevelId_returnsCodes_whenLevelExists() {
    when(levelRepository.findById(1L)).thenReturn(java.util.Optional.of(testLevel));

    Code code1 =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("CODE1")
            .type(CodeType.MAIN)
            .points(100)
            .createdAt(Instant.now())
            .build();

    Code code2 =
        Code.builder()
            .id(2L)
            .level(testLevel)
            .value("CODE2")
            .type(CodeType.BONUS)
            .points(50)
            .createdAt(Instant.now())
            .build();

    when(codeRepository.findByLevelIdOrderByCreatedAt(1L)).thenReturn(List.of(code1, code2));

    List<CodeResponse> responses = codeService.getCodesByLevelId(1L);

    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).getValue()).isEqualTo("CODE1");
    assertThat(responses.get(1).getValue()).isEqualTo("CODE2");
  }

  @Test
  void getCodeById_returnsCode_whenCodeExists() {
    Code code =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("CODE123")
            .type(CodeType.MAIN)
            .points(100)
            .createdAt(Instant.now())
            .build();

    when(codeRepository.findById(1L)).thenReturn(java.util.Optional.of(code));

    CodeResponse response = codeService.getCodeById(1L);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getValue()).isEqualTo("CODE123");
  }

  @Test
  void getCodeById_throwsIllegalArgumentException_whenCodeDoesNotExist() {
    when(codeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> codeService.getCodeById(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Код не найден");
  }

  @Test
  void updateCode_updatesCode_whenUserIsAuthor() {
    Code code =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("OLDCODE")
            .type(CodeType.MAIN)
            .points(100)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(codeRepository.findById(1L)).thenReturn(java.util.Optional.of(code));
    when(codeRepository.existsByCodeValue("NEWCODE")).thenReturn(false);

    CreateCodeRequest request = new CreateCodeRequest();
    request.setValue("NEWCODE");
    request.setType(CodeType.BONUS);
    request.setPoints(200);

    Code updatedCode =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("NEWCODE")
            .type(CodeType.BONUS)
            .points(200)
            .createdAt(Instant.now())
            .build();
    when(codeRepository.save(any(Code.class))).thenReturn(updatedCode);

    CodeResponse response = codeService.updateCode(1L, request, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getValue()).isEqualTo("NEWCODE");
    assertThat(response.getType()).isEqualTo(CodeType.BONUS);
    assertThat(response.getPoints()).isEqualTo(200);

    verify(codeRepository).save(any(Code.class));
  }

  @Test
  void deleteCode_deletesCode_whenUserIsAuthor() {
    Code code =
        Code.builder()
            .id(1L)
            .level(testLevel)
            .value("CODE123")
            .type(CodeType.MAIN)
            .points(100)
            .createdAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(codeRepository.findById(1L)).thenReturn(java.util.Optional.of(code));

    codeService.deleteCode(1L, authentication);

    verify(questService).validateAuthorOrAdmin(authorUser);
    verify(questService).validateQuestAuthor(authorUser, testLevel.getQuest().getId());
    verify(codeRepository).delete(code);
  }

  @Test
  void deleteCode_throwsIllegalArgumentException_whenCodeDoesNotExist() {
    when(userService.getCurrentUser(authentication)).thenReturn(authorUser);
    when(codeRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> codeService.deleteCode(999L, authentication))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Код не найден");

    verify(codeRepository, never()).delete(any());
  }
}
