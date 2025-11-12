package common;

public class Protocol {

    public static final String DELIMITER = "|";
    public static final String DATA_SEPARATOR = ";"; // 예: status:SUCCESS;playerId:P001
    public static final String FIELD_SEPARATOR = ":"; // 예: playerId:P001

    // 1XX: 연결 및 인증
    public static final String LOGIN_REQ = "LOGIN_REQ";              // 로그인 요청
    public static final String LOGIN_RES = "LOGIN_RES";              // 로그인 응답
    public static final String LOGOUT_REQ = "LOGOUT_REQ";            // 로그아웃 요청
    public static final String HEARTBEAT = "HEARTBEAT";              // 연결 유지

    // 2XX: 게임 룸 관리 (추가)
    public static final String ROOM_LIST_REQ = "ROOM_LIST_REQ";      // 방 목록 요청
    public static final String ROOM_LIST_RES = "ROOM_LIST_RES";      // 방 목록 응답
    public static final String ROOM_CREATE_REQ = "ROOM_CREATE_REQ";  // 방 생성 요청
    public static final String ROOM_CREATE_RES = "ROOM_CREATE_RES";  // 방 생성 응답
    public static final String ROOM_JOIN_REQ = "ROOM_JOIN_REQ";      // 방 입장 요청
    public static final String ROOM_JOIN_RES = "ROOM_JOIN_RES";      // 방 입장 응답

    // 3XX: 게임 진행 (추후 구현에 필요)
    // ...
}