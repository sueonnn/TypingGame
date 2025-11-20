package server;

import common.Protocol;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.ArrayList; // 추가
import java.util.List;     // 추가
import java.util.HashMap;  // 추가

public class GameServer {
    private static final int PORT = 12345;

    // 현재 접속 중인 클라이언트 핸들러 목록
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    // 방 목록 관리
    private final Map<String, GameRoom> rooms = Collections.synchronizedMap(new HashMap<>());

    // 플레이어 ID 생성을 위한 카운터
    private final AtomicInteger playerIdCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ 서버 시작됨. Port: " + PORT);

            while (true) {
                //
                Socket clientSocket = serverSocket.accept();
                System.out.println("⭐ 새 클라이언트 연결 수락: " + clientSocket.getInetAddress());

                // 새로운 스레드에서 클라이언트 처리
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("❌ 서버 오류: " + e.getMessage());
        }
    }

    /**
     * 새로운 클라이언트를 목록에 추가합니다. (로그인 성공 시)
     */
    public void addClient(ClientHandler handler) {
        clients.add(handler);
        System.out.println(handler.getPlayerName() + " (" + handler.getPlayerId() + ") 로그인 완료. 현재 접속자 수: " + clients.size());
    }

    /**
     * 연결이 끊어진 클라이언트를 목록에서 제거합니다.
     */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        if (handler.getPlayerId() != null) {
            System.out.println(handler.getPlayerName() + " (" + handler.getPlayerId() + ") 연결 해제. 현재 접속자 수: " + clients.size());
        }
    }

    /**
     * 다음 플레이어 ID를 부여합니다.
     */
    public int getNextPlayerId() {
        return playerIdCounter.incrementAndGet();
    }

    /**
     * 모든 클라이언트에게 메시지를 브로드캐스트합니다. (추후 GAME_UPDATE 등에 사용)
     */
    public void broadcast(String type, Map<String, String> data) {
        for (ClientHandler client : clients) {
            client.sendMessage(type, data);
        }
    }

    /**
     * 현재 모든 방 목록을 프로토콜 응답 형식으로 반환합니다.
     * 예: room1:R001:2/4:waiting;room2:R002:4/4:playing
     */
    public String getRoomListProtocolString() {
        if (rooms.isEmpty()) {
            // 테스트를 위해 임시 방 2개 생성 (서버 시작 시 한 번 실행되어야 함)
            createRoom("즐거운게임방", 4);
            createRoom("네트워크마스터", 2);
        }

        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (GameRoom room : rooms.values()) {
            if (sb.length() > 0) {
                sb.append(Protocol.DATA_SEPARATOR);
            }
            sb.append("room").append(count).append(Protocol.FIELD_SEPARATOR).append(room.toProtocolString());
            count++;
        }
        return sb.toString();
    }

    /**
     * 방을 생성하고 목록에 추가합니다.
     */
    public GameRoom createRoom(String name, int maxPlayers) {
        GameRoom newRoom = new GameRoom(name, maxPlayers);
        rooms.put(newRoom.getRoomId(), newRoom);
        System.out.println("방 생성됨: " + newRoom.getRoomName() + " (" + newRoom.getRoomId() + ")");
        return newRoom;
    }

}