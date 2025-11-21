package client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class WaitingPanel extends JPanel {
    private final MainFrame mainFrame;
    private final String wordList;
    private final int timeLimit;

    private final JLabel countdownLabel; // B. 카운트다운
    private Timer timer;

    public WaitingPanel(MainFrame mainFrame, String wordList, int timeLimit) {
        this.mainFrame = mainFrame;
        this.wordList = wordList;
        this.timeLimit = timeLimit;

        setLayout(new BorderLayout());
        setBackground(new Color(50, 50, 50));

        // A. 알림 메시지 및 C. 팀 정보 요약 (간단화)
        JLabel messageLabel = new JLabel("<html><h1 style='color:white;'>모든 플레이어가 준비되었습니다.</h1><p style='color:lightgray;'>게임 로딩 중...</p></html>", SwingConstants.CENTER);
        messageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(messageLabel, BorderLayout.NORTH);

        // B. 카운트다운 레이블
        countdownLabel = new JLabel("3", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("맑은 고딕", Font.BOLD, 150));
        countdownLabel.setForeground(Color.YELLOW);
        add(countdownLabel, BorderLayout.CENTER);

        startCountdown();
    }

    private void startCountdown() {
        final int[] count = {3};
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (count[0] > 0) {
                        countdownLabel.setText(String.valueOf(count[0]));
                        count[0]--;
                    } else {
                        timer.cancel();
                        // 카운트다운 종료 후 인게임으로 전환
                        mainFrame.switchToGame(wordList);
                    }
                });
            }
        }, 0, 1000); // 1초 간격
    }
}