package client.ui;

import javax.swing.*;
import java.awt.*;
import client.network.ServerConnection;
import common.Protocol;

public class CreateRoomDialog extends JDialog {
    private final LobbyPanel lobbyPanel;
    private final ServerConnection connection;

    private JTextField roomNameField;
    private JComboBox<Integer> maxPlayersComboBox;
    private JButton createButton;

    public CreateRoomDialog(JFrame parent, LobbyPanel lobbyPanel, ServerConnection connection) {
        super(parent, "게임 방 만들기", true); // A. 제목: 모달 창 설정
        this.lobbyPanel = lobbyPanel;
        this.connection = connection;


        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 중앙 입력 필드
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // B. 입력 필드: 방 이름
        inputPanel.add(new JLabel("방 이름:"));
        roomNameField = new JTextField(lobbyPanel.getPlayerName() + "의 게임방");
        inputPanel.add(roomNameField);

        // C. 선택 필드: 최대 플레이어 수
        inputPanel.add(new JLabel("최대 인원:"));
        Integer[] maxPlayersOptions = {2, 4}; // 2명 또는 4명만 가능하도록 설정
        maxPlayersComboBox = new JComboBox<>(maxPlayersOptions);
        maxPlayersComboBox.setSelectedIndex(1); // 기본값 4명
        inputPanel.add(maxPlayersComboBox);

        add(inputPanel, BorderLayout.CENTER);

        // D. 제어 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        createButton = new JButton("생성"); // ROOM_CREATE_REQ 전송
        JButton cancelButton = new JButton("취소");

        createButton.addActionListener(e -> createRoom());
        cancelButton.addActionListener(e -> dispose()); // 창 닫기

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // 내용물에 맞게 창 크기 조정
        setLocationRelativeTo(parent); // 부모 창 중앙에 표시
    }

    /**
     * 방 생성 요청을 서버로 전송합니다.
     */
    private void createRoom() {
        String roomName = roomNameField.getText().trim();
        int maxPlayers = (int) maxPlayersComboBox.getSelectedItem();

        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "방 이름을 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ROOM_CREATE_REQ|데이터길이|roomName:이름;maxPlayers:4
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("roomName", roomName);
        data.put("maxPlayers", String.valueOf(maxPlayers));

        connection.sendMessage(Protocol.ROOM_CREATE_REQ, data);
        createButton.setEnabled(false); // 응답 기다리는 동안 비활성화
    }

    // 서버 응답 처리 후 팝업을 닫고 로비 목록을 새로고침
    public void handleCreationResponse(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            createButton.setEnabled(true);
            if (success) {
                JOptionPane.showMessageDialog(this, "방 생성 성공!", "알림", JOptionPane.INFORMATION_MESSAGE);
                dispose(); // 팝업 닫기
                lobbyPanel.requestRoomList(); // 방 목록 새로고침
            } else {
                JOptionPane.showMessageDialog(this, "방 생성 실패: " + message, "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}