package dn.questenginev2.statistic.sse;

import dn.questenginev2.statistic.event.StatisticsChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StatisticsSseBridge {

  private final StatisticsSseHub statisticsSseHub;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onStatisticsChanged(StatisticsChangedEvent event) {
    if (event.questId() != null) {
      statisticsSseHub.publish(event.questId());
    }
  }
}
