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
        setSize(400, 300);
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
        JPanel loginView = new JPanel(new BorderLayout(10, 10));

        // 중앙 패널 (로그인 입력 영역)
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 닉네임 입력
        loginPanel.add(new JLabel("닉네임:"));
        nameField = new JTextField("Player7");
        loginPanel.add(nameField);

        // 서버 IP 입력
        loginPanel.add(new JLabel("서버 IP:"));
        ipField = new JTextField("127.0.0.1"); // 로컬 테스트 시 기본값
        loginPanel.add(ipField);

        // 로그인 버튼
        loginButton = new JButton("게임 시작");
        loginButton.addActionListener(e -> attemptLogin());
        loginPanel.add(loginButton);

        // 상태 표시 레이블
        statusLabel = new JLabel("상태: 접속 대기 중", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE);

        // 컴포넌트 조합
        loginView.add(new JLabel("<html><h1 style='text-align:center;'>판뒤집기</h1></html>", SwingConstants.CENTER), BorderLayout.NORTH);
        loginView.add(loginPanel, BorderLayout.CENTER);
        loginView.add(statusLabel, BorderLayout.SOUTH);

        // contentPanel에 로그인 뷰 추가
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