package common;

import java.io.Serializable;

public class Word implements Serializable {
    private final int index; // 단어 목록 내 인덱스 (통신 시 참조용)
    private final String content; // 실제 단어 내용
    private final int points; // 해당 단어를 점령했을 때 얻는 점수
    private int capturedByTeam; // 0: 미점령, 1: 팀 1, 2: 팀 2

    public Word(int index, String content, int points) {
        this.index = index;
        this.content = content;
        this.points = points;
        this.capturedByTeam = 0; // 초기 상태: 미점령
    }

    // --- Getter & Setter ---
    public int getIndex() { return index; }
    public String getContent() { return content; }
    public int getPoints() { return points; }
    public int getCapturedByTeam() { return capturedByTeam; }

    public void setCapturedByTeam(int capturedByTeam) {
        this.capturedByTeam = capturedByTeam;
    }

    /**
     * GAME_START 메시지를 위한 프로토콜 문자열 형식 반환
     * 예: 0:안녕:5
     */
    public String toProtocolString() {
        return index + Protocol.FIELD_SEPARATOR + content + Protocol.FIELD_SEPARATOR + points;
    }

    /**
     * GAME_STATE 메시지를 위한 간략 상태 반환
     * 예: 0:1 (인덱스 0번 단어는 팀 1이 점령)
     */
    public String toStateString() {
        return index + Protocol.FIELD_FIELD_SEPARATOR + capturedByTeam;
    }
}