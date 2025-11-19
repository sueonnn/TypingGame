package client.ui;

import javax.swing.*;
import javax.swing.table.*;
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

        // ==== 1. 배경 패널 ====
        Image bgImage = new ImageIcon(
                getClass().getResource("/tg_start1.png")
        ).getImage();

        setLayout(new BorderLayout());

        BackgroundPanel root = new BackgroundPanel(bgImage);
        root.setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        // ==== 2. 상단 정보 바 (흰색 둥근 바) ====
        JPanel topBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

        JLabel infoLabel = new JLabel(
                "환영합니다, " + playerName + " (ID : " + playerId + ")님",
                SwingConstants.CENTER
        );
        infoLabel.setFont(UITheme.SUBTITLE_FONT);
        infoLabel.setForeground(Color.DARK_GRAY);

        topBar.add(infoLabel, BorderLayout.CENTER);
        root.add(topBar, BorderLayout.NORTH);

        // ==== 3. 중앙 래퍼 ====
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        root.add(centerWrapper, BorderLayout.CENTER);

        // ==== 3-1. 테이블 모델 & JTable ====
        String[] columnNames = {"No.", "방 이름", "방 인원", "상태", "방 ID"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setRowHeight(40);
        roomTable.setShowGrid(false);
        roomTable.setIntercellSpacing(new Dimension(0, 8));
        roomTable.setOpaque(false);

        LobbyCellRenderer cellRenderer = new LobbyCellRenderer();
        for (int i = 0; i < roomTable.getColumnCount(); i++) {
            roomTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // 방 ID 컬럼 숨기기
        TableColumn idColumn = roomTable.getColumnModel().getColumn(4);
        idColumn.setMinWidth(0);
        idColumn.setMaxWidth(0);
        idColumn.setPreferredWidth(0);

        roomTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        roomTable.getColumnModel().getColumn(1).setPreferredWidth(500);
        roomTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        roomTable.getColumnModel().getColumn(3).setPreferredWidth(120);

//        // ==== 3-2. 헤더 패널 ====
//        JPanel headerPanel = new JPanel(new GridBagLayout());
//        headerPanel.setOpaque(false);
//
//        GridBagConstraints hg = new GridBagConstraints();
//        hg.gridy = 0;
//        hg.insets = new Insets(0, 0, 0, 8);
//        hg.fill = GridBagConstraints.HORIZONTAL;
//        hg.weighty = 0;
//
//        JLabel noHeader = createHeaderLabel("방 번호");
//        JLabel nameHeader = createHeaderLabel("방 이름");
//        JLabel countHeader = createHeaderLabel("방 인원");
//        JLabel stateHeader = createHeaderLabel("상태");
//
//        hg.gridx = 0;
//        hg.weightx = 0.12;
//        headerPanel.add(noHeader, hg);
//
//        hg.gridx = 1;
//        hg.weightx = 0.53;
//        headerPanel.add(nameHeader, hg);
//
//        hg.gridx = 2;
//        hg.weightx = 0.15;
//        headerPanel.add(countHeader, hg);
//
//        hg.gridx = 3;
//        hg.weightx = 0.15;
//        hg.insets = new Insets(0, 0, 0, 0);
//        headerPanel.add(stateHeader, hg);
//
//        // ==== 3-3. 테이블 컨테이너 ====
//        JPanel tableContainer = new JPanel(new BorderLayout());
//        tableContainer.setOpaque(false);
//
//        tableContainer.setBorder(
//                BorderFactory.createEmptyBorder(60, 200, 10, 185)
//        );
//
//        JScrollPane scrollPane = new JScrollPane(roomTable);
//        scrollPane.setOpaque(false);
//        scrollPane.getViewport().setOpaque(false);
//        scrollPane.setBorder(null);
//        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
//
//        tableContainer.add(headerPanel, BorderLayout.NORTH);
//        tableContainer.add(scrollPane, BorderLayout.CENTER);
//
//        centerWrapper.add(tableContainer, BorderLayout.CENTER);
     // ==== 3-2. 테이블 컨테이너 (칠판 안을 거의 꽉 채우도록 여백만 조금) ====
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.setBorder(
                BorderFactory.createEmptyBorder(60, 200, 10, 185)
        );

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // ==== 3-3. JTable 헤더를 그대로 사용 + 스타일 적용 ====
        JTableHeader header = roomTable.getTableHeader();
        header.setPreferredSize(new Dimension(0, 44)); // 헤더 높이
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setDefaultRenderer(new LobbyHeaderRenderer());

        tableContainer.add(scrollPane, BorderLayout.CENTER);
        centerWrapper.add(tableContainer, BorderLayout.CENTER);

        // ==== 3-4. 버튼 영역 ====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 160, 0)
        );

        RoundButton refreshButton = new RoundButton("새로고침");
        refreshButton.setFont(UITheme.BUTTON_FONT);
        refreshButton.setPreferredSize(new Dimension(160, 50));

        RoundButton createButton = new RoundButton("방 만들기");
        createButton.setFont(UITheme.BUTTON_FONT);
        createButton.setPreferredSize(new Dimension(160, 50));

        RoundButton joinButton = new RoundButton("방 입장");
        joinButton.setFont(UITheme.BUTTON_FONT);
        joinButton.setPreferredSize(new Dimension(160, 50));

        refreshButton.addActionListener(e -> requestRoomList());
        joinButton.addActionListener(e -> attemptJoinRoom());
        createButton.addActionListener(e -> {
            CreateRoomDialog dialog = new CreateRoomDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    this,
                    connection
            );
            dialog.setVisible(true);
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);

        centerWrapper.add(buttonPanel, BorderLayout.SOUTH);

        // ==== 4. 초기 방 목록 요청 ====
        requestRoomList();
    }

    // ================== 로직 부분 ==================
    public void requestRoomList() {
        connection.sendMessage(Protocol.ROOM_LIST_REQ, 0, "");
    }

    private void attemptJoinRoom() {
        int selectedRow = roomTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "입장할 방을 선택해주세요.",
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String roomId = (String) tableModel.getValueAt(selectedRow, 4);

        Map<String, String> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("team", "1");

        connection.sendMessage(Protocol.ROOM_JOIN_REQ, data);
    }

    public void updateRoomList(String dataPart) {
        tableModel.setRowCount(0);

        if (!dataPart.contains("list" + Protocol.FIELD_SEPARATOR)) return;

        String listData = dataPart.split(
                "list" + Protocol.FIELD_SEPARATOR, 2
        )[1];

        String[] roomEntries = listData.split(Protocol.DATA_SEPARATOR);

        int no = 1;

        for (String entry : roomEntries) {
            String[] parts = entry.split(Protocol.FIELD_SEPARATOR, 2);
            if (parts.length < 2) continue;

            String roomInfo = parts[1];
            String[] roomDetails = roomInfo.split(":", 4);

            if (roomDetails.length >= 4) {
                Vector<String> row = new Vector<>();
                row.add("No." + no);
                row.add(roomDetails[1]);
                row.add(roomDetails[2]);
                row.add(convertState(roomDetails[3]));
                row.add(roomDetails[0]);

                tableModel.addRow(row);
                no++;
            }
        }
    }

    private String convertState(String raw) {
        if ("waiting".equalsIgnoreCase(raw)) return "대기 중";
        if ("playing".equalsIgnoreCase(raw)) return "게임 중";
        return raw;
    }

    public String getPlayerName() {
        return playerName;
    }

    // ================== UI 보조 메서드 ==================
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(UITheme.SUBTITLE_FONT);
        label.setForeground(Color.DARK_GRAY);
        label.setPreferredSize(new Dimension(10, 44));

        label.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 2, true),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)
                )
        );

        label.setBackground(Color.WHITE);
        label.setOpaque(true);

        return label;
    }

    private static class LobbyCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column
        ) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            label.setHorizontalAlignment(column == 1 ? LEFT : CENTER);
            label.setFont(table.getFont());
            label.setForeground(Color.BLACK);
            label.setOpaque(false);

            return new CellBackgroundPanel(label, column, isSelected);
        }
    }

    private static class CellBackgroundPanel extends JPanel {
        private final JComponent content;
        private final int column;
        private final boolean selected;

        public CellBackgroundPanel(JComponent content, int column, boolean selected) {
            this.content = content;
            this.column = column;
            this.selected = selected;

            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = getWidth();
            int h = getHeight();

            Color fill = selected ? new Color(255, 220, 120) : Color.WHITE;

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 20, 20);

            g2.setColor(Color.BLACK);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
        }
    }
 // ==== 헤더용 렌더러 (방 번호 / 방 이름 / 방 인원 / 상태) ====
    private static class LobbyHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, false, false, row, column);

            label.setHorizontalAlignment(CENTER);
            label.setFont(UITheme.SUBTITLE_FONT);
            label.setForeground(Color.DARK_GRAY);
            label.setOpaque(false);

            return new HeaderBackgroundPanel(label);
        }
    }

    private static class HeaderBackgroundPanel extends JPanel {
        private final JComponent content;

        public HeaderBackgroundPanel(JComponent content) {
            this.content = content;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);

            g2.dispose();
        }
    }

}
