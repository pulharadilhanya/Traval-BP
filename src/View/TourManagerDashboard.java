package View;

import controller.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tour Manager Dashboard.
 *
 * Structure mirrors AdminDashboard: top bar (NORTH) + sidebar nav (WEST) +
 * card-swapped content (CENTER), built with a CardLayout so every section
 * lives on one JFrame instead of popping new windows.
 *
 * "home" is the hub screen: a page heading, five clickable stat cards
 * (Tour Packages / Schedule Management / Manage Hotels / Manage Vehicles /
 * Assign Tour Guide) each linking to its own screen, and a single
 * "Today's Summary" card underneath listing recent package activity plus
 * booking/guide totals. "packages", "schedule", "hotels" and "vehicles"
 * are fully wired CRUD screens. "guides" is Guide Assignment, wired to its
 * own DB-backed table.
 */
public class TourManagerDashboard extends JFrame {
    private final Color DARK=Theme.DARK, OCEAN=Theme.OCEAN,
      SUN=Theme.SUN, LIGHT=Theme.LIGHT,
      GREEN=Theme.GREEN, RED=Theme.RED, ORANGE=Theme.ORANGE, PURPLE=Theme.PURPLE;

    private final String username;
    private final TourManagerController tc=new TourManagerController();
    private final BookingController bc=new BookingController();
    private final UserController uc=new UserController();
    private final HotelController hc=new HotelController();
    private final VehicleController vc=new VehicleController();
    private final ScheduleController sc=new ScheduleController();
    private final GuideAssignmentController gac=new GuideAssignmentController();

    private JTable table; private DefaultTableModel model;
    private JTextField tId,tName,tDest,tDesc,tDur,tPrice;
    private JComboBox<String> tStatus;

    // ---- Schedule Management screen state -------------------------------------
    private JTable sTable; private DefaultTableModel sModel;
    private List<Object[]> sRawRows = new java.util.ArrayList<>();
    private JTextField sId,sDate,sStart,sEnd,sLocation,sNotes;
    private JComboBox<String> sTourCombo, sStatus;

    // ---- Manage Hotels screen state ----------------------------------------
    private JTable hTable; private DefaultTableModel hModel;
    private JTextField hId,hName,hLocation,hRooms,hPrice;
    private JComboBox<String> hStatus;

    // ---- Manage Vehicles screen state --------------------------------------
    private JTable vTable; private DefaultTableModel vModel;
    private JTextField vId,vPlate,vSeats;
    private JComboBox<String> vType, vStatus;

    // ---- Assign Tour Guide screen state ------------------------------------
    private JTable gTable; private DefaultTableModel gModel;
    private List<Object[]> gRawRows = new java.util.ArrayList<>();
    private JTextField gId,gDate,gNotes;
    private JComboBox<String> gTourCombo, gGuideCombo, gStatus;

    private final CardLayout cl = new CardLayout();
    private final JPanel contentPanel = new JPanel(cl);
    private final Map<String,NavButton> navButtons = new LinkedHashMap<>();

    // ---- Home screen bits that need refreshing on every visit ------------
    private JPanel statsRow, summaryRows;

