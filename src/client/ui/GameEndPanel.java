package client.ui;

import common.Protocol;
import client.network.ServerConnection;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameEndPanel extends JPanel {

    private final MainFrame mainFrame;
    private final ServerConnection connection;  // 서버 연결
    private final String myPlayerId;            // 내 플레이어 ID

    private JLabel resultLabel;     // "1팀 승리!" / "2팀 승리!" / "무승부"
    private JLabel scoreLabel;      // "최종 스코어: 1팀 15점 vs 2팀 13점"
    private JLabel subLabel;        // 한 줄 안내 문구
    private JLabel team1Label;      // 왼쪽 팀 점수 (색상 강조용)
    private JLabel team2Label;      // 오른쪽 팀 점수

    public GameEndPanel(MainFrame mainFrame,
                        ServerConnection connection,
                        String myPlayerId) {
        this.mainFrame = mainFrame;
        this.connection = connection;
        this.myPlayerId = myPlayerId;

        setLayout(new BorderLayout());
        setOpaque(false);

        // ==== 배경 (칠판 이미지) ====
        Image bgImage = new ImageIcon(
                getClass().getResource("/tg_start1.png")
        ).getImage();

        BackgroundPanel root = new BackgroundPanel(bgImage);
        root.setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        // ==== 중앙 카드 영역 ====
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        root.add(centerWrapper, BorderLayout.CENTER);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 40;

                // 흰 카드 배경
                g2.setColor(new Color(255, 255, 255, 240));
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // 테두리
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(700, 350));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        centerWrapper.add(card);

        // ===== 카드 상단: 결과 텍스트 =====
        resultLabel = new JLabel("결과", SwingConstants.CENTER);
        resultLabel.setFont(UITheme.TITLE_FONT.deriveFont(50f));
        resultLabel.setForeground(Color.BLACK);
        card.add(resultLabel, BorderLayout.NORTH);

        // ===== 카드 중앙: 점수 정보 =====
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        card.add(centerPanel, BorderLayout.CENTER);

        scoreLabel = new JLabel("최종 스코어: -", SwingConstants.CENTER);
        scoreLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(22f));
        scoreLabel.setForeground(Color.DARK_GRAY);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subLabel = new JLabel("로비로 돌아가 새 게임을 시작해 보세요.", SwingConstants.CENTER);
        subLabel.setFont(UITheme.NORMAL_FONT.deriveFont(18f));
        subLabel.setForeground(new Color(90, 90, 90));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(scoreLabel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(subLabel);
        centerPanel.add(Box.createVerticalStrut(25));

        // 팀별 점수 한눈에 보이도록 가로 배치
        JPanel teamScorePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        teamScorePanel.setOpaque(false);

        team1Label = new JLabel("1팀: 0점", SwingConstants.CENTER);
        team1Label.setFont(UITheme.SUBTITLE_FONT.deriveFont(24f));
        team1Label.setOpaque(false);

        team2Label = new JLabel("2팀: 0점", SwingConstants.CENTER);
        team2Label.setFont(UITheme.SUBTITLE_FONT.deriveFont(24f));
        team2Label.setOpaque(false);

        teamScorePanel.add(team1Label);
        teamScorePanel.add(team2Label);

        centerPanel.add(teamScorePanel);

        // ===== 하단: 로비로 돌아가기 버튼 =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        bottomPanel.setOpaque(false);

        RoundButton backButton = new RoundButton("로비로 돌아가기");
        backButton.setFont(UITheme.BUTTON_FONT.deriveFont(22f));
        backButton.setPreferredSize(new Dimension(260, 60));
        backButton.addActionListener(e -> {
            Map<String, String> data = new HashMap<>();
            data.put("playerId", myPlayerId);
            connection.sendMessage(Protocol.ROOM_LEAVE_REQ, data);

            mainFrame.switchToLobby();
        });

        bottomPanel.add(backButton);
        root.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * GameServer.onGameEnd → MainFrame.switchToGameEnd에서 호출
     *
     * @param winner "1", "2", "DRAW"/"0" 등
     * @param score1 1팀 점수
     * @param score2 2팀 점수
     */
    public void updateResults(String winner, int score1, int score2) {
        // 1. 결과 텍스트
        String resultText;
        if ("1".equals(winner)) {
            resultText = "1팀 승리!";
        } else if ("2".equals(winner)) {
            resultText = "2팀 승리!";
        } else {
            resultText = "무승부";
        }
        resultLabel.setText(resultText);

        // 2. 기본 점수 문구
        scoreLabel.setText(
                String.format("최종 스코어: 1팀 %d점 vs 2팀 %d점", score1, score2)
        );

        // 3. 팀별 라벨 + 색상 강조
        team1Label.setText("1팀 : " + score1 + "점");
        team2Label.setText("2팀 : " + score2 + "점");

        Color loseColor = new Color(120, 120, 120);
        Color winColor1 = new Color(220, 80, 80);   // 빨강
        Color winColor2 = new Color(50, 110, 220);  // 파랑

        if ("1".equals(winner)) {
            team1Label.setForeground(winColor1);
            team2Label.setForeground(loseColor);
        } else if ("2".equals(winner)) {
            team1Label.setForeground(loseColor);
            team2Label.setForeground(winColor2);
        } else { // 무승부
            team1Label.setForeground(new Color(200, 120, 120));
            team2Label.setForeground(new Color(120, 140, 220));
        }
    }
}
