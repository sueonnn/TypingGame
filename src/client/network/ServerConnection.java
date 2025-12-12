package client.network;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import client.ui.CreateRoomDialog;
import client.ui.MainFrame;
import client.ui.LobbyPanel;
import client.ui.RoomPanel;
import client.ui.GamePanel;
import client.ui.MessageDialog;
import common.Protocol;

public class ServerConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final MainFrame mainFrame; // MainFrame 참조 유지
    private boolean isConnected = false;

    private static final int SERVER_PORT = 12345;

    public ServerConnection(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * 서버에 연결을 시도하고 통신 스트림을 초기화합니다.
     */
    public boolean connect(String serverIp) {
        try {
            socket = new Socket(serverIp, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true;

            // 서버로부터 메시지를 계속 수신할 스레드 시작
            new Thread(this::listenToServer).start();
            return true;
        } catch (Exception e) {
        	MessageDialog.showError(
        	        mainFrame,
        	        "연결 오류",
        	        "서버 연결 실패.\nIP: " + serverIp + ", Port: " + SERVER_PORT
        	);
        	/*
            JOptionPane.showMessageDialog(mainFrame,
                    "서버 연결 실패. IP: " + serverIp + ", Port: " + SERVER_PORT,
                    "연결 오류", JOptionPane.ERROR_MESSAGE);
            */
            isConnected = false;
            return false;
        }
    }

    /**
     * 서버로 데이터를 전송하는 일반화된 메소드 (데이터가 Map 형태일 때)
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

        System.out.println("클라이언트 전송: " + message);
        out.println(message);
    }

    /**
     * 서버로 데이터를 전송하는 오버로드 메소드 (데이터가 없을 때, 예: ROOM_LIST_REQ)
     * 메시지 구조: TYPE|LENGTH|DATA
     */
    public void sendMessage(String type, int length, String data) {
        String message = type + Protocol.DELIMITER + length + Protocol.DELIMITER + data;
        System.out.println("클라이언트 전송: " + message);
        out.println(message);
    }

    /**
     * 서버로 로그인 요청 메시지를 전송합니다.
     */
    public void sendLoginRequest(String playerName) {
        if (!isConnected) {
            System.err.println("오류: 서버에 연결되어 있지 않습니다.");
            return;
        }

        // LOGIN_REQ|데이터길이|playerName:이름
        Map<String, String> data = new HashMap<>();
        data.put("playerName", playerName);
        sendMessage(Protocol.LOGIN_REQ, data);
    }

    /**
     * 서버로부터의 응답을 계속 수신하는 스레드 로직
     */
    private void listenToServer() {
        try {
            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                System.out.println("클라이언트 수신: " + rawMessage);
                handleServerMessage(rawMessage);
            }
        } catch (Exception e) {
            System.err.println("서버 연결 끊김: " + e.getMessage());
            isConnected = false;
            mainFrame.updateStatus("연결 끊김");
        }
    }

    /**
     * 서버 메시지를 처리하여 UI를 업데이트하는 로직
     */
    /**
     * 서버 메시지를 처리하여 UI를 업데이트하는 로직
     */
    private void handleServerMessage(String rawMessage) {
        String[] parts = rawMessage.split("\\" + Protocol.DELIMITER, 3);
        if (parts.length < 3) return;

        String type = parts[0];
        String dataPart = parts[2];

        // 1. LOGIN_RES 처리 (성공/실패)
        if (type.equals(Protocol.LOGIN_RES)) {
            // status 필드에서 SUCCESS / FAIL 읽기
            String status = getAttributeValue(dataPart, "status");

            if ("SUCCESS".equals(status)) {
                // 로그인 성공 케이스
                String playerName = getAttributeValue(dataPart, "playerName");
                String playerId   = getAttributeValue(dataPart, "playerId");

                // UI 작업은 EDT에서 실행
                SwingUtilities.invokeLater(() ->
                        mainFrame.handleLoginSuccess(playerName, playerId)
                );

            } else {
                // 로그인 실패 케이스 (닉네임 중복)
                // 서버에서 reason 또는 message 둘 중 하나를 보낸다고 가정
                String reason  = getAttributeValue(dataPart, "reason");   // 예: "DUPLICATE_NAME"
                String message = getAttributeValue(dataPart, "message");  // 예: "닉네임이 이미 존재합니다"

                // reason이 없으면 message를 reason처럼 사용
                String reasonOrMessage = !"알 수 없음".equals(reason) ? reason : message;

                // MainFrame에 실패 처리 위임
                SwingUtilities.invokeLater(() ->
                        mainFrame.handleLoginFailure(reasonOrMessage)
                );
            }
            return;
        }


        // 2. ROOM_LIST_RES 처리
        if (type.equals(Protocol.ROOM_LIST_RES)) {
            if (mainFrame.getCurrentPanel() instanceof LobbyPanel lobbyPanel) {
                SwingUtilities.invokeLater(() -> lobbyPanel.updateRoomList(dataPart));
            }
            return;
        }

        // 3. ROOM_CREATE_RES 처리
        if (type.equals(Protocol.ROOM_CREATE_RES)) {
            String status  = getAttributeValue(dataPart, "status");
            boolean success = status.equals("SUCCESS");
            String message = success
                    ? getAttributeValue(dataPart, "roomId")
                    : getAttributeValue(dataPart, "message");

            // 열려 있는 CreateRoomDialog 찾아서 응답 전달
            for (Window window : Window.getWindows()) {
                if (window instanceof CreateRoomDialog dialog && window.isShowing()) {
                    dialog.handleCreationResponse(success, message);
                    break;
                }
            }
            return;
        }

     // 4. ROOM_JOIN_RES 처리 (성공/실패 안내만)
        if (type.equals(Protocol.ROOM_JOIN_RES)) {
            String status = getAttributeValue(dataPart, "status");
            if (status.equals("SUCCESS")) {
            	 if (mainFrame.getCurrentPanel() instanceof LobbyPanel) {
                     SwingUtilities.invokeLater(() ->
                             MessageDialog.showInfo(
                                     mainFrame,
                                     "알림",
                                     "방 입장 성공!"
                             )
                     );
                 }
            } else {
                String message = getAttributeValue(dataPart, "message");
                SwingUtilities.invokeLater(() ->
                        MessageDialog.showError(
                                mainFrame,
                                "오류",
                                "방 입장 실패: " + message
                        )
                );
            }
            return;
        }

        // 5. ROOM_UPDATE 처리 (방 상태 동기화)
        if (type.equals(Protocol.ROOM_UPDATE)) {
            String playersString = getAttributeValue(dataPart, "players");
            String roomId        = getAttributeValue(dataPart, "roomId");
            String roomName      = getAttributeValue(dataPart, "roomName");
            String roomCreatorId = getAttributeValue(dataPart, "roomCreatorId");

            SwingUtilities.invokeLater(() -> {
                if (mainFrame.getCurrentPanel() instanceof LobbyPanel) {
                    // 로비에서 받으면 RoomPanel로 전환
                    mainFrame.switchToRoom(roomId, roomName, playersString, roomCreatorId);
                } else if (mainFrame.getCurrentPanel() instanceof RoomPanel roomPanel) {
                    // 방 화면에서는 목록만 갱신
                    roomPanel.updateRoomState(roomId, roomName, playersString, roomCreatorId);
                }
            });
            return;
        }

        // 6. GAME_START 처리 (초기 보드 + 제한 시간 → WaitingPanel로)
        if (type.equals(Protocol.GAME_START)) {
            // 서버는 board:..., timeLimit:... 을 보냄
            String boardString = getAttributeValue(dataPart, "board");
            try {
                int timeLimit = Integer.parseInt(getAttributeValue(dataPart, "timeLimit"));
                // 3초 카운트다운용 WaitingPanel로 먼저 전환
                SwingUtilities.invokeLater(() -> mainFrame.switchToWaiting(boardString, timeLimit));
            } catch (NumberFormatException ignored) {}
            return;
        }


        // 7. WORD_CAPTURE 처리 (특정 인덱스 단어를 누가 뺏었는지)
//        if (type.equals(Protocol.WORD_CAPTURE)) {
//            if (mainFrame.getCurrentPanel() instanceof GamePanel gamePanel) {
//                try {
//                    int index      = Integer.parseInt(getAttributeValue(dataPart, "wordIndex"));
//                    int team       = Integer.parseInt(getAttributeValue(dataPart, "team"));
//                    String capturedBy = getAttributeValue(dataPart, "capturedBy");
//                    int points     = Integer.parseInt(getAttributeValue(dataPart, "points"));
//
//                    SwingUtilities.invokeLater(() ->
//                            gamePanel.updateWordCapture(index, team, capturedBy, points)
//                    );
//                } catch (NumberFormatException ignored) {}
//            }
//            return;
//        }

        // 8. GAME_UPDATE 처리 (보드/점수/남은시간 동기화)
        if (type.equals(Protocol.GAME_UPDATE)) {
            if (mainFrame.getCurrentPanel() instanceof GamePanel gamePanel) {
                try {
                    String boardString = getAttributeValue(dataPart, "board");
                    int score1   = Integer.parseInt(getAttributeValue(dataPart, "score1"));
                    int score2   = Integer.parseInt(getAttributeValue(dataPart, "score2"));
                    int timeLeft = Integer.parseInt(getAttributeValue(dataPart, "timeLeft"));

                    SwingUtilities.invokeLater(() ->
                            gamePanel.updateGameState(boardString, score1, score2, timeLeft)
                    );
                } catch (NumberFormatException ignored) {}
            }
            return;
        }



        // 9. GAME_END 처리 (게임 종료 → GameEndPanel)
        if (type.equals(Protocol.GAME_END)) {
            try {
                String winner = getAttributeValue(dataPart, "winner");      // "1", "2", "DRAW"

                int score1 = Integer.parseInt(getAttributeValue(dataPart, "score1"));
                int score2 = Integer.parseInt(getAttributeValue(dataPart, "score2"));
                String mvp = getAttributeValue(dataPart, "mvp");

                SwingUtilities.invokeLater(() ->
                        mainFrame.switchToGameEnd(winner, score1, score2, mvp)
                );
            } catch (NumberFormatException ignored) {}
            return;
        }

        // ★ 9-1. CHAT_MSG 처리 (방 채팅)
        if (type.equals(Protocol.CHAT_MSG)) {
            String senderName = getAttributeValue(dataPart, "senderName");
            String message    = getAttributeValue(dataPart, "message");

            SwingUtilities.invokeLater(() -> {
                if (mainFrame.getCurrentPanel() instanceof RoomPanel roomPanel) {
                    roomPanel.appendChatMessage(senderName, message);
                }
            });
            return;
        }


        // 10. ERROR 처리
        if (type.equals(Protocol.ERROR)) {
            String code    = getAttributeValue(dataPart, "code");
            String message = getAttributeValue(dataPart, "message");
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "오류 [" + code + "]: " + message,
                            "게임 오류",
                            JOptionPane.ERROR_MESSAGE
                    )
            );
            return;
        }

        // 그 외 타입은 일단 무시
    }


    /**
     * 데이터 문자열에서 특정 속성 값 추출 (단순 파싱)
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


}