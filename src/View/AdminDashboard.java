package View;

import controller.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin Dashboard.
 *
 * Structure: top bar (NORTH) + sidebar nav (WEST) + card-swapped content (CENTER).
 *
 * Notable behaviours:
 *  - System Settings has been moved OFF the sidebar and into the account menu
 *    in the top-right, next to the admin's name.
 *  - Every screen reloads its data on navigation (see switchTo), which is what
 *    fixes the Reports screen showing stale numbers.
 *  - The top bar deliberately has no notification bell / unread badge.
 */
public class AdminDashboard extends JFrame {
    private final Color DARK=Theme.DARK, OCEAN=Theme.OCEAN,
      LIGHT=Theme.LIGHT,
      GREEN=Theme.GREEN, RED=Theme.RED, ORANGE=Theme.ORANGE, PURPLE=Theme.PURPLE;

    private final String username;
    private final UserController uc = new UserController();
    private final TourController tc = new TourController();
    private final BookingController bc = new BookingController();
    private final AdminController ac = new AdminController();

    private JTable userTable, tourTable, bookTable, recentTable, reportTable;
    private DefaultTableModel userModel, tourModel, bookModel, recentModel, reportModel;
    private JTextField uId,uUser,uPass,uEmail,uContact;
    private JComboBox<String> uRole;
    private JTextField trId,trName,trDest,trDesc,trDur,trPrice;
    private JComboBox<String> trStatus;

    private final CardLayout cl = new CardLayout();
    private final JPanel contentPanel = new JPanel(cl);
    private final Map<String,NavButton> navButtons = new LinkedHashMap<>();

    // ---- Report Generator state ------------------------------------------
    private JComboBox<String> reportType, reportStatus;
    private JTextField dateFrom, dateTo;
    private JPanel reportSummaryHolder;
    private JLabel reportCountLbl;

    /** The exact data currently on screen — reused verbatim by the PDF/CSV
     *  exporters so what you print is always what you see. */
    private String[] repHeaders = new String[0];
    private List<String[]> repRows = new ArrayList<>();
    private List<String[]> repSummary = new ArrayList<>();
    private String repTitle = "Booking Report";
    private String repSubtitle = "";

    // ---- Report defaults (fixed values) ------------------------------------
    private String prefDefaultReport = "Booking Report";
    private int prefDefaultRangeDays = 30;

    private static final String[] REPORT_TYPES = {
        "Booking Report", "Revenue by Tour", "Tour Report", "User Report"
    };

