package server;

import common.Player;
import common.Protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRoom {
    private static final AtomicInteger roomIdCounter = new AtomicInteger(0);
    private final String roomId;
    private final String roomName;
    private final int maxPlayers;
    private int currentPlayers;
    private String state; // waiting, playing
    // 방 안에 있는 플레이어 목록 (ID -> Player 객체)
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private String roomCreatorId; // 방장 ID

    public GameRoom(String roomName, int maxPlayers) {
        this.roomId = "R" + roomIdCounter.incrementAndGet();
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = 0;
        this.state = "waiting"; // 초기 상태
        this.roomCreatorId = null; // 초기 방장 없음
    }

    // --- Getter 메소드 ---
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCurrentPlayers() { return currentPlayers; }
    public String getState() { return state; }

    public void startGame() {
        this.state = "playing";
    }

    // --- 플레이어 관리 로직 ---

    /**
     * 플레이어를 방에 추가하고 방장 설정 및 teamNumber 설정
     */
    public boolean addPlayer(Player player, int teamNumber) {
        if (currentPlayers >= maxPlayers) return false;
        if (players.containsKey(player.getPlayerId())) return false;

        player.setTeamNumber(teamNumber);

        if (currentPlayers == 0) { // 첫 번째 입장 플레이어를 방장으로 설정
            player.setRoomCreator(true);
            roomCreatorId = player.getPlayerId();
        }
        players.put(player.getPlayerId(), player);
        currentPlayers++;
        return true;
    }

    /**
     * 플레이어를 방에서 제거
     */
    public void removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            currentPlayers--;

            // 방장이 나간 경우 새 방장 지정
            if (playerId.equals(roomCreatorId)) {
                if (!players.isEmpty()) {
                    // 남아있는 플레이어 중 한 명을 새 방장으로
                    Player newOwner = players.values().iterator().next();
                    roomCreatorId = newOwner.getPlayerId();

                    // Player 객체들에 roomCreator 플래그 갱신 (있다면)
                    for (Player p : players.values()) {
                        p.setRoomCreator(p.getPlayerId().equals(roomCreatorId));
                    }
                } else {
                    // 방이 완전히 비었으면 방장 없음
                    roomCreatorId = null;
                }
            }
        }
    }


    // --- 상태 업데이트 로직 ---

    /**
     * 플레이어의 준비 상태 변경
     */
    public void setPlayerReady(String playerId, boolean readyStatus) {
        Player p = players.get(playerId);
        if (p != null) {
            p.setReady(readyStatus);
        }
    }

    /**
     * 방 안의 모든 플레이어가 준비 완료 상태인지 확인합니다.
     */
//    public boolean isAllReady() {
//        if (currentPlayers == 0 || currentPlayers < maxPlayers) {
//            // 인원이 최대 인원(2인용이면 2명)을 채우지 않았다면 시작 불가능
//            return false;
//        }
//        for (common.Player player : players.values()) {
//            if (!player.isReady()) {
//                return false; // 한 명이라도 NOT READY면 false
//            }
//        }
//        return true; // 모두 준비 완료
//    }

    public boolean isAllReady() {
        if (players.isEmpty()) return false;

        for (Player p : players.values()) {
            if (!p.isReady()) {
                return false;
            }
        }
        return true;
    }

    // --- 프로토콜 문자열 생성 ---

    /**
     * ROOM_UPDATE 메시지를 위한 플레이어 목록 문자열 생성
     */

    public String getPlayersProtocolString() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players.values()) {
            if (sb.length() > 0) {
                sb.append(Protocol.FIELD_FIELD_SEPARATOR);
            }
            sb.append(p.toProtocolString());
        }
        return sb.toString();
    }


    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }

    public String getRoomCreatorId() { return roomCreatorId; }
    public Map<String, Player> getPlayers() { return players; }


    /**
     * 방 정보를 프로토콜 문자열 형식으로 반환합니다.
     * 예: R001:즐거운게임방:2/4:waiting
     */
    public String toProtocolString() {
        return roomId + ":" + roomName + ":" + currentPlayers + "/" + maxPlayers + ":" + state;
    }

    /**
     * 게임이 종료된 후 방의 상태를 정리합니다.
     * (플레이어의 ready 상태 초기화, 방 상태를 'waiting'으로 복귀 등)
     */
    public void endGame() { // ⭐ 이 메소드를 추가해야 합니다.
        // 1. 방 상태를 'waiting'으로 복귀
        this.state = "waiting";

        // 2. 모든 플레이어의 준비 상태를 'not ready'로 초기화
        for (common.Player player : players.values()) {
            player.setReady(false);
        }

        // TODO: 필요하다면 채팅 로그 정리 등 추가 작업 수행
        System.out.println("방 [" + roomName + "]의 게임이 종료되고 대기 상태로 돌아왔습니다.");
    }







    // TODO: 플레이어 입장/퇴장 및 상태 변경 로직 추가 필요
}