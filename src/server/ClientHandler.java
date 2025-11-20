package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import common.Protocol;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private String playerName;

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
            // 연결 종료 시 처리
            server.removeClient(this);
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

    // Getter 메소드
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
}