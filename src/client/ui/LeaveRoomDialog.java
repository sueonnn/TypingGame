package client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LeaveRoomDialog extends JDialog {

    public interface LeaveCallback {
        void onConfirm();
    }

    public LeaveRoomDialog(Window parent, LeaveCallback callback) {
        super(parent, "방 나가기", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        // ===== 배경 패널 =====
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

                g2.setColor(Color.BLACK); // 테두리
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        bg.setOpaque(false);
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(30, 40, 30, 40));
        setContentPane(bg);

        // ===== 제목 =====
        JLabel title = new JLabel("알림", SwingConstants.CENTER);
        title.setFont(UITheme.TITLE_FONT.deriveFont(30f));
        title.setForeground(Color.BLACK);
        bg.add(title, BorderLayout.NORTH);

        // ===== 메시지 =====
        JLabel message = new JLabel(
                "<html><div style='text-align:center;'>정말로 방을 나가시겠습니까?</div></html>",
                SwingConstants.CENTER
        );
        message.setFont(UITheme.NORMAL_FONT.deriveFont(20f));
        message.setForeground(Color.DARK_GRAY);
        message.setBorder(new EmptyBorder(20, 10, 20, 10));
        bg.add(message, BorderLayout.CENTER);

        // ===== 버튼 영역 =====
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttons.setOpaque(false);

        RoundButton yesBtn = new RoundButton("예");
        yesBtn.setFont(UITheme.BUTTON_FONT.deriveFont(20f));
        yesBtn.setPreferredSize(new Dimension(120, 45));
        yesBtn.addActionListener(e -> {
            dispose();
            callback.onConfirm();
        });

        RoundButton noBtn = new RoundButton("아니오");
        noBtn.setFont(UITheme.BUTTON_FONT.deriveFont(20f));
        noBtn.setPreferredSize(new Dimension(120, 45));
        noBtn.addActionListener(e -> dispose());

        buttons.add(yesBtn);
        buttons.add(noBtn);
        bg.add(buttons, BorderLayout.SOUTH);

        setSize(420, 230);
        setLocationRelativeTo(parent);
    }
}
