package View;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Map;

/** Small helpers shared by every dashboard — form layout, buttons, tables,
 *  search boxes, sidebar nav buttons, KPI/stat cards and pill-style status
 *  badges. Keeping them here means all four role dashboards inherit the same
 *  look, so a theme change lands everywhere at once. */
public final class Widgets {
    private Widgets(){}
    static final Color DARK=Theme.DARK;

    public static JLabel lbl(String s,int x,int y){
        JLabel l=new JLabel(s);
        l.setFont(new Font("Segoe UI Emoji",Font.BOLD,12));
        l.setForeground(DARK);
        l.setBounds(x,y,150,26);
        return l;
    }

    public static JTextField field(){
        JTextField t=new JTextField();
        t.setFont(new Font("Segoe UI",Font.PLAIN,13));
        t.setBackground(Color.WHITE);
        t.setForeground(Theme.DARK);
        t.setCaretColor(Theme.OCEAN);
        t.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(Theme.BORDER,1),
          BorderFactory.createEmptyBorder(7,9,7,9)));
        return t;
    }

    public static JButton btn(String text,Color bg){
        return UIHelper.primaryButton(text,bg,Color.WHITE);
    }

    /** A low-emphasis outlined button, for secondary actions like "Clear". */
    public static JButton ghostBtn(String text,Color accent){
        return UIHelper.outlineButton(text,accent);
    }

    /** Adds a "label:  field" row to a GridBagLayout form panel at the given row. */
    public static void addField(JPanel form,int row,String label,JComponent field){
        GridBagConstraints lc=new GridBagConstraints();
        lc.gridx=0; lc.gridy=row; lc.anchor=GridBagConstraints.WEST;
        lc.insets=new Insets(7,10,7,8);
        JLabel l=new JLabel(label);
        l.setFont(new Font("Segoe UI Emoji",Font.BOLD,12));
        l.setForeground(Theme.DARK);
        form.add(l,lc);

        GridBagConstraints fc=new GridBagConstraints();
        fc.gridx=1; fc.gridy=row; fc.weightx=1; fc.fill=GridBagConstraints.HORIZONTAL;
        fc.insets=new Insets(7,0,7,10);
        if(field instanceof JComboBox){
            field.setFont(new Font("Segoe UI",Font.PLAIN,13));
            styleCombo((JComboBox<?>)field);
        }
        form.add(field,fc);
    }

    /** Makes a combo box match the flat, bordered look of the text fields. */
    public static void styleCombo(JComboBox<?> combo){
        combo.setFont(new Font("Segoe UI",Font.PLAIN,13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(Theme.DARK);
        combo.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        combo.setFocusable(false);
    }

    /** Lays a set of buttons out evenly in a row, filling the available width. */
    public static JPanel buttonRow(JButton... buttons){
        JPanel row=new JPanel(new GridLayout(1,buttons.length,8,0));
        row.setOpaque(false);
        for(JButton b:buttons) row.add(b);
        return row;
    }

    /** Adds a labeled search field to a toolbar panel and returns the field. */
    public static JTextField searchBox(JPanel toolbar,String label){
        JLabel l=new JLabel(label);
        l.setFont(new Font("Segoe UI Emoji",Font.BOLD,12));
        l.setForeground(Theme.DARK);
        JTextField t=new JTextField(16);
        t.setFont(new Font("Segoe UI",Font.PLAIN,13));
        t.setBackground(Color.WHITE);
        t.setForeground(Theme.DARK);
        t.setCaretColor(Theme.OCEAN);
        t.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(Theme.OCEAN,1),
          BorderFactory.createEmptyBorder(6,9,6,9)));
        toolbar.add(l);
        toolbar.add(t);
        return t;
    }

    // =====================================================================
    // Tables
    // =====================================================================

    /**
     * Styles a JTable and — importantly — forces the column headers to be
     * readable.
     *
     * Why a custom header renderer is required: calling only
     * header.setBackground(...) / setForeground(...) is unreliable, because
     * several Look-and-Feels (Windows and Metal in particular) install their
     * own header renderer that paints a system-drawn gradient and ignores the
     * colours you set. The result is the bug you saw in the screenshots —
     * white header text landing on a light system-grey strip, so the column
     * names were effectively invisible. Installing our own opaque renderer
     * below takes the LAF out of the loop entirely, so the header is ALWAYS
     * white bold text on deep petrol (contrast ratio ~12:1).
     */
    public static void styleTable(JTable t){
        t.setRowHeight(32);
        t.setFont(new Font("Segoe UI",Font.PLAIN,13));
        t.setForeground(Theme.DARK);
        t.setBackground(Color.WHITE);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0,0));
        t.setSelectionBackground(Theme.SKY);
        t.setSelectionForeground(Theme.DARK);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        applyHeaderStyle(t);

        // Zebra-striped rows so wide tables full of CRUD data are easy to scan.
        DefaultTableCellRenderer stripe=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tbl,Object v,boolean sel,boolean foc,int row,int col){
                Component c=super.getTableCellRendererComponent(tbl,v,sel,foc,row,col);
                if(!sel){
                    c.setBackground(row%2==0?Color.WHITE:Theme.CREAM);
                    c.setForeground(Theme.DARK);
                }
                setBorder(BorderFactory.createEmptyBorder(0,10,0,8));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class,stripe);
    }

    /**
     * Installs the always-visible header renderer. Public and separate from
     * styleTable so it can be re-applied after a table's model is swapped —
     * setModel() rebuilds the column model, which drops any per-column
     * renderers that were previously installed.
     */
    public static void applyHeaderStyle(JTable t){
        JTableHeader h=t.getTableHeader();
        if(h==null) return;
        h.setReorderingAllowed(false);
        h.setResizingAllowed(true);
        h.setPreferredSize(new Dimension(h.getWidth(),38));
        h.setBackground(Theme.DARK);
        h.setForeground(Color.WHITE);
        h.setFont(new Font("Segoe UI",Font.BOLD,13));
        h.setBorder(BorderFactory.createEmptyBorder());

        h.setDefaultRenderer(new TableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tbl,Object value,
                    boolean sel,boolean foc,int row,int col){
                JLabel l=new JLabel(value==null?"":value.toString()){
                    @Override protected void paintComponent(Graphics g){
                        Graphics2D g2=(Graphics2D)g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        // Solid deep-petrol fill, painted by us — never by the LAF.
                        g2.setColor(Theme.DARK);
                        g2.fillRect(0,0,getWidth(),getHeight());
                        // Thin divider between columns + warm underline for the whole strip.
                        g2.setColor(Theme.NAVY_2);
                        g2.drawLine(getWidth()-1,7,getWidth()-1,getHeight()-8);
                        g2.setColor(Theme.SUN);
                        g2.fillRect(0,getHeight()-3,getWidth(),3);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                l.setOpaque(false);
                l.setForeground(Color.WHITE);          // white on #12303A -> ~12:1 contrast
                l.setFont(new Font("Segoe UI",Font.BOLD,13));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0,10,0,8));
                l.setPreferredSize(new Dimension(l.getPreferredSize().width,38));
                return l;
            }
        });
    }

    /**
     * Colors a status/state column (e.g. "Confirmed"/"Cancelled"/"Pending")
     * with bold coloured text so CRUD tables read at a glance.
     */
    public static void colorStatusColumn(JTable t,int col,Map<String,Color> colors){
        if(col<0||col>=t.getColumnModel().getColumnCount()) return;
        t.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tbl,Object v,boolean sel,boolean foc,int row,int c){
                JLabel l=(JLabel) super.getTableCellRendererComponent(tbl,v,sel,foc,row,c);
                l.setHorizontalAlignment(CENTER);
                l.setFont(new Font("Segoe UI",Font.BOLD,12));
                if(!sel) l.setBackground(row%2==0?Color.WHITE:Theme.CREAM);
                Color c2=colors.getOrDefault(String.valueOf(v),Theme.SUBTEXT);
                l.setForeground(sel?Theme.DARK:c2.darker());
                l.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                return l;
            }
        });
    }

    /**
     * Renders a status column as a small rounded, filled "pill" —
     * used for Approved / Pending / Cancelled style status badges.
     */
    public static void pillStatusColumn(JTable t,int col,Map<String,Color> colors){
        if(col<0||col>=t.getColumnModel().getColumnCount()) return;
        t.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable tbl,Object v,boolean sel,boolean foc,int row,int c){
                String text=v==null?"":String.valueOf(v);
                Color base=colors.getOrDefault(text,Theme.SUBTEXT);
                Color rowBg=sel?Theme.SKY:(row%2==0?Color.WHITE:Theme.CREAM);
                JLabel l=new JLabel(text){
                    @Override protected void paintComponent(Graphics g){
                        Graphics2D g2=(Graphics2D)g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(rowBg);
                        g2.fillRect(0,0,getWidth(),getHeight());
                        if(!text.isEmpty()){
                            int pw=Math.min(getWidth()-10,Math.max(64,getFontMetrics(getFont()).stringWidth(text)+26));
                            int px=(getWidth()-pw)/2;
                            g2.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),40));
                            g2.fillRoundRect(px,6,pw,getHeight()-12,14,14);
                            g2.setColor(new Color(base.getRed(),base.getGreen(),base.getBlue(),110));
                            g2.drawRoundRect(px,6,pw,getHeight()-12,14,14);
                        }
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                l.setHorizontalAlignment(CENTER);
                l.setFont(new Font("Segoe UI",Font.BOLD,12));
                l.setForeground(base.darker());
                l.setOpaque(false);
                return l;
            }
        });
    }

    public static javax.swing.border.TitledBorder coloredTitle(String title){
        javax.swing.border.TitledBorder b = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Theme.OCEAN, 1), title);
        b.setTitleColor(Theme.DARK);
        b.setTitleFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        return b;
    }

    // =====================================================================
    // Cards & page furniture
    // =====================================================================

    /** Wraps content in a white "card" with a soft border, so every screen
     *  reads as a set of clean panels rather than a flat grey page. */
    public static JPanel card(String title){
        JPanel p=new JPanel(new BorderLayout(0,12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER,1),
            BorderFactory.createEmptyBorder(16,18,18,18)));
        if(title!=null){
            JPanel head=new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
            JLabel t=new JLabel(title);
            t.setFont(new Font("Segoe UI Emoji",Font.BOLD,15));
            t.setForeground(Theme.DARK);
            head.add(t,BorderLayout.WEST);
            JPanel rule=new JPanel();
            rule.setPreferredSize(new Dimension(0,3));
            rule.setBackground(Theme.SUN);
            JPanel wrap=new JPanel(new BorderLayout(0,8));
            wrap.setOpaque(false);
            wrap.add(head,BorderLayout.CENTER);
            wrap.add(rule,BorderLayout.SOUTH);
            p.add(wrap,BorderLayout.NORTH);
        }
        return p;
    }

    /** A plain white card with no title strip — used for toolbars and KPI rows. */
    public static JPanel plainCard(){
        JPanel p=new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER,1),
            BorderFactory.createEmptyBorder(16,18,16,18)));
        return p;
    }

    /** Standard page heading: big title on the left, breadcrumb on the right. */
    public static JPanel pageHeading(String title,String subtitle,String breadcrumb){
        JPanel head=new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));

        JPanel box=new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box,BoxLayout.Y_AXIS));
        JLabel h1=new JLabel(title);
        h1.setFont(new Font("Segoe UI",Font.BOLD,23));
        h1.setForeground(Theme.DARK);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(h1);
        if(subtitle!=null){
            JLabel h2=new JLabel(subtitle);
            h2.setFont(new Font("Segoe UI",Font.PLAIN,12));
            h2.setForeground(Theme.SUBTEXT);
            h2.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(Box.createVerticalStrut(3));
            box.add(h2);
        }
        head.add(box,BorderLayout.WEST);

        if(breadcrumb!=null){
            JLabel crumb=new JLabel(breadcrumb);
            crumb.setForeground(Theme.SUBTEXT);
            crumb.setFont(new Font("Segoe UI",Font.PLAIN,12));
            JPanel wrap=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,6));
            wrap.setOpaque(false);
            wrap.add(crumb);
            head.add(wrap,BorderLayout.EAST);
        }
        return head;
    }

    public static void styleTabs(JTabbedPane tabs){
        tabs.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        tabs.setBackground(Theme.LIGHT);
        tabs.setForeground(Theme.DARK);
        tabs.setOpaque(true);
        tabs.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
    }

    // =====================================================================
    // Sidebar / KPI widgets
    // =====================================================================

    /** A left-nav sidebar button: full width, flat, highlights when active. */
    public static JButton sidebarButton(String text, boolean active){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(active){
                    g2.setColor(Theme.OCEAN);
                    g2.fillRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                } else if(getModel().isRollover()){
                    g2.setColor(Theme.NAVY_2);
                    g2.fillRoundRect(8,2,getWidth()-16,getHeight()-4,10,10);
                }
                g2.setFont(getFont());
                g2.setColor(active?Color.WHITE:new Color(196,216,222));
                FontMetrics fm=g2.getFontMetrics();
                int ty=(getHeight()-fm.getHeight())/2+fm.getAscent();
                g2.drawString(getText(),22,ty);
                g2.dispose();
            }
            @Override public boolean isOpaque(){return false;}
        };
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("Segoe UI Emoji",active?Font.BOLD:Font.PLAIN,13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(220,42));
        b.setPreferredSize(new Dimension(220,42));
        return b;
    }

    /** A KPI/stat card: coloured icon chip, big number, title, muted subtitle
     *  and an optional "View X ->" link that fires onClick. */
    public static JPanel statCard(String icon,String title,String value,String subtitle,
                                   String linkText,Color accent,Runnable onClick){
        JPanel card=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setColor(accent);
                g2.fillRect(0,0,getWidth(),4);   // accent cap along the top edge
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER,1),
            BorderFactory.createEmptyBorder(16,18,14,18)));

        JLabel iconLbl=new JLabel(icon){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),38));
                g2.fillOval(0,0,36,36);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setPreferredSize(new Dimension(36,36));
        iconLbl.setMaximumSize(new Dimension(36,36));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji",Font.PLAIN,16));
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl=new JLabel(value);
        valLbl.setFont(new Font("Segoe UI",Font.BOLD,28));
        valLbl.setForeground(Theme.DARK);
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        valLbl.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));

        JLabel titleLbl=new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI",Font.BOLD,12));
        titleLbl.setForeground(Theme.SUBTEXT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLbl);
        card.add(valLbl);
        card.add(titleLbl);

        if(subtitle!=null){
            JLabel subLbl=new JLabel(subtitle);
            subLbl.setFont(new Font("Segoe UI",Font.PLAIN,11));
            subLbl.setForeground(Theme.SUBTEXT);
            subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            subLbl.setBorder(BorderFactory.createEmptyBorder(2,0,8,0));
            card.add(subLbl);
        }
        if(linkText!=null){
            JButton link=new JButton(linkText);
            link.setFont(new Font("Segoe UI",Font.BOLD,12));
            link.setForeground(accent.darker());
            link.setBorderPainted(false);
            link.setContentAreaFilled(false);
            link.setFocusPainted(false);
            link.setHorizontalAlignment(SwingConstants.LEFT);
            link.setMargin(new Insets(4,0,0,0));
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.setAlignmentX(Component.LEFT_ALIGNMENT);
            if(onClick!=null) link.addActionListener(e->onClick.run());
            card.add(link);
        }
        return card;
    }

    /** One row of the "Booking Overview" breakdown card: coloured bullet,
     *  status label, count and percentage — right-aligned like a mini table. */
    public static JPanel breakdownRow(String status,Color dot,int count,double pct,boolean bold){
        JPanel row=new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(7,0,7,0));

        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        left.setOpaque(false);
        if(dot!=null){
            JLabel bullet=new JLabel("\u25CF");
            bullet.setForeground(dot);
            left.add(bullet);
        }
        JLabel name=new JLabel(status);
        name.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,13));
        name.setForeground(Theme.DARK);
        left.add(name);

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,18,0));
        right.setOpaque(false);
        JLabel cnt=new JLabel(String.valueOf(count));
        cnt.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,13));
        cnt.setForeground(Theme.DARK);
        JLabel pctLbl=new JLabel(String.format("%.2f%%",pct));
        pctLbl.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,13));
        pctLbl.setForeground(Theme.SUBTEXT);
        right.add(cnt); right.add(pctLbl);

        row.add(left,BorderLayout.WEST);
        row.add(right,BorderLayout.EAST);
        return row;
    }

    /** A compact date-entry field pre-filled with a value (Report Generator). */
    public static JTextField dateField(String value){
        JTextField t=new JTextField(value,10);
        t.setFont(new Font("Segoe UI",Font.PLAIN,13));
        t.setBackground(Color.WHITE);
        t.setForeground(Theme.DARK);
        t.setCaretColor(Theme.OCEAN);
        t.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(Theme.BORDER,1),
          BorderFactory.createEmptyBorder(7,9,7,9)));
        return t;
    }

    /** A small labeled block (label above field) used in filter toolbars. */
    public static JPanel labeledBlock(String label,JComponent comp){
        JPanel p=new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel l=new JLabel(label);
        l.setFont(new Font("Segoe UI",Font.BOLD,11));
        l.setForeground(Theme.SUBTEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        if(comp instanceof JComboBox) styleCombo((JComboBox<?>)comp);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(5));
        p.add(comp);
        return p;
    }
}
