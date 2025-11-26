package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import common.Protocol;
import common.Player;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private String playerName;
    private GameRoom currentRoom; // 현재 입장한 방 참조

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // 통신 스트림 초기화
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                System.out.println("서버 수신 [" + clientSocket.getInetAddress() + "]: " + rawMessage);
                handleClientMessage(rawMessage);
            }
        } catch (Exception e) {
            System.out.println("클라이언트 연결 해제: " + (playerName != null ? playerName : "미로그인 사용자"));
        } finally {
            // ⭐ 1. 방에 들어가 있었다면 방에서 제거
            try {
                if (currentRoom != null && playerId != null) {
                    GameRoom roomToLeave = this.currentRoom;
                    this.currentRoom = null;

                    roomToLeave.removePlayer(this.playerId);
                    // 다른 클라이언트들에게도 방 정보 갱신
                    server.broadcastRoomUpdate(roomToLeave);
                }
            } catch (Exception ignore) {
                // 로그만 남겨도 됨
                // System.out.println("연결 종료 중 방 정리 오류: " + ignore.getMessage());
            }

            // ⭐ 2. 클라이언트 목록에서 제거 (기존 코드)
            server.removeClient(this);

            // ⭐ 3. 소켓 닫기 (기존 코드)
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }


    /**
     * 클라이언트 메시지를 파싱하고 처리합니다.
     */
    private void handleClientMessage(String rawMessage) {
        String[] parts = rawMessage.split("\\" + Protocol.DELIMITER, 3);
        if (parts.length < 3) return;

        String type = parts[0];
        String dataPart = parts[2];

        switch (type) {
            case Protocol.LOGIN_REQ:
                handleLoginRequest(dataPart);
                break;
            case Protocol.ROOM_LIST_REQ: // 방 목록 요청 처리 (추가)
                handleRoomListRequest();
                break;
            case Protocol.ROOM_CREATE_REQ: // 방 생성 요청 처리 (추가)
                handleRoomCreateRequest(dataPart);
                break;
            case Protocol.ROOM_JOIN_REQ: // 방 입장 요청 처리 (추가)
                handleRoomJoinRequest(dataPart);
                break;
            case Protocol.GAME_READY: // 게임 준비/해제 처리 (추가)
                handleGameReadyRequest(dataPart);
                break;
            case Protocol.ROOM_LEAVE_REQ: // 방 나가기 요청 처리 (추가)
                handleRoomLeaveRequest();
                break;
            case Protocol.WORD_INPUT: // 단어 입력 요청 처리 (추가)
                handleWordInputRequest(dataPart);
                break;
            case Protocol.GAME_START_REQ:
                handleGameStartRequest(dataPart);
                break;
            case Protocol.CHAT_MSG:
                handleChatMessage(dataPart);
                break;
        }
    }

    /**
     * ROOM_LIST_REQ 메시지 처리: 방 목록 응답 전송
     */
    private void handleRoomListRequest() {
        // Server에서 방 목록 문자열을 가져옴
        String roomListString = server.getRoomListProtocolString();

        // ROOM_LIST_RES|데이터길이|room1:...;room2:...
        Map<String, String> responseData = new HashMap<>();
        responseData.put("list", roomListString);

        sendMessage(Protocol.ROOM_LIST_RES, responseData);
    }

    /**
     * LOGIN_REQ 메시지 처리: 플레이어 정보 저장 후 LOGIN_RES 전송
     */
    private void handleLoginRequest(String dataPart) {
        // dataPart 예: playerName:정수연
        String playerName = getAttributeValue(dataPart, "playerName");

        // 간단한 ID 부여 (실제로는 중복 검사 필요)
        this.playerId = "P" + server.getNextPlayerId();
        this.playerName = playerName;

        // 서버에 클라이언트 등록
        server.addClient(this);

        // LOGIN_RES 메시지 생성
        // LOGIN_RES|데이터길이|status:SUCCESS;playerId:P001;playerName:이름
        Map<String, String> responseData = new HashMap<>();
        responseData.put("status", "SUCCESS");
        responseData.put("playerId", this.playerId);
        responseData.put("playerName", this.playerName);

        sendMessage(Protocol.LOGIN_RES, responseData);
    }

    /**
     * ROOM_CREATE_REQ 메시지 처리: 방 생성 후 ROOM_CREATE_RES 전송
     */
    private void handleRoomCreateRequest(String dataPart) {
        String roomName = getAttributeValue(dataPart, "roomName");
        String maxPlayersStr = getAttributeValue(dataPart, "maxPlayers");
        int maxPlayers = Integer.parseInt(maxPlayersStr);

        // 실제 방 생성
        GameRoom newRoom = server.createRoom(roomName, maxPlayers);

        // ROOM_CREATE_RES 응답 생성
        Map<String, String> responseData = new HashMap<>();
        if (newRoom != null) {
            // 성공 응답: ROOM_CREATE_RES|...|status:SUCCESS;roomId:R001
            responseData.put("status", "SUCCESS");
            responseData.put("roomId", newRoom.getRoomId());
        } else {
            // 실패 응답 (예시): ROOM_CREATE_RES|...|status:FAIL;message:서버 오류
            responseData.put("status", "FAIL");
            responseData.put("message", "방 생성 중 알 수 없는 오류 발생");
        }

        sendMessage(Protocol.ROOM_CREATE_RES, responseData);
    }

    /**
     * 데이터 문자열에서 특정 속성 값 추출 (클라이언트 코드와 동일한 단순 파싱 로직)
     */
    private String getAttributeValue(String data, String key) {
        String pattern = key + Protocol.FIELD_SEPARATOR;
        int start = data.indexOf(pattern);
        if (start != -1) {
            start += pattern.length();
            int end = data.indexOf(Protocol.DATA_SEPARATOR, start);
            return (end != -1) ? data.substring(start, end) : data.substring(start);
        }
        return "알 수 없음";
    }

    /**
     * 클라이언트에게 메시지를 전송합니다.
     */
    public void sendMessage(String type, Map<String, String> data) {
        StringBuilder dataStr = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            dataStr.append(entry.getKey())
                    .append(Protocol.FIELD_SEPARATOR)
                    .append(entry.getValue())
                    .append(Protocol.DATA_SEPARATOR);
        }

        String dataPayload = dataStr.toString();
        int length = dataPayload.length();

        String message = type + Protocol.DELIMITER + length + Protocol.DELIMITER + dataPayload;

        System.out.println("서버 전송 [" + playerName + "]: " + message);
        out.println(message);
    }

    // --- 방 입장/퇴장 핸들러 ---

    /**
     * ROOM_JOIN_REQ 처리: 방에 입장시키고 ROOM_JOIN_RES 전송 후 ROOM_UPDATE 브로드캐스트
     */
