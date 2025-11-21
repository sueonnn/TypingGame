package client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import client.network.ServerConnection;
import common.Protocol;

public class RoomPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ServerConnection connection;
    private final String myPlayerId;

    // A. 방 정보
    private JLabel roomInfoLabel;

    // B. 플레이어 목록
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;

    // C. 팀 선택
    private JButton team1Button;
    private JButton team2Button;

    // D. 준비/시작 버튼
    private JButton readyButton;

    // E. 채팅창
    private JTextArea chatArea;

    // 상태 변수
    private String currentRoomId;
    private String currentRoomName;
    private boolean isReady = false;
    private boolean isRoomCreator = false;
    private String roomCreatorId;

    public RoomPanel(MainFrame mainFrame, ServerConnection connection, String myPlayerId) {
        this.mainFrame = mainFrame;
        this.connection = connection;
        this.myPlayerId = myPlayerId;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 1. 북쪽: 방 정보 (A)
        roomInfoLabel = new JLabel("방 정보 로딩 중...", SwingConstants.CENTER);
        roomInfoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        add(roomInfoLabel, BorderLayout.NORTH);

        // 2. 중앙: 플레이어 목록 및 채팅 (B, E)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // B. 플레이어 목록
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setBorder(BorderFactory.createTitledBorder("플레이어 목록 (닉네임 | 팀 | 상태)"));
        centerPanel.add(new JScrollPane(playerList));

        // E. 채팅창 (임시)
        chatArea = new JTextArea(10, 20);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("채팅"));
        centerPanel.add(chatScrollPane);

        add(centerPanel, BorderLayout.CENTER);

        // 3. 남쪽: 제어판 (C, D, F)
        JPanel controlPanel = new JPanel(new BorderLayout());

        // C. 팀 선택 영역
        JPanel teamPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        team1Button = new JButton("팀 1 선택");
        team2Button = new JButton("팀 2 선택");
        teamPanel.setBorder(BorderFactory.createTitledBorder("팀 선택"));
        teamPanel.add(team1Button);
        teamPanel.add(team2Button);

        // D, F. 준비/시작 및 나가기 버튼
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        readyButton = new JButton("준비 완료");
        JButton leaveButton = new JButton("방 나가기");

        // 액션 리스너 등록
        readyButton.addActionListener(e -> toggleReadyOrStart());
        leaveButton.addActionListener(e -> requestLeaveRoom());
        team1Button.addActionListener(e -> requestTeamChange(1));
        team2Button.addActionListener(e -> requestTeamChange(2));

        actionPanel.add(readyButton);
        actionPanel.add(leaveButton);

        controlPanel.add(teamPanel, BorderLayout.WEST);
        controlPanel.add(actionPanel, BorderLayout.EAST);

        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * 서버로부터 받은 방 입장 성공 정보로 UI를 초기화합니다.
     */
    public void initializeRoom(String roomId, String roomName, String playersString, String roomCreatorId) {
        this.currentRoomId = roomId;
        this.currentRoomName = roomName;
        this.roomCreatorId = roomCreatorId;
        updatePlayerList(playersString, roomName, true);
    }

    /**
     * ROOM_UPDATE 메시지를 받아 플레이어 목록을 갱신합니다.
     */
    public void updateRoomState(String roomId, String roomName, String playersString, String roomCreatorId) {
        if (!roomId.equals(this.currentRoomId)) return;
        this.currentRoomName = roomName;
        this.roomCreatorId = roomCreatorId;
        updatePlayerList(playersString, roomName, false);
    }

    private void updatePlayerList(String playersString, String roomName, boolean isInitialLoad) {
        playerListModel.clear();
        isRoomCreator = false;
        int readyCount = 0;
        int totalPlayers = 0;

        String[] playerEntries = playersString.split(Protocol.DATA_SEPARATOR);

        for (String entry : playerEntries) {
            String[] details = entry.split(Protocol.FIELD_SEPARATOR);
            if (details.length < 4) continue;

            String id = details[0];
            String name = details[1];
            String team = details[2];
            String status = details[3];
            totalPlayers++;

            if (status.equals("ready")) {
                readyCount++;
            }

            // 내 상태 업데이트
            if (id.equals(myPlayerId)) {
                isReady = status.equals("ready");
                isRoomCreator = id.equals(roomCreatorId);
            }

            String display = String.format("%s [팀 %s] (%s)", name, team, status.toUpperCase());
            if (id.equals(roomCreatorId)) {
                display += " ⭐(방장)";
            }
            playerListModel.addElement(display);
        }

        roomInfoLabel.setText(roomName + " - " + totalPlayers + "명 / " + "최대 인원");

        updateReadyButtonState(readyCount == totalPlayers);
    }


    private void updateReadyButtonState(boolean allReady) {
        if (isRoomCreator) {
            // 방장일 때
            if (allReady) {
                // 모두 준비 완료 → 게임 시작 가능
                readyButton.setText("게임 시작");
            } else {
                // 아직 다 안 됐어도 방장은 자기 준비 토글 가능
                readyButton.setText(isReady ? "준비 해제" : "준비 완료");
            }
            readyButton.setEnabled(true);
        } else {
            // 일반 플레이어
            readyButton.setText(isReady ? "준비 해제" : "준비 완료");
            readyButton.setEnabled(true);
        }
    }


    /** 준비 상태를 토글하거나, 방장이면 게임 시작을 누르는 메소드 */
    private void toggleReadyOrStart() {
        // 방장이고 버튼 텍스트가 "게임 시작"이면, 이제는 READY가 아니라 GAME_START_REQ 전송
        if (isRoomCreator && "게임 시작".equals(readyButton.getText())) {
            Map<String, String> data = new HashMap<>();
            data.put("roomId", currentRoomId);
            // playerId를 굳이 보낼 필요는 없지만, 보내도 되고 안 보내도 됨 (서버에서 this.playerId 사용)
            data.put("playerId", myPlayerId);

            connection.sendMessage(Protocol.GAME_START_REQ, data);
            return;
        }

        // 그 외(일반적인 준비/해제 토글)
        isReady = !isReady;
        sendReadyRequest(isReady);
    }

    /**
     * GAME_READY 메시지를 서버에 전송합니다.
     */
    private void sendReadyRequest(boolean status) {
        Map<String, String> data = new HashMap<>();
        data.put("playerId", myPlayerId);
        data.put("ready", String.valueOf(status)); // true 또는 false 전송

        connection.sendMessage(Protocol.GAME_READY, data);
    }

    /**
     * 팀 변경 요청을 서버에 전송합니다.
     */
    private void requestTeamChange(int newTeam) {
        if (isReady) {
            JOptionPane.showMessageDialog(this, "준비 상태에서는 팀을 변경할 수 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // ROOM_JOIN_REQ를 재전송하여 팀 변경 요청
        Map<String, String> data = new HashMap<>();
        data.put("roomId", currentRoomId);
        data.put("team", String.valueOf(newTeam));
        connection.sendMessage(Protocol.ROOM_JOIN_REQ, data);
    }

    /**
     * 방 나가기 요청을 서버에 전송하고 로비로 전환합니다.
     */
    private void requestLeaveRoom() {
        if (JOptionPane.showConfirmDialog(this, "정말로 방을 나가시겠습니까?", "방 나가기", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            // ROOM_LEAVE_REQ|데이터길이|playerId:P001
            Map<String, String> data = new HashMap<>();
            data.put("playerId", myPlayerId);
            connection.sendMessage(Protocol.ROOM_LEAVE_REQ, data);

            // 서버의 응답을 기다리지 않고 즉시 UI를 로비로 전환
            mainFrame.switchToLobby();
        }
    }
}