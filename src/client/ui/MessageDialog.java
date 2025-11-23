package client.ui;

 

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MessageDialog extends JDialog {

    public enum MessageType { INFO, ERROR }

    public static void showInfo(Window parent, String title, String message) {
        new MessageDialog(parent, title, message, MessageType.INFO).setVisible(true);
    }

    public static void showError(Window parent, String title, String message) {
        new MessageDialog(parent, title, message, MessageType.ERROR).setVisible(true);
    }

    private MessageDialog(Window parent, String title, String message, MessageType type) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        setUndecorated(true); // 윈도우 기본 테두리 제거 (방 만들기 창 느낌)
        
        //네 꼭짓점 배경 투명하게
        getRootPane().setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));	

        // 전체 배경 패널 (살짝 둥근 흰색)
        JPanel bg = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 40;

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        bg.setOpaque(false);
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(30, 40, 30, 40)); // 안쪽 여백
        setContentPane(bg);

        // ===== 상단 타이틀 =====
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(UITheme.TITLE_FONT.deriveFont(32f)); // "방 만들기"랑 비슷하게 굵게
        titleLabel.setForeground(Color.BLACK);
        bg.add(titleLabel, BorderLayout.NORTH);

        // ===== 중앙 메시지 =====
        JLabel msgLabel = new JLabel(
                "<html><div style='text-align:center;'>" +
                        message.replace("\n", "<br>") +
                        "</div></html>",
                SwingConstants.CENTER
        );
        msgLabel.setFont(UITheme.NORMAL_FONT.deriveFont(20f));
        msgLabel.setForeground(Color.DARK_GRAY);
        msgLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
        bg.add(msgLabel, BorderLayout.CENTER);

        // ===== 하단 버튼 영역 =====
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));

        String buttonText = "확인";
        RoundButton okButton = new RoundButton(buttonText);
        okButton.setFont(UITheme.BUTTON_FONT.deriveFont(22f));
        
        okButton.setPreferredSize(new Dimension(140, 50));
        okButton.setMargin(new Insets(5, 20, 5, 20));

        if (type == MessageType.ERROR) {
            // 에러는 글자색만 조금 더 진하게 (원하면 배경 색도 다르게)
            okButton.setForeground(Color.WHITE);
        }

        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        bg.add(buttonPanel, BorderLayout.SOUTH);

        setSize(480, 260);
        setLocationRelativeTo(parent);
    }
}
