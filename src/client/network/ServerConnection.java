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
import client.ui.LobbyPanel; // LobbyPanel 임포트
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
            JOptionPane.showMessageDialog(mainFrame,
                    "서버 연결 실패. IP: " + serverIp + ", Port: " + SERVER_PORT,
                    "연결 오류", JOptionPane.ERROR_MESSAGE);
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
    private void handleServerMessage(String rawMessage) {
        String[] parts = rawMessage.split("\\" + Protocol.DELIMITER, 3);
        if (parts.length < 3) return;
        String type = parts[0];
        String dataPart = parts[2];

        // 1. LOGIN_RES 처리
        if (type.equals(Protocol.LOGIN_RES)) {
            if (dataPart.contains("status" + Protocol.FIELD_SEPARATOR + "SUCCESS")) {
                String playerName = getAttributeValue(dataPart, "playerName");
                String playerId = getAttributeValue(dataPart, "playerId"); // ID 추출
                mainFrame.handleLoginSuccess(playerName, playerId);
            } else {
                String message = getAttributeValue(dataPart, "message");
                mainFrame.updateStatus("로그인 실패: " + message);
            }
            return;
        }

        // 2. ROOM_LIST_RES 처리
        if (type.equals(Protocol.ROOM_LIST_RES)) {
            // 로비 패널이 현재 화면에 있는지 확인하고 업데이트 요청
            if (mainFrame.getCurrentPanel() instanceof LobbyPanel) {
                LobbyPanel lobby = (LobbyPanel) mainFrame.getCurrentPanel();
                // Swing 스레드에서 UI 업데이트 요청
                SwingUtilities.invokeLater(() -> lobby.updateRoomList(dataPart));
            }
            return;
        }
        // 3. ROOM_CREATE_RES 처리 (추가)
        if (type.equals(Protocol.ROOM_CREATE_RES)) {
            String status = getAttributeValue(dataPart, "status");
            boolean success = status.equals("SUCCESS");
            String message = success ? getAttributeValue(dataPart, "roomId") : getAttributeValue(dataPart, "message");

            // 현재 열려 있는 CreateRoomDialog를 찾아 응답 처리 (단순화된 접근)
            Window[] windows = Window.getWindows();
            for (Window window : windows) {
                if (window instanceof CreateRoomDialog && window.isShowing()) {
                    ((CreateRoomDialog) window).handleCreationResponse(success, message);
                    break;
                }
            }
            return;
        }

        // TODO: 다른 메시지 타입 (ROOM_JOIN_RES, GAME_START 등) 처리 로직 추가
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