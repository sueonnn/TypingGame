//package client.ui;
//
//import javax.swing.*;
//import java.awt.*;
//import common.Protocol;
//
//public class GameEndPanel extends JPanel {
//    private final MainFrame mainFrame;
//
//    // A, B, C 영역 변수
//    private final JLabel resultLabel;
//    private final JLabel scoreLabel;
//    private final JLabel mvpLabel;
//
//    public GameEndPanel(MainFrame mainFrame) {
//        this.mainFrame = mainFrame;
//        setLayout(new BorderLayout(20, 20));
//        setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
//
//        // 중앙 결과 표시 영역
//        JPanel resultPanel = new JPanel(new GridLayout(3, 1, 10, 10));
//
//        // A. 결과 메시지
//        resultLabel = new JLabel("게임 종료!", SwingConstants.CENTER);
//        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 40));
//
//        // B. 최종 스코어
//        scoreLabel = new JLabel("팀 스코어: 0 vs 0", SwingConstants.CENTER);
//        scoreLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
//
//        // C. MVP (선택 사항)
//        mvpLabel = new JLabel("MVP: 없음", SwingConstants.CENTER);
//        mvpLabel.setFont(new Font("맑은 고딕", Font.ITALIC, 18));
//
//        resultPanel.add(resultLabel);
//        resultPanel.add(scoreLabel);
//        resultPanel.add(mvpLabel);
//
//        add(resultPanel, BorderLayout.CENTER);
//
//        // D. 제어 버튼
//        JButton lobbyButton = new JButton("로비로 돌아가기");
//        lobbyButton.setFont(new Font("맑은 고딕", Font.BOLD, 20));
//        lobbyButton.addActionListener(e -> mainFrame.switchToLobby()); // 로비로 전환
//
//        add(lobbyButton, BorderLayout.SOUTH);
//    }
//
//    /**
//     * 서버로부터 받은 최종 결과로 UI를 업데이트합니다.
//     */
//    public void updateResults(String winner, int score1, int score2, String mvp) {
//        String winnerText = (winner.equals("1") ? "빨강팀 승리!" : (winner.equals("2") ? "파랑팀 승리!" : "무승부"));
//        resultLabel.setText(winnerText);
//        resultLabel.setForeground(winner.equals("1") ? Color.RED : (winner.equals("2") ? Color.BLUE : Color.GRAY));
//
//        scoreLabel.setText(String.format("최종 스코어: 팀 1 (%d) vs 팀 2 (%d)", score1, score2));
//
//        if (mvp != null && !mvp.isEmpty()) {
//            // mvp 형식: P001:15 (ID:점수) -> ID만 표시한다고 가정
//            mvpLabel.setText("MVP: " + mvp.split(Protocol.FIELD_SEPARATOR)[0]);
//        }
//    }
//}

package client.ui;

import javax.swing.*;
import java.awt.*;

import client.network.ServerConnection;
import common.Protocol;

public class GameEndPanel extends JPanel {
    private final MainFrame mainFrame;

    // 결과 / 스코어만 사용
    private final JLabel resultLabel;
    private final JLabel scoreLabel;

    public GameEndPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 중앙 결과 표시 영역
        JPanel resultPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        // A. 결과 메시지
        resultLabel = new JLabel("게임 종료!", SwingConstants.CENTER);
        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 40));

        // B. 최종 스코어
        scoreLabel = new JLabel("팀 스코어: 0 vs 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 24));

        resultPanel.add(resultLabel);
        resultPanel.add(scoreLabel);

        add(resultPanel, BorderLayout.CENTER);

        // D. 제어 버튼
        JButton lobbyButton = new JButton("로비로 돌아가기");
        lobbyButton.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        lobbyButton.addActionListener(e -> {
            // 1) 서버에 방 나가기 요청 보내기
            ServerConnection conn = mainFrame.getConnection();
            if (conn != null) {
                conn.sendMessage(Protocol.ROOM_LEAVE_REQ, 0, "");
            }
            // 2) 로비 화면으로 전환
            mainFrame.switchToLobby();
        });

        add(lobbyButton, BorderLayout.SOUTH);
    }

    /**
     * 서버로부터 받은 최종 결과로 UI를 업데이트합니다.
     */
    public void updateResults(String winner, int score1, int score2) {
        String winnerText = (winner.equals("1")
                ? "빨강팀 승리!"
                : (winner.equals("2") ? "파랑팀 승리!" : "무승부"));

        resultLabel.setText(winnerText);
        resultLabel.setForeground(
                winner.equals("1") ? Color.RED
                        : (winner.equals("2") ? Color.BLUE : Color.GRAY)
        );

        scoreLabel.setText(
                String.format("최종 스코어: 팀 1 (%d) vs 팀 2 (%d)", score1, score2)
        );
    }
}
