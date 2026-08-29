package dn.questenginev2.quest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.entity.RegistrationStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
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
 * Проверяет Сценарий 1 (docs/02-processes/concurrency-scenarios.md) на реальной БД: несколько
 * конкурентных вызовов {@code approveTeam()} для одного и того же Quest у лимита команд не должны
 * превысить {@code Quest.maximumTeams}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApproveTeamRaceIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private QuestRepository questRepository;
  @Autowired private QuestAuthorRepository questAuthorRepository;
  @Autowired private QuestRegistrationRepository questRegistrationRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;
  @Autowired private QuestProgressRepository questProgressRepository;
  @Autowired private LevelProgressRepository levelProgressRepository;
  @Autowired private LevelRepository levelRepository;
  @Autowired private HintRepository hintRepository;
  @Autowired private CodeRepository codeRepository;
  @Autowired private CodeSubmissionRepository codeSubmissionRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Quest quest;
  private String authorToken;
  private final List<Team> teams = new ArrayList<>();

  private static final int MAXIMUM_TEAMS = 3;
  private static final int PENDING_TEAMS_COUNT = 10;

  @BeforeEach
  void setUp() throws Exception {
    codeSubmissionRepository.deleteAll();
    levelProgressRepository.deleteAll();
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    questRegistrationRepository.deleteAll();
    questAuthorRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamJoinRequestRepository.deleteAll();
    teamRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    User authorUser = new User();
    authorUser.setUsername("race-author");
    authorUser.setPublicName("Race Author");
    authorUser.setEmail("race-author@example.com");
    authorUser.setPasswordHash(passwordEncoder.encode("password123"));
    authorUser.setRole(UserRole.AUTHOR);
    authorUser = userRepository.save(authorUser);

    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"race-author\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    authorToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    quest =
        questRepository.save(
            Quest.builder()
                .title("Race Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.REGISTRATION)
                .maximumTeams(MAXIMUM_TEAMS)
                .build());
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    // Больше PENDING-заявок, чем лимит команд — иначе гонку невозможно спровоцировать.
    for (int i = 0; i < PENDING_TEAMS_COUNT; i++) {
      User captain = new User();
      captain.setUsername("captain" + i);
      captain.setPublicName("Captain " + i);
      captain.setEmail("captain" + i + "@example.com");
      captain.setPasswordHash(passwordEncoder.encode("password123"));
      captain.setRole(UserRole.PLAYER);
      captain = userRepository.save(captain);

      Team team = teamRepository.save(Team.builder().name("Team " + i).captain(captain).build());
      teams.add(team);

      questRegistrationRepository.save(
          QuestRegistration.builder()
              .quest(quest)
              .team(team)
              .status(RegistrationStatus.PENDING)
              .build());
    }
  }

  @Test
  void approveTeam_neverExceedsMaximumTeams_underConcurrentApprovals() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(PENDING_TEAMS_COUNT);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(PENDING_TEAMS_COUNT);

    for (Team team : teams) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              mockMvc.perform(
                  put("/api/quests/register/" + quest.getId() + "/approve/" + team.getId())
                      .header("Authorization", "Bearer " + authorToken));
            } catch (Exception e) {
              throw new RuntimeException(e);
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(finished).isTrue();

    long approvedCount =
        questRegistrationRepository.countByQuestIdAndStatus(
            quest.getId(), RegistrationStatus.APPROVED);

    assertThat(approvedCount)
        .as("Количество APPROVED-команд не должно превышать лимит квеста, несмотря на гонку")
        .isEqualTo(MAXIMUM_TEAMS);
  }
}
