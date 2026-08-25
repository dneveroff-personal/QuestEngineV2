package dn.questenginev2.common.constants;

public final class Routes {

  // COMMON
  public static final String API = "/api";
  public static final String AUTH = "/auth";
  public static final String TEAMS = API + "/teams";
  public static final String QUESTS = API + "/quests";
  public static final String USERS = API + "/users";

  // Auth
  public static final String REGISTER = AUTH + "/register";
  public static final String LOGIN = AUTH + "/login";
  public static final String RESET_ADMIN_PASSWORD = AUTH + "/reset-admin-password";

  // User
  public static final String USER_ID = "/{userId:\\d+}";
  public static final String SET_ROLE = USER_ID + "/role";
  public static final String RESET_PASSWORD = USER_ID + "/reset-password";

  // Team
  public static final String TEAM_ID = "/{teamId:\\d+}";
  public static final String TEAM_ID_JOIN_REQUEST = TEAM_ID + "/request";
  public static final String JOIN_REQUESTS = "/requests";
  public static final String APPROVE_JOIN_REQUEST = JOIN_REQUESTS + "/{requestId}/approve";
  public static final String REJECT_JOIN_REQUEST = JOIN_REQUESTS + "/{requestId}/reject";
  public static final String MY = "/my";
  public static final String MEMBERS = TEAM_ID + "/members";
  public static final String LEAVE = "/leave";
  public static final String TRANSFER_CAPTAIN = "/transfer-captain" + USER_ID;

  // Level
  public static final String LEVELS = API + "/levels";
  public static final String LEVEL_ID = "/{levelId:\\d+}";

  // Quest
  public static final String QUEST_ID = "/{questId:\\d+}";
  public static final String QUEST_BY_AUTHOR = "/authors/{authorId:\\d+}";
  public static final String QUEST_UPCOMING = "/upcoming";
  public static final String QUEST_LEVELS = QUEST_ID + "/levels";
  public static final String LEVEL_HINTS = QUEST_LEVELS + LEVEL_ID + "/hints";
  public static final String LEVEL_CODES = QUEST_LEVELS + LEVEL_ID + "/codes";
  public static final String QUEST_PUBLISH = QUEST_ID + "/publish";
  public static final String QUEST_FINISH = QUEST_ID + "/finish";

  // Quest Registration
  public static final String QUEST_REGISTER = QUESTS + "/register";

  // Quest Progress
  public static final String QUEST_PROGRESS = QUESTS + "/progress";
  public static final String QUEST_PROGRESS_CODES = QUEST_ID + TEAM_ID + "/codes";

  // Hint
  public static final String HINTS = API + "/hints";
  public static final String HINT_ID = "/{hintId:\\d+}";

  // Code
  public static final String CODES = API + "/codes";
  public static final String CODE_ID = "/{codeId:\\d+}";
}
