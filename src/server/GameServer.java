package server;

import common.Protocol;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import common.WordPool;
import common.Player;
import java.util.concurrent.*;


public class GameServer {
    private static final int PORT = 12345;

    // 현재 접속 중인 클라이언트 핸들러 목록
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    // 방 목록 관리
    private final Map<String, GameRoom> rooms = Collections.synchronizedMap(new HashMap<>());

    // 게임 로직 인스턴스 저장 (Room ID -> GameLogic)
    private final Map<String, GameLogic> activeGames = new HashMap<>();

    // 플레이어 ID 생성을 위한 카운터
    private final AtomicInteger playerIdCounter = new AtomicInteger(0);

    private final WordPool wordPool = new WordPool();              // 단어 풀
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);                   // 게임 타이머용
    private final ConcurrentHashMap<String, GameLogic> games =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public ClientHandler getClientById(String playerId) {
        synchronized (clients) { // synchronizedSet 이라 순회할 땐 동기화 필요
            for (ClientHandler ch : clients) {
                if (playerId.equals(ch.getPlayerId())) {
                    return ch;  // 찾으면 바로 반환
                }
            }
        }
        return null; // 못 찾으면 null
    }

    public boolean isPlayerNameTaken(String name) {
        if (name == null) return false;

        synchronized (clients) {
            for (ClientHandler ch : clients) {
                String existing = ch.getPlayerName();
                if (name.equals(existing)) {
                    return true;
                }
            }
        }
        return false;
    }




    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버 시작됨. Port: " + PORT);

            while (true) {
                //
                Socket clientSocket = serverSocket.accept();
                System.out.println(" 새 클라이언트 연결 수락: " + clientSocket.getInetAddress());

                // 새로운 스레드에서 클라이언트 처리
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println(" 서버 오류: " + e.getMessage());
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
     * 특정 방의 모든 멤버에게 ROOM_UPDATE 메시지를 전송합니다.
     */
    public void broadcastRoomUpdate(GameRoom room) {
        Map<String, String> data = new HashMap<>();
        data.put("roomId", room.getRoomId());
        data.put("roomName", room.getRoomName());
        data.put("players", room.getPlayersProtocolString());
        data.put("roomCreatorId", room.getRoomCreatorId());

        for (ClientHandler ch : room.getPlayers().values().stream()
                .map(p -> getClientById(p.getPlayerId()))
                .filter(Objects::nonNull)
                .toList()) {

            ch.sendMessage(Protocol.ROOM_UPDATE, data);
        }
    }



    /**
     * 다음 플레이어 ID를 부여합니다.
     */
    public int getNextPlayerId() {
        return playerIdCounter.incrementAndGet();
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

    // --- GAME START & END 핸들러 ---

    /**
     * 게임을 시작하고 GAME_START 메시지를 브로드캐스트합니다.
     */
    public void startGame(GameRoom room) {
        String roomId = room.getRoomId();

        // 이미 게임 중이면 무시
        if (games.containsKey(roomId)) return;

        // 1) 단어 30개 랜덤 뽑기
        java.util.List<String> words = wordPool.getRandomWords(30);
        if (words.size() < 30) {
            System.out.println("경고: 단어가 30개보다 적습니다. size=" + words.size());
        }

        // 2) GameLogic 생성
        GameLogic logic = new GameLogic(room, words, scheduler, this);
        games.put(roomId, logic);

        // 3) 방 상태 playing 으로
        room.startGame();

        // 4) ROOM_UPDATE 로 상태 갱신 (로비/방 UI 표현용)
        broadcastRoomUpdate(room);

        // 5) GAME_START 브로드캐스트 (초기 보드 + 제한시간)
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", roomId);
        data.put("board", logic.toBoardString());                    // "사과,1/바나나,2/..."
        data.put("timeLimit", String.valueOf(GameLogic.GAME_DURATION));

        broadcastToRoom(room, common.Protocol.GAME_START, data);

        // 6) 타이머 시작
        logic.start();
    }


    void broadcastToRoom(GameRoom room, String type, java.util.Map<String, String> data) {
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                GameRoom cr = ch.getCurrentRoom();
                if (cr != null && cr.getRoomId().equals(room.getRoomId())) {
                    ch.sendMessage(type, data);
                }
            }
        }
    }

    /**
     * GAME_END 메시지를 브로드캐스트합니다.
     */
    public void broadcastGameEnd(GameRoom room, int score1, int score2) {
        Map<String, String> data = new HashMap<>();
        data.put("winner", score1 > score2 ? "1" : (score2 > score1 ? "2" : "0"));
        data.put("team1Score", String.valueOf(score1));
        data.put("team2Score", String.valueOf(score2));
        // TODO: mvp:P001:15 추가

        broadcast(room, Protocol.GAME_END, data);
        activeGames.remove(room.getRoomId()); // 게임 종료 후 로직 제거
    }

    // --- 실시간 업데이트 브로드캐스트 ---

    /**
     * WORD_CAPTURE 메시지를 브로드캐스트합니다.
     */
    public void broadcastWordCapture(GameRoom room, int wordIndex, int team, String capturedBy, int points) {
        Map<String, String> data = new HashMap<>();
        data.put("wordIndex", String.valueOf(wordIndex));
        data.put("team", String.valueOf(team));
        data.put("capturedBy", capturedBy);
        data.put("points", String.valueOf(points));

        broadcast(room, Protocol.WORD_CAPTURE, data);
    }

    /**
     * GAME_STATE 메시지를 브로드캐스트합니다.
     */
    public void broadcastGameState(GameRoom room, int time, int score1, int score2, String wordStates) {
        Map<String, String> data = new HashMap<>();
        data.put("time", String.valueOf(time));
        data.put("team1Score", String.valueOf(score1));
        data.put("team2Score", String.valueOf(score2));
        data.put("wordStates", wordStates);

        broadcast(room, Protocol.GAME_STATE, data);
    }

    /**
     * 특정 방의 모든 멤버에게 메시지를 전송합니다.
     */
    public void broadcast(GameRoom room, String type, Map<String, String> data) {

        // Player 객체의 ID를 가져와서, GameServer의 전체 클라이언트 목록(clients)에서 해당 ClientHandler를 찾습니다.
        for (common.Player playerInRoom : room.getPlayers().values()) {

            String targetPlayerId = playerInRoom.getPlayerId();

            // GameServer에 연결된 모든 ClientHandler를 순회합니다.
            for (ClientHandler clientHandler : clients) {

                // Player ID가 일치하는 ClientHandler를 찾습니다.
                if (clientHandler.getPlayerId().equals(targetPlayerId)) {

                    clientHandler.sendMessage(type, data);
                    break; // 찾았으니 다음 플레이어로 이동
                }
            }
        }
    }
    // --- WORD_INPUT 핸들러 ---

    /**
     * ClientHandler가 WORD_INPUT을 받으면 호출하는 메소드
     */
    // GameServer 안에 추가
    public void handleWordInput(String roomId, String playerId, String wordContent) {
        GameLogic logic = games.get(roomId);
        if (logic == null) {
            return; // 게임 안 열려있으면 무시
        }

        GameRoom room = logic.getRoom();
        Player player = room.getPlayers().get(playerId);
        if (player == null) return;

        int team = player.getTeamNumber();
        boolean changed = logic.applyWord(team, wordContent);
        if (!changed) return; // 보드 변화 없으면 굳이 브로드캐스트 안 함

        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", roomId);
        data.put("board", logic.toBoardString());
        data.put("score1", String.valueOf(logic.getScore1()));
        data.put("score2", String.valueOf(logic.getScore2()));
        data.put("timeLeft", String.valueOf(logic.getRemainingSeconds()));

        broadcastToRoom(room, common.Protocol.GAME_UPDATE, data);
    }


    public void onGameTick(GameLogic logic) {
        GameRoom room = logic.getRoom();

        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", room.getRoomId());
        data.put("board", logic.toBoardString());
        data.put("score1", String.valueOf(logic.getScore1()));
        data.put("score2", String.valueOf(logic.getScore2()));
        data.put("timeLeft", String.valueOf(logic.getRemainingSeconds()));

        broadcastToRoom(room, common.Protocol.GAME_UPDATE, data);
    }

    public void onGameEnd(GameLogic logic) {
        GameRoom room = logic.getRoom();
        String roomId = room.getRoomId();

        games.remove(roomId);

        int score1 = logic.getScore1();
        int score2 = logic.getScore2();
        String winner;
        if (score1 > score2) winner = "1";
        else if (score2 > score1) winner = "2";
        else winner = "DRAW"; // 무승부

        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomId", roomId);
        data.put("winner", winner);
        data.put("score1", String.valueOf(score1));
        data.put("score2", String.valueOf(score2));

        broadcastToRoom(room, common.Protocol.GAME_END, data);

        // 방 상태 waiting 으로 + ready 리셋
        room.endGame();
        broadcastRoomUpdate(room);
    }

}