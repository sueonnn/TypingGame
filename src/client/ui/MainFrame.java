package client.ui;

import javax.swing.*;
import java.awt.*;
import client.network.ServerConnection;
import client.GameClient;

public class MainFrame extends JFrame {
    private final ServerConnection connection;

    // UI 전환을 위한 요소
    private final JPanel contentPanel; // 내용을 담을 CardLayout 패널
    private JPanel currentPanel; // 현재 화면에 표시 중인 패널 참조

    // 로그인 UI 요소
    private JTextField nameField;
    private JTextField ipField;
    private JButton loginButton;
    private JLabel statusLabel;

    public MainFrame(GameClient client) {
        super("판뒤집기 - 로그인");

        this.connection = new ServerConnection(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLayout(new BorderLayout(10, 10));

        // CardLayout을 사용하여 패널 전환 관리
        contentPanel = new JPanel(new CardLayout());
        add(contentPanel, BorderLayout.CENTER);

        // 로그인 화면 초기화 및 추가
        initializeLoginComponent();

        setVisible(true);
    }

    /**
     * 로그인 화면의 UI 컴포넌트를 초기화하고 contentPanel에 추가합니다.
     */
    private void initializeLoginComponent() {

        // 1. 배경 이미지 로드
        Image bgImage = new ImageIcon(
                getClass().getResource("/tg_start.png")   // images 폴더가 Source Folder라면 이 경로
        ).getImage();

        // 2. 배경을 그리는 패널 사용
        BackgroundPanel loginView = new BackgroundPanel(bgImage);
        loginView.setLayout(new BorderLayout());
        loginView.setOpaque(true);

        // ===== 1. 중앙 영역 (제목 + 폼 + 위쪽 빈 공간) =====
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        loginView.add(center, BorderLayout.CENTER);

        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.fill = GridBagConstraints.NONE;
        gbcCenter.insets = new Insets(10, 10, 10, 10);

        // 1-1. 맨 위 빈 공간 (내용을 아래로 밀어내는 역할)
        gbcCenter.gridy = 0;
        gbcCenter.weighty = 1;
        center.add(Box.createVerticalStrut(0), gbcCenter);

        // 1-2. 큰 제목 "판뒤집기"
        JLabel titleLabel = new JLabel(" ", SwingConstants.CENTER);
        // 제목 더 크게
        titleLabel.setFont(UITheme.TITLE_FONT); // 72pt 정도, 원하면 숫자 조정
        titleLabel.setForeground(UITheme.ACCENT);

        gbcCenter.gridy = 1;
        gbcCenter.weighty = 1;                 // 제목 자체는 고정 높이
        gbcCenter.anchor = GridBagConstraints.CENTER;
        center.add(titleLabel, gbcCenter);

        // 1-3. 닉네임/서버IP/게임 시작 폼 패널
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setPreferredSize(new Dimension(700, 300));  // 전체 폼 크기

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        // 닉네임 레이블
        JLabel nameLabel = new JLabel("닉네임");
        nameLabel.setFont(UITheme.NORMAL_FONT);
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(nameLabel, gbc);

        // 닉네임 입력칸
        nameField = new RoundJTextField(15);
        nameField.setText("Player7");
        nameField.setPreferredSize(new Dimension(280, 40));
        gbc.gridx = 1; gbc.gridy = 0;
        formPanel.add(nameField, gbc);

        // 서버 IP 레이블
        JLabel ipLabel = new JLabel("서버 IP");
        ipLabel.setFont(UITheme.NORMAL_FONT);
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(ipLabel, gbc);

        // 서버 IP 입력칸
        ipField = new RoundJTextField(15);
        ipField.setText("127.0.0.1");
        ipField.setPreferredSize(new Dimension(280, 40));
        gbc.gridx = 1; gbc.gridy = 1;
        formPanel.add(ipField, gbc);

        // 게임 시작 버튼 (두 줄 높이)
        loginButton = new RoundButton("게임 시작");
        loginButton.setFont(UITheme.BUTTON_FONT);
        loginButton.setPreferredSize(new Dimension(150, 90));
        loginButton.addActionListener(e -> attemptLogin());
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(Color.BLACK);

        gbc.gridx = 2; gbc.gridy = 0;
        gbc.gridheight = 2;                           // 닉네임/서버IP 두 줄 합치기
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(loginButton, gbc);

        // 폼 패널을 중앙 패널의 아래쪽에 배치
        gbcCenter.gridy = 2;
        gbcCenter.weighty = 0;                        // 제목 바로 아래 위치
        gbcCenter.anchor = GridBagConstraints.NORTH;  // 위쪽으로 붙이기
        center.add(formPanel, gbcCenter);

        // ===== 2. 하단 상태 표시 =====
        statusLabel = new JLabel("상태: 접속 대기 중", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.NORMAL_FONT);
        statusLabel.setForeground(UITheme.TEXT_SUB);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 40, 0));
        loginView.add(statusLabel, BorderLayout.SOUTH);

        // ===== 3. UI 테마 적용 및 contentPanel에 등록 =====
        UITheme.applyTheme(loginView);

        contentPanel.add(loginView, "Login");
        currentPanel = loginView;
    }



    /**
     * 로그인 시도 로직 (버튼 클릭 시)
     */
    private void attemptLogin() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        updateStatus("서버 연결 시도 중...");

        // 1. 서버 연결 시도
        if (connection.connect(ip)) {
            updateStatus("서버 연결 성공. 로그인 요청 중...");
            // 2. 로그인 요청 전송 (LOGIN_REQ|...)
            connection.sendLoginRequest(name);
            loginButton.setEnabled(false);
        } else {
            updateStatus("연결 실패");
        }
    }

    /**
     * 로그인 성공 시 처리 로직: 로비 화면으로 전환
     */
    public void handleLoginSuccess(String playerName, String playerId) {
        updateStatus(playerName + "님, 로그인 성공!");

        // 로비 패널 생성
        LobbyPanel lobbyPanel = new LobbyPanel(connection, playerName, playerId);

        // 패널 전환
        contentPanel.add(lobbyPanel, "Lobby");
        CardLayout cl = (CardLayout) (contentPanel.getLayout());
        cl.show(contentPanel, "Lobby");
        currentPanel = lobbyPanel; // 현재 패널 업데이트

        setTitle("판뒤집기 - 로비"); // 프레임 제목 변경
        // 로그인 버튼 재활성화 (로그인 화면으로 돌아갈 경우를 대비)
        loginButton.setEnabled(true);
    }

    /**
     * 상태 레이블 텍스트 업데이트
     */
    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("상태: " + status));
    }

    /**
     * 현재 표시 중인 패널을 반환합니다. (ServerConnection에서 사용)
     */
    public JPanel getCurrentPanel() {
        return currentPanel;
    }
}