package client.ui;

import client.network.ServerConnection;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 인게임 화면 패널
 * - 서버에서 받은 단어 보드를 표시
 * - 현재 점수, 남은 시간 표시
 * - 내가 어느 팀인지 표시
 * - 단어 입력을 서버로 전송
 */
public class GamePanel extends JPanel {

    private final ServerConnection connection;
    private final String myPlayerId;


    // 보드 UI
    private JPanel boardPanel;
    private JLabel[] wordLabels;

    // 상단 정보
    private JLabel timerLabel;
    private JLabel score1Label;
    private JLabel score2Label;
    private JLabel myTeamLabel;   // ★ 내 팀 표시

    // 입력 영역
    private JTextField inputField;
    private JButton sendButton;

    // 내 팀 정보 (1, 2, 0: 미지정)
    private int myTeam = 0;

    public GamePanel(ServerConnection connection, String myPlayerId,int myTeam) {
        this.connection = connection;
        this.myPlayerId = myPlayerId;
        this.myTeam = myTeam;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== 상단: 남은 시간, 점수, 내 팀 =====
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));

        timerLabel = new JLabel("남은 시간: 0초");
        score1Label = new JLabel("1팀: 0점");
        score2Label = new JLabel("2팀: 0점");
        myTeamLabel = new JLabel("내 팀: -");  // 기본은 미지정

        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        score1Label.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        score2Label.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        myTeamLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

        topPanel.add(timerLabel);
        topPanel.add(score1Label);
        topPanel.add(score2Label);
        topPanel.add(myTeamLabel);

        add(topPanel, BorderLayout.NORTH);

        // ===== 중앙: 단어 보드 =====
        boardPanel = new JPanel();
        add(boardPanel, BorderLayout.CENTER);

        // ===== 하단: 입력 영역 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        inputField = new JTextField();
        sendButton = new JButton("입력");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // ===== 리스너 =====
        sendButton.addActionListener(this::sendWord);
        inputField.addActionListener(this::sendWord);

        // ESC 누르면 입력창 비우기 (있으면 편함)
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    inputField.setText("");
                }
            }
        });
    }

    /**
     * WaitingPanel → GamePanel 전환 시, 최초 1번 호출
     * 서버에서 받은 boardString으로 보드 UI 초기화
     *
     * boardString 예:
     *   "단어1,0/단어2,0/단어3,1/단어4,2/..."
     *   (단어,점령팀)
     */
    public void initializeGame(String boardString) {
        if (boardString == null || boardString.isEmpty()) return;

        String[] entries = boardString.split("/");
        int cardCount = entries.length;

        wordLabels = new JLabel[cardCount];

        boardPanel.removeAll();

        int cols = 5; // 5열 고정, 필요하면 조절
        int rows = (int) Math.ceil(cardCount / (double) cols);
        boardPanel.setLayout(new GridLayout(rows, cols, 5, 5));

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
            label.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            label.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            applyOwnerTeamColor(label, ownerTeam);

            wordLabels[i] = label;
            boardPanel.add(label);
        }

        revalidate();
        repaint();
        inputField.requestFocusInWindow();
    }

    /**
     * 각 카드의 배경색을 팀에 따라 칠해줌
     */
    private void applyOwnerTeamColor(JLabel label, int ownerTeam) {
        Color bg;
        switch (ownerTeam) {
            case 1 -> bg = new Color(255, 220, 220); // 1팀(빨강)
            case 2 -> bg = new Color(220, 220, 255); // 2팀(파랑)
            default -> bg = Color.WHITE;             // 미점령
        }
        label.setBackground(bg);
    }

    /**
     * 단어 입력을 서버로 전송
     */
    private void sendWord(ActionEvent e) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("word", text);

        connection.sendMessage(Protocol.WORD_INPUT, data);

        inputField.setText("");
        inputField.requestFocusInWindow();
    }

    /**
     * 서버에서 GAME_UPDATE를 받을 때마다 호출됨
     *
     * @param boardString  "단어,점령팀/..." 형식
     * @param score1       1팀 점수
     * @param score2       2팀 점수
     * @param timeLeft     남은 시간(초)
     */
    public void updateGameState(String boardString, int score1, int score2, int timeLeft) {
        // 점수 / 시간 UI 갱신
        score1Label.setText("1팀: " + score1 + "점");
        score2Label.setText("2팀: " + score2 + "점");
        timerLabel.setText("남은 시간: " + timeLeft + "초");

        // 보드 상태 갱신
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

    // ================== 내 팀 표시 관련 ==================

    /**
     * MainFrame / RoomPanel 쪽에서
     * GamePanel 생성 후 한 번 호출해주면 됨.
     *
     * 예:
     *   GamePanel gamePanel = new GamePanel(connection, myId);
     *   gamePanel.setMyTeam(내팀번호);
     */
    public void setMyTeam(int team) {
        this.myTeam = team;

        String teamText;
        if (team == 1) {
            teamText = "1팀 (빨강)";
        } else if (team == 2) {
            teamText = "2팀 (파랑)";
        } else {
            teamText = "-";
        }

        myTeamLabel.setText("내 팀: " + teamText);
    }

    public int getMyTeam() {
        return myTeam;
    }

    public String getMyPlayerId() {
        return myPlayerId;
    }
}
