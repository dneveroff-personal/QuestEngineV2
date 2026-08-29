package dn.questenginev2.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.RegistrationStatus;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.quest.service.QuestProgressService;
import dn.questenginev2.team.entity.Team;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestStartSchedulerTest {

  @Mock private QuestRepository questRepository;
  @Mock private QuestRegistrationRepository questRegistrationRepository;
  @Mock private QuestProgressService questProgressService;
  @Mock private Clock clock;

  @InjectMocks private QuestStartScheduler questStartScheduler;

  private final Instant fixedNow = Instant.parse("2026-08-24T21:00:00Z");

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(fixedNow);
  }

  private Quest quest(Long id) {
    return Quest.builder()
        .id(id)
        .title("Q")
        .description("D")
        .status(QuestStatus.REGISTRATION)
        .build();
  }

  @Test
  void startDueQuests_doesNothing_whenNoQuestsDue() {
    when(questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, fixedNow))
        .thenReturn(List.of());

    questStartScheduler.startDueQuests();

    verify(questRepository, never()).tryStartQuest(anyLong());
  }

  @Test
  void startDueQuests_createsProgressForEachApprovedTeam_whenTransitionWon() {
    Quest quest = quest(1L);
    when(questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, fixedNow))
        .thenReturn(List.of(quest));
    when(questRepository.tryStartQuest(1L)).thenReturn(1);

    Team teamA = Team.builder().id(10L).build();
    Team teamB = Team.builder().id(20L).build();
    QuestRegistration regA =
        QuestRegistration.builder()
            .quest(quest)
            .team(teamA)
            .status(RegistrationStatus.APPROVED)
            .build();
    QuestRegistration regB =
        QuestRegistration.builder()
            .quest(quest)
            .team(teamB)
            .status(RegistrationStatus.APPROVED)
            .build();
    when(questRegistrationRepository.findByQuestIdAndStatus(1L, RegistrationStatus.APPROVED))
        .thenReturn(List.of(regA, regB));

    questStartScheduler.startDueQuests();

    verify(questProgressService).createProgress(1L, 10L);
    verify(questProgressService).createProgress(1L, 20L);
  }

  @Test
  void startDueQuests_doesNotCreateProgress_whenTransitionLost() {
    Quest quest = quest(1L);
    when(questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, fixedNow))
        .thenReturn(List.of(quest));
    when(questRepository.tryStartQuest(1L)).thenReturn(0);

    questStartScheduler.startDueQuests();

    verify(questRegistrationRepository, never()).findByQuestIdAndStatus(anyLong(), any());
    verify(questProgressService, never()).createProgress(anyLong(), anyLong());
  }

  @Test
  void startDueQuests_skipsTeam_whenProgressAlreadyExists() {
    // Сценарий 7: параллельный approveTeam() уже создал QuestProgress для одной из команд.
    Quest quest = quest(1L);
    when(questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, fixedNow))
        .thenReturn(List.of(quest));
    when(questRepository.tryStartQuest(1L)).thenReturn(1);

    Team teamA = Team.builder().id(10L).build();
    Team teamB = Team.builder().id(20L).build();
    QuestRegistration regA =
        QuestRegistration.builder()
            .quest(quest)
            .team(teamA)
            .status(RegistrationStatus.APPROVED)
            .build();
    QuestRegistration regB =
        QuestRegistration.builder()
            .quest(quest)
            .team(teamB)
            .status(RegistrationStatus.APPROVED)
            .build();
    when(questRegistrationRepository.findByQuestIdAndStatus(1L, RegistrationStatus.APPROVED))
        .thenReturn(List.of(regA, regB));
    doThrow(new IllegalArgumentException("Прогресс для этой команды уже существует"))
        .when(questProgressService)
        .createProgress(1L, 10L);

    questStartScheduler.startDueQuests();

    verify(questProgressService).createProgress(1L, 10L);
    verify(questProgressService).createProgress(1L, 20L);
  }

  @Test
  void startDueQuests_processesMultipleQuestsIndependently() {
    Quest questOne = quest(1L);
    Quest questTwo = quest(2L);
    when(questRepository.findByStatusAndStartTimeLessThanEqual(QuestStatus.REGISTRATION, fixedNow))
        .thenReturn(List.of(questOne, questTwo));
    when(questRepository.tryStartQuest(1L)).thenReturn(1);
    when(questRepository.tryStartQuest(2L)).thenReturn(1);
    when(questRegistrationRepository.findByQuestIdAndStatus(anyLong(), any()))
        .thenReturn(List.of());

    questStartScheduler.startDueQuests();

    verify(questRepository).tryStartQuest(1L);
    verify(questRepository).tryStartQuest(2L);
  }
}
