package client.ui;

import client.network.ServerConnection;
import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class GamePanel extends JPanel {

    private final ServerConnection connection;
    private final String myPlayerId;

    private JPanel boardPanel;
    private JTextField inputField;
    private JLabel scoreLabel;
    private JLabel timerLabel;

    // word -> JLabel 매핑
    private final Map<String, JLabel> wordLabelMap = new HashMap<>();

    public GamePanel(ServerConnection connection, String myPlayerId) {
        this.connection = connection;
        this.myPlayerId = myPlayerId;

        setLayout(new BorderLayout(10, 10));

        // 위쪽 점수/타이머
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        scoreLabel = new JLabel("팀 1: 0 | 팀 2: 0", SwingConstants.LEFT);
        timerLabel = new JLabel("남은 시간: 60초", SwingConstants.RIGHT);
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        topPanel.add(scoreLabel);
        topPanel.add(timerLabel);
        add(topPanel, BorderLayout.NORTH);

        // 가운데 카드 영역
        boardPanel = new JPanel(new GridLayout(5, 6, 10, 10)); // 30개 기준 5x6
        add(boardPanel, BorderLayout.CENTER);

        // 아래쪽 입력창
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        bottomPanel.add(new JLabel("단어 입력: "), BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 엔터 치면 WORD_INPUT 전송
        inputField.addActionListener(this::onEnterInput);
    }

    /**
     * GAME_START 에서 받은 board 문자열로 초기화
     * board 예: "사과,1/바나나,2/포도,1/..."
     */
    public void initializeGame(String boardString) {
        rebuildBoard(boardString);
    }

    /**
     * 서버에서 GAME_UPDATE 로 넘어온 상태 반영
     */
    public void updateGameState(String boardString, int score1, int score2, int timeLeft) {
        rebuildBoard(boardString);
        scoreLabel.setText(String.format("팀 1: %d | 팀 2: %d", score1, score2));
        timerLabel.setText(String.format("남은 시간: %d초", timeLeft));
    }

    private void rebuildBoard(String boardString) {
        boardPanel.removeAll();
        wordLabelMap.clear();

        if (boardString == null || boardString.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        String[] entries = boardString.split("/"); // "사과,1"
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length < 2) continue;

            String word = parts[0];
            int team;
            try {
                team = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                team = 0;
            }

            JLabel label = new JLabel(word, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            label.setFont(new Font("맑은 고딕", Font.BOLD, 18));

            // 팀 색깔: 1 → 핑크, 2 → 연두, 0 → 회색
            if (team == 1) {
                label.setBackground(new Color(255, 192, 203)); // 핑크
            } else if (team == 2) {
                label.setBackground(new Color(144, 238, 144)); // 연두
            } else {
                label.setBackground(Color.LIGHT_GRAY);
            }

            boardPanel.add(label);
            wordLabelMap.put(word, label);
        }

        revalidate();
        repaint();
        inputField.requestFocusInWindow();
    }

    private void onEnterInput(ActionEvent e) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        Map<String, String> data = new HashMap<>();
        data.put("word", text);
        connection.sendMessage(Protocol.WORD_INPUT, data);

        inputField.setText("");
    }
}
