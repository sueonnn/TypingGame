package client.ui;

import javax.swing.*;
import java.awt.*;

public class RoundButton extends JButton {

    // 기본 오렌지 계열 색 (위 / 아래)
    private Color topColor    = new Color(0xFFB84D); // 밝은 오렌지
    private Color bottomColor = new Color(0xFF9800); // 진한 오렌지
    private int cornerRadius  = 18;                  // 모서리 둥근 정도

    public RoundButton(String text) {
        super(text);

        // 기본 버튼 UI 비활성화
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);

        setForeground(Color.WHITE);  // 글자색
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 24, 8, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // hover / press 에 따라 약간 명도 조정
        ButtonModel model = getModel();
        Color top = topColor;
        Color bottom = bottomColor;

        if (model.isPressed()) {
            top = top.darker();
            bottom = bottom.darker();
        } else if (model.isRollover()) {
            top = top.brighter();
            bottom = bottom.brighter();
        }

        // 1) 오렌지 배경 (사각형 + 둥근 모서리)
        GradientPaint gp = new GradientPaint(
                0, 0, top,
                0, h, bottom
        );
        g2.setPaint(gp);
        // 살짝 안쪽에 그려서 테두리 공간 확보
        int x = 1;
        int y = 1;
        int bw = w - 3;
        int bh = h - 3;
        g2.fillRoundRect(x, y, bw, bh, cornerRadius, cornerRadius);

        // 2) 흰색 테두리
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, bw, bh, cornerRadius, cornerRadius);

        // 3) 텍스트 직접 그리기
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();

        int tx = (w - textW) / 2;
        int ty = (h + textH) / 2 - 2; // 살짝 위로 보정

        g2.setColor(getForeground());
        g2.drawString(text, tx, ty);

        g2.dispose();
    }

    // 필요 시 외부에서 색·반경 변경 가능
    public void setTopColor(Color c)    { this.topColor = c; repaint(); }
    public void setBottomColor(Color c) { this.bottomColor = c; repaint(); }
    public void setCornerRadius(int r)  { this.cornerRadius = r; repaint(); }
}