    /** Builds and shows the Admin Dashboard window for the given logged-in user. */
    public AdminDashboard(String username) {
        this.username = username;
        setTitle("Tourism Management System \u2014 Admin Dashboard");
        setSize(1320, 780);
        setMinimumSize(new Dimension(1100,660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
        setVisible(true);
    }

    // ------------------------------------------------------------------
    // Overall layout
    // ------------------------------------------------------------------
    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT);
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.add(buildSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(LIGHT);
        contentPanel.add(wrap(buildHome()), "home");
        contentPanel.add(wrap(buildUsers()), "users");
        contentPanel.add(wrap(buildTours()), "tours");
        contentPanel.add(wrap(buildBookings()), "bookings");
        contentPanel.add(wrap(buildReportGenerator()), "reports");
        body.add(contentPanel, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        switchTo("home");
    }

    private JPanel wrap(JPanel p){
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(LIGHT);
        outer.add(p, BorderLayout.CENTER);
        return outer;
    }

    /**
     * Shows a screen AND refreshes it. Refreshing on navigation is what makes
     * the Reports page (and every table) reflect the current database instead
     * of whatever was loaded when the window was first built.
     */
    private void switchTo(String key){
        cl.show(contentPanel, key);
        for (Map.Entry<String,NavButton> e : navButtons.entrySet()) {
            e.getValue().setActiveState(e.getKey().equals(key));
        }
        switch (key) {
            case "home":     refreshHome();     break;
            case "users":    loadUsers();       break;
            case "tours":    loadTours();       break;
            case "bookings": loadBookings();    break;
            case "reports":  generateReport();  break;
            default: break;
        }
    }

    // ------------------------------------------------------------------
    // Top bar
    // ------------------------------------------------------------------
    private JPanel buildTopBar(){
        JPanel bar = new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,Theme.DARK,getWidth(),0,Theme.HEADER));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(Theme.SUN);
                g2.fillRect(0,getHeight()-3,getWidth(),3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100,70));
        bar.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,12,13));
        left.setOpaque(false);
        JLabel logo = new JLabel("\uD83C\uDFDD");
        logo.setFont(new Font("Segoe UI Emoji",Font.PLAIN,26));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles,BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Tourism Management System");
        t1.setForeground(Color.WHITE);
        t1.setFont(new Font("Segoe UI",Font.BOLD,16));
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t2 = new JLabel("Travel BP Sri Lanka");
        t2.setForeground(new Color(186,214,221));
        t2.setFont(new Font("Segoe UI",Font.PLAIN,11));
        t2.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(t1); titles.add(t2);
        left.add(logo); left.add(titles);

        // The top bar intentionally has no account menu; Log out is
        // available from the sidebar instead.
        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    private String displayName(){
        return (username==null||username.isBlank()) ? "Admin" : username;
    }

    private void confirmLogout(){
        if (JOptionPane.showConfirmDialog(this,"Log out of the Admin Dashboard?","Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            UIHelper.backToLogin(this);
        }
    }

    // ------------------------------------------------------------------
    // Sidebar
    // ------------------------------------------------------------------
    private JPanel buildSidebar(){
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(DARK);
        side.setPreferredSize(new Dimension(232, 0));
        side.setBorder(BorderFactory.createEmptyBorder(22,0,20,0));

        JPanel profile = new JPanel();
        profile.setOpaque(false);
        profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.setMaximumSize(new Dimension(232,110));
        profile.setBorder(BorderFactory.createEmptyBorder(0,22,18,20));

        JLabel avatar = new JLabel("\uD83D\uDC64");
        avatar.setFont(new Font("Segoe UI Emoji",Font.PLAIN,30));
        avatar.setForeground(Color.WHITE);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel(displayName());
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Segoe UI",Font.BOLD,15));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel role = new JLabel("Admin");
        role.setForeground(new Color(160,190,198));
        role.setFont(new Font("Segoe UI",Font.PLAIN,11));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.add(avatar); profile.add(Box.createVerticalStrut(6));
        profile.add(name); profile.add(role);

        side.add(profile);
        side.add(divider());
        side.add(Box.createVerticalStrut(12));

        side.add(navItem("Dashboard","home"));
        side.add(navItem("Manage Users","users"));
        side.add(navItem("Manage Tours","tours"));
        side.add(navItem("Approve Bookings","bookings"));
        side.add(navItem("View Reports","reports"));

        side.add(Box.createVerticalGlue());
        side.add(divider());
        side.add(Box.createVerticalStrut(10));

        JButton logout = new NavButton("Log out", false);
        logout.addActionListener(e -> confirmLogout());
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logout);
        return side;
    }

    private JComponent divider(){
        JPanel d = new JPanel();
        d.setBackground(Theme.NAVY_2);
        d.setMaximumSize(new Dimension(232,1));
        d.setPreferredSize(new Dimension(232,1));
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        return d;
    }

    private NavButton navItem(String label, String key){
        NavButton b = new NavButton(label, key.equals("home"));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> switchTo(key));
        navButtons.put(key, b);
        return b;
    }

    /** Sidebar nav button with a mutable "active" highlight. */
    private class NavButton extends JButton {
        private boolean active;
        NavButton(String text, boolean active){
            super(text);
            this.active = active;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(new Font("Segoe UI",active?Font.BOLD:Font.PLAIN,13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(232,44));
            setPreferredSize(new Dimension(232,44));
            setBorder(BorderFactory.createEmptyBorder(0,20,0,10));
        }
        void setActiveState(boolean a){
            active = a;
            setFont(new Font("Segoe UI",active?Font.BOLD:Font.PLAIN,13));
            repaint();
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(active){
                g2.setColor(Theme.OCEAN);
                g2.fillRoundRect(10,3,getWidth()-20,getHeight()-6,10,10);
                g2.setColor(Theme.SUN);
                g2.fillRoundRect(10,9,4,getHeight()-18,4,4);
            } else if(getModel().isRollover()){
                g2.setColor(Theme.NAVY_2);
                g2.fillRoundRect(10,3,getWidth()-20,getHeight()-6,10,10);
            }
            g2.setFont(getFont());
            g2.setColor(active?Color.WHITE:new Color(196,216,222));
            FontMetrics fm=g2.getFontMetrics();
            int ty=(getHeight()-fm.getHeight())/2+fm.getAscent();
            g2.drawString(getText(),24,ty);
            g2.dispose();
        }
        @Override public boolean isOpaque(){ return false; }
    }

    // ------------------------------------------------------------------
    // Dashboard home
    // ------------------------------------------------------------------
    private JPanel homeRoot;
    private JPanel statsRow, overviewCard;

    private JPanel buildHome(){
        homeRoot = new JPanel(new BorderLayout(0,16));
        homeRoot.setBackground(LIGHT);
        homeRoot.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));

        JPanel headRow = Widgets.pageHeading("Admin Dashboard",
            "Welcome back, "+displayName()+". Here is what's happening today.", null);

        statsRow = new JPanel(new GridLayout(1,5,16,0));
        statsRow.setOpaque(false);

        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints g1=new GridBagConstraints();
        g1.gridx=0; g1.gridy=0; g1.weightx=0.66; g1.weighty=1; g1.fill=GridBagConstraints.BOTH; g1.insets=new Insets(0,0,0,8);
        lower.add(buildRecentBookingsCard(), g1);
        GridBagConstraints g2c=new GridBagConstraints();
        g2c.gridx=1; g2c.gridy=0; g2c.weightx=0.34; g2c.weighty=1; g2c.fill=GridBagConstraints.BOTH; g2c.insets=new Insets(0,8,0,0);
        overviewCard = buildOverviewCard();
        lower.add(overviewCard, g2c);

        homeRoot.add(headRow, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0,16));
        mid.setOpaque(false);
        mid.add(statsRow, BorderLayout.NORTH);
        mid.add(lower, BorderLayout.CENTER);
        homeRoot.add(mid, BorderLayout.CENTER);

        refreshHome();
        return homeRoot;
    }

    private JPanel dashCardHeader(String title, String linkText, Runnable onLink){
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI Emoji",Font.BOLD,15));
        t.setForeground(DARK);
        h.add(t, BorderLayout.WEST);
        if (linkText != null) {
            JButton link = new JButton(linkText);
            link.setFont(new Font("Segoe UI",Font.BOLD,12));
            link.setForeground(Theme.OCEAN);
            link.setBorderPainted(false);
            link.setContentAreaFilled(false);
            link.setFocusPainted(false);
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (onLink != null) link.addActionListener(e -> onLink.run());
            h.add(link, BorderLayout.EAST);
        }
        return h;
    }

    private JPanel buildRecentBookingsCard(){
        JPanel card = Widgets.plainCard();
        card.setLayout(new BorderLayout(0,8));
        card.add(dashCardHeader("\uD83D\uDCCB  Recent Bookings","View All \u2192", () -> switchTo("bookings")), BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"Booking ID","Customer","Tour","Date","Status"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        recentTable = new JTable(recentModel);
        Widgets.styleTable(recentTable);
        Widgets.pillStatusColumn(recentTable,4,Map.of(
            "Confirmed",GREEN,"Approved",GREEN,"Pending",ORANGE,"Cancelled",RED));
        JScrollPane sp = new JScrollPane(recentTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildOverviewCard(){
        JPanel card = Widgets.plainCard();
        card.setLayout(new BorderLayout(0,4));
        card.add(dashCardHeader("\uD83D\uDCCA  Booking Overview", null, null), BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setName("overviewRows");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(rows, BorderLayout.NORTH);
        card.add(top, BorderLayout.CENTER);
        return card;
    }

    /** Recomputes every live number on the Dashboard home screen from the DB. */
    private void refreshHome(){
        if (homeRoot == null) return;

        List<Object[]> allBookings = bc.getAll();
        int totalUsers = uc.getAllUsers().size();
        int totalBookings = allBookings.size();
        long approved = allBookings.stream().filter(r -> "Confirmed".equals(r[6])).count();
        long pending = allBookings.stream().filter(r -> "Pending".equals(r[6])).count();
        long cancelled = allBookings.stream().filter(r -> "Cancelled".equals(r[6])).count();
        double revenue = allBookings.stream()
            .filter(r -> "Confirmed".equals(r[6]))
            .mapToDouble(r -> parseAmount(String.valueOf(r[5])))
            .sum();

        statsRow.removeAll();
        statsRow.add(Widgets.statCard("\uD83D\uDC65","Total Users",String.valueOf(totalUsers),
            "All System Users","View Users \u2192",OCEAN, () -> switchTo("users")));
        statsRow.add(Widgets.statCard("\uD83D\uDCC4","Total Bookings",String.valueOf(totalBookings),
            "All Bookings","View Bookings \u2192",Theme.HEADER, () -> switchTo("bookings")));
        statsRow.add(Widgets.statCard("\u2705","Approved Bookings",String.valueOf(approved),
            "Confirmed","View Approved \u2192",GREEN, () -> switchTo("bookings")));
        statsRow.add(Widgets.statCard("\u23F3","Pending Bookings",String.valueOf(pending),
            "Awaiting Approval","View Pending \u2192",ORANGE, () -> switchTo("bookings")));
        statsRow.add(Widgets.statCard("\uD83D\uDCB0","Total Revenue (LKR)",moneyCompact(revenue),
            "From Confirmed Bookings","View Reports \u2192",PURPLE, () -> switchTo("reports")));
        statsRow.revalidate();
        statsRow.repaint();

        recentModel.setRowCount(0);
        allBookings.stream().limit(7).forEach(r ->
            recentModel.addRow(new Object[]{r[0], r[1], r[2], r[4], displayStatus(String.valueOf(r[6]))}));

        JPanel rows = (JPanel) findByName(overviewCard, "overviewRows");
        if (rows != null) {
            rows.removeAll();
            int total = totalBookings == 0 ? 1 : totalBookings;
            rows.add(Widgets.breakdownRow("Approved", GREEN, (int) approved, approved*100.0/total, false));
            rows.add(Widgets.breakdownRow("Pending", ORANGE, (int) pending, pending*100.0/total, false));
            rows.add(Widgets.breakdownRow("Cancelled", RED, (int) cancelled, cancelled*100.0/total, false));
            rows.add(new JSeparator());
            rows.add(Widgets.breakdownRow("Total", null, totalBookings, totalBookings==0?0:100.0, true));
            rows.revalidate();
            rows.repaint();
        }
    }

    private Component findByName(Container c, String name){
        for (Component comp : c.getComponents()) {
            if (name.equals(comp.getName())) return comp;
            if (comp instanceof Container) {
                Component found = findByName((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String displayStatus(String raw){
        return "Confirmed".equals(raw) ? "Approved" : raw;
    }

    private double parseAmount(String s){
        try { return Double.parseDouble(s.replace("Rs.","").replace(",","").trim()); }
        catch (Exception e){ return 0; }
    }

    private String money(double v){
        return String.format(Locale.US,"%,.2f", v);
    }

    /** Money without decimals - used on the KPI cards, where "416,000.00"
     *  plus a currency prefix overflows the card and gets clipped. Tables and
     *  exports keep the full 2-decimal form from money(). */
    private String moneyCompact(double v){
        return String.format(Locale.US,"%,.0f", v);
    }

    // ------------------------------------------------------------------
    // Manage Users
    // ------------------------------------------------------------------
    private JPanel buildUsers(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Manage Users","Create, update and remove system accounts.","Home / Manage Users"),
              BorderLayout.NORTH);

        JPanel split=new JPanel(new BorderLayout(14,0));
        split.setOpaque(false);

        JPanel card=Widgets.card("User Details");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        uId=Widgets.field(); uUser=Widgets.field(); uPass=Widgets.field();
        uEmail=Widgets.field(); uContact=Widgets.field();
        uRole=new JComboBox<>(new String[]{"Admin","TourManager","TourGuide","ServiceProvider"});

        Widgets.addField(f,0,"ID:",uId);
        Widgets.addField(f,1,"Username:",uUser);
        Widgets.addField(f,2,"Password:",uPass);
        Widgets.addField(f,3,"Email:",uEmail);
        Widgets.addField(f,4,"Contact:",uContact);
        Widgets.addField(f,5,"Role:",uRole);

        JButton add=Widgets.btn("Add",GREEN), upd=Widgets.btn("Update",OCEAN),
                del=Widgets.btn("Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=6; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);

        JLabel hint=new JLabel("<html><i>Leave Password blank to keep<br>the current one when updating.</i></html>");
        hint.setFont(new Font("Segoe UI",Font.PLAIN,11));
        hint.setForeground(Theme.SUBTEXT);
        GridBagConstraints hc=new GridBagConstraints();
        hc.gridx=0; hc.gridy=7; hc.gridwidth=2; hc.fill=GridBagConstraints.HORIZONTAL;
        hc.insets=new Insets(12,10,0,10);
        f.add(hint,hc);

        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=8; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  All Users");
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        toolbar.setOpaque(false);
        JTextField search=Widgets.searchBox(toolbar,"Search:");
        JButton refresh=Widgets.btn("Refresh",OCEAN);
        toolbar.add(refresh);

        userModel=new DefaultTableModel(new String[]{"ID","Username","Email","Contact","Role"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        userTable=new JTable(userModel);
        Widgets.styleTable(userTable);
        Widgets.colorStatusColumn(userTable,4,Map.of(
            "Admin",RED,"TourManager",OCEAN,"TourGuide",GREEN,"ServiceProvider",ORANGE));
        loadUsers();
        JScrollPane sp=new JScrollPane(userTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);

        JPanel tableBody=new JPanel(new BorderLayout(0,8));
        tableBody.setOpaque(false);
        tableBody.add(toolbar,BorderLayout.NORTH);
        tableBody.add(sp,BorderLayout.CENTER);
        tableCard.add(tableBody,BorderLayout.CENTER);

        userTable.getSelectionModel().addListSelectionListener(e->{
            int r=userTable.getSelectedRow();
            if(r>=0){
                uId.setText(str(userModel.getValueAt(r,0)));
                uUser.setText(str(userModel.getValueAt(r,1)));
                uEmail.setText(str(userModel.getValueAt(r,2)));
                uContact.setText(str(userModel.getValueAt(r,3)));
                uRole.setSelectedItem(str(userModel.getValueAt(r,4)));
            }
        });

        add.addActionListener(e->{
            if(uUser.getText().isBlank()||uPass.getText().isBlank()){
                msg("Username and password are required."); return;
            }
            if(uContact.getText().trim().length()>10){
                msg("\u26A0 Contact number cannot be more than 10 digits."); return;
            }
            if(uc.addUser(uUser.getText(),uPass.getText(),uEmail.getText(),uContact.getText(),(String)uRole.getSelectedItem())){
                msg("User added."); loadUsers(); clearForm();
            } else msg("Add failed - that username may already exist.");
        });
        upd.addActionListener(e->{
            if(uContact.getText().trim().length()>10){
                msg("\u26A0 Contact number cannot be more than 10 digits."); return;
            }
            try{
                int id=Integer.parseInt(uId.getText().trim());
                boolean ok = uPass.getText().isEmpty()
                    ? uc.updateUserKeepPassword(id,uUser.getText(),uEmail.getText(),uContact.getText(),(String)uRole.getSelectedItem())
                    : uc.updateUser(id,uUser.getText(),uPass.getText(),uEmail.getText(),uContact.getText(),(String)uRole.getSelectedItem());
                msg(ok?"User updated.":"Update failed.");
                if(ok) loadUsers();
            }catch(Exception ex){msg("Select a user, or enter a valid numeric ID.");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(uId.getText().trim());
                if(JOptionPane.showConfirmDialog(this,"Delete user "+id+"?","Confirm",
                        JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                    if(uc.deleteUser(id)){msg("User deleted."); loadUsers(); clearForm();}
                    else msg("Delete failed.");
                }
            }catch(Exception ex){msg("Select a user, or enter a valid numeric ID.");}
        });
        clr.addActionListener(e->clearForm());
        refresh.addActionListener(e->{ search.setText(""); loadUsers(); });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doUserSearch(search.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doUserSearch(search.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        split.add(card,BorderLayout.WEST);
        split.add(tableCard,BorderLayout.CENTER);
        p.add(split,BorderLayout.CENTER);
        return p;
    }
    private String str(Object o){ return o==null?"":o.toString(); }
    private void doUserSearch(String q){
        if(userModel==null) return;
        userModel.setRowCount(0);
        List<Object[]> rows = q.isBlank() ? uc.getAllUsers() : uc.searchUsers(q);
        for(Object[] r:rows) userModel.addRow(r);
    }
    private void loadUsers(){
        if(userModel==null) return;
        userModel.setRowCount(0);
        for(Object[] r:uc.getAllUsers()) userModel.addRow(r);
    }
    private void clearForm(){
        uId.setText("");uUser.setText("");uPass.setText("");
        uEmail.setText("");uContact.setText("");uRole.setSelectedIndex(0);
        userTable.clearSelection();
    }

    // ------------------------------------------------------------------
    // Manage Tours
    // ------------------------------------------------------------------
    private JPanel buildTours(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Manage Tours","Add, update and remove tour packages.","Home / Manage Tours"),
              BorderLayout.NORTH);

        JPanel split=new JPanel(new BorderLayout(14,0));
        split.setOpaque(false);

        JPanel card=Widgets.card("\uD83D\uDDFA  Tour Details");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        trId=Widgets.field(); trId.setEditable(false);
        trName=Widgets.field(); trDest=Widgets.field(); trDesc=Widgets.field();
        trDur=Widgets.field(); trPrice=Widgets.field();
        trStatus=new JComboBox<>(new String[]{"Available","Cancelled","In-Progress","Completed"});

        Widgets.addField(f,0,"ID:",trId);
        Widgets.addField(f,1,"Name:",trName);
        Widgets.addField(f,2,"Destination:",trDest);
        Widgets.addField(f,3,"Description:",trDesc);
        Widgets.addField(f,4,"Duration (days):",trDur);
        Widgets.addField(f,5,"Price (LKR):",trPrice);
        Widgets.addField(f,6,"Status:",trStatus);

        JButton add=Widgets.btn("Add",GREEN), upd=Widgets.btn("Update",OCEAN),
                del=Widgets.btn("Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=7; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=8; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDDFA  All Tours");
        tourModel=new DefaultTableModel(new String[]{"ID","Name","Destination","Description","Days","Price (LKR)","Status"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        tourTable=new JTable(tourModel); Widgets.styleTable(tourTable);
        Widgets.pillStatusColumn(tourTable,6,Map.of(
            "Available",GREEN,"Cancelled",RED,
            "In-Progress",OCEAN,"Completed",GREEN));
        loadTours();

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField search=Widgets.searchBox(top,"Search:");
        JButton refresh=Widgets.btn("Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{ search.setText(""); loadTours(); });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doTourSearch(search.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doTourSearch(search.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });
        JScrollPane sp=new JScrollPane(tourTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);
        JPanel tableBody=new JPanel(new BorderLayout(0,8)); tableBody.setOpaque(false);
        tableBody.add(top,BorderLayout.NORTH); tableBody.add(sp,BorderLayout.CENTER);
        tableCard.add(tableBody,BorderLayout.CENTER);

        tourTable.getSelectionModel().addListSelectionListener(e->{
            int r=tourTable.getSelectedRow();
            if(r>=0){
                trId.setText(str(tourModel.getValueAt(r,0)));
                trName.setText(str(tourModel.getValueAt(r,1)));
                trDest.setText(str(tourModel.getValueAt(r,2)));
                trDesc.setText(str(tourModel.getValueAt(r,3)));
                trDur.setText(str(tourModel.getValueAt(r,4)));
                trPrice.setText(str(tourModel.getValueAt(r,5)).replace("Rs. ","").replace(",",""));
                trStatus.setSelectedItem(str(tourModel.getValueAt(r,6)));
            }
        });

        add.addActionListener(e->{
            try{
                if(tc.addTour(trName.getText(),trDest.getText(),trDesc.getText(),
                        Integer.parseInt(trDur.getText()),Float.parseFloat(trPrice.getText()),
                        (String)trStatus.getSelectedItem())){
                    msg("Tour added."); loadTours(); refreshHome(); clearTourForm();
                } else msg("Add failed.");
            }catch(Exception ex){ msg("Enter valid values for duration and price."); }
        });
        upd.addActionListener(e->{
            try{
                int id=Integer.parseInt(trId.getText().trim());
                if(tc.updateTour(id,trName.getText(),trDest.getText(),trDesc.getText(),
                        Integer.parseInt(trDur.getText()),Float.parseFloat(trPrice.getText()),
                        (String)trStatus.getSelectedItem())){
                    msg("Tour updated."); loadTours(); refreshHome();
                } else msg("Update failed.");
            }catch(Exception ex){ msg("Select a tour, or enter valid values."); }
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(trId.getText().trim());
                if(JOptionPane.showConfirmDialog(this,"Delete tour "+id+"?","Confirm",
                        JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                    if(tc.deleteTour(id)){msg("Tour deleted."); loadTours(); refreshHome(); clearTourForm();}
                    else msg("Delete failed.");
                }
            }catch(Exception ex){ msg("Select a tour, or enter a valid numeric ID."); }
        });
        clr.addActionListener(e->clearTourForm());

        split.add(card,BorderLayout.WEST);
        split.add(tableCard,BorderLayout.CENTER);
        p.add(split,BorderLayout.CENTER);
        return p;
    }
    private void clearTourForm(){
        trId.setText(""); trName.setText(""); trDest.setText(""); trDesc.setText("");
        trDur.setText(""); trPrice.setText(""); trStatus.setSelectedIndex(0);
        tourTable.clearSelection();
    }
    private void doTourSearch(String q){
        if(tourModel==null) return;
        tourModel.setRowCount(0);
        List<Tour> rows = q.isBlank() ? tc.getAllTours() : tc.searchTours(q);
        for(Tour t:rows) tourModel.addRow(tourRow(t));
    }
    private void loadTours(){
        if(tourModel==null) return;
        tourModel.setRowCount(0);
        for(Tour t:tc.getAllTours()) tourModel.addRow(tourRow(t));
    }
    private Object[] tourRow(Tour t){
        return new Object[]{t.getTourId(),t.getName(),t.getDestination(),t.getDescription(),
                            t.getDurationDays(),money(t.getPrice()),t.getStatus()};
    }

    // ------------------------------------------------------------------
    // Approve Bookings
    // ------------------------------------------------------------------
    private JPanel buildBookings(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Approve Bookings","Review pending requests and approve or cancel them.","Home / Approve Bookings"),
              BorderLayout.NORTH);

        JPanel card=Widgets.card("\uD83D\uDCCB  All Bookings");
        bookModel=new DefaultTableModel(new String[]{"ID","Customer","Tour","Guests","Date","Amount (LKR)","Status"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        bookTable=new JTable(bookModel); Widgets.styleTable(bookTable);
        Widgets.pillStatusColumn(bookTable,6,Map.of(
            "Confirmed",GREEN,"Approved",GREEN,"Pending",ORANGE,"Cancelled",RED));
        loadBookings();

        JButton refresh=Widgets.btn("Refresh",OCEAN);
        JButton addBooking=Widgets.btn("+ Add Booking",GREEN);
        JButton approve=Widgets.btn("Approve",GREEN);
        JButton pending=Widgets.btn("Pending",ORANGE);
        JButton cancel=Widgets.btn("Reject",RED);
        addBooking.addActionListener(e->openAddBookingDialog());
        approve.addActionListener(e->changeStatusViaAdmin("Confirmed"));
        pending.addActionListener(e->changeStatusViaAdmin("Pending"));
        cancel.addActionListener(e->changeStatusViaAdmin("Cancelled"));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField bookSearch=Widgets.searchBox(top,"Search:");
        top.add(refresh); top.add(addBooking); top.add(approve); top.add(pending); top.add(cancel);
        refresh.addActionListener(e->{bookSearch.setText(""); loadBookings();});
        bookSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchBookings(bookSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchBookings(bookSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JScrollPane sp=new JScrollPane(bookTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(top,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        card.add(body,BorderLayout.CENTER);
        p.add(card,BorderLayout.CENTER);
        return p;
    }
    /** Applies the chosen status ("Confirmed"/"Pending"/"Cancelled") to the selected booking. */
    private void changeStatusViaAdmin(String targetStatus){
        int r=bookTable.getSelectedRow();
        if(r<0){msg("Select a booking first."); return;}
        try{
            int id=Integer.parseInt(str(bookModel.getValueAt(r,0)));
            boolean ok = switch(targetStatus){
                case "Confirmed" -> ac.approveBooking(id);
                case "Cancelled" -> ac.rejectBooking(id);
                default -> ac.pendingBooking(id);
            };
            msg(ok ? ("Booking set to "+targetStatus+".") : "Update failed.");
            if(ok){ loadBookings(); refreshHome(); }
        }catch(Exception ex){ msg("Could not read that booking's ID."); }
    }
    /** Opens a small form for creating a brand-new booking against an existing tour. */
    private void openAddBookingDialog(){
        List<Tour> tours = tc.getAllTours();
        if(tours.isEmpty()){ msg("Add a tour first (Manage Tours) before creating a booking."); return; }

        JComboBox<String> tourCombo = new JComboBox<>();
        for(Tour t: tours) tourCombo.addItem(t.getTourId()+" - "+t.getName()+" (Rs. "+t.getPrice()+")");
        JTextField customer = Widgets.field();
        JTextField guests = Widgets.field();
        JTextField amount = Widgets.field();
        Widgets.styleCombo(tourCombo);

        Runnable suggestAmount = () -> {
            try{
                int idx = tourCombo.getSelectedIndex();
                int n = Integer.parseInt(guests.getText().trim());
                if(idx>=0 && n>0) amount.setText(String.valueOf(tours.get(idx).getPrice()*n));
            }catch(Exception ignored){}
        };
        tourCombo.addActionListener(e->suggestAmount.run());
        guests.addActionListener(e->suggestAmount.run());

        JPanel form=new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        Widgets.addField(form,0,"Tour:",tourCombo);
        Widgets.addField(form,1,"Customer Name:",customer);
        Widgets.addField(form,2,"Guests:",guests);
        Widgets.addField(form,3,"Amount (LKR):",amount);
        form.setPreferredSize(new Dimension(360,180));

        int res = JOptionPane.showConfirmDialog(this, form, "Add Booking",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(res == JOptionPane.OK_OPTION){
            try{
                int tourId = tours.get(tourCombo.getSelectedIndex()).getTourId();
                String name = customer.getText().trim();
                int n = Integer.parseInt(guests.getText().trim());
                float total = Float.parseFloat(amount.getText().trim());
                if(name.isEmpty()){ msg("\u26A0 Enter a customer name"); return; }
                if(bc.addBooking(tourId,name,n,total)){
                    msg("\u2705 Booking added."); loadBookings(); refreshHome();
                } else msg("\u274C Could not add booking.");
            }catch(Exception ex){ msg("\u26A0 Enter valid guests/amount values."); }
        }
    }
    private void loadBookings(){
        if(bookModel==null) return;
        bookModel.setRowCount(0);
        for(Object[] r:bc.getAll())
            bookModel.addRow(new Object[]{r[0],r[1],r[2],r[3],r[4],
                                          money(parseAmount(String.valueOf(r[5]))),r[6]});
    }
    private void doSearchBookings(String q){
        if(bookModel==null) return;
        bookModel.setRowCount(0);
        List<Object[]> rows = q.isBlank() ? bc.getAll() : bc.search(q);
        for(Object[] r:rows)
            bookModel.addRow(new Object[]{r[0],r[1],r[2],r[3],r[4],
                                          money(parseAmount(String.valueOf(r[5]))),r[6]});
    }

    // ==================================================================
    // View Reports  -  Report Generator
    // ==================================================================
    private JPanel buildReportGenerator(){
        JPanel p = new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("View Reports",
              "Pick a report, set the date range, then export to PDF.",
              "Home / View Reports"), BorderLayout.NORTH);

        // ---- Filter toolbar ------------------------------------------------
        // Filters on the top row, actions on their own row underneath. A single
        // FlowLayout row was silently clipping the buttons once the filters got
        // wide enough to wrap.
        JPanel filterCard = Widgets.plainCard();
        filterCard.setLayout(new BorderLayout(0,14));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT,18,0));
        filters.setOpaque(false);

        reportType = new JComboBox<>(REPORT_TYPES);
        reportStatus = new JComboBox<>(new String[]{"All Status","Approved","Pending","Cancelled"});
        LocalDate now = LocalDate.now();
        dateFrom = Widgets.dateField(now.minusDays(prefDefaultRangeDays).format(DateTimeFormatter.ISO_LOCAL_DATE));
        dateTo   = Widgets.dateField(now.format(DateTimeFormatter.ISO_LOCAL_DATE));

        filters.add(Widgets.labeledBlock("Report Type", reportType));
        filters.add(Widgets.labeledBlock("Status", reportStatus));
        filters.add(Widgets.labeledBlock("Date From (yyyy-MM-dd)", dateFrom));
        filters.add(Widgets.labeledBlock("Date To (yyyy-MM-dd)", dateTo));

        JButton pdf      = Widgets.btn("Export PDF", RED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        actions.setOpaque(false);
        actions.add(pdf);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);
        actionRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER),
            BorderFactory.createEmptyBorder(12,0,0,0)));
        actionRow.add(actions, BorderLayout.WEST);

        filterCard.add(filters, BorderLayout.NORTH);
        filterCard.add(actionRow, BorderLayout.CENTER);

        // ---- Summary cards -------------------------------------------------
        reportSummaryHolder = new JPanel(new GridLayout(1,4,16,0));
        reportSummaryHolder.setOpaque(false);
        reportSummaryHolder.setBorder(BorderFactory.createEmptyBorder(16,0,0,0));

        // ---- Detail table --------------------------------------------------
        JPanel previewCard = Widgets.plainCard();
        previewCard.setLayout(new BorderLayout(0,10));

        JPanel previewHead = new JPanel(new BorderLayout());
        previewHead.setOpaque(false);
        JLabel previewTitle = new JLabel("Report Details");
        previewTitle.setFont(new Font("Segoe UI",Font.BOLD,15));
        previewTitle.setForeground(DARK);
        previewHead.add(previewTitle, BorderLayout.WEST);
        reportCountLbl = new JLabel(" ");
        reportCountLbl.setFont(new Font("Segoe UI",Font.PLAIN,12));
        reportCountLbl.setForeground(Theme.SUBTEXT);
        previewHead.add(reportCountLbl, BorderLayout.EAST);
        previewCard.add(previewHead, BorderLayout.NORTH);

        reportModel = new DefaultTableModel(new String[]{"Booking ID","Tour","Customer","Date","Amount (LKR)","Status"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        reportTable = new JTable(reportModel);
        Widgets.styleTable(reportTable);
        JScrollPane sp = new JScrollPane(reportTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);
        previewCard.add(sp, BorderLayout.CENTER);

        pdf.addActionListener(e -> exportReportPdf());
        reportType.addActionListener(e -> generateReport());
        reportStatus.addActionListener(e -> generateReport());
        dateFrom.addActionListener(e -> generateReport());
        dateTo.addActionListener(e -> generateReport());
        dateFrom.addFocusListener(new java.awt.event.FocusAdapter(){
            @Override public void focusLost(java.awt.event.FocusEvent e){ generateReport(); }});
        dateTo.addFocusListener(new java.awt.event.FocusAdapter(){
            @Override public void focusLost(java.awt.event.FocusEvent e){ generateReport(); }});

        JPanel top = new JPanel(new BorderLayout(0,16));
        top.setOpaque(false);
        top.add(filterCard, BorderLayout.NORTH);
        top.add(reportSummaryHolder, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0,16));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(previewCard, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private void resetReportFilters(){
        LocalDate now = LocalDate.now();
        reportType.setSelectedItem(prefDefaultReport);
        reportStatus.setSelectedIndex(0);
        dateFrom.setText(now.minusDays(prefDefaultRangeDays).format(DateTimeFormatter.ISO_LOCAL_DATE));
        dateTo.setText(now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        generateReport();
    }

    private LocalDate parseDateSafe(String s, LocalDate fallback){
        try { return LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException | NullPointerException ex) { return fallback; }
    }

    /**
     * Rebuilds the whole Reports screen from the database: the detail table,
     * the summary cards and the data held for export. Called on every filter
     * change AND whenever the Reports screen is opened, so it can never show
     * stale figures.
     */
    private void generateReport(){
        if (reportTable == null) return;   // not built yet

        LocalDate from = parseDateSafe(dateFrom.getText(), LocalDate.MIN);
        LocalDate to   = parseDateSafe(dateTo.getText(),   LocalDate.MAX);
        if (from.isAfter(to)) { LocalDate t=from; from=to; to=t; }

        String type = String.valueOf(reportType.getSelectedItem());
        repTitle = type;
        repRows = new ArrayList<>();
        repSummary = new ArrayList<>();

        switch (type) {
            case "Revenue by Tour": buildRevenueReport(from, to); break;
            case "Tour Report":     buildTourReport();            break;
            case "User Report":     buildUserReport();            break;
            default:                buildBookingReport(from, to); break;
        }

        String range = (from.equals(LocalDate.MIN) || to.equals(LocalDate.MAX))
            ? "All dates"
            : from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "  to  "
              + to.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        repSubtitle = range + "   \u2022   " + repRows.size() + " record"
                    + (repRows.size()==1?"":"s");

        // ---- Push into the on-screen table -------------------------------
        DefaultTableModel m = new DefaultTableModel(repHeaders, 0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        for (String[] r : repRows) m.addRow(r);
        reportModel = m;
        reportTable.setModel(m);
        // setModel() rebuilds the column model, so the styling and the status
        // pill renderer have to be re-applied here.
        Widgets.styleTable(reportTable);
        int statusCol = indexOf(repHeaders, "Status");
        if (statusCol >= 0) {
            Widgets.pillStatusColumn(reportTable, statusCol, Map.of(
                "Approved",GREEN,"Confirmed",GREEN,"Completed",GREEN,"Available",GREEN,
                "Pending",ORANGE,"Full",ORANGE,"In-Progress",OCEAN,"Cancelled",RED));
        }
        int roleCol = indexOf(repHeaders, "Role");
        if (roleCol >= 0) {
            Widgets.colorStatusColumn(reportTable, roleCol, Map.of(
                "Admin",RED,"TourManager",OCEAN,"TourGuide",GREEN,"ServiceProvider",ORANGE));
        }

        reportCountLbl.setText(repRows.size() + " record" + (repRows.size()==1?"":"s") + "  \u2022  " + range);

        // ---- Summary cards -------------------------------------------------
        Color[] accents = { OCEAN, GREEN, ORANGE, PURPLE };
        String[] icons  = { "\uD83D\uDCC4", "\u2705", "\u23F3", "\uD83D\uDCB0" };
        reportSummaryHolder.removeAll();
        reportSummaryHolder.setLayout(new GridLayout(1, Math.max(1, repSummary.size()), 16, 0));
        for (int i = 0; i < repSummary.size(); i++) {
            reportSummaryHolder.add(Widgets.statCard(
                icons[i % icons.length], repSummary.get(i)[0], repSummary.get(i)[1],
                null, null, accents[i % accents.length], null));
        }
        reportSummaryHolder.revalidate();
        reportSummaryHolder.repaint();
    }

    private int indexOf(String[] arr, String needle){
        for (int i=0;i<arr.length;i++) if (needle.equals(arr[i])) return i;
        return -1;
    }

    /** Filters bookings by date + status and totals them up. */
    private void buildBookingReport(LocalDate from, LocalDate to){
        repHeaders = new String[]{"Booking ID","Tour","Customer","Guests","Date","Amount (LKR)","Status"};
        String wanted = String.valueOf(reportStatus.getSelectedItem());

        int total=0, approved=0, pending=0, cancelled=0;
        double revenue=0;

        for (Object[] r : bc.getAll()) {
            LocalDate d = parseDateSafe(String.valueOf(r[4]), null);
            if (d != null && (d.isBefore(from) || d.isAfter(to))) continue;

            String status = displayStatus(String.valueOf(r[6]));
            if (!"All Status".equals(wanted) && !wanted.equals(status)) continue;

            double amt = parseAmount(String.valueOf(r[5]));
            total++;
            if ("Approved".equals(status))  { approved++; revenue += amt; }
            if ("Pending".equals(status))   pending++;
            if ("Cancelled".equals(status)) cancelled++;

            repRows.add(new String[]{ str(r[0]), str(r[2]), str(r[1]), str(r[3]),
                                      str(r[4]), money(amt), status });
        }

        repSummary.add(new String[]{"Total Bookings", String.valueOf(total)});
        repSummary.add(new String[]{"Approved Bookings", String.valueOf(approved)});
        repSummary.add(new String[]{"Pending Bookings", String.valueOf(pending)});
        repSummary.add(new String[]{"Total Revenue (LKR)", moneyCompact(revenue)});
        if (cancelled > 0) repSummary.add(new String[]{"Cancelled", String.valueOf(cancelled)});
    }

    /** Groups approved bookings by tour to show where the money comes from. */
    private void buildRevenueReport(LocalDate from, LocalDate to){
        repHeaders = new String[]{"Tour","Bookings","Guests","Approved Revenue (LKR)","Share %"};

        Map<String,double[]> agg = new LinkedHashMap<>(); // tour -> {bookings, guests, revenue}
        double grand = 0;
        int totalBookings = 0;

        for (Object[] r : bc.getAll()) {
            LocalDate d = parseDateSafe(String.valueOf(r[4]), null);
            if (d != null && (d.isBefore(from) || d.isAfter(to))) continue;

            String tour = str(r[2]).isEmpty() ? "(unassigned tour)" : str(r[2]);
            double[] cell = agg.computeIfAbsent(tour, k -> new double[3]);
            cell[0] += 1;
            try { cell[1] += Integer.parseInt(str(r[3])); } catch (Exception ignore) {}
            totalBookings++;

            if ("Confirmed".equals(String.valueOf(r[6]))) {
                double amt = parseAmount(String.valueOf(r[5]));
                cell[2] += amt;
                grand += amt;
            }
        }

        for (Map.Entry<String,double[]> e : agg.entrySet()) {
            double[] c = e.getValue();
            double share = grand == 0 ? 0 : c[2] * 100.0 / grand;
            repRows.add(new String[]{ e.getKey(), String.valueOf((int)c[0]),
                                      String.valueOf((int)c[1]), money(c[2]),
                                      String.format(Locale.US,"%.2f", share) });
        }
        repRows.sort((a,b) -> Double.compare(
            parseAmount(b[3]), parseAmount(a[3])));   // biggest earner first

        repSummary.add(new String[]{"Tours With Bookings", String.valueOf(agg.size())});
        repSummary.add(new String[]{"Total Bookings", String.valueOf(totalBookings)});
        repSummary.add(new String[]{"Approved Revenue (LKR)", moneyCompact(grand)});
        repSummary.add(new String[]{"Average per Tour (LKR)",
            moneyCompact(agg.isEmpty() ? 0 : grand / agg.size())});
    }

    private void buildTourReport(){
        repHeaders = new String[]{"ID","Name","Destination","Days","Price (LKR)","Status"};
        List<Tour> tours = tc.getAllTours();
        double totalValue = 0;
        int available = 0;
        for (Tour t : tours) {
            totalValue += t.getPrice();
            if ("Available".equalsIgnoreCase(t.getStatus())) available++;
            repRows.add(new String[]{ String.valueOf(t.getTourId()), t.getName(), t.getDestination(),
                                      String.valueOf(t.getDurationDays()), money(t.getPrice()), t.getStatus() });
        }
        repSummary.add(new String[]{"Total Tours", String.valueOf(tours.size())});
        repSummary.add(new String[]{"Available Tours", String.valueOf(available)});
        repSummary.add(new String[]{"Combined Value (LKR)", moneyCompact(totalValue)});
        repSummary.add(new String[]{"Average Price (LKR)",
            moneyCompact(tours.isEmpty() ? 0 : totalValue / tours.size())});
    }

    private void buildUserReport(){
        repHeaders = new String[]{"ID","Username","Email","Contact","Role"};
        List<Object[]> users = uc.getAllUsers();
        Map<String,Integer> byRole = new LinkedHashMap<>();
        for (Object[] u : users) {
            String role = u.length>4 ? str(u[4]) : "";
            byRole.merge(role, 1, Integer::sum);
            repRows.add(new String[]{ str(u[0]), str(u[1]), str(u[2]), str(u[3]), role });
        }
        repSummary.add(new String[]{"Total Users", String.valueOf(users.size())});
        repSummary.add(new String[]{"Admins", String.valueOf(byRole.getOrDefault("Admin",0))});
        repSummary.add(new String[]{"Tour Guides", String.valueOf(byRole.getOrDefault("TourGuide",0))});
        repSummary.add(new String[]{"Service Providers", String.valueOf(byRole.getOrDefault("ServiceProvider",0))});
    }

    // ---- Export / print --------------------------------------------------

    private String suggestedFileName(String ext){
        return repTitle.toLowerCase(Locale.ENGLISH).replace(' ','_')
             + "_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "." + ext;
    }

    /** Writes the report currently on screen to a real PDF file. */
    private void exportReportPdf(){
        generateReport();  // no separate "Generate" step anymore - always export the current filters
        if (repHeaders.length == 0) { msg("No data matches the current filters."); return; }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save report as PDF");
        chooser.setSelectedFile(new File(suggestedFileName("pdf")));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".pdf"))
            file = new File(file.getParentFile(), file.getName() + ".pdf");

        try {
            PdfExporter.writeTableReport(file, repTitle, repSubtitle, repSummary, repHeaders, repRows);
            int open = JOptionPane.showConfirmDialog(this,
                "PDF saved to:\n" + file.getAbsolutePath() + "\n\nOpen it now?",
                "Report exported", JOptionPane.YES_NO_OPTION);
            if (open == JOptionPane.YES_OPTION) openFile(file);
        } catch (Exception ex) {
            msg("PDF export failed: " + ex.getMessage());
        }
    }

    private void openFile(File file){
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                msg("Saved, but this system has no default PDF viewer configured.");
            }
        } catch (Exception ex) {
            msg("Saved, but the file could not be opened automatically:\n" + ex.getMessage());
        }
    }

    private void exportReportCsv(){
        if (repHeaders.length == 0) { msg("Generate a report first."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save report as CSV");
        chooser.setSelectedFile(new File(suggestedFileName("csv")));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".csv"))
            file = new File(file.getParentFile(), file.getName() + ".csv");

        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            w.println(csvLine(repHeaders));
            for (String[] r : repRows) w.println(csvLine(r));
            msg("CSV saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            msg("CSV export failed: " + ex.getMessage());
        }
    }

    /** Quotes each field so values containing commas survive the round trip. */
    private String csvLine(String[] cells){
        StringBuilder b = new StringBuilder();
        for (int i=0;i<cells.length;i++){
            if (i>0) b.append(',');
            String v = cells[i]==null?"":cells[i];
            b.append('"').append(v.replace("\"","\"\"")).append('"');
        }
        return b.toString();
    }

    private void printReport(){
        if (reportModel == null || reportModel.getRowCount() == 0) {
            msg("Nothing to print - generate a report first."); return;
        }
        try {
            boolean done = reportTable.print(JTable.PrintMode.FIT_WIDTH,
                new java.text.MessageFormat("Travel BP Sri Lanka \u2014 " + repTitle),
                new java.text.MessageFormat("Page {0}"));
            if (!done) msg("Printing was cancelled.");
        } catch (PrinterException ex) {
            msg("Print failed: " + ex.getMessage());
        }
    }

    /** Shows a message dialog, using an error icon/title for failures and
     *  warnings and a plain info dialog for success/status messages. */
    private void msg(String s){
        if(isProblemMessage(s)){
            JOptionPane.showMessageDialog(this,s,"Error",JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,s,"Success",JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private boolean isProblemMessage(String s){
        if(s.startsWith("\u274C") || s.startsWith("\u26A0")) return true;
        String low = s.toLowerCase();
        return low.contains("fail") || low.contains("could not") || low.contains("invalid")
            || low.contains("select") || low.contains("enter") || low.contains("required");
    }
}
