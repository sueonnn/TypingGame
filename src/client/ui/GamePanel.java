package client.ui;

import client.network.ServerConnection;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * 인게임 화면 패널
 * - 서버에서 받은 단어 보드를 칠판 위 카드처럼 표시
 * - 상단 타임바 (남은 시간에 따라 색/길이 줄어듦)
 * - 좌우 팀 점수 패널
 * - 하단 입력창에서 단어 입력
 */
public class GamePanel extends JPanel {

    private final ServerConnection connection;
    private final String myPlayerId;

    // 내 팀 번호 (1, 2, 0: 미지정)
    private int myTeam = 0;

    // ===== 상단 타이머바 =====
    private TimerBar timerBar;
    private int maxTime = 0; // 처음 updateGameState 들어온 timeLeft를 기준으로 설정

    // ===== 칠판 위 카드 보드 =====
    private JPanel boardPanel;
    private JLabel[] wordLabels;

    // ===== 좌/우 점수 패널 =====
    private ScorePanel team1ScorePanel;
    private ScorePanel team2ScorePanel;

    // ===== 하단 입력 영역 =====
    private JTextField inputField;
    private JButton sendButton;

    public GamePanel(ServerConnection connection, String myPlayerId, int myTeam) {
        this.connection = connection;
        this.myPlayerId = myPlayerId;
        this.myTeam = myTeam;
        initUI();
    }

    // ================== UI 초기 구성 ==================

    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // ==== 배경 이미지 (로비/방과 동일 tg_start1.png) ====
        Image bgImage = new ImageIcon(
                getClass().getResource("/tg_start1.png")
        ).getImage();

