package server;

import java.util.concurrent.atomic.AtomicInteger;

public class GameRoom {
    private static final AtomicInteger roomIdCounter = new AtomicInteger(0);
    private final String roomId;
    private final String roomName;
    private final int maxPlayers;
    private int currentPlayers;
    private String state; // waiting, playing

    public GameRoom(String roomName, int maxPlayers) {
        this.roomId = "R" + roomIdCounter.incrementAndGet();
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = 0;
        this.state = "waiting"; // 초기 상태
    }

    // --- Getter 메소드 ---
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCurrentPlayers() { return currentPlayers; }
    public String getState() { return state; }

    /**
     * 방 정보를 프로토콜 문자열 형식으로 반환합니다.
     * 예: R001:즐거운게임방:2/4:waiting
     */
    public String toProtocolString() {
        return roomId + ":" + roomName + ":" + currentPlayers + "/" + maxPlayers + ":" + state;
    }

    // TODO: 플레이어 입장/퇴장 및 상태 변경 로직 추가 필요
}