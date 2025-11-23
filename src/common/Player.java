package common;

public class Player {
    private final String playerId;
    private final String playerName;
    private int teamNumber; // 0: 미지정, 1: 팀 1, 2: 팀 2
    private boolean isReady;
    private boolean isRoomCreator; // 방장 여부

    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamNumber = 0;
        this.isReady = false;
        this.isRoomCreator = false;
    }

    // --- Getter & Setter ---
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getTeamNumber() { return teamNumber; }
    public boolean isReady() { return isReady; }
    public boolean isRoomCreator() { return isRoomCreator; }

    public void setTeamNumber(int teamNumber) { this.teamNumber = teamNumber; }
    public void setReady(boolean ready) { this.isReady = ready; }
    public void setRoomCreator(boolean isRoomCreator) { this.isRoomCreator = isRoomCreator; }

    /**
     * 프로토콜 전송을 위한 문자열 형식 반환
     * 예: P001:정수연:1:ready
     */
    public String toProtocolString() {
        String readyStatus = isReady ? "ready" : "notready";
        return playerId + Protocol.FIELD_SEPARATOR + playerName + Protocol.FIELD_SEPARATOR + teamNumber + Protocol.FIELD_SEPARATOR + readyStatus;
    }
}