package View;

import controller.LoginController;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Login — the application's entry screen. Collects a username/password,
 * authenticates via LoginController, and opens the correct role-specific
 * dashboard (Admin / Tour Manager / Tour Guide / Service Provider) based
 * on the returned User subtype.
 */
public class Login extends JFrame {

    // ---- Palette ---------------------------------------------------------
    // All colours come from Theme, so the login screen shares the same
    // palette as the rest of the app.
    private final Color DARK       = Theme.DARK;
    private final Color CARD_BG    = Theme.CARD_BG;      // login card surface
    private final Color FIELD_BG   = Theme.FIELD_BG;     // input surface
    private final Color BORDER_CLR = Theme.FIELD_BORDER;
    private final Color TEXT_LIGHT = Theme.TEXT_LIGHT;
    private final Color TEXT_MUTED = Theme.TEXT_MUTED;
    private final Color SUN        = Theme.SUN;          // warm sand accent
    private final Color BTN_BLUE   = Theme.OCEAN;        // teal - login button
    private final Color BTN_BLUE_H = Theme.OCEAN_HOVER;  // hover

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnTogglePassword;
    private boolean passwordVisible = false;

    private LoginController loginController;

    /** Builds and shows the Login window. */
    public Login() {

        loginController = new LoginController();

        setTitle("Tourism Management System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();

        setVisible(true);
    }

    private void buildUI() {

        getContentPane().setBackground(Theme.LOGIN_BG);
        setLayout(new GridBagLayout());

        RoundedPanel card = new RoundedPanel(22, CARD_BG);
        card.setPreferredSize(new Dimension(1150, 620));
        card.setLayout(new GridLayout(1, 2));
        card.setBorder(new EmptyBorder(0, 0, 0, 0));

        card.add(buildLeftPanel());
        card.add(buildRightPanel());

        add(card);
    }

    // ---------------- LEFT BRANDING PANEL ----------------
private JPanel buildLeftPanel() {

    JPanel left = new JPanel(new GridBagLayout());
    left.setOpaque(false);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.CENTER;

    JPanel brandPanel = new JPanel();
    brandPanel.setOpaque(false);
    brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

    // Logo
    JLabel logo = buildLogoLabel();
    logo.setAlignmentX(Component.CENTER_ALIGNMENT);
    brandPanel.add(logo);

    brandPanel.add(Box.createVerticalStrut(25));

    // Main Title
    JLabel title = new JLabel("TRAVEL BP");
    title.setFont(new Font("Georgia", Font.BOLD, 72)); // Larger
    title.setForeground(Color.WHITE);
    title.setAlignmentX(Component.CENTER_ALIGNMENT);
    brandPanel.add(title);

    brandPanel.add(Box.createVerticalStrut(12));

    // Subtitle
    JLabel subtitle = new JLabel("TOUR MANAGEMENT SYSTEM");
    subtitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
    subtitle.setForeground(SUN);
    subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
    brandPanel.add(subtitle);

    left.add(brandPanel, gbc);

    return left;
}

    private JLabel buildLogoLabel() {
        // Try to load an actual logo image if present; otherwise draw a placeholder circle.
        ImageIcon icon = null;
        java.net.URL url = getClass().getResource("/logo.png");
        if (url != null) {
            Image img = new ImageIcon(url).getImage()
                    .getScaledInstance(140, 140, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }

        JLabel label;
        if (icon != null) {
            label = new JLabel(icon);
        } else {
            label = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(0, 0, 140, 140);
                    g2.setColor(DARK);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(1, 1, 137, 137);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
                    FontMetrics fm = g2.getFontMetrics();
                    String glyph = "\u2708"; // airplane glyph
                    int tx = (140 - fm.stringWidth(glyph)) / 2;
                    int ty = (140 + fm.getAscent()) / 2 - 6;
                    g2.setColor(DARK);
                    g2.drawString(glyph, tx, ty);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(140, 140);
                }
            };
        }
        return label;
    }

    // ---------------- RIGHT FORM PANEL ----------------

    private JPanel buildRightPanel() {

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(70, 70, 40, 70));

