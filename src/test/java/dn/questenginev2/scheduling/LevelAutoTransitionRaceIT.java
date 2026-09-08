package dn.questenginev2.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
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
import dn.questenginev2.user.repository.UserRepository;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Проверяет Сценарий 5 (docs/02-processes/concurrency-scenarios.md) на реальной БД: Job 2
 * (автопереход) и CodeSubmission (завершение кодом) одновременно претендуют на один и тот же
 * LevelProgress. Ровно один путь должен победить.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LevelAutoTransitionRaceIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private QuestRepository questRepository;
  @Autowired private LevelRepository levelRepository;
  @Autowired private CodeRepository codeRepository;
  @Autowired private CodeSubmissionRepository codeSubmissionRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private QuestProgressRepository questProgressRepository;
  @Autowired private LevelProgressRepository levelProgressRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private LevelAutoTransitionScheduler levelAutoTransitionScheduler;
  @Autowired private QuestAuthorRepository questAuthorRepository;
  @Autowired private QuestRegistrationRepository questRegistrationRepository;
  @Autowired private HintRepository hintRepository;

  private Quest quest;
  private Team team;
  private LevelProgress levelProgress;
  private String teamMemberToken;

  @BeforeEach
  void setUp() throws Exception {
    codeSubmissionRepository.deleteAll();
    levelProgressRepository.deleteAll();
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    levelRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    hintRepository.deleteAll();
    questRegistrationRepository.deleteAll();
    questAuthorRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    User teamMemberUser = new User();
    teamMemberUser.setUsername("racer1");
    teamMemberUser.setPublicName("Racer One");
    teamMemberUser.setEmail("racer1@example.com");
    teamMemberUser.setPasswordHash(passwordEncoder.encode("password123"));
    teamMemberUser.setRole(UserRole.PLAYER);
    teamMemberUser = userRepository.save(teamMemberUser);

    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"racer1\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    teamMemberToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    team = teamRepository.save(Team.builder().name("Racing Team").captain(teamMemberUser).build());
    teamMemberRepository.save(
        TeamMember.builder()
            .team(team)
            .user(teamMemberUser)
            .role(TeamRole.CAPTAIN)
            .joinedAt(Instant.now())
            .build());

    quest =
        questRepository.save(
            Quest.builder()
                .title("Race Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.RUNNING)
                .build());

    Level level =
        levelRepository.save(
            Level.builder()
                .quest(quest)
                .title("L1")
                .orderIndex(1)
                .timeoutSeconds(1)
                .requiredMainCodesCount(1)
                .build());
    codeRepository.save(
        Code.builder().level(level).value("siniy").type(CodeType.MAIN).codeIndex(1).build());

    QuestProgress questProgress =
        questProgressRepository.save(
            QuestProgress.builder()
                .quest(quest)
                .team(team)
                .status(QuestProgressStatus.RUNNING)
                .questStartedAt(Instant.now())
                .build());

    // autoTransitionAt уже в прошлом — Job 2 сразу считает этот LevelProgress кандидатом.
    levelProgress =
        levelProgressRepository.save(
            LevelProgress.builder()
                .questProgress(questProgress)
                .level(level)
                .status(LevelProgressStatus.ACTIVE)
                .openedAt(Instant.now().minusSeconds(10))
                .autoTransitionAt(Instant.now().minusSeconds(1))
                .build());
  }

  @Test
  void exactlyOnePathWins_whenCodeSubmissionAndAutoTransitionRaceForSameLevelProgress()
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(2);

    // Поток 1: реальный HTTP-запрос ввода правильного кода.
    executor.submit(
        () -> {
          try {
            startLatch.await();
            mockMvc.perform(
                post("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/codes")
                    .header("Authorization", "Bearer " + teamMemberToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"value\":\"siniy\"}"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            doneLatch.countDown();
          }
        });

    // Поток 2: планировщик Job 2, вызванный напрямую (не дожидаясь реального таймера).
    executor.submit(
        () -> {
          try {
            startLatch.await();
            levelAutoTransitionScheduler.autoTransitionDueLevels();
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            doneLatch.countDown();
          }
        });

    startLatch.countDown();
    boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(finished).isTrue();

    LevelProgress reloaded = levelProgressRepository.findById(levelProgress.getId()).orElseThrow();
    // Ровно один путь должен был победить: либо COMPLETED (код), либо AUTO_TRANSITIONED (Job 2) —
    // но не оставшийся ACTIVE (это означало бы, что ни один путь не сработал) и не оба одновременно
    // (структурно невозможно, статус — одно поле, но проверяем явно на всякий случай).
    assertThat(reloaded.getStatus())
        .isIn(LevelProgressStatus.COMPLETED, LevelProgressStatus.AUTO_TRANSITIONED);

    // QuestProgress должен быть продвинут (FINISHED, т.к. это единственный уровень квеста) —
    // независимо от того, какой из двух путей выиграл (ADR-0009: без разницы
    // CODES/AUTO_TRANSITION).
    QuestProgress reloadedQuestProgress =
        questProgressRepository.findById(levelProgress.getQuestProgress().getId()).orElseThrow();
    assertThat(reloadedQuestProgress.getStatus()).isEqualTo(QuestProgressStatus.FINISHED);
  }
}
