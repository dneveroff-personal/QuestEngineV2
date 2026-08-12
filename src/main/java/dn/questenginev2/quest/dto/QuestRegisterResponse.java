package dn.questenginev2.quest.dto;

import dn.questenginev2.quest.entity.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestRegisterResponse {

  private Long questId;
  private Long teamId;
  private String teamName;
  private RegistrationStatus status;
}
