package client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class RoundButton extends JButton {

    private boolean hover = false;

    public RoundButton(String text) {
        super(text);

        setOpaque(false);              // 부모 배경을 그대로 보이게
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);
        setBorder(null);
        setMargin(new Insets(0, 0, 0, 0));

        // hover 이벤트
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 25;

        // 1) 기존 배경은 건드리지 않고, 그냥 둥근 버튼만 그린다.
        Color top = hover ? new Color(255, 185, 80) : new Color(255, 200, 100);
        Color bottom = hover ? new Color(255, 150, 30) : new Color(255, 160, 40);
        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);

        Shape round = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc);

        g2.setPaint(gp);
        g2.fill(round);

        // 2) 흰색 외곽선 (기존 스타일 유지)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, arc, arc));

        // 3) 텍스트
        g2.setFont(getFont());
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (h + fm.getAscent()) / 2 - 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // 기본 테두리는 사용하지 않음
    }
}
