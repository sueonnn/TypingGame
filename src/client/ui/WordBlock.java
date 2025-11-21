package client.ui;

import javax.swing.*;
import java.awt.*;

public class WordBlock extends JPanel {
    private final int index;
    private final String content;
    private final int points;
    private int capturedByTeam;
    private final JLabel contentLabel;

    // 이미지에 맞게 색상 정의
    private static final Color TEAM1_COLOR = new Color(220, 80, 80); // 빨강
    private static final Color TEAM2_COLOR = new Color(50, 100, 200); // 파랑
    private static final Color NEUTRAL_COLOR = new Color(200, 200, 200); // 회색 (미점령)

    public WordBlock(int index, String content, int points) {
        this.index = index;
        this.content = content;
        this.points = points;
        this.capturedByTeam = 0;

        // 카드 크기 설정 (예시)
        setPreferredSize(new Dimension(100, 60));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createRaisedBevelBorder()); // 입체적인 카드 효과

        // 단어 내용
        contentLabel = new JLabel(content, SwingConstants.CENTER);
        contentLabel.setForeground(Color.WHITE);
        contentLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));

        // 점수 (상단 또는 하단에 작게 표시)
        JLabel pointsLabel = new JLabel(String.valueOf(points), SwingConstants.LEFT);
        pointsLabel.setFont(new Font("맑은 고딕", Font.BOLD, 10));
        pointsLabel.setForeground(Color.YELLOW);

        add(contentLabel, BorderLayout.CENTER);
        add(pointsLabel, BorderLayout.NORTH); // 점수를 상단에 표시

        setOpaque(true);
        updateColor();
    }

    public int getIndex() { return index; }
    public String getContent() { return content; }

    public void setCapturedByTeam(int team) {
        if (this.capturedByTeam != team) {
            this.capturedByTeam = team;
            SwingUtilities.invokeLater(this::updateColor);
        }
    }

    private void updateColor() {
        switch (capturedByTeam) {
            case 1:
                setBackground(TEAM1_COLOR);
                contentLabel.setForeground(Color.WHITE);
                break;
            case 2:
                setBackground(TEAM2_COLOR);
                contentLabel.setForeground(Color.WHITE);
                break;
            case 0:
            default:
                setBackground(NEUTRAL_COLOR);
                contentLabel.setForeground(Color.BLACK);
                break;
        }
    }
}