package View;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared UI helpers for the Tourism Management System.
 * Loads images robustly from classpath OR project folders (works in IDE + Ant).
 */
public final class UIHelper {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private UIHelper() {}

    public static ImageIcon loadImage(String name, int w, int h) {
        if (w <= 0 || h <= 0) return null;
        try {
            BufferedImage source = CACHE.get(name);
            if (source == null) {
               

                if (source == null) {
                    URL url = UIHelper.class.getResource("/" + name);
                    if (url == null) url = UIHelper.class.getClassLoader().getResource(name);
                    if (url == null) url = UIHelper.class.getResource("/resources/" + name);
                    if (url == null) url = UIHelper.class.getClassLoader().getResource("resources/" + name);
                    if (url != null) source = ImageIO.read(url);
                }

                if (source == null) {
                    String[] tries = {
                        "src/resources/" + name,
                        "resources/" + name,
                        "build/resources/" + name,
                        "../src/resources/" + name
                    };
                    for (String p : tries) {
                        File f = new File(p);
                        if (f.exists()) { source = ImageIO.read(f); if (source != null) break; }
                    }
                }
                if (source == null) {
                    System.out.println("[UIHelper] Image not found: " + name);
                    return null;
                }
                CACHE.put(name, source);
            }
            Image img = source.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.out.println("[UIHelper] loadImage error: " + e.getMessage());
            return null;
        }
    }

    public static void backToLogin(JFrame current) {
        if (current != null) current.dispose();
        new Login();
    }

    /**
     * Fully self-painted button. Earlier versions relied on the ButtonUI delegate
     * (via super.paintComponent -> ui.update/paint) to draw the text, and on
     * isRollover()/isPressed() model state for the fill. On some Look-and-Feels
     * (GTK/Linux, some Windows skins) that pipeline only repaints — or only paints
     * the fill — on hover/focus, so the button looked blank/invisible until the
     * cursor touched it. To make this bulletproof across every platform, this
     * button draws its OWN background, border AND text every time, and never
     * delegates to the UI or to super.paintComponent() at all — so there is no
     * code path where the fill depends on mouse state.
     */
    public static JButton primaryButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background always painted — pressed/hover only shade it, never hide it.
                Color base = getBackground();
                if (!isEnabled()) base = new Color(180, 180, 180);
                else if (getModel().isPressed()) base = base.darker();
                else if (getModel().isRollover()) base = base.brighter();
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // Subtle border so the button reads as a button even on flat colors.
                g2.setColor(base.darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // Text, centered, drawn ourselves (no dependency on ButtonUI at all).
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int tx = (getWidth() - fm.stringWidth(txt)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(getForeground());
                g2.drawString(txt, tx, ty);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setRolloverEnabled(true);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setMargin(new Insets(0, 0, 0, 0));
        return b;
    }

    /**
     * A low-emphasis "outline" button — transparent fill, coloured 1px border
     * and coloured text, filling in softly on hover. Used for secondary
     * actions (Clear, Cancel) so a toolbar isn't a wall of solid colour.
     * Like primaryButton it paints everything itself, so it can't fall victim
     * to a Look-and-Feel that refuses to honour setBackground().
     */
    public static JButton outlineButton(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                boolean hot = getModel().isRollover() || getModel().isPressed();
                if (hot) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                }
                g2.setColor(isEnabled() ? accent : new Color(180, 180, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int tx = (getWidth() - fm.stringWidth(txt)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(isEnabled() ? accent.darker() : new Color(150, 150, 150));
                g2.drawString(txt, tx, ty);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setRolloverEnabled(true);
        b.setForeground(accent);
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setMargin(new Insets(0, 0, 0, 0));
        return b;
    }
}
