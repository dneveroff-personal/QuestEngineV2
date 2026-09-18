package dn.questenginev2.team.dto;

import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.entity.RegistrationStatus;
import java.time.Instant;

/**
 * One quest registration of a team, including quest summary.
 *
 * <p>Includes all registration statuses and quest statuses (incl. {@code FINISHED}) so the client
 * does not need N+1 calls via {@code /quests/upcoming}.
 */
public record TeamQuestItemResponse(
    Long registrationId,
    RegistrationStatus registrationStatus,
    Instant registrationCreatedAt,
    Long questId,
    String title,
    String description,
    QuestType type,
    QuestStatus questStatus,
    Instant startTime,
    Instant finishTime) {}
