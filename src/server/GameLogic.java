// server/GameLogic.java
package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class GameLogic {

    static final int GAME_DURATION = 60; // 60초 게임

    private final GameRoom room;
    private final List<Card> cards = new ArrayList<>();
    private final ScheduledExecutorService scheduler;
    private final GameServer server;

    private ScheduledFuture<?> future;
    private int remainingSeconds = GAME_DURATION;

    GameLogic(GameRoom room,
              List<String> words,
              ScheduledExecutorService scheduler,
              GameServer server) {

        this.room = room;
        this.scheduler = scheduler;
        this.server = server;

        // 30개까지만 사용
        int limit = Math.min(30, words.size());
        for (int i = 0; i < limit; i++) {
            int team = (i < 15) ? 1 : 2; // 처음 15개는 팀1(핑크), 나머지 15개는 팀2(연두)
            cards.add(new Card(words.get(i), team));
        }
    }

    void start() {
        future = scheduler.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                if (future != null) future.cancel(false);
                server.onGameEnd(this);
            } else {
                server.onGameTick(this);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 플레이어가 단어를 입력했을 때 처리.
     * 해당 단어가 있으면 ownerTeam 을 입력한 사람의 팀으로 변경.
     */
    boolean applyWord(int team, String word) {
        String target = word.trim();
        if (target.isEmpty()) return false;

        for (Card c : cards) {
            if (c.word.equalsIgnoreCase(target)) {
                if (c.ownerTeam == team) {
                    // 이미 내 팀 색이면 변화 없음
                    return false;
                }
                c.ownerTeam = team;
                return true;
            }
        }
        return false;
    }

    /**
     * 서버 → 클라이언트로 보내는 보드 상태 문자열
     * 예: "사과,1/바나나,2/포도,1/..."
     */
    String toBoardString() {
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) {
            if (sb.length() > 0) sb.append('/');
            sb.append(c.word).append(',').append(c.ownerTeam);
        }
        return sb.toString();
    }

    int getScore1() {
        int cnt = 0;
        for (Card c : cards) {
            if (c.ownerTeam == 1) cnt++;
        }
        return cnt;
    }

    int getScore2() {
        int cnt = 0;
        for (Card c : cards) {
            if (c.ownerTeam == 2) cnt++;
        }
        return cnt;
    }

    int getRemainingSeconds() {
        return remainingSeconds;
    }

    GameRoom getRoom() {
        return room;
    }

    private static class Card {
        final String word;
        int ownerTeam;

        Card(String word, int ownerTeam) {
            this.word = word;
            this.ownerTeam = ownerTeam;
        }
    }
}