//    private void handleRoomJoinRequest(String dataPart) {
//        String roomId = getAttributeValue(dataPart, "roomId");
//        int teamNumber = Integer.parseInt(getAttributeValue(dataPart, "team"));
//
//        GameRoom room = server.getRoom(roomId);
//        Map<String, String> responseData = new HashMap<>();
//
//        if (room == null) {
//            responseData.put("status", "FAIL");
//            responseData.put("message", "E007: 존재하지 않는 방입니다.");
//            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
//            return;
//        }
//
//        // 이미 그 방에 내가 들어가 있는 경우 → 그냥 성공 응답만 다시 보내고 끝
//        if (room.hasPlayer(this.playerId)) {
//            this.currentRoom = room; // 혹시나 null이면 다시 세팅
//
//            Player me = room.getPlayers().get(this.playerId);
//            int myTeam = (me != null) ? me.getTeamNumber() : teamNumber;
//
//            responseData.put("status", "SUCCESS");
//            responseData.put("team", String.valueOf(myTeam));
//            responseData.put("players", room.getPlayersProtocolString());
//            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
//            // 이미 방 상태는 최신이라면 broadcast는 생략 가능하지만, 필요하면 유지
//            // server.broadcastRoomUpdate(room);
//            return;
//        }
//
//        // 새로 입장하는 경우만 인원 수 체크
//        if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
//            responseData.put("status", "FAIL");
//            responseData.put("message", "E003: 방이 가득 찼습니다.");
//            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
//            return;
//        }
//
//        // 진짜로 새로 추가
//        Player player = new Player(this.playerId, this.playerName);
//        if (room.addPlayer(player, teamNumber)) {
//            this.currentRoom = room;
//
//            responseData.put("status", "SUCCESS");
//            responseData.put("team", String.valueOf(teamNumber));
//            responseData.put("players", room.getPlayersProtocolString());
//            responseData.put("roomCreatorId", room.getRoomCreatorId());
//            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
//            server.broadcastRoomUpdate(room);
//        } else {
//            responseData.put("status", "FAIL");
//            responseData.put("message", "알 수 없는 오류로 입장에 실패했습니다.");
//            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
//        }
//    }

    private void handleRoomJoinRequest(String dataPart) {
        String roomId = getAttributeValue(dataPart, "roomId");
        String teamStr = getAttributeValue(dataPart, "team");
        int requestedTeam = 0;
        try {
            requestedTeam = Integer.parseInt(teamStr);
        } catch (NumberFormatException ignored) {}

        GameRoom room = server.getRoom(roomId);
        Map<String, String> responseData = new HashMap<>();

        if (room == null) {
            responseData.put("status", "FAIL");
            responseData.put("message", "E007: 존재하지 않는 방입니다.");
            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
            return;
        }

        // 이미 그 방에 내가 들어가 있는 경우  → "팀 변경" 요청으로 처리
        if (room.hasPlayer(this.playerId)) {
            this.currentRoom = room;

            Player me = room.getPlayers().get(this.playerId);
            if (me != null && (requestedTeam == 1 || requestedTeam == 2)) {
                me.setTeamNumber(requestedTeam);   // ★ 팀 변경
            }

            responseData.put("status", "SUCCESS");
            responseData.put("team", String.valueOf(me.getTeamNumber()));
            responseData.put("players", room.getPlayersProtocolString());
            responseData.put("roomCreatorId", room.getRoomCreatorId());
            sendMessage(Protocol.ROOM_JOIN_RES, responseData);

            // 방/로비에 갱신 브로드캐스트
            server.broadcastRoomUpdate(room);
            return;
        }

        // 여기부터는 "새로 들어오는 경우"만
        if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
            responseData.put("status", "FAIL");
            responseData.put("message", "E003: 방이 가득 찼습니다.");
            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
            return;
        }

        // ★★★ 모드(2인/4인)에 따라 자동 팀 배정 ★★★
        int teamNumber;
        int maxPlayers = room.getMaxPlayers();

        if (maxPlayers == 2) {
            // 2인용: 1명 들어와 있으면 무조건 다른 팀으로
            teamNumber = (room.getCurrentPlayers() == 0) ? 1 : 2;
        } else {
            // 4인용(팀전): 인원 수를 보고 균형 맞추기
            int team1Count = 0;
            int team2Count = 0;
            for (Player p : room.getPlayers().values()) {
                if (p.getTeamNumber() == 1) team1Count++;
                else if (p.getTeamNumber() == 2) team2Count++;
            }
            // 더 적은 쪽에 넣기 (2 vs 2로 맞추는 방향)
            teamNumber = (team1Count <= team2Count) ? 1 : 2;
        }

        Player player = new Player(this.playerId, this.playerName);
        if (room.addPlayer(player, teamNumber)) {
            this.currentRoom = room;

            responseData.put("status", "SUCCESS");
            responseData.put("team", String.valueOf(teamNumber));
            responseData.put("players", room.getPlayersProtocolString());
            responseData.put("roomCreatorId", room.getRoomCreatorId());
            sendMessage(Protocol.ROOM_JOIN_RES, responseData);

            server.broadcastRoomUpdate(room);
        } else {
            responseData.put("status", "FAIL");
            responseData.put("message", "알 수 없는 오류로 입장에 실패했습니다.");
            sendMessage(Protocol.ROOM_JOIN_RES, responseData);
        }
    }


    /**
     * GAME_READY 요청 처리: 준비 상태 변경 후 ROOM_UPDATE 브로드캐스트
     */
    private void handleGameReadyRequest(String dataPart) {
        if (currentRoom == null) return;

        boolean isReady = Boolean.parseBoolean(getAttributeValue(dataPart, "ready"));
        currentRoom.setPlayerReady(this.playerId, isReady);

        // 준비 상태 UI 갱신만 브로드캐스트
        server.broadcastRoomUpdate(currentRoom);
    }


    /**
     * ROOM_LEAVE_REQ 처리: 방에서 퇴장 후 ROOM_UPDATE 브로드캐스트
     */
    private void handleRoomLeaveRequest() {
        if (currentRoom == null) return;

        GameRoom roomToLeave = this.currentRoom;
        this.currentRoom = null;

        roomToLeave.removePlayer(this.playerId);

        // 클라이언트에게 ROOM_LEAVE_RES (별도 정의 필요) 대신,
        // 클라이언트에서 UI를 로비로 전환하고 ROOM_UPDATE를 브로드캐스트
        server.broadcastRoomUpdate(roomToLeave);
    }

    /**
     * WORD_INPUT 요청 처리: GameServer로 단어 입력 전달
     */
    private void handleWordInputRequest(String dataPart) {
        if (currentRoom == null) return;

        String wordContent = getAttributeValue(dataPart, "word");

        // GameServer를 통해 GameLogic으로 전달
        server.handleWordInput(currentRoom.getRoomId(), this.playerId, wordContent);
    }

    /**
     * GAME_START_REQ 처리: 방장 + 모두 준비 상태일 때만 게임 시작
     */
    /**
     * GAME_START_REQ 처리: 방장 + 모두 준비 상태일 때만 게임 시작
     */
    private void handleGameStartRequest(String dataPart) {
        if (currentRoom == null) return;

        // 반드시 방장만 시작할 수 있게 체크
        if (!playerId.equals(currentRoom.getRoomCreatorId())) {
            // 방장이 아닌데 시작 요청하면 무시
            return;
        }

        // 모두 ready인지 확인 (한 명이라도 notready면 시작 안 함)
        if (!currentRoom.isAllReady()) {
            // 아직 준비 안 된 사람이 있으면 시작 안 함 (지금은 조용히 무시)
            return;
        }

        // ★★★ 2인용 방이라면 팀 구성 추가 검사 ★★★
        if (currentRoom.getMaxPlayers() == 2) {
            int team1Count = 0;
            int team2Count = 0;

            for (Player p : currentRoom.getPlayers().values()) {
                int t = p.getTeamNumber();
                if (t == 1) team1Count++;
                else if (t == 2) team2Count++;
            }

            // 2명이 모두 들어와 있고, 1팀 1명 / 2팀 1명이 아닌 경우 → 시작 불가
            if (!(team1Count == 1 && team2Count == 1)) {
                Map<String, String> errorData = new HashMap<>();
                errorData.put("code", "E020");
                errorData.put("message",
                        "2인 경기에서는 두 플레이어가 서로 다른 팀을 선택해야 합니다.\n" +
                                "팀을 다시 선택한 뒤 게임을 시작해 주세요.");

                // 이 ClientHandler(= 방장 클라이언트)에게만 팝업 전송
                sendMessage(Protocol.ERROR, errorData);
                return;
            }
        }

        // 여기서만 진짜 게임 시작
        server.startGame(currentRoom);
    }


    /**
     * CHAT_MSG 처리: 현재 방의 모든 플레이어에게 채팅을 브로드캐스트합니다.
     */
    private void handleChatMessage(String dataPart) {
        // 방에 속해있지 않으면 무시
        if (currentRoom == null) {
            return;
        }

        String message = getAttributeValue(dataPart, "message");
        if (message == null || message.trim().isEmpty()
                || "알 수 없음".equals(message)) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("roomId", currentRoom.getRoomId());
        data.put("senderId", this.playerId);
        data.put("senderName", this.playerName);
        data.put("message", message);

        // 같은 방 모든 인원에게 브로드캐스트
        server.broadcastToRoom(currentRoom, Protocol.CHAT_MSG, data);
    }



    // Getter 메소드
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public GameRoom getCurrentRoom() { return currentRoom; }


}