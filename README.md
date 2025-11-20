# 네트워크 프로그래밍 텀프로젝트 제안서

## 팀명 : 7
팀원 : 정수연, 이건우

1. 🎯 프로젝트 개요

| **게임명** | 판뒤집기 |
| --- | --- |
| 플랫폼 | 데스크톱 (Java 기반) |
| 핵심 기술 | Java GUI |
| 게임 목표 | 제한 시간 내에 화면의 고정된 단어를 타자로 입력하여 더 많이 **자신의 팀 색깔로 뒤집는(점령하는)** 팀이 승리 |
| 최대 인원 | 4명 (2 vs 2 또는 1 vs 1 팀전 가능) |

### 2. 💡 핵심 게임 메커니즘

| 요소 | 세부 사항 |
| --- | --- |
| **게임 보드** | 화면 중앙에 수십 개의 한국어 단어 블록이 **고정된 상태**로 배치됩니다. |
| **입력 방식** | 플레이어는 화면에 보이는 단어를 입력 창에 정확히 입력해야 합니다. |
| **점령 로직** | 단어 입력을 완료하면 해당 단어 블록의 색깔이
**플레이어의 팀 색깔**
로 즉시 변경됩니다. |
| **경쟁 (재점령)** | 이미 상대 팀이 점령한 단어라도, 아군 플레이어가 해당 단어를 다시 입력하면
**점령 상태를 뒤집어**
점수를 뺏어올 수 있습니다. |
| 승리 조건 | 제한 시간 (예: 60초) 종료 시, **점령한 단어의 총합 점수**가 높은 팀이 승리합니다. |
|  |  |
|  |  |

### 3.  ⭐️게임 아이디어 요약

- **기반:** 타자 게임
- **특징 :** 화면에 **고정된 단어들**이 있고, 플레이어들이 해당 단어를 **더 많이** 입력하여 **단어의 색깔/상태를 뒤집는(점령하는) 방식**의 경쟁 게임입니다.
- **목표:** 제한 시간 내에 더 많은 단어를 뒤집어 **더 높은 점수를 얻는 것**

```java
판뒤집기/
├── src/
│   ├── common/
│   │   ├── Protocol.java          # 통신 프로토콜 정의
│   │   ├── GameConstants.java     # 게임 상수
│   │   ├── Word.java             # 단어 클래스
│   │   └── Player.java           # 플레이어 정보
│   │
│   ├── server/
│   │   ├── GameServer.java       # 메인 서버
│   │   ├── ClientHandler.java    # 클라이언트 핸들러
│   │   ├── GameRoom.java         # 게임 룸 관리
│   │   └── GameLogic.java        # 게임 로직
│   │
│   └── client/
│       ├── GameClient.java       # 메인 클라이언트
│       ├── ui/
│       │   ├── MainFrame.java    # 메인 화면
│       │   ├── GamePanel.java    # 게임 화면
│       │   └── WordBlock.java    # 단어 블록 UI
│       └── network/
│           └── ServerConnection.java  # 서버 연결
```

### 4. 통신 프로토콜

```java
// Protocol.java
public class Protocol {
    // 메시지 타입
    public static final int LOGIN = 100;
    public static final int GAME_START = 200;
    public static final int WORD_INPUT = 300;
    public static final int GAME_UPDATE = 400;
    public static final int GAME_END = 500;
    
    // 메시지 구조: [TYPE][LENGTH][DATA]
    // 예: "300|7|안녕"
}
```

### 주요 메시지 흐름
```
1. 로그인/방 입장
   Client → Server: LOGIN|playerName|teamNumber
   Server → Client: LOGIN_OK|playerId|roomInfo

2. 게임 시작
   Server → All: GAME_START|wordList|timeLimit

3. 단어 입력
   Client → Server: WORD_INPUT|playerId|word
   Server → All: GAME_UPDATE|wordIndex|teamColor|score

4. 게임 종료
   Server → All: GAME_END|winnerTeam|finalScores
```

### 5.

