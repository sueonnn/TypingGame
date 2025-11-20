package client.ui;

import javax.swing.*;
import java.awt.*;
import client.network.ServerConnection;
import client.GameClient;

import java.awt.Color;
import java.awt.Font;

public class UITheme {

    // 색
    public static final Color BG_MAIN   = new Color(20, 24, 40);
    public static final Color BG_PANEL  = new Color(35, 40, 60);
    public static final Color ACCENT    = new Color(255, 200, 80);
    public static final Color TEXT_NORMAL = Color.WHITE;
    public static final Color TEXT_SUB    = new Color(180, 190, 210);

    // 폰트 로더
    private static Font loadFont(String path, float size) {
        try {
            // fonts 소스폴더 안의 리소스 읽기 "/EliceDigitalBaeum_Bold.ttf"
            var is = UITheme.class.getResourceAsStream(path);
            if (is == null) {
                System.out.println("폰트 리소스를 찾을 수 없음: " + path);
                return new Font("맑은 고딕", Font.PLAIN, (int) size);
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, is);
            return base.deriveFont(size);
        } catch (Exception e) {
            System.out.println("폰트 로드 실패: " + path + " / " + e.getMessage());
            return new Font("맑은 고딕", Font.PLAIN, (int) size);
        }
    }

    // <폰트들> 
    // EliceDigitalBaeum_Bold
    public static final Font TITLE_FONT    = loadFont("/EliceDigitalBaeum_Bold.ttf", 72f); // 큰 제목 (게임 로고용)
    public static final Font SUBTITLE_FONT = loadFont("/EliceDigitalBaeum_Bold.ttf", 28f); // 로비 상단 등
    public static final Font NORMAL_FONT   = loadFont("/EliceDigitalBaeum_Bold.ttf", 22f); // 일반 텍스트
    public static final Font BUTTON_FONT   = loadFont("/EliceDigitalBaeum_Bold.ttf", 24f); // 버튼

    // 공통으로 적용할 테마
    public static void applyTheme(JComponent comp) {

        comp.setFont(NORMAL_FONT);
        comp.setForeground(TEXT_NORMAL);

        // 배경 이미지를 가진 패널, 색 덮지 않게  (BackgroundPanel 예외)
        if (!(comp instanceof BackgroundPanel)) {
            comp.setBackground(BG_MAIN);
        }

        for (Component child : comp.getComponents()) {
            if (child instanceof JComponent) {
                applyTheme((JComponent) child);
            }
        }
    }
}
