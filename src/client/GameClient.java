package client;

import client.ui.MainFrame;

public class GameClient {
    public static void main(String[] args) {
        // UI 스레드에서 MainFrame 실행
        new MainFrame(new GameClient());
    }

    // TODO: MainFrame에서 로비 화면으로 전환할 때 사용할 정적/인스턴스 메소드 추가
    // public static void switchToLobby(String playerName) { ... }
}