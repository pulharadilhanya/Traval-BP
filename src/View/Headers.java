package View;

import javax.swing.*;
import java.awt.*;

/**
 * Shared dashboard top bar used by the Tour Manager, Tour Guide and Service
 * Provider screens, so all three match the Admin Dashboard's header.
 *
 * Layout: brand block on the left (system name + company), the role title in
 * the middle, and Back / Logout on the right. Deliberately contains NO
 * notification bell or unread-count badge.
 */
public final class Headers {
    private Headers(){}

    /** Kept at the original signature so the three dashboards call it unchanged;
     *  the colour arguments are now only used for the two action buttons. */
    public static JPanel header(JFrame owner, String title, Color dark, Color gold, Color teal, Color red){
        JPanel h = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Horizontal petrol -> teal wash, so the bar has some depth
                // instead of reading as one flat block of colour.
                g2.setPaint(new GradientPaint(0, 0, Theme.DARK, getWidth(), 0, Theme.HEADER));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.SUN);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setBackground(Theme.DARK);
        h.setPreferredSize(new Dimension(1150, 72));
        h.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // ---- Brand block ---------------------------------------------------
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 14));
        left.setOpaque(false);

        JLabel logo = new JLabel("\uD83C\uDFDD");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        logo.setForeground(Color.WHITE);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Tourism Management System");
        t1.setForeground(Color.WHITE);
        t1.setFont(new Font("Segoe UI", Font.BOLD, 16));
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t2 = new JLabel("Travel BP Sri Lanka");
        t2.setForeground(new Color(186, 214, 221));
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        t2.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(t1);
        titles.add(t2);

        left.add(logo);
        left.add(titles);
        h.add(left, BorderLayout.WEST);

        // ---- Role chip -----------------------------------------------------
        JLabel role = new JLabel(title == null ? "" : title.trim());
        role.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        role.setForeground(Color.WHITE);
        role.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        role.setOpaque(false);
        JPanel roleWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 22)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        roleWrap.setOpaque(false);
        roleWrap.add(role);
        h.add(roleWrap, BorderLayout.CENTER);

        // ---- Actions -------------------------------------------------------
        JButton back = UIHelper.outlineButton("\u2190 Back", Color.WHITE);
        JButton logout = UIHelper.primaryButton("Log out", Theme.RED, Color.WHITE);
        back.addActionListener(e -> UIHelper.backToLogin(owner));
        logout.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(owner, "Log out of the system?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                UIHelper.backToLogin(owner);
            }
        });

        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        r.setOpaque(false);
        r.add(back);
        r.add(logout);
        h.add(r, BorderLayout.EAST);
        return h;
    }
}
