package View;

import controller.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Tour Guide Dashboard — top bar + left sidebar nav (Dashboard / Assigned
 * Tours / Attendance / Tour Status / Travel Info / Log out) with a
 * CardLayout content area, a stat-card overview on the Dashboard home
 * screen, and a filterable Tour Status / Travel Info screen.
 *
 * Data notes: "Assigned Tours", "Attendance" and "Tour Status" are backed
 * by the real database via TourGuideController/TourController. "Travel Info"
 * has no backing table in this build, so it ships as a static reference
 * list (Attractions / Accommodation / Transport / Emergency Contacts /
 * Guidelines) the guide can filter and browse.
 */
public class TourGuideDashboard extends JFrame {
    private final Color DARK=Theme.DARK, OCEAN=Theme.OCEAN,
      LIGHT=Theme.LIGHT,
      GREEN=Theme.GREEN, RED=Theme.RED, ORANGE=Theme.ORANGE;

    private final String username;
    private final TourGuideController tgc = new TourGuideController();

    private final CardLayout cl = new CardLayout();
    private final JPanel contentPanel = new JPanel(cl);
    private final Map<String,NavButton> navButtons = new LinkedHashMap<>();

    // Assigned Tours screen
    private JTable table; private DefaultTableModel model;
    // Attendance screen
    private JTable attTable; private DefaultTableModel attModel;
    private JTextField aTourId, aTourist;
    private int selectedAttendanceId = -1;
    private boolean selectedAttendancePresent = true;
    // Tour Status screen
    private JTable statusTable; private DefaultTableModel statusModel;
    private JComboBox<String> statusFilter;
    // Travel Info screen
    private JTable travelTable; private DefaultTableModel travelModel;
    private String travelCategoryFilter = "All Information";
    private final Map<String,TravelInfoRow> travelRows = new LinkedHashMap<>();

    // Dashboard home
    private JPanel homeRoot;
    private JPanel statsRow;
    private DefaultTableModel homeToursModel;
    private JPanel scheduleList;

