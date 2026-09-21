package dn.questenginev2.statistic.event;

/** Fired after ranking-relevant domain changes; SSE bridge pushes a fresh snapshot. */
public record StatisticsChangedEvent(Long questId) {}
