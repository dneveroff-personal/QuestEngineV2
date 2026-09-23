package dn.questenginev2.bonuspenalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonusPenaltyServiceImplTest {

  @Mock private ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;
  @Mock private CodeSubmissionRepository codeSubmissionRepository;
  @Mock private HintProgressRepository hintProgressRepository;

  private BonusPenaltyServiceImpl service;
  private QuestProgress questProgress;

  @BeforeEach
  void setUp() {
    service =
        new BonusPenaltyServiceImpl(
            manualTimeAdjustmentRepository, codeSubmissionRepository, hintProgressRepository);
    questProgress = QuestProgress.builder().id(100L).build();
  }

  @Test
  void getAdjustmentSeconds_sumsManualCodeAndHint_forBonus() {
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.BONUS))
        .thenReturn(300L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_BONUS))
        .thenReturn(200L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.BONUS))
        .thenReturn(50L);

    long result = service.getAdjustmentSeconds(questProgress, TimeAdjustmentType.BONUS);

    assertThat(result).isEqualTo(550L);
  }

  @Test
  void getAdjustmentSeconds_sumsManualCodeAndHint_forPenalty() {
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.PENALTY))
        .thenReturn(100L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_PENALTY))
        .thenReturn(40L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.PENALTY))
        .thenReturn(10L);

    long result = service.getAdjustmentSeconds(questProgress, TimeAdjustmentType.PENALTY);

    assertThat(result).isEqualTo(150L);
  }

  @Test
  void getTotalAdjustmentSeconds_isPenaltyMinusBonus_acrossAllSources() {
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.BONUS))
        .thenReturn(300L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_BONUS))
        .thenReturn(200L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.BONUS))
        .thenReturn(50L);
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.PENALTY))
        .thenReturn(100L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_PENALTY))
        .thenReturn(40L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.PENALTY))
        .thenReturn(10L);

    assertThat(service.getTotalAdjustmentSeconds(questProgress)).isEqualTo(-400L);
  }

  @Test
  void getTotalAdjustmentSeconds_returnsZero_whenNoSources() {
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.BONUS))
        .thenReturn(0L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_BONUS))
        .thenReturn(0L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.BONUS))
        .thenReturn(0L);
    when(manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            100L, TimeAdjustmentType.PENALTY))
        .thenReturn(0L);
    when(codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            100L, CodeSubmissionResult.CORRECT_PENALTY))
        .thenReturn(0L);
    when(hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            100L, HintType.PENALTY))
        .thenReturn(0L);

    assertThat(service.getTotalAdjustmentSeconds(questProgress)).isZero();
  }
}
