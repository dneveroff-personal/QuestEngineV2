package dn.questenginev2.quest.entity;

import java.time.Instant;

public interface QuestShortProjection {

  Long getId();

  String getTitle();

  Instant startTime();
}
