package dn.questenginev2.common.constants;

public final class Routes {

    // COMMON
    public static final String API = "/api";
    public static final String AUTH = "/auth";
    public static final String TEAMS = API + "/teams";

    // Auth
    public static final String REGISTER = AUTH + "/register";
    public static final String LOGIN = AUTH + "/login";

    //Team
    public static final String TEAM_ID = "/{teamId:\\d+}";
    public static final String TEAM_ID_JOIN_REQUEST = TEAM_ID + "/request";
    public static final String JOIN_REQUESTS = "/requests";
    public static final String APPROVE_JOIN_REQUEST = JOIN_REQUESTS + "/{requestId}/approve";

}
