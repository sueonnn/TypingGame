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
    public static final String ROOM_UPDATE = "ROOM_UPDATE";
    public static final String ROOM_LEAVE_REQ = "ROOM_LEAVE_REQ";     // 방 퇴장 요청

    // 3XX: 게임 진행 (추후 구현에 필요)
    public static final String GAME_READY = "GAME_READY";            // 게임 준비
    public static final String GAME_START = "GAME_START";            // 게임 시작
    public static final String WORD_INPUT = "WORD_INPUT";            // 단어 입력
    public static final String WORD_CAPTURE = "WORD_CAPTURE";        // 단어 점령 알림
    public static final String GAME_STATE = "GAME_STATE";            // 게임 상태 동기화
    public static final String GAME_END = "GAME_END";                // 게임 종료
    public static final String GAME_UPDATE = "GAME_UPDATE";
    public static final String GAME_START_REQ = "GAME_START_REQ";


    // 4XX: 채팅 (추후 구현)
    public static final String CHAT_MSG = "CHAT_MSG";

    // 5XX: 에러 처리 추가
    public static final String ERROR = "ERROR";

    // 프로토콜 구분자 외에, 데이터 내부의 필드를 구분할 때 사용 (예: wordStates 내부)
    public static final String FIELD_FIELD_SEPARATOR = "@";



}