    /** Builds and shows the Tour Guide Dashboard window for the given logged-in user. */
    public TourGuideDashboard(String u){
        this.username = u;
        setTitle("Tourism Management System \u2014 Tour Guide Dashboard");
        setSize(1320, 780);
        setMinimumSize(new Dimension(1100,660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        seedTravelInfo();
        buildUI();
        setVisible(true);
    }

    // ------------------------------------------------------------------
    // Overall layout
    // ------------------------------------------------------------------
    private void buildUI(){
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT);
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.add(buildSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(LIGHT);
        contentPanel.add(wrap(buildHome()), "home");
        contentPanel.add(wrap(buildTours()), "tours");
        contentPanel.add(wrap(buildAttendance()), "attendance");
        contentPanel.add(wrap(buildTourStatus()), "status");
        contentPanel.add(wrap(buildTravelInfo()), "travel");
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

    private void switchTo(String key){
        cl.show(contentPanel, key);
        for (Map.Entry<String,NavButton> e : navButtons.entrySet()) {
            e.getValue().setActiveState(e.getKey().equals(key));
        }
        switch (key) {
            case "home":       refreshHome();     break;
            case "tours":      loadTours();       break;
            case "attendance": loadAttendance();  break;
            case "status":     loadTourStatus();  break;
            case "travel":     loadTravelInfo();  break;
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

        // The top bar intentionally has no notification bell or account
        // menu; Log out is available from the sidebar instead.
        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    private String displayName(){
        return (username==null||username.isBlank()) ? "Tour Guide" : username;
    }

    private void confirmLogout(){
        if (JOptionPane.showConfirmDialog(this,"Log out of the Tour Guide Dashboard?","Confirm",
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

        JLabel avatar = new JLabel("\uD83E\uDDD1\u200D\uD83C\uDF93");
        avatar.setFont(new Font("Segoe UI Emoji",Font.PLAIN,30));
        avatar.setForeground(Color.WHITE);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel(displayName());
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Segoe UI",Font.BOLD,15));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel role = new JLabel("Tour Guide");
        role.setForeground(new Color(160,190,198));
        role.setFont(new Font("Segoe UI",Font.PLAIN,11));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.add(avatar); profile.add(Box.createVerticalStrut(6));
        profile.add(name); profile.add(role);

        side.add(profile);
        side.add(divider());
        side.add(Box.createVerticalStrut(12));

        side.add(navItem("Dashboard","home"));
        side.add(navItem("Assigned Tours","tours"));
        side.add(navItem("Attendance","attendance"));
        side.add(navItem("Tour Status","status"));
        side.add(navItem("Travel Info","travel"));

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

    // ==================================================================
    // Dashboard home
    // ==================================================================
    private JPanel buildHome(){
        homeRoot = new JPanel(new BorderLayout(0,16));
        homeRoot.setBackground(LIGHT);
        homeRoot.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));

        JPanel headRow = Widgets.pageHeading("Tour Guide Dashboard",
            "Welcome back, "+displayName()+"! Here's your activity overview.", null);

        statsRow = new JPanel(new GridLayout(1,4,16,0));
        statsRow.setOpaque(false);

        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints g1=new GridBagConstraints();
        g1.gridx=0; g1.gridy=0; g1.weightx=0.62; g1.weighty=1; g1.fill=GridBagConstraints.BOTH; g1.insets=new Insets(0,0,0,8);
        lower.add(buildMyToursCard(), g1);
        GridBagConstraints g2c=new GridBagConstraints();
        g2c.gridx=1; g2c.gridy=0; g2c.weightx=0.38; g2c.weighty=1; g2c.fill=GridBagConstraints.BOTH; g2c.insets=new Insets(0,8,0,0);
        lower.add(buildScheduleCard(), g2c);

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

    private JPanel buildMyToursCard(){
        JPanel card = Widgets.plainCard();
        card.setLayout(new BorderLayout(0,8));
        card.add(dashCardHeader("\uD83E\uDDED  My Assigned Tours","View All \u2192", () -> switchTo("tours")), BorderLayout.NORTH);

        homeToursModel = new DefaultTableModel(new String[]{"Tour ID","Tour Name","Destination","Days","Status"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}};
        JTable homeTable = new JTable(homeToursModel);
        Widgets.styleTable(homeTable);
        Widgets.pillStatusColumn(homeTable,4,Map.of(
            "Available",GREEN,"In-Progress",Theme.SUN,"Completed",GREEN,"Cancelled",RED));
        JScrollPane sp = new JScrollPane(homeTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildScheduleCard(){
        JPanel card = Widgets.plainCard();
        card.setLayout(new BorderLayout(0,4));
        card.add(dashCardHeader("\uD83D\uDCC6  Today's Schedule","View Full Schedule \u2192", () -> switchTo("tours")), BorderLayout.NORTH);

        scheduleList = new JPanel();
        scheduleList.setOpaque(false);
        scheduleList.setLayout(new BoxLayout(scheduleList, BoxLayout.Y_AXIS));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(scheduleList, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(top);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel scheduleItem(String title, String sub, Color dot){
        JPanel row = new JPanel(new BorderLayout(10,0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER),
            BorderFactory.createEmptyBorder(9,2,9,2)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,58));

        JLabel bullet = new JLabel("\u25CF");
        bullet.setForeground(dot);
        bullet.setFont(new Font("Segoe UI",Font.PLAIN,12));
        bullet.setVerticalAlignment(SwingConstants.TOP);
        bullet.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel(title);
        t1.setFont(new Font("Segoe UI",Font.BOLD,13));
        t1.setForeground(DARK);
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t2 = new JLabel(sub);
        t2.setFont(new Font("Segoe UI",Font.PLAIN,11));
        t2.setForeground(Theme.SUBTEXT);
        t2.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t1); text.add(t2);

        row.add(bullet, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    /** Recomputes every live number on the Dashboard home screen. */
    private void refreshHome(){
        if (homeRoot == null) return;

        List<Tour> tours = tgc.getAssignedTours();
        List<Attendance> attendance = tgc.getAllAttendance();

        int assignedCount = tours.size();
        long inProgress = tours.stream().filter(t -> "In-Progress".equals(t.getStatus())).count();

        long presentCount = attendance.stream().filter(Attendance::isPresent).count();
        String attendancePct = attendance.isEmpty() ? "\u2014"
            : Math.round((presentCount * 100.0) / attendance.size()) + "%";

        statsRow.removeAll();
        statsRow.add(Widgets.statCard("\uD83E\uDDED","Assigned Tours",String.valueOf(assignedCount),
            "Active Tours","View Tours \u2192",OCEAN, () -> switchTo("tours")));
        statsRow.add(Widgets.statCard("\u2705","Attendance",attendancePct,
            "Overall",  "View Attendance \u2192",GREEN, () -> switchTo("attendance")));
        statsRow.add(Widgets.statCard("\uD83D\uDD04","Tour Status",String.valueOf(inProgress),
            "In Progress","View Status \u2192",ORANGE, () -> switchTo("status")));
        statsRow.add(Widgets.statCard("\u2139\uFE0F","Travel Info",String.valueOf(travelRows.size()),
            "Travel Updates","View Info \u2192",Theme.PURPLE, () -> switchTo("travel")));
        statsRow.revalidate();
        statsRow.repaint();

        homeToursModel.setRowCount(0);
        for (Tour t : tours) homeToursModel.addRow(new Object[]{
            t.getTourId(), t.getName(), t.getDestination(), t.getDurationDays(), t.getStatus()});

        scheduleList.removeAll();
        List<Tour> upcoming = tours.stream()
            .filter(t -> "In-Progress".equals(t.getStatus()) || "Available".equals(t.getStatus()))
            .limit(4).toList();
        if (upcoming.isEmpty()) {
            JLabel empty = new JLabel("Nothing on your schedule right now.");
            empty.setFont(new Font("Segoe UI",Font.PLAIN,12));
            empty.setForeground(Theme.SUBTEXT);
            empty.setBorder(BorderFactory.createEmptyBorder(8,2,8,2));
            scheduleList.add(empty);
        } else {
            for (Tour t : upcoming) {
                Color dot = "In-Progress".equals(t.getStatus()) ? Theme.SUN : OCEAN;
                scheduleList.add(scheduleItem(t.getName(), "\uD83D\uDCCD "+t.getDestination()+"  \u00B7  "+t.getStatus(), dot));
            }
        }
        scheduleList.revalidate();
        scheduleList.repaint();
    }

    // ==================================================================
    // Assigned Tours
    // ==================================================================
    private JPanel buildTours(){
        JPanel p=new JPanel(new BorderLayout(0,16));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Assigned Tours",
            "The tours currently assigned to you.", "Dashboard / Assigned Tours"), BorderLayout.NORTH);

        JPanel card=Widgets.card("\uD83E\uDDED  Tours Assigned To You");

        model=new DefaultTableModel(new String[]{"ID","Name","Destination","Days","Price","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(model); Widgets.styleTable(table);
        Widgets.pillStatusColumn(table,5,Map.of(
            "Available",GREEN,"In-Progress",Theme.SUN,"Completed",GREEN,"Cancelled",RED));
        loadTours();

        JButton refresh=Widgets.btn(" Refresh",OCEAN);
        JButton avail=Widgets.btn("\uD83D\uDD01 Mark Available",GREEN);
        JButton start=Widgets.btn("\u25B6 Mark In-Progress",Theme.SUN);
        JButton done=Widgets.btn("\u2705 Mark Complete",GREEN);
        JButton cancel=Widgets.btn("\u274C Cancel",RED);
        avail.addActionListener(e->change("Available"));
        start.addActionListener(e->change("In-Progress"));
        done.addActionListener(e->change("Completed"));
        cancel.addActionListener(e->change("Cancelled"));

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        top.setOpaque(false);
        
        JTextField tourSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        top.add(refresh); top.add(avail); top.add(start); top.add(done); top.add(cancel);
        refresh.addActionListener(e->{tourSearch.setText(""); loadTours(); refreshHome();});
        tourSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchTours(tourSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchTours(tourSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JScrollPane sp=new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(top,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        card.add(body,BorderLayout.CENTER);

        p.add(card,BorderLayout.CENTER);
        return p;
    }
    private void change(String s){
        int r=table.getSelectedRow();
        if(r<0){msg("Select a tour first"); return;}
        int id=(int)model.getValueAt(r,0);
        if(tgc.updateTourStatus(id,s)){
            msg("\u2705 Status: "+s);
            loadTours(); refreshHome();
        }
    }
    private void loadTours(){
        model.setRowCount(0);
        for(Tour t:tgc.getAssignedTours()) model.addRow(new Object[]{
            t.getTourId(),t.getName(),t.getDestination(),
            t.getDurationDays(),"Rs. "+t.getPrice(),t.getStatus()});
    }
    private void doSearchTours(String q){
        model.setRowCount(0);
        List<Tour> rows = q.isBlank() ? tgc.getAssignedTours() : tgc.searchAssignedTours(q);
        for(Tour t:rows) model.addRow(new Object[]{
            t.getTourId(),t.getName(),t.getDestination(),
            t.getDurationDays(),"Rs. "+t.getPrice(),t.getStatus()});
    }

    // ==================================================================
    // Attendance
    // ==================================================================
    private JPanel buildAttendance(){
        JPanel p=new JPanel(new BorderLayout(0,16));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Attendance",
            "Mark and review tourist attendance for your tours.", "Dashboard / Attendance"), BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(14,14));
        row.setOpaque(false);

        JPanel card=Widgets.card(" Mark Tourist Attendance");
        card.setPreferredSize(new Dimension(320,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        aTourId=Widgets.field(); aTourist=Widgets.field();
        Widgets.addField(f,0,"Tour ID:",aTourId);
        Widgets.addField(f,1,"Tourist Name:",aTourist);

        JButton present=Widgets.btn("\u2705 Present",GREEN), absent=Widgets.btn("\u274C Absent",RED),
                upd=Widgets.btn("\u270F\uFE0F Update",OCEAN), del=Widgets.btn("\uD83D\uDDD1 Delete",RED),
                clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(3,1,0,8));
        btnBox.setOpaque(false); btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(present,absent));
        btnBox.add(Widgets.buttonRow(upd,del));
        btnBox.add(clr);
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=2; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=3; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  Attendance Records");
        attModel=new DefaultTableModel(new String[]{"ID","Tour ID","Tourist","Date","Present"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        attTable=new JTable(attModel); Widgets.styleTable(attTable);
        Widgets.pillStatusColumn(attTable,4,Map.of("Yes",GREEN,"No",RED));
        loadAttendance();

        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField attSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        top.add(refresh);
        refresh.addActionListener(e->{attSearch.setText(""); loadAttendance();});
        attSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchAttendance(attSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchAttendance(attSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JScrollPane sp=new JScrollPane(attTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(top,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        tableCard.add(body,BorderLayout.CENTER);

        attTable.getSelectionModel().addListSelectionListener(e->{
            int r=attTable.getSelectedRow();
            if(r>=0){
                selectedAttendanceId=Integer.parseInt(String.valueOf(attModel.getValueAt(r,0)));
                aTourId.setText(String.valueOf(attModel.getValueAt(r,1)));
                aTourist.setText(String.valueOf(attModel.getValueAt(r,2)));
                selectedAttendancePresent="Yes".equals(String.valueOf(attModel.getValueAt(r,4)));
            }
        });

        present.addActionListener(e->mark(true));
        absent.addActionListener(e->mark(false));
        upd.addActionListener(e->updateAttendance());
        del.addActionListener(e->deleteAttendance());
        clr.addActionListener(e->clearAttendance());

        row.add(card,BorderLayout.WEST);
        row.add(tableCard,BorderLayout.CENTER);
        p.add(row, BorderLayout.CENTER);
        return p;
    }
    /** Present/Absent always record a brand-new attendance entry. */
    private void mark(boolean present){
        try{
            int tourId=Integer.parseInt(aTourId.getText().trim());
            String name=aTourist.getText().trim();
            if(name.isEmpty()){ msg("\u26A0 Enter tourist name"); return; }
            if(tgc.markAttendance(tourId,name,present)){
                msg("\u2705 Attendance recorded");
                loadAttendance(); clearAttendance();
                refreshHome();
            } else msg("\u274C Failed");
        }catch(Exception ex){ msg("\u26A0 Enter a valid Tour ID"); }
    }
    /** Updates the selected attendance record's tour/tourist name, keeping its present status. */
    private void updateAttendance(){
        if(selectedAttendanceId<0){ msg("\u26A0 Select a record first"); return; }
        try{
            int tourId=Integer.parseInt(aTourId.getText().trim());
            String name=aTourist.getText().trim();
            if(name.isEmpty()){ msg("\u26A0 Enter tourist name"); return; }
            if(tgc.updateAttendance(selectedAttendanceId,tourId,name,selectedAttendancePresent)){
                msg("\u2705 Updated");
                loadAttendance(); clearAttendance();
                refreshHome();
            } else msg("\u274C Failed");
        }catch(Exception ex){ msg("\u26A0 Enter a valid Tour ID"); }
    }
    /** Deletes the selected attendance record. */
    private void deleteAttendance(){
        if(selectedAttendanceId<0){ msg("\u26A0 Select a record first"); return; }
        if(JOptionPane.showConfirmDialog(this,"Delete attendance record "+selectedAttendanceId+"?",
                "Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        if(tgc.deleteAttendance(selectedAttendanceId)){
            msg("\u2705 Deleted");
            loadAttendance(); clearAttendance();
            refreshHome();
        } else msg("\u274C Failed");
    }
    private void clearAttendance(){
        aTourId.setText(""); aTourist.setText("");
        selectedAttendanceId=-1; selectedAttendancePresent=true;
        attTable.clearSelection();
    }
    private void loadAttendance(){
        attModel.setRowCount(0);
        for(Attendance a:tgc.getAllAttendance()) attModel.addRow(new Object[]{
            a.getAttendanceId(), a.getTourId(), a.getTouristName(), a.getDate(), a.isPresent()?"Yes":"No"});
    }
    private void doSearchAttendance(String q){
        attModel.setRowCount(0);
        List<Attendance> rows = q.isBlank() ? tgc.getAllAttendance() : tgc.searchAttendance(q);
        for(Attendance a:rows) attModel.addRow(new Object[]{
            a.getAttendanceId(), a.getTourId(), a.getTouristName(), a.getDate(), a.isPresent()?"Yes":"No"});
    }

    // ==================================================================
    // Tour Status
    // ==================================================================
    private JPanel buildTourStatus(){
        JPanel p=new JPanel(new BorderLayout(0,16));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Tour Status",
            "Track progress on every tour you're running.", "Dashboard / Tour Status"), BorderLayout.NORTH);

        JPanel card=Widgets.card("\uD83D\uDD04  Tour Progress");

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        filterBar.setOpaque(false);
        statusFilter = new JComboBox<>(new String[]{"All Status","Available","In-Progress","Completed","Cancelled"});
        Widgets.styleCombo(statusFilter);
        JButton search = Widgets.btn("Search",OCEAN);
        JButton clear = Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        search.addActionListener(e -> loadTourStatus());
        clear.addActionListener(e -> { statusFilter.setSelectedIndex(0); loadTourStatus(); });
        filterBar.add(Widgets.labeledBlock("Status",statusFilter));
        filterBar.add(search); filterBar.add(clear);

        statusModel = new DefaultTableModel(new String[]{"Tour ID","Name","Destination","Days","Price","Progress","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        statusTable = new JTable(statusModel);
        Widgets.styleTable(statusTable);
        Widgets.pillStatusColumn(statusTable,6,Map.of(
            "Available",GREEN,"In-Progress",Theme.SUN,"Completed",GREEN,"Cancelled",RED));
        statusTable.getColumnModel().getColumn(5).setCellRenderer(new ProgressCellRenderer());
        loadTourStatus();

        JScrollPane sp=new JScrollPane(statusTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(filterBar,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        card.add(body,BorderLayout.CENTER);

        p.add(card,BorderLayout.CENTER);
        return p;
    }

    private int progressFor(String status){
        if (status == null) return 0;
        switch (status) {
            case "Completed":   return 100;
            case "In-Progress": return 50;
            case "Cancelled":   return 0;
            default:            return 0; // Available / upcoming
        }
    }

    private void loadTourStatus(){
        statusModel.setRowCount(0);
        String filter = statusFilter == null ? "All Status" : String.valueOf(statusFilter.getSelectedItem());
        for (Tour t : tgc.getAssignedTours()) {
            if (!"All Status".equals(filter) && !filter.equals(t.getStatus())) continue;
            statusModel.addRow(new Object[]{
                t.getTourId(), t.getName(), t.getDestination(), t.getDurationDays(),
                "Rs. "+t.getPrice(), progressFor(t.getStatus()), t.getStatus()});
        }
    }

    /** Renders an int percentage (0-100) as a small coloured progress bar. */
    private class ProgressCellRenderer extends JProgressBar implements javax.swing.table.TableCellRenderer {
        ProgressCellRenderer(){ super(0,100); setBorderPainted(false); setStringPainted(true); }
        @Override public Component getTableCellRendererComponent(JTable tbl, Object value, boolean sel,
                boolean foc, int row, int col){
            int val = value instanceof Integer ? (Integer)value : 0;
            setValue(val);
            setString(val+"%");
            setForeground(val>=100?GREEN:(val>0?Theme.SUN:Theme.SUBTEXT));
            setBackground(row%2==0?Color.WHITE:Theme.CREAM);
            return this;
        }
    }

    /** Left-side category filter button for the Travel Info screen — a
     *  light-theme sibling of NavButton, styled for a white card. */
    private class CategoryButton extends JButton {
        private boolean active;
        CategoryButton(String text, boolean active){
            super(text);
            this.active = active;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setHorizontalAlignment(SwingConstants.LEFT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(200,38));
            setPreferredSize(new Dimension(200,38));
            setBorder(BorderFactory.createEmptyBorder(0,14,0,8));
            applyFont();
        }
        private void applyFont(){ setFont(new Font("Segoe UI",active?Font.BOLD:Font.PLAIN,12)); }
        void setActiveState(boolean a){ active=a; applyFont(); repaint(); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(active){
                g2.setColor(Theme.SKY);
                g2.fillRoundRect(4,2,getWidth()-8,getHeight()-4,8,8);
                g2.setColor(Theme.OCEAN);
                g2.fillRoundRect(4,6,4,getHeight()-12,4,4);
            } else if(getModel().isRollover()){
                g2.setColor(Theme.CREAM);
                g2.fillRoundRect(4,2,getWidth()-8,getHeight()-4,8,8);
            }
            g2.setFont(getFont());
            g2.setColor(active?Theme.OCEAN:Theme.DARK);
            FontMetrics fm=g2.getFontMetrics();
            int ty=(getHeight()-fm.getHeight())/2+fm.getAscent();
            g2.drawString(getText(),16,ty);
            g2.dispose();
        }
        @Override public boolean isOpaque(){ return false; }
    }

    // ==================================================================
    // Travel Info
    // ==================================================================
    private static class TravelInfoRow {
        String title, category, description, date;
        TravelInfoRow(String t,String c,String d,String dt){title=t;category=c;description=d;date=dt;}
    }

    private void seedTravelInfo(){
        travelRows.put("t1", new TravelInfoRow("Temple of the Tooth","Attractions",
            "Sacred Buddhist temple located in Kandy, one of the most venerated Buddhist sites in Sri Lanka. Dress modestly and remove footwear before entering.","01/05/2026"));
        travelRows.put("t2", new TravelInfoRow("Topaz Hotel - Kandy","Accommodation",
            "Recommended hotel near Kandy city, popular with tour groups for its central location and lake views.","01/05/2026"));
        travelRows.put("t3", new TravelInfoRow("Private Bus Service","Transport",
            "Contact for private bus arrangements for group transfers between destinations.","30/04/2026"));
        travelRows.put("t4", new TravelInfoRow("Tourist Police - 119","Emergency Contacts",
            "24/7 support line for tourists in case of emergencies, lost documents, or safety concerns.","30/04/2026"));
        travelRows.put("t5", new TravelInfoRow("Tour Guidelines","Guidelines",
            "Important rules to follow during the tour, including group timing, safety briefings and conduct.","29/04/2026"));
    }

    private JPanel buildTravelInfo(){
        JPanel p=new JPanel(new BorderLayout(0,16));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Travel Info",
            "Stay informed and provide the best experience to your tourists.", "Dashboard / Travel Info"), BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(14,14));
        row.setOpaque(false);

        // Left: category filter list
        JPanel catCard = Widgets.card(null);
        catCard.setPreferredSize(new Dimension(210,0));
        JPanel catList = new JPanel();
        catList.setOpaque(false);
        catList.setLayout(new BoxLayout(catList, BoxLayout.Y_AXIS));
        String[] cats = {"All Information","Attractions","Accommodation","Transport","Emergency Contacts","Guidelines"};
        for (String c : cats) {
            CategoryButton b = new CategoryButton(c, c.equals(travelCategoryFilter));
            b.addActionListener(e -> {
                travelCategoryFilter = c;
                for (Component comp : catList.getComponents()) {
                    if (comp instanceof CategoryButton cb) cb.setActiveState(cb.getText().equals(c));
                }
                loadTravelInfo();
            });
            catList.add(b);
        }
        catCard.add(catList, BorderLayout.NORTH);

        // Right: info table
        JPanel tableCard = Widgets.plainCard();
        tableCard.setLayout(new BorderLayout(0,8));

        travelModel = new DefaultTableModel(new String[]{"Title","Category","Description","Date"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        travelTable = new JTable(travelModel);
        Widgets.styleTable(travelTable);
        loadTravelInfo();

        JButton view = Widgets.btn("\uD83D\uDC41 View Selected",OCEAN);
        view.addActionListener(e -> viewSelectedTravelInfo());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JLabel hint = new JLabel("Select a row, then click View for the full description.");
        hint.setFont(new Font("Segoe UI",Font.PLAIN,11));
        hint.setForeground(Theme.SUBTEXT);
        top.add(view); top.add(hint);

        JScrollPane sp = new JScrollPane(travelTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        tableCard.add(top, BorderLayout.NORTH);
        tableCard.add(sp, BorderLayout.CENTER);

        row.add(catCard, BorderLayout.WEST);
        row.add(tableCard, BorderLayout.CENTER);
        p.add(row, BorderLayout.CENTER);
        return p;
    }

    private void loadTravelInfo(){
        if (travelModel == null) return;
        travelModel.setRowCount(0);
        for (TravelInfoRow r : travelRows.values()) {
            if (!"All Information".equals(travelCategoryFilter) && !travelCategoryFilter.equals(r.category)) continue;
            travelModel.addRow(new Object[]{ r.title, r.category, r.description, r.date });
        }
    }

    private void viewSelectedTravelInfo(){
        int sel = travelTable.getSelectedRow();
        if (sel < 0) { msg("Select a row first."); return; }
        String title = String.valueOf(travelModel.getValueAt(sel,0));
        String category = String.valueOf(travelModel.getValueAt(sel,1));
        String desc = String.valueOf(travelModel.getValueAt(sel,2));
        JOptionPane.showMessageDialog(this, desc, title+"  \u2014  "+category, JOptionPane.INFORMATION_MESSAGE);
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