```java
public class MessageType {
    // 1XX: 연결 및 인증
    public static final String LOGIN_REQ = "LOGIN_REQ";              // 로그인 요청
    public static final String LOGIN_RES = "LOGIN_RES";              // 로그인 응답
    public static final String LOGOUT_REQ = "LOGOUT_REQ";            // 로그아웃 요청
    public static final String HEARTBEAT = "HEARTBEAT";              // 연결 유지
    
    // 2XX: 게임 룸 관리
    public static final String ROOM_LIST_REQ = "ROOM_LIST_REQ";      // 방 목록 요청
    public static final String ROOM_LIST_RES = "ROOM_LIST_RES";      // 방 목록 응답
    public static final String ROOM_CREATE_REQ = "ROOM_CREATE_REQ";  // 방 생성 요청
    public static final String ROOM_CREATE_RES = "ROOM_CREATE_RES";  // 방 생성 응답
    public static final String ROOM_JOIN_REQ = "ROOM_JOIN_REQ";      // 방 입장 요청
    public static final String ROOM_JOIN_RES = "ROOM_JOIN_RES";      // 방 입장 응답
    public static final String ROOM_LEAVE_REQ = "ROOM_LEAVE_REQ";    // 방 퇴장 요청
    public static final String ROOM_UPDATE = "ROOM_UPDATE";          // 방 정보 업데이트
    
    // 3XX: 게임 진행
    public static final String GAME_READY = "GAME_READY";            // 게임 준비
    public static final String GAME_START = "GAME_START";            // 게임 시작
    public static final String WORD_INPUT = "WORD_INPUT";            // 단어 입력
    public static final String WORD_CAPTURE = "WORD_CAPTURE";        // 단어 점령 알림
    public static final String GAME_STATE = "GAME_STATE";            // 게임 상태 동기화
    public static final String GAME_END = "GAME_END";                // 게임 종료
    
    // 4XX: 채팅
    public static final String CHAT_MSG = "CHAT_MSG";                // 채팅 메시지
    
    // 5XX: 에러
    public static final String ERROR = "ERROR";                      // 에러 메시지
}
```

### 6.  프로젝트 계획

### Week 1: 기초 구조

1. 프로토콜 정의 완료
2. 기본 서버-클라이언트 연결
3. 단순 메시지 주고받기 테스트

### Week 2: 핵심 기능

1. 게임 룸 시스템
2. 단어 입력 및 점령 로직
3. 실시간 동기화

### Week 3: UI/UX

1. 게임 화면 디자인
2. 애니메이션 효과
3. 사운드 효과 (선택)

### Week 4: 마무리

1. 버그 수정
2. 성능 최적화
3. 최종 테스트 및 문서화

### 7. 메시지 상세 정의

### 7.1 연결 및 인증

```java
*// 로그인 요청// Client → Server*
LOGIN_REQ|데이터길이|playerName:이름

*// 로그인 응답// Server → Client*
LOGIN_RES|데이터길이|status:SUCCESS;playerId:P001;playerName:이름

*// 하트비트 (30초마다)// Client ↔ Server*
HEARTBEAT|0|
```

### 7.2 게임 룸 관리

java

```java
*// 방 목록 요청/응답// Client → Server*
ROOM_LIST_REQ|0|

*// Server → Client*
ROOM_LIST_RES|데이터길이|room1:R001:2/4:waiting;room2:R002:4/4:playing

*// 방 생성// Client → Server*
ROOM_CREATE_REQ|데이터길이|roomName:즐거운게임방;maxPlayers:4

*// Server → Client*
ROOM_CREATE_RES|데이터길이|status:SUCCESS;roomId:R003

*// 방 입장// Client → Server*
ROOM_JOIN_REQ|데이터길이|roomId:R001;team:1

*// Server → Client*
ROOM_JOIN_RES|데이터길이|status:SUCCESS;team:1;players:P001:이름:1;P002:김철수:2

*// 방 정보 업데이트 (브로드캐스트)// Server → All Clients in Room*
ROOM_UPDATE|데이터길이|players:P001:이름:1:ready;P002:김철수:2:notready
```

### 7.3 게임 진행

java

```java
*// 게임 준비// Client → Server*
GAME_READY|데이터길이|playerId:P001;ready:true

*// 게임 시작 (모든 플레이어 준비 완료 시)// Server → All Clients*
GAME_START|데이터길이|timeLimit:60;wordList:0:안녕:5;1:사과:3;2:컴퓨터:7;3:네트워크:10

*// 단어 입력// Client → Server*
WORD_INPUT|데이터길이|playerId:P001;word:안녕

*// 단어 점령 알림 (브로드캐스트)// Server → All Clients*
WORD_CAPTURE|데이터길이|wordIndex:0;team:1;capturedBy:P001;points:5

*// 게임 상태 동기화 (1초마다)// Server → All Clients*
GAME_STATE|데이터길이|time:45;team1Score:25;team2Score:30;wordStates:0:1;1:2;2:1;3:0

*// 게임 종료// Server → All Clients*
GAME_END|데이터길이|winner:1;team1Score:55;team2Score:45;mvp:P001:15
```