        JLabel heading = new JLabel("Welcome Back!");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 30));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(heading);

        right.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("Please login to access your account");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(sub);

        right.add(Box.createVerticalStrut(35));

        // Username
        right.add(createFieldLabel("Username"));
        right.add(Box.createVerticalStrut(8));
        txtUsername = new JTextField();
        JPanel userField = wrapFieldWithIcon("\uD83D\uDC64", txtUsername, null);
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleField(txtUsername);
        txtUsername.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        right.add(userField);

        right.add(Box.createVerticalStrut(20));

        // Password
        right.add(createFieldLabel("Password"));
        right.add(Box.createVerticalStrut(8));
        txtPassword = new JPasswordField();
        styleField(txtPassword);
        txtPassword.setEchoChar('\u2022');
        txtPassword.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        btnTogglePassword = new JButton("\uD83D\uDC41");
        stylePlainIconButton(btnTogglePassword);
        btnTogglePassword.addActionListener(e -> togglePasswordVisibility());

        JPanel passField = wrapFieldWithIcon("\uD83D\uDD12", txtPassword, btnTogglePassword);
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(passField);

        right.add(Box.createVerticalStrut(30));

        // Login button
        JButton btnLogin = new RoundedButton("→]  LOG IN", BTN_BLUE, BTN_BLUE_H, Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnLogin.setPreferredSize(new Dimension(Integer.MAX_VALUE, 48));
        right.add(btnLogin);

        right.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("© 2025 TRAVEL BP. All Rights Reserved.");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(TEXT_MUTED);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(footer);

        // Enter key triggers login
        btnLogin.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());

        return right;
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_LIGHT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel wrapFieldWithIcon(String iconGlyph, JTextField field, JButton trailingButton) {

        RoundedPanel wrapper = new RoundedPanel(10, FIELD_BG);
        wrapper.setLayout(new BorderLayout(8, 0));
        wrapper.setBorder(new EmptyBorder(8, 14, 8, 14));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        wrapper.setPreferredSize(new Dimension(400, 46));

        JLabel iconLbl = new JLabel(iconGlyph);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        iconLbl.setForeground(TEXT_MUTED);
        wrapper.add(iconLbl, BorderLayout.WEST);

        wrapper.add(field, BorderLayout.CENTER);

        if (trailingButton != null) {
            wrapper.add(trailingButton, BorderLayout.EAST);
        }

        return wrapper;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        txtPassword.setEchoChar(passwordVisible ? '\0' : '\u2022');
        btnTogglePassword.setText(passwordVisible ? "\uD83D\uDE48" : "\uD83D\uDC41");
    }

    private void styleField(JTextField field) {
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
    }

    private void stylePlainIconButton(JButton btn) {
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btn.setForeground(TEXT_MUTED);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void checkDbStatus(JLabel label) {

        new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                return database.DBConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();

                    if (ok) {
                        label.setText("Connected to MySQL - tourism_db");
                        label.setForeground(Theme.LEAF);
                    } else {
                        label.setText("Not connected to MySQL");
                        label.setForeground(Theme.RED);
                    }

                } catch (Exception ex) {
                    label.setText("Not connected to MySQL");
                    label.setForeground(Theme.RED);
                }
            }

        }.execute();
    }

    private void doLogin() {

        String username = txtUsername.getText().trim();
        String password = String.valueOf(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter username and password.");

            return;
        }

        User loggedIn = loginController.login(username, password);

        if (loggedIn == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid username or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);

            return;
        }

        dispose();

        switch (loggedIn.getRole()) {

            case "Admin":
                new AdminDashboard(loggedIn.getUsername());
                break;

            case "TourManager":
                new TourManagerDashboard(loggedIn.getUsername());
                break;

            case "TourGuide":
                new TourGuideDashboard(loggedIn.getUsername());
                break;

            case "ServiceProvider":
                new ServiceProviderDashboard(loggedIn.getUsername());
                break;

            default:
                JOptionPane.showMessageDialog(null, "Unknown role.");
        }
    }

    // ---------------- Reusable rounded components ----------------

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color normal;
        private final Color hover;

        RoundedButton(String text, Color normal, Color hover, Color fg) {
            super(text);
            this.normal = normal;
            this.hover = hover;
            setForeground(fg);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getModel().isRollover() ? hover : normal;
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}