    /** Builds and shows the Tour Manager Dashboard window for the given logged-in user. */
    public TourManagerDashboard(String u){
        this.username = u;
        setTitle("Tourism Management System \u2014 Tour Manager Dashboard");
        setSize(1320,780);
        setMinimumSize(new Dimension(1100,660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI(); setVisible(true);
    }

    private String displayName(){
        return (username==null||username.isBlank()) ? "Tour Manager" : username;
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
        contentPanel.add(wrap(buildPackagesScreen()), "packages");
        contentPanel.add(wrap(buildScheduleScreen()), "schedule");
        contentPanel.add(wrap(buildHotelsScreen()), "hotels");
        contentPanel.add(wrap(buildVehiclesScreen()), "vehicles");
        contentPanel.add(wrap(buildGuidesScreen()), "guides");
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
        if ("home".equals(key)) refreshHome();
        if ("packages".equals(key)) load();
        if ("schedule".equals(key)) loadSchedules();
        if ("hotels".equals(key)) loadHotels();
        if ("vehicles".equals(key)) loadVehicles();
        if ("guides".equals(key)) loadGuideAssignments();
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

    private void confirmLogout(){
        if (JOptionPane.showConfirmDialog(this,"Log out of the Tour Manager Dashboard?","Confirm",
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
        JLabel role = new JLabel("Tour Manager");
        role.setForeground(new Color(160,190,198));
        role.setFont(new Font("Segoe UI",Font.PLAIN,11));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.add(avatar); profile.add(Box.createVerticalStrut(6));
        profile.add(name); profile.add(role);

        side.add(profile);
        side.add(divider());
        side.add(Box.createVerticalStrut(12));

        side.add(navItem("Dashboard","home"));
        side.add(navItem("Tour Package","packages"));
        side.add(navItem("Schedule Management","schedule"));
        side.add(navItem("Manage Hotels","hotels"));
        side.add(navItem("Manage Vehicles","vehicles"));
        side.add(navItem("Assign Tour Guide","guides"));

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

    /** Sidebar nav button with a mutable "active" highlight — same look as AdminDashboard's. */
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
        JPanel root = new JPanel(new BorderLayout(0,16));
        root.setBackground(LIGHT);
        root.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));

        JPanel headRow = Widgets.pageHeading("Tour Manager Dashboard",
            "Welcome back, "+displayName()+"! Here is your overview.", null);

        statsRow = new JPanel(new GridLayout(1,5,14,0));
        statsRow.setOpaque(false);

        JPanel summaryCard = Widgets.card("Today's Summary");
        summaryRows = new JPanel();
        summaryRows.setOpaque(false);
        summaryRows.setLayout(new BoxLayout(summaryRows, BoxLayout.Y_AXIS));
        summaryCard.add(summaryRows, BorderLayout.CENTER);

        root.add(headRow, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0,16));
        mid.setOpaque(false);
        mid.add(statsRow, BorderLayout.NORTH);
        mid.add(summaryCard, BorderLayout.CENTER);
        root.add(mid, BorderLayout.CENTER);

        refreshHome();
        return root;
    }

    private JPanel summaryRow(String icon,String label,String value,Color accent){
        JPanel row=new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(9,2,9,2));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        left.setOpaque(false);
        JLabel ic=new JLabel(icon);
        ic.setFont(new Font("Segoe UI Emoji",Font.PLAIN,16));
        JLabel lab=new JLabel(label);
        lab.setFont(new Font("Segoe UI",Font.PLAIN,13));
        lab.setForeground(DARK);
        left.add(ic); left.add(lab);
        JLabel val=new JLabel(value);
        val.setFont(new Font("Segoe UI",Font.BOLD,14));
        val.setForeground(accent);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        right.setOpaque(false); right.add(val);
        row.add(left,BorderLayout.WEST);
        row.add(right,BorderLayout.EAST);
        return row;
    }

    /** Pulls fresh counts and recent packages every time the hub is shown. */
    private void refreshHome(){
        if (statsRow == null) return;

        List<TourPackage> packages = tc.getAllPackages();
        int bookings = bc.getAll().size();
        int schedules = sc.getAll().size();
        long availableHotels = hc.getAll().stream()
            .filter(h -> "Available".equalsIgnoreCase(h.getStatus())).count();
        long availableVehicles = vc.getAll().stream()
            .filter(v -> "Available".equalsIgnoreCase(v.getStatus())).count();
        int guides = 0;
        for (Object[] u : uc.getAllUsers()){
            if (u.length>4 && "TourGuide".equalsIgnoreCase(String.valueOf(u[4]))) guides++;
        }

        statsRow.removeAll();
        statsRow.add(Widgets.statCard("\uD83E\uDDF3","Tour Packages",String.valueOf(packages.size()),
            "Total Packages","Manage \u2192",OCEAN, () -> switchTo("packages")));
        statsRow.add(Widgets.statCard("\uD83D\uDDD3\uFE0F","Schedule Management",String.valueOf(schedules),
            "Total Schedules","Manage \u2192",GREEN, () -> switchTo("schedule")));
        statsRow.add(Widgets.statCard("\uD83C\uDFE8","Manage Hotels",String.valueOf(availableHotels),
            "Available Hotels","Manage \u2192",PURPLE, () -> switchTo("hotels")));
        statsRow.add(Widgets.statCard("\uD83D\uDE8C","Manage Vehicles",String.valueOf(availableVehicles),
            "Available Vehicles","Manage \u2192",ORANGE, () -> switchTo("vehicles")));
        statsRow.add(Widgets.statCard("\uD83E\uDDED","Assign Tour Guide",String.valueOf(guides),
            "Total Guides","Manage \u2192",Theme.HEADER, () -> switchTo("guides")));
        statsRow.revalidate();
        statsRow.repaint();

        summaryRows.removeAll();
        int shown=0;
        for (int i=packages.size()-1; i>=0 && shown<4; i--, shown++){
            TourPackage t = packages.get(i);
            String status = t.getStatus()==null? "" : t.getStatus();
            Color c = status.equalsIgnoreCase("Available") ? GREEN
                : status.equalsIgnoreCase("In-Progress") ? ORANGE
                : status.equalsIgnoreCase("Completed") ? OCEAN
                : status.equalsIgnoreCase("Cancelled") ? RED : DARK;
            if (shown>0) summaryRows.add(new JSeparator());
            summaryRows.add(summaryRow("\uD83D\uDCE6","Package \""+t.getName()+"\"",status,c));
        }
        if (shown==0){
            summaryRows.add(summaryRow("\u2139","No tour packages yet","Add one from Tour Packages",DARK));
        }
        summaryRows.add(new JSeparator());
        summaryRows.add(summaryRow("\u2705","Total Bookings",String.valueOf(bookings),OCEAN));
        summaryRows.add(new JSeparator());
        summaryRows.add(summaryRow("\uD83E\uDDED","Total Guides",String.valueOf(guides),Theme.HEADER));
        summaryRows.revalidate();
        summaryRows.repaint();
    }

    // ==================================================================
    // Schedule Management screen (tourId + date/time/location/notes/status)
    // ==================================================================
    private JPanel buildScheduleScreen(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Schedule Management",
              "Set tour dates, start/end times, locations and notes for each package.",
              "Dashboard / Schedule Management"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14,14));
        body.setOpaque(false);

