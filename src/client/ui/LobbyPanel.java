package client.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import client.network.ServerConnection;
import common.Protocol;

public class LobbyPanel extends JPanel {
    private final ServerConnection connection;
    private final String playerName;
    private final String playerId;

    private final DefaultTableModel tableModel;
    private final JTable roomTable;

    public LobbyPanel(ServerConnection connection, String playerName, String playerId) {
        this.connection = connection;
        this.playerName = playerName;
        this.playerId = playerId;

        setLayout(new BorderLayout(10, 10));

        // A. 내 정보 표시
        JLabel infoLabel = new JLabel("환영합니다, " + playerName + " (ID: " + playerId + ")님", SwingConstants.CENTER);
        infoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        add(infoLabel, BorderLayout.NORTH);

        // B. 방 목록 테이블
        String[] columnNames = {"방 이름", "인원", "상태", "방 ID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            // 마지막 컬럼(방 ID)은 사용자에게 보이지 않게 처리
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(roomTable);
        add(scrollPane, BorderLayout.CENTER);

        // C, D. 제어 버튼 및 입장 버튼
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("새로고침");
        JButton createButton = new JButton("방 만들기");
        JButton joinButton = new JButton("방 입장");

        refreshButton.addActionListener(e -> requestRoomList()); // 새로고침
        createButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "방 만들기 기능은 다음 단계에 구현됩니다.")); // TODO
        joinButton.addActionListener(e -> attemptJoinRoom()); // 방 입장

        createButton.addActionListener(e -> {
            // 방 만들기 버튼 클릭 시 팝업 띄우기
            CreateRoomDialog dialog = new CreateRoomDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    this,
                    connection
            );
            dialog.setVisible(true);
        });

        controlPanel.add(refreshButton);
        controlPanel.add(createButton);
        controlPanel.add(joinButton);

        add(controlPanel, BorderLayout.SOUTH);

        // 로비 진입 시 자동으로 방 목록 요청
        requestRoomList();
    }

    /**
     * 서버에 방 목록 요청 메시지를 보냅니다.
     */
   public void requestRoomList() {
        // ROOM_LIST_REQ|0|
        connection.sendMessage(Protocol.ROOM_LIST_REQ, 0, "");
    }

    /**
     * 선택된 방에 입장 요청을 보냅니다.
     */
    private void attemptJoinRoom() {
        int selectedRow = roomTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "입장할 방을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 테이블의 마지막 숨겨진 컬럼에서 방 ID를 가져옴
        String roomId = (String) tableModel.getValueAt(selectedRow, 3);

        // ROOM_JOIN_REQ|데이터길이|roomId:R001;team:1 (팀은 일단 1로 가정)
        Map<String, String> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("team", "1"); // 임시 팀 번호

        connection.sendMessage(Protocol.ROOM_JOIN_REQ, data);
    }

    /**
     * 서버로부터 받은 방 목록을 파싱하여 UI에 업데이트합니다.
     */
    public void updateRoomList(String dataPart) {
        // 테이블 초기화
        tableModel.setRowCount(0);

        // dataPart 예: list:room1:R001:2/4:waiting;room2:R002:4/4:playing
        if (!dataPart.contains("list" + Protocol.FIELD_SEPARATOR)) return;

        String listData = dataPart.split("list" + Protocol.FIELD_SEPARATOR, 2)[1];
        String[] roomEntries = listData.split(Protocol.DATA_SEPARATOR);

        for (String entry : roomEntries) {
            // entry 예: room1:R001:2/4:waiting
            String[] parts = entry.split(Protocol.FIELD_SEPARATOR, 2);
            if (parts.length < 2) continue;

            // roomInfo 예: R001:2/4:waiting
            String roomInfo = parts[1];

            // roomDetails 예: [R001, 즐거운게임방, 2/4, waiting] (추가된 GameRoom.java 참조)
            String[] roomDetails = roomInfo.split(":", 4);

            if (roomDetails.length >= 4) {
                Vector<String> row = new Vector<>();
                row.add(roomDetails[1]); // 방 이름
                row.add(roomDetails[2]); // 인원 (2/4)
                row.add(roomDetails[3]); // 상태 (waiting/playing)
                row.add(roomDetails[0]); // 방 ID (숨김 컬럼)
                tableModel.addRow(row);
            }
        }
    }
    // getPlayerName 메소드 추가 (팝업에서 방 이름 기본값 설정 시 필요)
    public String getPlayerName() {
        return playerName;
    }

}