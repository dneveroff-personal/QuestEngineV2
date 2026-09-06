package dn.questenginev2.bonuspenalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonusPenaltyServiceImplTest {

  @Mock private ManualTimeAdjustmentRepository repository;

  private BonusPenaltyServiceImpl service;

  private QuestProgress questProgress;

  @BeforeEach
  void setUp() {
    service = new BonusPenaltyServiceImpl(repository);

    questProgress = QuestProgress.builder().id(100L).build();
  }

  @Test
  void shouldReturnBonusSeconds() {
    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.BONUS))
        .thenReturn(300L);

    long result = service.getAdjustmentSeconds(questProgress, TimeAdjustmentType.BONUS);

    assertThat(result).isEqualTo(300L);
  }

  @Test
  void shouldReturnPenaltySeconds() {
    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.PENALTY))
        .thenReturn(500L);

    long result = service.getAdjustmentSeconds(questProgress, TimeAdjustmentType.PENALTY);

    assertThat(result).isEqualTo(500L);
  }

  @Test
  void shouldCalculateTotalAdjustmentAsPenaltyMinusBonus() {
    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.BONUS))
        .thenReturn(300L);

    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.PENALTY))
        .thenReturn(500L);

    long result = service.getTotalAdjustmentSeconds(questProgress);

    assertThat(result).isEqualTo(200L);
  }

  @Test
  void shouldReturnNegativeAdjustmentWhenBonusIsGreaterThanPenalty() {
    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.BONUS))
        .thenReturn(1000L);

    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.PENALTY))
        .thenReturn(300L);

    long result = service.getTotalAdjustmentSeconds(questProgress);

    assertThat(result).isEqualTo(-700L);
  }

  @Test
  void shouldReturnZeroWhenThereAreNoAdjustments() {
    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.BONUS))
        .thenReturn(0L);

    when(repository.sumActiveSecondsByQuestProgressIdAndType(100L, TimeAdjustmentType.PENALTY))
        .thenReturn(0L);

    long result = service.getTotalAdjustmentSeconds(questProgress);

    assertThat(result).isZero();
  }
}
