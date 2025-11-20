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
        // 타이틀바를 없애고, 부모 중앙에 뜨는 모달 다이얼로그
        super(parent, true);
        this.lobbyPanel = lobbyPanel;
        this.connection = connection;

        setUndecorated(true);                 // 뒷 배경 투명화
        setBackground(new Color(0, 0, 0, 0)); 
        
        setUndecorated(true);                 // 윈도우 기본 테두리 제거
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0, 0, 0, 0)); // 투명 배경 느낌

        // ===== 1. 전체를 감싸는 카드 패널 =====
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 120;                         // 둥근 정도

                // 흰색 카드
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // 검정 외곽선
                g2.setStroke(new BasicStroke(4f));
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80)); // 내부 여백
        getContentPane().add(card, BorderLayout.CENTER);

        // ===== 2. 상단 제목 "방 만들기" =====
        JLabel titleLabel = new JLabel("방 만들기", SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UITheme.TITLE_FONT.deriveFont(40f));
        titleLabel.setForeground(Color.BLACK);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(40));

        // ===== 3. "방 이름" 라벨 =====
        JLabel nameLabel = new JLabel("방 이름", SwingConstants.CENTER);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(22f));
        nameLabel.setForeground(Color.BLACK);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(15));

        // ===== 4. 방 이름 입력 필드 (넓고 둥근 박스 느낌) =====
        JPanel nameFieldWrapper = new JPanel();
        nameFieldWrapper.setOpaque(false);
        nameFieldWrapper.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        roomNameField = new JTextField(lobbyPanel.getPlayerName() + "의 게임방");
        roomNameField.setFont(UITheme.NORMAL_FONT.deriveFont(20f));
        roomNameField.setPreferredSize(new Dimension(420, 70));
        roomNameField.setHorizontalAlignment(SwingConstants.LEFT);
        roomNameField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(0, 70, 100), 2, 30),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        nameFieldWrapper.add(roomNameField);

        nameFieldWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameFieldWrapper);
        card.add(Box.createVerticalStrut(50));

        // ===== 5. "최대 인원" 라벨 =====
        JLabel maxLabel = new JLabel("최대 인원", SwingConstants.CENTER);
        maxLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        maxLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(22f));
        maxLabel.setForeground(Color.BLACK);
        card.add(maxLabel);
        card.add(Box.createVerticalStrut(15));

        // ===== 6. 최대 인원 선택 필드 =====
        JPanel maxWrapper = new JPanel();
        maxWrapper.setOpaque(false);
        maxWrapper.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        Integer[] maxPlayersOptions = {2, 4};
        maxPlayersComboBox = new JComboBox<>(maxPlayersOptions);
        maxPlayersComboBox.setFont(UITheme.NORMAL_FONT.deriveFont(18f));
        maxPlayersComboBox.setPreferredSize(new Dimension(220, 45));
        maxPlayersComboBox.setBackground(Color.WHITE);
        maxPlayersComboBox.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(0, 70, 100), 2, 20),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        maxWrapper.add(maxPlayersComboBox);
        maxWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(maxWrapper);
        card.add(Box.createVerticalStrut(45));

        // ===== 7. 하단 버튼 영역 (생성 / 취소) =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        buttonPanel.setOpaque(false);

        createButton = new RoundButton("생성");
        createButton.setFont(UITheme.BUTTON_FONT);
        createButton.setPreferredSize(new Dimension(150, 55));

        JButton cancelButton = new RoundButton("취소");
        cancelButton.setFont(UITheme.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(150, 55));

        createButton.addActionListener(e -> createRoom());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(buttonPanel);

        // ===== 8. 크기 & 위치 설정 =====
        pack();                           // 레이아웃 반영
        setSize(720, 540);                // 디자인에 맞게 넉넉하게
        setLocationRelativeTo(parent);    // 부모(로비) 중앙에 표시
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
                // 성공 테마 팝업
                showThemedMessage("방 생성 성공!", true);
            } else {
                // 실패도 같은 스타일로
                showThemedMessage("방 생성 실패: " + message, false);
            }
        });
    }

    private void showThemedMessage(String text, boolean success) {

        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));	//뒷배경 투명화
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 60;

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setStroke(new BasicStroke(3f));
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        dialog.getContentPane().add(card, BorderLayout.CENTER);

        // 메시지 라벨
        JLabel msgLabel = new JLabel(text, SwingConstants.CENTER);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(22f));
        msgLabel.setForeground(Color.BLACK);
        card.add(Box.createVerticalStrut(10));
        card.add(msgLabel);
        card.add(Box.createVerticalStrut(20));

        // 확인 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);

        RoundButton okButton = new RoundButton("확인");
        okButton.setFont(UITheme.BUTTON_FONT);
        okButton.setPreferredSize(new Dimension(140, 50));

        okButton.addActionListener(e -> {
            dialog.dispose();
            if (success) {
                // 성공일 때만 방 만들기 창 닫고, 로비 목록 새로고침
                dispose();                 // CreateRoomDialog 닫기
                lobbyPanel.requestRoomList();
            }
        });

        buttonPanel.add(okButton);
        card.add(buttonPanel);

        dialog.pack();
        dialog.setSize(360, 180);
        dialog.setLocationRelativeTo(this); // 방 만들기 창 중앙에 표시
        dialog.setVisible(true);
    }

    /**
     * 둥근 테두리용 커스텀 Border (텍스트 필드/콤보박스용)
     */
    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int arc;

        public RoundedLineBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int offs = thickness / 2;
            g2.drawRoundRect(x + offs, y + offs,
                             width - thickness, height - thickness,
                             arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = thickness;
            return insets;
        }
    }
}