        JPanel card=Widgets.card("\uD83D\uDDD3\uFE0F  Schedule Details");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        sId=Widgets.field(); sId.setEditable(false);
        sTourCombo=new JComboBox<>();
        sDate=Widgets.field(); sStart=Widgets.field(); sEnd=Widgets.field();
        sLocation=Widgets.field(); sNotes=Widgets.field();
        sStatus=new JComboBox<>(new String[]{"Active","Completed","Cancelled"});

        Widgets.addField(f,0," ID:",sId);
        Widgets.addField(f,1,"Tour Package:",sTourCombo);
        Widgets.addField(f,2,"Tour Date (yyyy-mm-dd):",sDate);
        Widgets.addField(f,3,"Start Time (HH:mm):",sStart);
        Widgets.addField(f,4,"End Time (HH:mm):",sEnd);
        Widgets.addField(f,5,"Location / Route:",sLocation);
        Widgets.addField(f,6,"Notes:",sNotes);
        Widgets.addField(f,7,"Status:",sStatus);

        JButton add=Widgets.btn("\u2795 Add",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),//add button create
                del=Widgets.btn("\uD83D\uDDD1 Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=8; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=9; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  All Schedules");
        sModel=new DefaultTableModel(new String[]{"ID","Package Name","Tour Date","Start","End","Location / Route","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        sTable=new JTable(sModel); Widgets.styleTable(sTable);
        Widgets.pillStatusColumn(sTable,6,Map.of(
            "Active",GREEN,"Completed",OCEAN,"Cancelled",RED));

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField search=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{search.setText(""); loadSchedules();});
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doScheduleSearch(search.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doScheduleSearch(search.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });
        JScrollPane sp=new JScrollPane(sTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel bodyInner=new JPanel(new BorderLayout(0,8)); bodyInner.setOpaque(false);
        bodyInner.add(top,BorderLayout.NORTH); bodyInner.add(sp,BorderLayout.CENTER);
        tableCard.add(bodyInner,BorderLayout.CENTER);

        sTable.getSelectionModel().addListSelectionListener(e->{
            int r=sTable.getSelectedRow();
            if(r>=0 && r<sRawRows.size()){
                Object[] raw=sRawRows.get(r);
                sId.setText(String.valueOf(raw[0]));
                sTourCombo.setSelectedItem(raw[1]+" - "+raw[2]);
                sDate.setText(String.valueOf(raw[3]));
                sStart.setText(String.valueOf(raw[4]));
                sEnd.setText(String.valueOf(raw[5]));
                sLocation.setText(raw[6]==null?"":raw[6].toString());
                sNotes.setText(raw[7]==null?"":raw[7].toString());
                sStatus.setSelectedItem(String.valueOf(raw[8]));
            }
        });

        add.addActionListener(e->{
            Integer tourId=selectedTourId();
            if(tourId==null){ msg("\u26A0 Choose a tour package"); return; }
            try{
                if(sc.addSchedule(tourId,sDate.getText().trim(),sStart.getText().trim(),sEnd.getText().trim(),
                        sLocation.getText(),sNotes.getText(),(String)sStatus.getSelectedItem())){
                    msg("\u2705 Scheduled"); loadSchedules(); clearSchedule();
                } else msg("\u274C Failed \u2014 check the date/time format");
            }catch(Exception ex){msg("\u26A0 Use yyyy-mm-dd for the date and HH:mm for times");}
        });
        upd.addActionListener(e->{
            Integer tourId=selectedTourId();
            if(tourId==null){ msg("\u26A0 Choose a tour package"); return; }
            try{
                int id=Integer.parseInt(sId.getText());
                if(sc.updateSchedule(id,tourId,sDate.getText().trim(),sStart.getText().trim(),sEnd.getText().trim(),
                        sLocation.getText(),sNotes.getText(),(String)sStatus.getSelectedItem())){
                    msg("\u2705 Updated"); loadSchedules();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Select a schedule row first, or check the date/time format");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(sId.getText());
                if(JOptionPane.showConfirmDialog(this,"Delete schedule "+id+"?")==JOptionPane.YES_OPTION){
                    if(sc.deleteSchedule(id)){msg("\u2705 Deleted"); loadSchedules(); clearSchedule();} else msg("\u274C Failed");
                }
            }catch(Exception ex){msg("\u26A0 Select a schedule row first");}
        });
        clr.addActionListener(e->clearSchedule());

        body.add(card,BorderLayout.WEST);
        body.add(tableCard,BorderLayout.CENTER);
        p.add(body,BorderLayout.CENTER);
        return p;
    }

    /** Reads the leading "id - " prefix off the selected combo entry. */
    private Integer selectedTourId(){
        Object sel=sTourCombo.getSelectedItem();
        if(sel==null) return null;
        String s=sel.toString();
        int dash=s.indexOf(" - ");
        if(dash<0) return null;
        try{ return Integer.parseInt(s.substring(0,dash).trim()); }catch(Exception ex){ return null; }
    }

    private void refreshTourCombo(){
        String prev = sTourCombo.getSelectedItem()==null? null : sTourCombo.getSelectedItem().toString();
        sTourCombo.removeAllItems();
        for(TourPackage t : tc.getAllPackages()) sTourCombo.addItem(t.getTourId()+" - "+t.getName());
        if(prev!=null) sTourCombo.setSelectedItem(prev);
    }

    private void doScheduleSearch(String q){
        sRawRows = q.isBlank() ? sc.getAll() : sc.search(q);
        fillScheduleTable();
    }
    private void loadSchedules(){
        refreshTourCombo();
        sRawRows = sc.getAll();
        fillScheduleTable();
    }
    private void fillScheduleTable(){
        sModel.setRowCount(0);
        for(Object[] r: sRawRows) sModel.addRow(new Object[]{
            r[0], r[2], r[3], r[4], r[5], r[6], r[8]});
    }
    private void clearSchedule(){
        sId.setText(""); sDate.setText(""); sStart.setText(""); sEnd.setText("");
        sLocation.setText(""); sNotes.setText("");
        if(sTourCombo.getItemCount()>0) sTourCombo.setSelectedIndex(0);
        sStatus.setSelectedIndex(0);
        sTable.clearSelection();
    }

    // ==================================================================
    // Manage Hotels screen
    // (name, location, available rooms, price per night, status)
    // ==================================================================
    private JPanel buildHotelsScreen(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Manage Hotels",
              "Add and update partner hotels, room availability and pricing.",
              "Dashboard / Manage Hotels"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14,14));
        body.setOpaque(false);

        JPanel card=Widgets.card("\uD83C\uDFE8  Hotel Details");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        hId=Widgets.field(); hId.setEditable(false);
        hName=Widgets.field(); hLocation=Widgets.field();
        hRooms=Widgets.field(); hPrice=Widgets.field();
        hStatus=new JComboBox<>(new String[]{"Available","Full","Closed"});

        Widgets.addField(f,0," ID:",hId);
        Widgets.addField(f,1,"Hotel Name:",hName);
        Widgets.addField(f,2,"Location:",hLocation);
        Widgets.addField(f,3,"Available Rooms:",hRooms);
        Widgets.addField(f,4,"Price / Night (LKR):",hPrice);
        Widgets.addField(f,5,"Status:",hStatus);

        JButton add=Widgets.btn("\u2795 Add",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),
                del=Widgets.btn("\uD83D\uDDD1 Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=6; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=7; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  All Hotels");
        hModel=new DefaultTableModel(new String[]{"ID","Hotel Name","Location","Available Rooms","Price/Night (LKR)","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        hTable=new JTable(hModel); Widgets.styleTable(hTable);
        Widgets.pillStatusColumn(hTable,5,Map.of("Available",GREEN,"Full",ORANGE,"Closed",RED));
        JScrollPane sp=new JScrollPane(hTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField hSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{hSearch.setText(""); loadHotels();});
        hSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchHotels(hSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchHotels(hSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JPanel bodyInner=new JPanel(new BorderLayout(0,8)); bodyInner.setOpaque(false);
        bodyInner.add(top,BorderLayout.NORTH); bodyInner.add(sp,BorderLayout.CENTER);
        tableCard.add(bodyInner,BorderLayout.CENTER);

        hTable.getSelectionModel().addListSelectionListener(e->{
            int r=hTable.getSelectedRow();
            if(r>=0){
                hId.setText(String.valueOf(hModel.getValueAt(r,0)));
                hName.setText(String.valueOf(hModel.getValueAt(r,1)));
                hLocation.setText(String.valueOf(hModel.getValueAt(r,2)));
                hRooms.setText(String.valueOf(hModel.getValueAt(r,3)));
                hPrice.setText(String.valueOf(hModel.getValueAt(r,4)));
                hStatus.setSelectedItem(String.valueOf(hModel.getValueAt(r,5)));
            }
        });

        add.addActionListener(e->{
            try{
                if(hc.addHotel(hName.getText(),hLocation.getText(),
                        Integer.parseInt(hRooms.getText()),Float.parseFloat(hPrice.getText()),
                        (String)hStatus.getSelectedItem())){
                    msg("\u2705 Added"); loadHotels(); clearHotel();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Enter valid values");}
        });
        upd.addActionListener(e->{
            try{
                if(hc.updateHotel(Integer.parseInt(hId.getText()),hName.getText(),hLocation.getText(),
                        Integer.parseInt(hRooms.getText()),Float.parseFloat(hPrice.getText()),
                        (String)hStatus.getSelectedItem())){
                    msg("\u2705 Updated"); loadHotels();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Select a hotel row first, or check the values");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(hId.getText());
                if(JOptionPane.showConfirmDialog(this,"Delete hotel "+id+"?")==JOptionPane.YES_OPTION){
                    if(hc.deleteHotel(id)){msg("\u2705 Deleted"); loadHotels(); clearHotel();} else msg("\u274C Failed");
                }
            }catch(Exception ex){msg("\u26A0 Select a hotel row first");}
        });
        clr.addActionListener(e->clearHotel());

        body.add(card,BorderLayout.WEST);
        body.add(tableCard,BorderLayout.CENTER);
        p.add(body,BorderLayout.CENTER);
        return p;
    }
    private void loadHotels(){
        hModel.setRowCount(0);
        for(Hotel h: hc.getAll()) hModel.addRow(new Object[]{
            h.getHotelId(),h.getName(),h.getLocation(),h.getRooms(),h.getPricePerNight(),h.getStatus()});
    }
    private void doSearchHotels(String q){
        hModel.setRowCount(0);
        List<Hotel> rows = q.isBlank() ? hc.getAll() : hc.searchHotels(q);
        for(Hotel h: rows) hModel.addRow(new Object[]{
            h.getHotelId(),h.getName(),h.getLocation(),h.getRooms(),h.getPricePerNight(),h.getStatus()});
    }
    private void clearHotel(){
        hId.setText(""); hName.setText(""); hLocation.setText("");
        hRooms.setText(""); hPrice.setText(""); hStatus.setSelectedIndex(0);
        hTable.clearSelection();
    }

    // ==================================================================
    // Manage Vehicles screen
    // ==================================================================
    private JPanel buildVehiclesScreen(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Manage Vehicles",
              "Track vehicle type, capacity, registration and availability status.",
              "Dashboard / Manage Vehicles"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14,14));
        body.setOpaque(false);

        JPanel card=Widgets.card("\uD83D\uDE8C  Vehicle Details");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        vId=Widgets.field(); vId.setEditable(false);
        vType=new JComboBox<>(new String[]{"Bus","Van","Car"});
        vType.setEditable(true);
        vPlate=Widgets.field(); vSeats=Widgets.field();
        vStatus=new JComboBox<>(new String[]{"Available","Assigned","Maintenance"});

        Widgets.addField(f,0," ID:",vId);
        Widgets.addField(f,1,"Vehicle Type:",vType);
        Widgets.addField(f,2,"Capacity:",vSeats);
        Widgets.addField(f,3,"Reg. Number:",vPlate);
        Widgets.addField(f,4,"Status:",vStatus);

        JButton add=Widgets.btn("\u2795 Add",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),
                del=Widgets.btn("\uD83D\uDDD1 Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=5; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=6; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  All Vehicles");
        vModel=new DefaultTableModel(new String[]{"ID","Vehicle Type","Capacity","Reg. Number","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        vTable=new JTable(vModel); Widgets.styleTable(vTable);
        Widgets.pillStatusColumn(vTable,4,Map.of(
            "Available",GREEN,"Assigned",OCEAN,"Maintenance",RED));
        JScrollPane sp=new JScrollPane(vTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField vSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{vSearch.setText(""); loadVehicles();});
        vSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchVehicles(vSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchVehicles(vSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JPanel bodyInner=new JPanel(new BorderLayout(0,8)); bodyInner.setOpaque(false);
        bodyInner.add(top,BorderLayout.NORTH); bodyInner.add(sp,BorderLayout.CENTER);
        tableCard.add(bodyInner,BorderLayout.CENTER);

        vTable.getSelectionModel().addListSelectionListener(e->{
            int r=vTable.getSelectedRow();
            if(r>=0){
                vId.setText(String.valueOf(vModel.getValueAt(r,0)));
                vType.setSelectedItem(String.valueOf(vModel.getValueAt(r,1)));
                vSeats.setText(String.valueOf(vModel.getValueAt(r,2)));
                vPlate.setText(String.valueOf(vModel.getValueAt(r,3)));
                vStatus.setSelectedItem(String.valueOf(vModel.getValueAt(r,4)));
            }
        });

        add.addActionListener(e->{
            try{
                if(vc.addVehicle(String.valueOf(vType.getSelectedItem()),vPlate.getText(),
                        Integer.parseInt(vSeats.getText()),(String)vStatus.getSelectedItem())){
                    msg("\u2705 Added"); loadVehicles(); clearVehicle();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Enter valid values");}
        });
        upd.addActionListener(e->{
            try{
                if(vc.updateVehicle(Integer.parseInt(vId.getText()),String.valueOf(vType.getSelectedItem()),
                        vPlate.getText(),Integer.parseInt(vSeats.getText()),(String)vStatus.getSelectedItem())){
                    msg("\u2705 Updated"); loadVehicles();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Select a vehicle row first, or check the values");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(vId.getText());
                if(JOptionPane.showConfirmDialog(this,"Delete vehicle "+id+"?")==JOptionPane.YES_OPTION){
                    if(vc.deleteVehicle(id)){msg("\u2705 Deleted"); loadVehicles(); clearVehicle();} else msg("\u274C Failed");
                }
            }catch(Exception ex){msg("\u26A0 Select a vehicle row first");}
        });
        clr.addActionListener(e->clearVehicle());

        body.add(card,BorderLayout.WEST);
        body.add(tableCard,BorderLayout.CENTER);
        p.add(body,BorderLayout.CENTER);
        return p;
    }
    private void loadVehicles(){
        vModel.setRowCount(0);
        for(Vehicle v: vc.getAll()) vModel.addRow(new Object[]{
            v.getVehicleId(),v.getType(),v.getSeats(),v.getPlateNo(),v.getStatus()});
    }
    private void doSearchVehicles(String q){
        vModel.setRowCount(0);
        List<Vehicle> rows = q.isBlank() ? vc.getAll() : vc.searchVehicles(q);
        for(Vehicle v: rows) vModel.addRow(new Object[]{
            v.getVehicleId(),v.getType(),v.getSeats(),v.getPlateNo(),v.getStatus()});
    }
    private void clearVehicle(){
        vId.setText(""); vSeats.setText(""); vPlate.setText("");
        if(vType.getItemCount()>0) vType.setSelectedIndex(0);
        vStatus.setSelectedIndex(0);
        vTable.clearSelection();
    }

    // ==================================================================
    // Assign Tour Guide screen
    //
    // A "guide" is just a users row with role = TourGuide — there's no
    // separate guides table, so the guide picker shows name + contact
    // (what's actually stored in the users table).
    // ==================================================================
    private JPanel buildGuidesScreen(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Assign Tour Guide",
              "Match available tour guides to upcoming tour packages.",
              "Dashboard / Assign Tour Guide"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14,14));
        body.setOpaque(false);

        JPanel card=Widgets.card("\uD83E\uDDED  Assign Guide");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        gId=Widgets.field(); gId.setEditable(false);
        gTourCombo=new JComboBox<>();
        gGuideCombo=new JComboBox<>();
        gDate=Widgets.field(); gNotes=Widgets.field();
        gStatus=new JComboBox<>(new String[]{"Assigned","Completed","Cancelled"});

        Widgets.addField(f,0," ID:",gId);
        Widgets.addField(f,1,"Tour Package:",gTourCombo);
        Widgets.addField(f,2,"Tour Date (yyyy-mm-dd):",gDate);
        Widgets.addField(f,3,"Select Tour Guide:",gGuideCombo);
        Widgets.addField(f,4,"Notes:",gNotes);
        Widgets.addField(f,5,"Status:",gStatus);

        JButton assign=Widgets.btn("\u2795 Assign Guide",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),
                del=Widgets.btn("\uD83D\uDDD1 Remove",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false);
        btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(assign,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=6; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=7; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  Assigned Guides");
        gModel=new DefaultTableModel(new String[]{"ID","Guide Name","Tour Package","Tour Date","Notes","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        gTable=new JTable(gModel); Widgets.styleTable(gTable);
        Widgets.pillStatusColumn(gTable,5,Map.of(
            "Assigned",OCEAN,"Completed",GREEN,"Cancelled",RED));
        JScrollPane sp=new JScrollPane(gTable);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField gSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{gSearch.setText(""); loadGuideAssignments();});
        gSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchGuides(gSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchGuides(gSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        JPanel bodyInner=new JPanel(new BorderLayout(0,8)); bodyInner.setOpaque(false);
        bodyInner.add(top,BorderLayout.NORTH); bodyInner.add(sp,BorderLayout.CENTER);
        tableCard.add(bodyInner,BorderLayout.CENTER);

        gTable.getSelectionModel().addListSelectionListener(e->{
            int r=gTable.getSelectedRow();
            if(r>=0 && r<gRawRows.size()){
                Object[] raw=gRawRows.get(r);
                gId.setText(String.valueOf(raw[0]));
                gGuideCombo.setSelectedItem(raw[1]+" - "+raw[2]);
                gTourCombo.setSelectedItem(raw[3]+" - "+raw[4]);
                gDate.setText(String.valueOf(raw[5]));
                gNotes.setText(raw[6]==null?"":raw[6].toString());
                gStatus.setSelectedItem(String.valueOf(raw[7]));
            }
        });

        assign.addActionListener(e->{
            Integer tourId=selectedIdFromCombo(gTourCombo);
            Integer guideId=selectedIdFromCombo(gGuideCombo);
            if(tourId==null){ msg("\u26A0 Choose a tour package"); return; }
            if(guideId==null){ msg("\u26A0 Choose a tour guide"); return; }
            try{
                if(gac.assignGuide(guideId,tourId,gDate.getText().trim(),gNotes.getText(),(String)gStatus.getSelectedItem())){
                    msg("\u2705 Guide assigned"); loadGuideAssignments(); clearGuideAssignment();
                } else msg("\u274C Failed \u2014 check the date format");
            }catch(Exception ex){msg("\u26A0 Use yyyy-mm-dd for the date");}
        });
        upd.addActionListener(e->{
            Integer tourId=selectedIdFromCombo(gTourCombo);
            Integer guideId=selectedIdFromCombo(gGuideCombo);
            if(tourId==null){ msg("\u26A0 Choose a tour package"); return; }
            if(guideId==null){ msg("\u26A0 Choose a tour guide"); return; }
            try{
                int id=Integer.parseInt(gId.getText());
                if(gac.updateAssignment(id,guideId,tourId,gDate.getText().trim(),gNotes.getText(),(String)gStatus.getSelectedItem())){
                    msg("\u2705 Updated"); loadGuideAssignments();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Select an assignment row first, or check the date format");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(gId.getText());
                if(JOptionPane.showConfirmDialog(this,"Remove assignment "+id+"?")==JOptionPane.YES_OPTION){
                    if(gac.deleteAssignment(id)){msg("\u2705 Removed"); loadGuideAssignments(); clearGuideAssignment();} else msg("\u274C Failed");
                }
            }catch(Exception ex){msg("\u26A0 Select an assignment row first");}
        });
        clr.addActionListener(e->clearGuideAssignment());

        body.add(card,BorderLayout.WEST);
        body.add(tableCard,BorderLayout.CENTER);
        p.add(body,BorderLayout.CENTER);
        return p;
    }

    /** Reads the leading "id - " prefix off a "id - label" combo entry. */
    private Integer selectedIdFromCombo(JComboBox<String> combo){
        Object sel=combo.getSelectedItem();
        if(sel==null) return null;
        String s=sel.toString();
        int dash=s.indexOf(" - ");
        if(dash<0) return null;
        try{ return Integer.parseInt(s.substring(0,dash).trim()); }catch(Exception ex){ return null; }
    }

    private void refreshGuidePickers(){
        String prevTour = gTourCombo.getSelectedItem()==null? null : gTourCombo.getSelectedItem().toString();
        gTourCombo.removeAllItems();
        for(TourPackage t : tc.getAllPackages()) gTourCombo.addItem(t.getTourId()+" - "+t.getName());
        if(prevTour!=null) gTourCombo.setSelectedItem(prevTour);

        String prevGuide = gGuideCombo.getSelectedItem()==null? null : gGuideCombo.getSelectedItem().toString();
        gGuideCombo.removeAllItems();
        for(Object[] g : gac.getAvailableGuides()) gGuideCombo.addItem(g[0]+" - "+g[1]);
        if(prevGuide!=null) gGuideCombo.setSelectedItem(prevGuide);
    }

    private void loadGuideAssignments(){
        refreshGuidePickers();
        gRawRows = gac.getAll();
        gModel.setRowCount(0);
        for(Object[] r: gRawRows) gModel.addRow(new Object[]{
            r[0], r[2], r[4], r[5], r[6], r[7]});
    }
    private void doSearchGuides(String q){
        gRawRows = q.isBlank() ? gac.getAll() : gac.search(q);
        gModel.setRowCount(0);
        for(Object[] r: gRawRows) gModel.addRow(new Object[]{
            r[0], r[2], r[4], r[5], r[6], r[7]});
    }
    private void clearGuideAssignment(){
        gId.setText(""); gDate.setText(""); gNotes.setText("");
        if(gTourCombo.getItemCount()>0) gTourCombo.setSelectedIndex(0);
        if(gGuideCombo.getItemCount()>0) gGuideCombo.setSelectedIndex(0);
        gStatus.setSelectedIndex(0);
        gTable.clearSelection();
    }

    // ==================================================================
    // Tour Package screen
    // ==================================================================
    private JPanel buildPackagesScreen(){
        JPanel p=new JPanel(new BorderLayout(14,14));
        p.setBackground(LIGHT); p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));

        JPanel headWrap = new JPanel(new BorderLayout());
        headWrap.setOpaque(false);
        headWrap.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));
        headWrap.add(Widgets.pageHeading("Tour Packages","Manage all tour packages","Dashboard / Tour Packages"), BorderLayout.CENTER);
        p.add(headWrap, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14,14));
        body.setOpaque(false);

        JPanel card=Widgets.card("\uD83D\uDDFA\uFE0F  Tour Package");
        card.setPreferredSize(new Dimension(340,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        tId=Widgets.field(); tName=Widgets.field(); tDest=Widgets.field();
        tDesc=Widgets.field(); tDur=Widgets.field(); tPrice=Widgets.field();
        tStatus=new JComboBox<>(new String[]{"Available","Cancelled","In-Progress","Completed"});

        Widgets.addField(f,0," ID:",tId);
        Widgets.addField(f,1,"Name:",tName);
        Widgets.addField(f,2,"Destination:",tDest);
        Widgets.addField(f,3,"Description:",tDesc);
        Widgets.addField(f,4,"Duration (days):",tDur);
        Widgets.addField(f,5,"Price:",tPrice);
        Widgets.addField(f,6,"Status:",tStatus);

        JButton add=Widgets.btn("\u2795 Add",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),
                del=Widgets.btn("\uD83D\uDDD1 Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
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

        JPanel tableCard=Widgets.card("\uD83D\uDCCB  All Tour Packages");
        model=new DefaultTableModel(new String[]{"ID","Name","Destination","Description","Days","Price","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(model); Widgets.styleTable(table);
        Widgets.pillStatusColumn(table,6,Map.of(
            "Available",GREEN,"Cancelled",RED,
            "In-Progress",OCEAN,"Completed",GREEN));
        load();

        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); top.setOpaque(false);
        JTextField search=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        JButton refresh=Widgets.btn("\uD83D\uDD04 Refresh",OCEAN);
        top.add(refresh);
        refresh.addActionListener(e->{search.setText(""); load();});
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearch(search.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearch(search.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });
        JScrollPane sp=new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel bodyInner=new JPanel(new BorderLayout(0,8)); bodyInner.setOpaque(false);
        bodyInner.add(top,BorderLayout.NORTH); bodyInner.add(sp,BorderLayout.CENTER);
        tableCard.add(bodyInner,BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e->{
            int r=table.getSelectedRow();
            if(r>=0){
                tId.setText(model.getValueAt(r,0).toString());
                tName.setText(model.getValueAt(r,1).toString());
                tDest.setText(model.getValueAt(r,2).toString());
                tDesc.setText(model.getValueAt(r,3).toString());
                tDur.setText(model.getValueAt(r,4).toString());
                tPrice.setText(model.getValueAt(r,5).toString());
                tStatus.setSelectedItem(model.getValueAt(r,6).toString());
            }
        });

        add.addActionListener(e->{
            try{
                if(tc.addPackage(tName.getText(),tDest.getText(),tDesc.getText(),
                    Integer.parseInt(tDur.getText()),Float.parseFloat(tPrice.getText()),
                    (String)tStatus.getSelectedItem())){
                    msg("\u2705 Added"); load(); clear();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Enter valid values");}
        });
        upd.addActionListener(e->{
            try{
                if(tc.updatePackage(Integer.parseInt(tId.getText()),tName.getText(),tDest.getText(),tDesc.getText(),
                    Integer.parseInt(tDur.getText()),Float.parseFloat(tPrice.getText()),
                    (String)tStatus.getSelectedItem())){
                    msg("\u2705 Updated"); load();
                } else msg("\u274C Failed");
            }catch(Exception ex){msg("\u26A0 Invalid values");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(tId.getText());
                if(JOptionPane.showConfirmDialog(this,"Delete tour "+id+"?")==JOptionPane.YES_OPTION){
                    if(tc.deletePackage(id)){msg("\u2705 Deleted"); load(); clear();} else msg("\u274C Failed");
                }
            }catch(Exception ex){msg("\u26A0 Invalid ID");}
        });
        clr.addActionListener(e->clear());

        body.add(card,BorderLayout.WEST);
        body.add(tableCard,BorderLayout.CENTER);
        p.add(body,BorderLayout.CENTER);
        return p;
    }

    private void doSearch(String q){
        model.setRowCount(0);
        List<TourPackage> rows = q.isBlank() ? tc.getAllPackages() : tc.searchPackages(q);
        for(TourPackage t:rows) model.addRow(new Object[]{
            t.getTourId(),t.getName(),t.getDestination(),t.getDescription(),
            t.getDurationDays(),t.getPrice(),t.getStatus()});
    }
    private void load(){
        model.setRowCount(0);
        for(TourPackage t:tc.getAllPackages()) model.addRow(new Object[]{
            t.getTourId(),t.getName(),t.getDestination(),t.getDescription(),
            t.getDurationDays(),t.getPrice(),t.getStatus()});
    }
    private void clear(){tId.setText("");tName.setText("");tDest.setText("");tDesc.setText("");tDur.setText("");tPrice.setText("");tStatus.setSelectedIndex(0);}
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