### 7.4 채팅

java

```java
*// 게임 준비// Client → Server*
GAME_READY|데이터길이|playerId:P001;ready:true

*// 게임 시작 (모든 플레이어 준비 완료 시)// Server → All Clients*
GAME_START|데이터길이|timeLimit:60;wordList:0:안녕:5;1:사과:3;2:컴퓨터:7;3:네트워크:10

*// 단어 입력// Client → Server*
WORD_INPUT|데이터길이|playerId:P001;word:안녕

*// 단어 점령 알림 (브로드캐스트)// Server → All Clients*
WORD_CAPTURE|데이터길이|wordIndex:0;team:1;capturedBy:P001;points:5

*// 게임 상태 동기화 (1초마다)// Server → All Clients*
GAME_STATE|데이터길이|time:45;team1Score:25;team2Score:30;wordStates:0:1;1:2;2:1;3:0

*// 게임 종료// Server → All Clients*
GAME_END|데이터길이|winner:1;team1Score:55;team2Score:45;mvp:P001:15
```

### 7.5 에러 처리

java

```java
*// 에러 메시지// Server → Client*
ERROR|데이터길이|code:E001;message:이미 점령된 단어입니다

*// 에러 코드*
E001: 이미 점령된 단어
E002: 잘못된 단어 입력
E003: 방이 가득 참
E004: 게임이 이미 시작됨
E005: 권한 없음
E006: 타임아웃
```

### 8. 메시지 처리 클래스

java

```java
*// Message.java*
public class Message {
    private String type;
    private int length;
    private Map<String, String> data;
    
    *// 메시지 생성*
    public static Message create(String type, Map<String, String> data) {
        Message msg = new Message();
        msg.type = type;
        msg.data = data;
        msg.length = calculateLength(data);
        return msg;
    }
    
    *// 메시지 파싱*
    public static Message parse(String rawMessage) {
        String[] parts = rawMessage.split("\\|", 3);
        Message msg = new Message();
        msg.type = parts[0];
        msg.length = Integer.parseInt(parts[1]);
        msg.data = parseData(parts[2]);
        return msg;
    }
    
    *// 메시지 직렬화*
    public String serialize() {
        StringBuilder dataStr = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            dataStr.append(entry.getKey())
                   .append(FIELD_SEPARATOR)
                   .append(entry.getValue())
                   .append(DATA_SEPARATOR);
        }
        return type + DELIMITER + length + DELIMITER + dataStr.toString();
    }
}
```

### 9. 네트워크 통신 흐름도

[게임 시작까지의 흐름]
1. Client → Server: LOGIN_REQ
2. Server → Client: LOGIN_RES
3. Client → Server: ROOM_LIST_REQ
4. Server → Client: ROOM_LIST_RES
5. Client → Server: ROOM_JOIN_REQ
6. Server → Client: ROOM_JOIN_RES
7. Server → All: ROOM_UPDATE
8. Client → Server: GAME_READY
9. Server → All: GAME_START

[게임 중 흐름]
1. Client → Server: WORD_INPUT
2. Server → All: WORD_CAPTURE
3. Server → All: GAME_STATE (1초마다)
4. Server → All: GAME_END

[연결 유지]
- Client ↔ Server: HEARTBEAT (30초마다)

### 10. 보안 고려사항

```java
public class SecurityProtocol {
    *// 메시지 검증*
    public static boolean validateMessage(Message msg) {
        *// 1. 메시지 길이 검증*
        if (msg.getLength() > MAX_MESSAGE_SIZE) return false;
        
        *// 2. 메시지 타입 검증*
        if (!isValidMessageType(msg.getType())) return false;
        
        *// 3. 데이터 무결성 검증*
        if (msg.getLength() != msg.getData().length()) return false;
        
        return true;
    }
    
    *// SQL Injection 방지*
    public static String sanitizeInput(String input) {
        return input.replaceAll("[^가-힣a-zA-Z0-9\\s]", "");
    }
}
```