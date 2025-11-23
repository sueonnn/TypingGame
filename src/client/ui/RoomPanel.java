package client.ui;

import client.network.ServerConnection;
import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ServerConnection connection;
    private final String myPlayerId;

    // ===== 상태 =====
    private String currentRoomId;
    private String currentRoomName;
    private String roomCreatorId;
    private boolean isReady = false;       // 나의 준비 상태
    private boolean isRoomCreator = false; // 내가 방장인지 여부

    // ===== 상단 바 =====
    private JLabel titleLabel;

    // ===== 캐릭터 슬롯 (4칸) =====
    private JLabel[] avatarLabels = new JLabel[4];
    private JLabel[] nameLabels   = new JLabel[4];
    private JLabel[] teamLabels   = new JLabel[4];
    private JLabel[] readyLabels  = new JLabel[4];

    // ===== 버튼 =====
    private RoundButton team1Button;
    private RoundButton team2Button;
    private RoundButton readyButton;
    private RoundButton exitButton;

    // ===== 채팅 =====
    private JTextArea chatArea;
    private JTextField chatInputField;

    public RoomPanel(MainFrame mainFrame, ServerConnection connection, String myPlayerId) {
        this.mainFrame = mainFrame;
        this.connection = connection;
        this.myPlayerId = myPlayerId;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // ==== 1. 배경 패널 (칠판 전체) ====
        Image bgImage = new ImageIcon(
                getClass().getResource("/tg_start1.png")
        ).getImage();

        BackgroundPanel root = new BackgroundPanel(bgImage);
        root.setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        // ==== 2. 상단 정보 바 (LobbyPanel과 동일한 느낌) ====
        JPanel topBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(10, 5, w - 20, h - 10, 30, 30);

                g2.dispose();
            }
        };
        topBar.setOpaque(false);
        topBar.setLayout(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 80, 10, 80));

        titleLabel = new JLabel("방 제목 - 참여 인원 : 0명", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.SUBTITLE_FONT);
        titleLabel.setForeground(Color.DARK_GRAY);

        topBar.add(titleLabel, BorderLayout.CENTER);
        root.add(topBar, BorderLayout.NORTH);

        // ==== 3. 칠판 영역 (캐릭터 + 버튼 + 채팅) ====
        JPanel boardLayer = new JPanel(null); // 절대 좌표 배치
        boardLayer.setOpaque(false);
        root.add(boardLayer, BorderLayout.CENTER);

        // ===== 3-1. 캐릭터 4칸 =====
        // 1600x900 기준, 칠판 안에서 좌우 균형 맞추기
        int[] slotX = {200, 400, 600, 800};
        int slotY = 200;
        int charW = 200;
        int charH = 230;

        String[] boogiePaths = {
                "/boogie01.png",
                "/boogie02.png",
                "/boogie03.png",
                "/boogie04.png"
        };

        for (int i = 0; i < 4; i++) {
            // 캐릭터 이미지
            ImageIcon icon = null;
            java.net.URL imgUrl = getClass().getResource(boogiePaths[i]);
            if (imgUrl != null) {
                Image img = new ImageIcon(imgUrl).getImage()
                        .getScaledInstance(charW, charH, Image.SCALE_SMOOTH);
                icon = new ImageIcon(img);
            }

            JLabel avatar = new JLabel(icon);
            avatar.setBounds(slotX[i], slotY, charW, charH);
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabels[i] = avatar;
            boardLayer.add(avatar);

            // 닉네임
            JLabel name = new JLabel("미참가", SwingConstants.CENTER);
            name.setFont(UITheme.NORMAL_FONT.deriveFont(20f));
            name.setForeground(Color.WHITE);
            name.setBounds(slotX[i], slotY + 240, charW, 30);
            nameLabels[i] = name;
            boardLayer.add(name);

            // 팀 정보
            JLabel team = new JLabel(" ", SwingConstants.CENTER);
            team.setFont(UITheme.NORMAL_FONT.deriveFont(16f));
            team.setForeground(Color.WHITE);
            team.setBounds(slotX[i], slotY + 270, charW, 25);
            teamLabels[i] = team;
            boardLayer.add(team);

            // 준비 상태
            JLabel ready = new JLabel(" ", SwingConstants.CENTER);
            ready.setFont(UITheme.NORMAL_FONT.deriveFont(16f));
            ready.setForeground(Color.WHITE);
            ready.setBounds(slotX[i], slotY + 300, charW, 25);
            readyLabels[i] = ready;
            boardLayer.add(ready);
        }

        // ===== 3-2. 버튼 4개 (하단 중앙) =====
        int buttonY = 570;

        team1Button = new RoundButton("1팀 참가");
        team1Button.setFont(UITheme.BUTTON_FONT);
        team1Button.setBounds(200, 590, 120, 40);
        boardLayer.add(team1Button);

        team2Button = new RoundButton("2팀 참가");
        team2Button.setFont(UITheme.BUTTON_FONT);
        team2Button.setBounds(330, 590, 120, 40);
        boardLayer.add(team2Button);

        readyButton = new RoundButton("준비 완료");
        readyButton.setFont(UITheme.BUTTON_FONT);
        readyButton.setBounds(610, buttonY, 200, 60);
        boardLayer.add(readyButton);

        exitButton = new RoundButton("방 나가기");
        exitButton.setFont(UITheme.BUTTON_FONT);
        exitButton.setBounds(820, buttonY, 200, 60);
        boardLayer.add(exitButton);

        // 버튼 리스너
        team1Button.addActionListener(e -> requestTeamChange(1));
        team2Button.addActionListener(e -> requestTeamChange(2));
        readyButton.addActionListener(e -> toggleReadyOrStart());
        exitButton.addActionListener(e -> requestLeaveRoom());

        // ===== 3-3. 채팅 영역 (오른쪽 칠판 안쪽) =====
        JPanel chatPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 40;

                // 흰색 채팅 배경
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // 진한 테두리
                g2.setColor(new Color(10, 80, 90));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        chatPanel.setOpaque(false);

        // 채팅창 (칠판 오른쪽)
        int chatX = 1030;
        int chatY = 50;
        int chatW = 370;
        int chatH = 585;

        chatPanel.setBounds(chatX, chatY, chatW, chatH);
        boardLayer.add(chatPanel);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(UITheme.NORMAL_FONT.deriveFont(16f));
        chatArea.setForeground(Color.DARK_GRAY);

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);

        // 내부 여백 감안해서 위치 조정
        chatScroll.setBounds(25, 25, chatW - 50, chatH - 130);
        chatPanel.add(chatScroll);

        chatInputField = new JTextField("메시지를 입력하세요");
        chatInputField.setFont(UITheme.NORMAL_FONT.deriveFont(16f));
        chatInputField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(10, 80, 90), 2, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                )
        );
        //chatInputField.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        chatInputField.setBounds(25, chatH - 70, chatW - 50, 45);
        chatPanel.add(chatInputField);

        // 아직 채팅 프로토콜은 없으니, UI만 준비해두고 액션은 나중에 추가 가능
    }

    // ================== 초기화 / 업데이트 ==================

    /**
     * 방 입장 직후 최초 1번 호출되는 초기화 메소드
     */
    public void initializeRoom(String roomId, String roomName, String playersString, String roomCreatorId) {
        this.currentRoomId = roomId;
        this.currentRoomName = roomName;
        this.roomCreatorId = roomCreatorId;
        updatePlayerList(playersString, roomName, true);
    }

    /**
     * ROOM_UPDATE 수신 시마다 호출되어 방 상태를 갱신
     */
    public void updateRoomState(String roomId, String roomName, String playersString, String roomCreatorId) {
        if (!roomId.equals(this.currentRoomId)) return;
        this.currentRoomName = roomName;
        this.roomCreatorId = roomCreatorId;
        updatePlayerList(playersString, roomName, false);
    }

    private static class PlayerSlotInfo {
        String id;
        String name;
        int team;
        String status; // ready / notready
        boolean isCreator;
    }

    /**
     * playersString을 파싱해서 상단 제목 + 4개 캐릭터 슬롯을 갱신
     */
    private void updatePlayerList(String playersString, String roomName, boolean isInitialLoad) {
        // 기본값으로 초기화
        for (int i = 0; i < 4; i++) {
            nameLabels[i].setText("미참가");
            teamLabels[i].setText(" ");
            readyLabels[i].setText(" ");
        }

        List<PlayerSlotInfo> team1 = new ArrayList<>();
        List<PlayerSlotInfo> team2 = new ArrayList<>();

        int totalPlayers = 0;
        int readyCount = 0;

        String[] playerEntries = playersString.split(Protocol.DATA_SEPARATOR);
        for (String entry : playerEntries) {
            String[] details = entry.split(Protocol.FIELD_SEPARATOR);
            if (details.length < 4) continue;

            PlayerSlotInfo info = new PlayerSlotInfo();
            info.id = details[0];
            info.name = details[1];

            try {
                info.team = Integer.parseInt(details[2]);
            } catch (NumberFormatException e) {
                info.team = 0;
            }

            info.status = details[3];
            info.isCreator = info.id.equals(roomCreatorId);

            totalPlayers++;
            if ("ready".equalsIgnoreCase(info.status)) {
                readyCount++;
            }

            // 내 상태 갱신
            if (info.id.equals(myPlayerId)) {
                isReady = "ready".equalsIgnoreCase(info.status);
                isRoomCreator = info.isCreator;
            }

            if (info.team == 1) team1.add(info);
            else if (info.team == 2) team2.add(info);
        }

        // 상단 바 텍스트 갱신
        titleLabel.setText(roomName + " - 참여 인원 : " + totalPlayers + "명");

        // 팀1은 왼쪽 2칸, 팀2는 오른쪽 2칸
        int idx = 0;
        for (PlayerSlotInfo p : team1) {
            if (idx >= 2) break;
            applyPlayerToSlot(idx, p);
            idx++;
        }

        idx = 2;
        for (PlayerSlotInfo p : team2) {
            if (idx >= 4) break;
            applyPlayerToSlot(idx, p);
            idx++;
        }

        boolean allReady = (totalPlayers > 0 && readyCount == totalPlayers);
        updateReadyButtonState(allReady);
    }

    private void applyPlayerToSlot(int slotIndex, PlayerSlotInfo info) {
        String nameText = info.name;
        if (info.isCreator) {
            nameText += " (방장)";
        }
        nameLabels[slotIndex].setText(nameText);

        if (info.team == 1) {
            teamLabels[slotIndex].setText("1팀");
        } else if (info.team == 2) {
            teamLabels[slotIndex].setText("2팀");
        } else {
            teamLabels[slotIndex].setText("팀 미지정");
        }

        if ("ready".equalsIgnoreCase(info.status)) {
            readyLabels[slotIndex].setText("준비 완료");
        } else {
            readyLabels[slotIndex].setText("준비 안함");
        }
    }

    // ================== 버튼 상태 / 동작 ==================

    private void updateReadyButtonState(boolean allReady) {
        if (isRoomCreator) {
            if (allReady) {
                readyButton.setText("게임 시작");
            } else {
                readyButton.setText(isReady ? "준비 해제" : "준비 완료");
            }
            readyButton.setEnabled(true);
        } else {
            readyButton.setText(isReady ? "준비 해제" : "준비 완료");
            readyButton.setEnabled(true);
        }
    }

    /**
     * 준비 토글 또는 방장일 경우 게임 시작 요청
     */
    private void toggleReadyOrStart() {
        // 방장 + 모두 준비 완료 → 게임 시작 요청
        if (isRoomCreator && "게임 시작".equals(readyButton.getText())) {
            Map<String, String> data = new HashMap<>();
            data.put("roomId", currentRoomId);
            data.put("playerId", myPlayerId);
            connection.sendMessage(Protocol.GAME_START_REQ, data);
            return;
        }

        // 그 외에는 준비 토글
        isReady = !isReady;
        sendReadyRequest(isReady);
    }

    private void sendReadyRequest(boolean status) {
        Map<String, String> data = new HashMap<>();
        data.put("playerId", myPlayerId);
        data.put("ready", String.valueOf(status));
        connection.sendMessage(Protocol.GAME_READY, data);
    }

    private void requestTeamChange(int newTeam) {
        if (isReady) {
            JOptionPane.showMessageDialog(
                    this,
                    "준비 상태에서는 팀을 변경할 수 없습니다.",
                    "경고",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("roomId", currentRoomId);
        data.put("team", String.valueOf(newTeam));
        connection.sendMessage(Protocol.ROOM_JOIN_REQ, data);
    }

    private void requestLeaveRoom() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        LeaveRoomDialog dialog = new LeaveRoomDialog(parent, () -> {
            // "예" 눌렀을 때 실행할 실제 로직
            Map<String, String> data = new HashMap<>();
            data.put("playerId", myPlayerId);
            connection.sendMessage(Protocol.ROOM_LEAVE_REQ, data);

            mainFrame.switchToLobby();
        });

        dialog.setVisible(true);
    }
}