        BackgroundPanel root = new BackgroundPanel(bgImage);
        root.setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);

        // ===== 1. 상단 타이머 바 영역 =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(30, 200, 10, 200));

        timerBar = new TimerBar();
        topPanel.add(timerBar, BorderLayout.CENTER);

        root.add(topPanel, BorderLayout.NORTH);

        // ===== 2. 중앙 칠판 영역 (카드 + 좌우 점수) =====
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(80, 220, 80, 220));
        root.add(centerWrapper, BorderLayout.CENTER);

        // 좌측 1팀 점수패널
        team1ScorePanel = new ScorePanel("1팀 (빨강)", new Color(255, 140, 140),
                myTeam == 1);
        centerWrapper.add(team1ScorePanel, BorderLayout.WEST);

        // 우측 2팀 점수패널
        team2ScorePanel = new ScorePanel("2팀 (파랑)", new Color(150, 170, 255),
                myTeam == 2);
        centerWrapper.add(team2ScorePanel, BorderLayout.EAST);

        // 중앙 카드 보드 (칠판 안의 카드들)
        boardPanel = new JPanel();
        boardPanel.setOpaque(false);
        centerWrapper.add(boardPanel, BorderLayout.CENTER);

        // ===== 3. 하단 입력 영역 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 360, 50, 360));

        inputField = new JTextField();
        inputField.setFont(UITheme.NORMAL_FONT.deriveFont(18f));
        inputField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(10, 80, 90), 2, true),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                )
        );

        sendButton = new RoundButton("입력");
        sendButton.setFont(UITheme.BUTTON_FONT.deriveFont(20f));
        sendButton.setPreferredSize(new Dimension(120, 50));

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        root.add(bottomPanel, BorderLayout.SOUTH);

        // ===== 이벤트 리스너 =====
        sendButton.addActionListener(this::sendWord);
        inputField.addActionListener(this::sendWord);

        // ESC 누르면 입력창 비우기
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    inputField.setText("");
                }
            }
        });
    }

    // ================== 게임 보드 초기화 ==================

    /**
     * WaitingPanel → GamePanel 전환 시, 최초 1번 호출.
     *
     * @param boardString "단어,점령팀/..." 형식 (예: "사과,1/바나나,0/포도,2")
     */
    public void initializeGame(String boardString) {
        if (boardString == null || boardString.isEmpty()) return;

        String[] entries = boardString.split("/");
        int cardCount = entries.length;

        wordLabels = new JLabel[cardCount];
        boardPanel.removeAll();

        // 30개 기준 6 x 5 정도로 배치
        int cols = 6;
        int rows = (int) Math.ceil(cardCount / (double) cols);
        boardPanel.setLayout(new GridLayout(rows, cols, 12, 12));

        for (int i = 0; i < cardCount; i++) {
            String entry = entries[i];
            String[] parts = entry.split(",");

            String word = parts.length > 0 ? parts[0] : "";
            int ownerTeam = 0;
            if (parts.length > 1) {
                try {
                    ownerTeam = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    ownerTeam = 0;
                }
            }

            JLabel label = new JLabel(word, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    BorderFactory.createEmptyBorder(8, 6, 8, 6)
            ));
            label.setFont(UITheme.NORMAL_FONT.deriveFont(18f));

            applyOwnerTeamColor(label, ownerTeam);

            wordLabels[i] = label;
            boardPanel.add(label);
        }

        boardPanel.revalidate();
        boardPanel.repaint();
        inputField.requestFocusInWindow();
    }

    /**
     * 각 카드의 배경색을 팀에 따라 칠해줍니다.
     */
    private void applyOwnerTeamColor(JLabel label, int ownerTeam) {
        Color bg;
        switch (ownerTeam) {
            case 1 -> bg = new Color(255, 220, 220); // 1팀(빨강)
            case 2 -> bg = new Color(220, 220, 255); // 2팀(파랑)
            default -> bg = Color.WHITE;             // 미점령
        }
        label.setBackground(bg);
        label.setForeground(Color.DARK_GRAY);
    }

    // ================== 서버 상태 갱신 반영 ==================

    /**
     * 서버에서 GAME_UPDATE를 받을 때마다 호출.
     *
     * @param boardString "단어,점령팀/..." 형식
     * @param score1      1팀 점수
     * @param score2      2팀 점수
     * @param timeLeft    남은 시간(초)
     */
    public void updateGameState(String boardString, int score1, int score2, int timeLeft) {
        // 최초 한 번 timeLeft를 maxTime으로 기억 (비율 계산용)
        if (maxTime == 0 && timeLeft > 0) {
            maxTime = timeLeft;
            timerBar.setMaxTime(maxTime);
        }

        // 타이머바 갱신
        timerBar.setTimeLeft(timeLeft);

        // 점수 갱신
        team1ScorePanel.setScore(score1);
        team2ScorePanel.setScore(score2);

        // 보드 갱신
        if (boardString == null || boardString.isEmpty() || wordLabels == null) return;

        String[] entries = boardString.split("/");
        for (int i = 0; i < entries.length && i < wordLabels.length; i++) {
            String[] parts = entries[i].split(",");

            String word = parts.length > 0 ? parts[0] : "";
            int ownerTeam = 0;
            if (parts.length > 1) {
                try {
                    ownerTeam = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    ownerTeam = 0;
                }
            }

            JLabel label = wordLabels[i];
            label.setText(word);
            applyOwnerTeamColor(label, ownerTeam);
        }

        boardPanel.repaint();
    }

    // ================== 단어 입력 전송 ==================

    private void sendWord(ActionEvent e) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("word", text);

        connection.sendMessage(Protocol.WORD_INPUT, data);

        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    // ================== 내 팀 / 플레이어 정보 ==================

    public void setMyTeam(int team) {
        this.myTeam = team;

        // 내 팀 하이라이트 다시 적용
        team1ScorePanel.setHighlight(team == 1);
        team2ScorePanel.setHighlight(team == 2);
        repaint();
    }

    public int getMyTeam() {
        return myTeam;
    }

    public String getMyPlayerId() {
        return myPlayerId;
    }

    // ================== 내부 UI 컴포넌트들 ==================

    /**
     * 상단 타이머 바 (색이 줄어드는 막대)
     */
    private static class TimerBar extends JComponent {
        private int maxTime = 0;
        private int timeLeft = 0;

        public void setMaxTime(int maxTime) {
            this.maxTime = maxTime;
            repaint();
        }

        public void setTimeLeft(int timeLeft) {
            this.timeLeft = Math.max(timeLeft, 0);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int arc = h; // 캡슐 모양

            // 배경 바 (연한 회색)
            g2.setColor(new Color(230, 230, 230, 220));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            // 남은 시간 비율
            double ratio = 0.0;
            if (maxTime > 0) {
                ratio = (double) timeLeft / (double) maxTime;
                ratio = Math.max(0.0, Math.min(1.0, ratio));
            }

            int filledWidth = (int) (w * ratio);

            // 타임바 그라데이션 (노랑 → 주황)
            Color start = new Color(255, 210, 80);
            Color end = new Color(255, 140, 40);
            GradientPaint gp = new GradientPaint(0, 0, start, w, h, end);

            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, filledWidth, h, arc, arc);

            // 테두리
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, 26);
        }
    }

    /**
     * 팀 점수 표시용 패널
     */
    private static class ScorePanel extends JPanel {
        private final JLabel titleLabel;
        private final JLabel scoreLabel;
        private final Color baseColor;
        private boolean highlight = false;

        public ScorePanel(String title, Color baseColor, boolean highlight) {
            this.baseColor = baseColor;
            this.highlight = highlight;

            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(20f));
            titleLabel.setForeground(Color.WHITE);

            scoreLabel = new JLabel("0 점", SwingConstants.CENTER);
            scoreLabel.setFont(UITheme.TITLE_FONT.deriveFont(36f));
            scoreLabel.setForeground(Color.WHITE);

            JPanel inner = new JPanel(new BorderLayout(0, 5)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int arc = 30;

                    Color bg = new Color(
                            baseColor.getRed(),
                            baseColor.getGreen(),
                            baseColor.getBlue(),
                            highlight ? 220 : 150
                    );

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                    g2.dispose();
                }
            };
            inner.setOpaque(false);
            inner.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            inner.add(titleLabel, BorderLayout.NORTH);
            inner.add(scoreLabel, BorderLayout.CENTER);

            add(inner, BorderLayout.CENTER);
        }

        public void setScore(int score) {
            scoreLabel.setText(score + " 점");
        }

        public void setHighlight(boolean highlight) {
            this.highlight = highlight;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(180, 200);
        }
    }
}