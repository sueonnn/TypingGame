package client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class WaitingPanel extends JPanel {

    private final MainFrame mainFrame;
    private final String wordList;
    private final int timeLimit;      // 필요하면 안내 문구에만 사용

    private JLabel titleLabel;        // "모든 플레이어가 준비되었습니다."
    private JLabel countdownLabel;    // 3, 2, 1 숫자
    private JLabel subLabel;          // "잠시 후 게임이 시작됩니다."
    private Timer timer;

    public WaitingPanel(MainFrame mainFrame, String wordList, int timeLimit) {
        this.mainFrame = mainFrame;
        this.wordList = wordList;
        this.timeLimit = timeLimit;

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
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                int w = getWidth();
                int h = getHeight();
                int arc = 40;

                g2.setColor(new Color(255, 255, 255, 240));
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(600, 320));
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        centerWrapper.add(card);

        // ===== 상단 안내 문구 =====
        titleLabel = new JLabel("모든 플레이어가 준비되었습니다.", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.SUBTITLE_FONT.deriveFont(26f));
        titleLabel.setForeground(Color.BLACK);
        card.add(titleLabel, BorderLayout.NORTH);

        // ===== 중앙 카운트다운 숫자 =====
        countdownLabel = new JLabel("3", SwingConstants.CENTER);
        countdownLabel.setFont(UITheme.TITLE_FONT.deriveFont(80f));
        countdownLabel.setForeground(new Color(255, 180, 60));
        card.add(countdownLabel, BorderLayout.CENTER);

        // ===== 하단 보조 문구 =====
        String subText = String.format(
                "약 3초 후 게임이 시작됩니다. (게임 시간: %d초)",
                timeLimit
        );
        subLabel = new JLabel(subText, SwingConstants.CENTER);
        subLabel.setFont(UITheme.NORMAL_FONT.deriveFont(18f));
        subLabel.setForeground(new Color(90, 90, 90));
        subLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        card.add(subLabel, BorderLayout.SOUTH);

        // 카운트다운 시작
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

                        // 숫자 바뀔 때 가볍게 색 변경 효과
                        countdownLabel.setForeground(
                                count[0] % 2 == 0
                                        ? new Color(255, 180, 60)
                                        : new Color(255, 140, 40)
                        );
                    } else {
                        timer.cancel();
                        // 3,2,1 끝난 뒤 실제 게임 화면으로 전환
                        mainFrame.switchToGame(wordList);
                    }
                });
            }
        }, 0, 1000); // 1초 간격
    }
}
