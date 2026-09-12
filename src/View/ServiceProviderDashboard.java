package View;

import controller.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service Provider Dashboard — top bar + left sidebar nav + a
 * card-swapped content area, with a Dashboard home screen made of four
 * stat cards and a Today's Summary card, and dedicated screens for Room
 * Availability, Vehicle Details, Confirm Reservation and Service Status.
 *
 * Data notes:
 *  - "Room Availability" manages the Hotel records this provider is
 *    responsible for (name/location/rooms/price/status) — there is no
 *    separate per-room table in the database, so each hotel row IS the
 *    "room availability" record, same as the rest of the app.
 *  - "Today's Summary" and the dashboard stat cards are computed live from
 *    hotels, vehicles and reservations already in the DB (no invented
 *    numbers).
 *  - "Service Status" logs are kept in memory for the running session
 *    (there's no service_logs table yet), seeded from the current
 *    hotel/vehicle condition so the screen isn't empty on first open.
 */
public class ServiceProviderDashboard extends JFrame {
    private final Color DARK=Theme.DARK, OCEAN=Theme.OCEAN,
      LIGHT=Theme.LIGHT, GREEN=Theme.GREEN, RED=Theme.RED, ORANGE=Theme.ORANGE, PURPLE=Theme.PURPLE;

    private final String username;
    private final HotelController hc = new HotelController();
    private final VehicleController vc = new VehicleController();
    private final ServiceProviderController spc = new ServiceProviderController();

    private final CardLayout cl = new CardLayout();
    private final JPanel contentPanel = new JPanel(cl);
    private final Map<String,NavButton> navButtons = new LinkedHashMap<>();

    // ---- Dashboard home ----
    private JPanel homeRoot, statsRow, summaryRows;
    private JLabel bellBadge;

    // ---- Room Availability (Hotels) ----
    private JTable hotelT; private DefaultTableModel hotelM;
    private JTextField hId,hName,hLoc,hRooms,hPrice, sRoomSearch;
    private JComboBox<String> hStatus, sRoomStatus;

    // ---- Vehicle Details ----
    private JTable vehT; private DefaultTableModel vehM;
    private JTextField vId,vType,vPlate,vSeats, sVehSearch;
    private JComboBox<String> vStatus, sVehStatus;

    // ---- Confirm Reservation ----
    private JTable resT; private DefaultTableModel resM;
    private JComboBox<String> rType;
    private JTextField rRefId,rCustomer;
    private JComboBox<String> sResType, sResStatus;

    // ---- Service Status ----
    private JTable statusT; private DefaultTableModel statusM;

    /** Builds and shows the Service Provider Dashboard window for the given logged-in user. */
    public ServiceProviderDashboard(String u){
        this.username = u;
        setTitle("Travel BP Sri Lanka \u2014 Service Provider Dashboard");
        setSize(1320,780);
        setMinimumSize(new Dimension(1100,660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        contentPanel.add(wrap(buildRooms()), "rooms");
        contentPanel.add(wrap(buildVehicles()), "vehicles");
        contentPanel.add(wrap(buildReservations()), "reservations");
        contentPanel.add(wrap(buildServiceStatus()), "status");
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
            case "home":         refreshHome();      break;
            case "rooms":        loadHotels();        break;
            case "vehicles":     loadVeh();           break;
            case "reservations": loadReservations();  break;
            case "status":       refreshStatus();     break;
            default: break;
        }
    }

    private String displayName(){
        return (username==null||username.isBlank()) ? "Service Provider" : username;
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
        JLabel t1 = new JLabel("Travel BP Sri Lanka");
        t1.setForeground(Color.WHITE);
        t1.setFont(new Font("Segoe UI",Font.BOLD,16));
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t2 = new JLabel("Tourism Management System");
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

    private void confirmLogout(){
        if (JOptionPane.showConfirmDialog(this,"Log out of the Service Provider Dashboard?","Confirm",
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

        JLabel avatar = new JLabel("\uD83C\uDFE8");
        avatar.setFont(new Font("Segoe UI Emoji",Font.PLAIN,30));
        avatar.setForeground(Color.WHITE);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel(displayName());
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Segoe UI",Font.BOLD,15));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel role = new JLabel("Service Provider");
        role.setForeground(new Color(160,190,198));
        role.setFont(new Font("Segoe UI",Font.PLAIN,11));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.add(avatar); profile.add(Box.createVerticalStrut(6));
        profile.add(name); profile.add(role);

        side.add(profile);
        side.add(divider());
        side.add(Box.createVerticalStrut(12));

        side.add(navItem("Dashboard","home"));
        side.add(navItem("Room Availability","rooms"));
        side.add(navItem("Vehicle Details","vehicles"));
        side.add(navItem("Confirm Reservation","reservations"));
        side.add(navItem("Service Status","status"));

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
    private JPanel buildHome(){
        homeRoot = new JPanel(new BorderLayout(0,16));
        homeRoot.setBackground(LIGHT);
        homeRoot.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));

        JPanel headRow = Widgets.pageHeading("Service Provider Dashboard",
            "Welcome back, "+displayName()+"! Here is your overview.", null);

        statsRow = new JPanel(new GridLayout(1,4,16,0));
        statsRow.setOpaque(false);

        JPanel summaryCard = Widgets.card("Today's Summary");
        summaryRows = new JPanel();
        summaryRows.setOpaque(false);
        summaryRows.setLayout(new BoxLayout(summaryRows, BoxLayout.Y_AXIS));
        summaryCard.add(summaryRows, BorderLayout.CENTER);

        homeRoot.add(headRow, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0,16));
        mid.setOpaque(false);
        mid.add(statsRow, BorderLayout.NORTH);
        mid.add(summaryCard, BorderLayout.CENTER);
        homeRoot.add(mid, BorderLayout.CENTER);

        refreshHome();
        return homeRoot;
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

    /** Recomputes every live number on the Dashboard home screen from the DB. */
    private void refreshHome(){
        if (homeRoot == null) return;

        List<Hotel> hotels = hc.getAll();
        List<Vehicle> vehicles = vc.getAll();
        List<Reservation> res = spc.getAllReservations();

        int availableRooms = hotels.stream()
            .filter(h -> "Available".equalsIgnoreCase(h.getStatus()))
            .mapToInt(Hotel::getRooms).sum();
        long availableVehicles = vehicles.stream()
            .filter(v -> "Available".equalsIgnoreCase(v.getStatus())).count();
        long pending = res.stream().filter(r -> "Pending".equals(r.getStatus())).count();
        long confirmed = res.stream().filter(r -> "Confirmed".equals(r.getStatus())).count();
        long cancelled = res.stream().filter(r -> "Cancelled".equals(r.getStatus())).count();

        boolean anyIssue = hotels.stream().anyMatch(h -> "Closed".equalsIgnoreCase(h.getStatus()))
            || vehicles.stream().anyMatch(v -> "Maintenance".equalsIgnoreCase(v.getStatus()));
        String overallStatus = anyIssue ? "Attention" : "Good";
        Color overallColor = anyIssue ? ORANGE : GREEN;

        String todayStr = LocalDate.now().toString();
        long newToday = res.stream().filter(r -> todayStr.equals(r.getReservationDate())).count();

        statsRow.removeAll();
        statsRow.add(Widgets.statCard("\uD83D\uDECF","Room Availability",String.valueOf(availableRooms),
            "Available Rooms","Manage Rooms \u2192",OCEAN, () -> switchTo("rooms")));
        statsRow.add(Widgets.statCard("\uD83D\uDE90","Vehicle Details",String.valueOf(availableVehicles),
            "Available Vehicles","Manage Vehicles \u2192",Theme.HEADER, () -> switchTo("vehicles")));
        statsRow.add(Widgets.statCard("\uD83D\uDCC5","Confirm Reservation",String.valueOf(pending),
            "Pending Requests","View Reservations \u2192",ORANGE, () -> switchTo("reservations")));
        statsRow.add(Widgets.statCard("\uD83D\uDCC8","Service Status",overallStatus,
            "Current Status","View Status \u2192",overallColor, () -> switchTo("status")));
        statsRow.revalidate();
        statsRow.repaint();

        summaryRows.removeAll();
        summaryRows.add(summaryRow("\uD83C\uDD95","New Reservations Today",String.valueOf(newToday),OCEAN));
        summaryRows.add(new JSeparator());
        summaryRows.add(summaryRow("\u2705","Confirmed Reservations",String.valueOf(confirmed),GREEN));
        summaryRows.add(new JSeparator());
        summaryRows.add(summaryRow("\u23F3","Pending Reservations",String.valueOf(pending),ORANGE));
        summaryRows.add(new JSeparator());
        summaryRows.add(summaryRow("\u274C","Cancelled Reservations",String.valueOf(cancelled),RED));
        summaryRows.revalidate();
        summaryRows.repaint();

        if (bellBadge != null) bellBadge.setText(String.valueOf(pending));
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

    // ------------------------------------------------------------------
    // Room Availability  (Hotel records)
    // ------------------------------------------------------------------
    private JPanel buildRooms(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Room Availability","Add and update the rooms/hotels you manage.",
            "Dashboard / Room Availability"), BorderLayout.NORTH);

        JPanel split=new JPanel(new BorderLayout(14,0));
        split.setOpaque(false);

        JPanel card=Widgets.card("Room / Hotel Details");
        card.setPreferredSize(new Dimension(330,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        hId=Widgets.field(); hName=Widgets.field(); hLoc=Widgets.field();
        hRooms=Widgets.field(); hPrice=Widgets.field();
        hStatus=new JComboBox<>(new String[]{"Available","Full","Closed"});

        Widgets.addField(f,0,"ID:",hId);
        Widgets.addField(f,1,"Name:",hName);
        Widgets.addField(f,2,"Location:",hLoc);
        Widgets.addField(f,3,"Rooms:",hRooms);
        Widgets.addField(f,4,"Price/Night (LKR):",hPrice);
        Widgets.addField(f,5,"Status:",hStatus);

        JButton add=Widgets.btn("+ Add Room",GREEN), upd=Widgets.btn("Update",OCEAN),
                del=Widgets.btn("Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false); btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=6; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=7; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("All Rooms");

        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        toolbar.setOpaque(false);
        sRoomSearch = new JTextField(14);
        sRoomStatus = new JComboBox<>(new String[]{"All Status","Available","Full","Closed"});
        toolbar.add(Widgets.labeledBlock("Search (name / location)", sRoomSearch));
        toolbar.add(Widgets.labeledBlock("Status", sRoomStatus));
        JButton search=Widgets.btn("Search",OCEAN), clearF=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel searchBtns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,18));
        searchBtns.setOpaque(false);
        searchBtns.add(search); searchBtns.add(clearF);
        toolbar.add(searchBtns);
        search.addActionListener(e->loadHotels());
        clearF.addActionListener(e->{ sRoomSearch.setText(""); sRoomStatus.setSelectedIndex(0); loadHotels(); });

        hotelM=new DefaultTableModel(new String[]{"ID","Name","Location","Rooms","Price/Night","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        hotelT=new JTable(hotelM); Widgets.styleTable(hotelT);
        Widgets.pillStatusColumn(hotelT,5,Map.of("Available",GREEN,"Full",ORANGE,"Closed",RED));
        loadHotels();
        JScrollPane sp=new JScrollPane(hotelT);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(toolbar,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        tableCard.add(body,BorderLayout.CENTER);

        hotelT.getSelectionModel().addListSelectionListener(e->{
            int r=hotelT.getSelectedRow();
            if(r>=0){
                hId.setText(hotelM.getValueAt(r,0).toString());
                hName.setText(hotelM.getValueAt(r,1).toString());
                hLoc.setText(hotelM.getValueAt(r,2).toString());
                hRooms.setText(hotelM.getValueAt(r,3).toString());
                hPrice.setText(hotelM.getValueAt(r,4).toString().replace("Rs. ",""));
                hStatus.setSelectedItem(hotelM.getValueAt(r,5).toString());
            }
        });

        add.addActionListener(e->{
            try{
                if(hc.addHotel(hName.getText(),hLoc.getText(),Integer.parseInt(hRooms.getText()),
                    Float.parseFloat(hPrice.getText()),(String)hStatus.getSelectedItem())){
                    msg("Room added."); loadHotels(); clearH(); refreshHome();
                } else msg("Could not add room.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        upd.addActionListener(e->{
            try{
                if(hc.updateHotel(Integer.parseInt(hId.getText()),hName.getText(),hLoc.getText(),
                    Integer.parseInt(hRooms.getText()),Float.parseFloat(hPrice.getText()),
                    (String)hStatus.getSelectedItem())){msg("Room updated."); loadHotels(); refreshHome();}
                else msg("Could not update room.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        del.addActionListener(e->{
            try{
                int id=Integer.parseInt(hId.getText());
                if(hc.deleteHotel(id)){msg("Room deleted."); loadHotels(); clearH(); refreshHome();}
            }catch(Exception ex){msg("Please select a valid ID.");}
        });
        clr.addActionListener(e->clearH());

        split.add(card,BorderLayout.WEST);
        split.add(tableCard,BorderLayout.CENTER);
        p.add(split,BorderLayout.CENTER);
        return p;
    }

    private void loadHotels(){
        hotelM.setRowCount(0);
        String q = sRoomSearch==null? "" : sRoomSearch.getText().trim().toLowerCase();
        String statusFilter = sRoomStatus==null? "All Status" : (String) sRoomStatus.getSelectedItem();
        for(Hotel h:hc.getAll()){
            boolean matchesQ = q.isEmpty() || h.getName().toLowerCase().contains(q) || h.getLocation().toLowerCase().contains(q);
            boolean matchesStatus = "All Status".equals(statusFilter) || statusFilter.equalsIgnoreCase(h.getStatus());
            if(matchesQ && matchesStatus){
                hotelM.addRow(new Object[]{
                    h.getHotelId(),h.getName(),h.getLocation(),h.getRooms(),"Rs. "+h.getPricePerNight(),h.getStatus()});
            }
        }
    }
    private void clearH(){hId.setText("");hName.setText("");hLoc.setText("");hRooms.setText("");hPrice.setText("");hStatus.setSelectedIndex(0);}

    // ------------------------------------------------------------------
    // Vehicle Details
    // ------------------------------------------------------------------
    private JPanel buildVehicles(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Vehicle Details","Add and update the vehicles you manage.",
            "Dashboard / Vehicle Details"), BorderLayout.NORTH);

        JPanel split=new JPanel(new BorderLayout(14,0));
        split.setOpaque(false);

        JPanel card=Widgets.card("Vehicle Details");
        card.setPreferredSize(new Dimension(330,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        vId=Widgets.field(); vType=Widgets.field(); vPlate=Widgets.field(); vSeats=Widgets.field();
        vStatus=new JComboBox<>(new String[]{"Available","Assigned","Maintenance"});

        Widgets.addField(f,0,"ID:",vId);
        Widgets.addField(f,1,"Type:",vType);
        Widgets.addField(f,2,"Plate No:",vPlate);
        Widgets.addField(f,3,"Seats:",vSeats);
        Widgets.addField(f,4,"Status:",vStatus);

        JButton add=Widgets.btn("+ Add Vehicle",GREEN), upd=Widgets.btn("Update",OCEAN),
                del=Widgets.btn("Delete",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(2,1,0,8));
        btnBox.setOpaque(false); btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(del,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=5; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);
        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=6; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("All Vehicles");

        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        toolbar.setOpaque(false);
        sVehSearch = new JTextField(14);
        sVehStatus = new JComboBox<>(new String[]{"All Status","Available","Assigned","Maintenance"});
        toolbar.add(Widgets.labeledBlock("Search (type / plate)", sVehSearch));
        toolbar.add(Widgets.labeledBlock("Status", sVehStatus));
        JButton search=Widgets.btn("Search",OCEAN), clearF=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel searchBtns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,18));
        searchBtns.setOpaque(false);
        searchBtns.add(search); searchBtns.add(clearF);
        toolbar.add(searchBtns);
        search.addActionListener(e->loadVeh());
        clearF.addActionListener(e->{ sVehSearch.setText(""); sVehStatus.setSelectedIndex(0); loadVeh(); });

        vehM=new DefaultTableModel(new String[]{"ID","Type","Plate","Seats","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        vehT=new JTable(vehM); Widgets.styleTable(vehT);
        Widgets.pillStatusColumn(vehT,4,Map.of("Available",GREEN,"Assigned",OCEAN,"Maintenance",RED));
        loadVeh();
        JScrollPane sp=new JScrollPane(vehT);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(toolbar,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        tableCard.add(body,BorderLayout.CENTER);

        vehT.getSelectionModel().addListSelectionListener(e->{
            int r=vehT.getSelectedRow();
            if(r>=0){
                vId.setText(vehM.getValueAt(r,0).toString());
                vType.setText(vehM.getValueAt(r,1).toString());
                vPlate.setText(vehM.getValueAt(r,2).toString());
                vSeats.setText(vehM.getValueAt(r,3).toString());
                vStatus.setSelectedItem(vehM.getValueAt(r,4).toString());
            }
        });

        add.addActionListener(e->{
            try{
                if(vc.addVehicle(vType.getText(),vPlate.getText(),Integer.parseInt(vSeats.getText()),
                    (String)vStatus.getSelectedItem())){msg("Vehicle added."); loadVeh(); clearV(); refreshHome();}
                else msg("Could not add vehicle.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        upd.addActionListener(e->{
            try{
                if(vc.updateVehicle(Integer.parseInt(vId.getText()),vType.getText(),vPlate.getText(),
                    Integer.parseInt(vSeats.getText()),(String)vStatus.getSelectedItem())){
                    msg("Vehicle updated."); loadVeh(); refreshHome();} else msg("Could not update vehicle.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        del.addActionListener(e->{
            try{
                if(vc.deleteVehicle(Integer.parseInt(vId.getText()))){msg("Vehicle deleted."); loadVeh(); clearV(); refreshHome();}
            }catch(Exception ex){msg("Please select a valid ID.");}
        });
        clr.addActionListener(e->clearV());

        split.add(card,BorderLayout.WEST);
        split.add(tableCard,BorderLayout.CENTER);
        p.add(split,BorderLayout.CENTER);
        return p;
    }

    private void loadVeh(){
        vehM.setRowCount(0);
        String q = sVehSearch==null? "" : sVehSearch.getText().trim().toLowerCase();
        String statusFilter = sVehStatus==null? "All Status" : (String) sVehStatus.getSelectedItem();
        for(Vehicle v:vc.getAll()){
            boolean matchesQ = q.isEmpty() || v.getType().toLowerCase().contains(q) || v.getPlateNo().toLowerCase().contains(q);
            boolean matchesStatus = "All Status".equals(statusFilter) || statusFilter.equalsIgnoreCase(v.getStatus());
            if(matchesQ && matchesStatus){
                vehM.addRow(new Object[]{
                    v.getVehicleId(),v.getType(),v.getPlateNo(),v.getSeats(),v.getStatus()});
            }
        }
    }
    private void clearV(){vId.setText("");vType.setText("");vPlate.setText("");vSeats.setText("");vStatus.setSelectedIndex(0);}

    // ------------------------------------------------------------------
    // Confirm Reservation
    // ------------------------------------------------------------------
    private JPanel buildReservations(){
        JPanel p=new JPanel(new BorderLayout(0,14));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Confirm Reservation","Review, confirm or reject incoming reservations.",
            "Dashboard / Confirm Reservation"), BorderLayout.NORTH);

        JPanel split=new JPanel(new BorderLayout(14,0));
        split.setOpaque(false);

        JPanel card=Widgets.card("New Reservation");
        card.setPreferredSize(new Dimension(330,0));
        JPanel f=new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        card.add(f,BorderLayout.CENTER);

        rType=new JComboBox<>(new String[]{"Hotel","Vehicle"});
        rRefId=Widgets.field(); rCustomer=Widgets.field();
        Widgets.addField(f,0,"Type:",rType);
        Widgets.addField(f,1,"Hotel/Vehicle ID:",rRefId);
        Widgets.addField(f,2,"Customer Name:",rCustomer);

        JButton add=Widgets.btn("+ Add",GREEN), upd=Widgets.btn("\u270F\uFE0F Update",OCEAN),
                confirm=Widgets.btn("Confirm",GREEN), pending=Widgets.btn("Pending",ORANGE),
                reject=Widgets.btn("Reject",RED), clr=Widgets.ghostBtn("Clear",Theme.SUBTEXT);
        JPanel btnBox=new JPanel(new GridLayout(3,1,0,8));
        btnBox.setOpaque(false); btnBox.setBorder(BorderFactory.createEmptyBorder(14,0,0,0));
        btnBox.add(Widgets.buttonRow(add,upd));
        btnBox.add(Widgets.buttonRow(confirm,reject));
        btnBox.add(Widgets.buttonRow(pending,clr));
        GridBagConstraints bc2=new GridBagConstraints();
        bc2.gridx=0; bc2.gridy=3; bc2.gridwidth=2; bc2.fill=GridBagConstraints.HORIZONTAL;
        bc2.insets=new Insets(6,10,0,10);
        f.add(btnBox,bc2);

        JLabel hint=new JLabel("<html><i>Select a row below to load its details,<br>then Update, Confirm, Reject or set Pending.</i></html>");
        hint.setFont(new Font("Segoe UI",Font.PLAIN,11));
        hint.setForeground(Theme.SUBTEXT);
        GridBagConstraints hc2=new GridBagConstraints();
        hc2.gridx=0; hc2.gridy=4; hc2.gridwidth=2; hc2.fill=GridBagConstraints.HORIZONTAL;
        hc2.insets=new Insets(12,10,0,10);
        f.add(hint,hc2);

        GridBagConstraints filler=new GridBagConstraints();
        filler.gridx=0; filler.gridy=5; filler.weighty=1; filler.fill=GridBagConstraints.VERTICAL;
        f.add(Box.createGlue(),filler);

        JPanel tableCard=Widgets.card("All Reservations");

        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        toolbar.setOpaque(false);
        sResType = new JComboBox<>(new String[]{"All Types","Hotel","Vehicle"});
        sResStatus = new JComboBox<>(new String[]{"All Status","Pending","Confirmed","Cancelled"});
        toolbar.add(Widgets.labeledBlock("Reservation Type", sResType));
        toolbar.add(Widgets.labeledBlock("Status", sResStatus));
        JButton search=Widgets.btn("Search",OCEAN), clearF=Widgets.ghostBtn("Clear",Theme.SUBTEXT),
                refresh=Widgets.ghostBtn("Refresh",OCEAN);
        JPanel searchBtns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,18));
        searchBtns.setOpaque(false);
        searchBtns.add(search); searchBtns.add(clearF); searchBtns.add(refresh);
        toolbar.add(searchBtns);
        search.addActionListener(e->loadReservations());
        clearF.addActionListener(e->{ sResType.setSelectedIndex(0); sResStatus.setSelectedIndex(0); loadReservations(); });
        refresh.addActionListener(e->loadReservations());

        resM=new DefaultTableModel(new String[]{"Res. ID","Type","Ref ID","Customer","Date","Status"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        resT=new JTable(resM); Widgets.styleTable(resT);
        Widgets.pillStatusColumn(resT,5,Map.of("Pending",ORANGE,"Confirmed",GREEN,"Cancelled",RED));
        loadReservations();
        JScrollPane sp=new JScrollPane(resT);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,8)); body.setOpaque(false);
        body.add(toolbar,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        tableCard.add(body,BorderLayout.CENTER);

        resT.getSelectionModel().addListSelectionListener(e->{
            int r=resT.getSelectedRow();
            if(r>=0){
                rType.setSelectedItem(String.valueOf(resM.getValueAt(r,1)));
                rRefId.setText(String.valueOf(resM.getValueAt(r,2)));
                rCustomer.setText(String.valueOf(resM.getValueAt(r,3)));
            }
        });

        add.addActionListener(e->{
            try{
                if(spc.addReservation((String)rType.getSelectedItem(),Integer.parseInt(rRefId.getText().trim()),
                    rCustomer.getText().trim(),"Pending")){
                    msg("Reservation added.");
                    loadReservations(); clearRes(); refreshHome();
                    if(resT.getRowCount()>0) resT.setRowSelectionInterval(0,0);
                } else msg("Could not add reservation.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        upd.addActionListener(e->{
            int r=resT.getSelectedRow();
            if(r<0){msg("Select a reservation first."); return;}
            try{
                int id=(int)resM.getValueAt(r,0);
                if(spc.updateReservationDetails(id,(String)rType.getSelectedItem(),
                        Integer.parseInt(rRefId.getText().trim()),rCustomer.getText().trim())){
                    msg("Reservation updated."); loadReservations(); refreshHome();
                } else msg("Could not update that reservation.");
            }catch(Exception ex){msg("Please enter valid values.");}
        });
        confirm.addActionListener(e->{
            int r=resT.getSelectedRow();
            if(r<0){msg("Select a reservation first."); return;}
            int id=(int)resM.getValueAt(r,0);
            if(spc.confirmReservation(id)){msg("Reservation confirmed."); loadReservations(); refreshHome();}
            else msg("Could not confirm that reservation.");
        });
        pending.addActionListener(e->{
            int r=resT.getSelectedRow();
            if(r<0){msg("Select a reservation first."); return;}
            int id=(int)resM.getValueAt(r,0);
            if(spc.updateReservationStatus(id,"Pending")){msg("Reservation set to pending."); loadReservations(); refreshHome();}
            else msg("Could not update that reservation.");
        });
        reject.addActionListener(e->{
            int r=resT.getSelectedRow();
            if(r<0){msg("Select a reservation first."); return;}
            int id=(int)resM.getValueAt(r,0);
            if(spc.updateReservationStatus(id,"Cancelled")){msg("Reservation rejected."); loadReservations(); refreshHome();}
            else msg("Could not reject that reservation.");
        });
        clr.addActionListener(e->clearRes());

        split.add(card,BorderLayout.WEST);
        split.add(tableCard,BorderLayout.CENTER);
        p.add(split,BorderLayout.CENTER);
        return p;
    }

    private void loadReservations(){
        resM.setRowCount(0);
        String typeFilter = sResType==null? "All Types" : (String) sResType.getSelectedItem();
        String statusFilter = sResStatus==null? "All Status" : (String) sResStatus.getSelectedItem();
        for(Reservation r: spc.getAllReservations()){
            boolean matchesType = "All Types".equals(typeFilter) || typeFilter.equalsIgnoreCase(r.getServiceType());
            boolean matchesStatus = "All Status".equals(statusFilter) || statusFilter.equalsIgnoreCase(r.getStatus());
            if(matchesType && matchesStatus){
                resM.addRow(new Object[]{
                    r.getReservationId(), r.getServiceType(), r.getRefId(), r.getCustomerName(),
                    r.getReservationDate(), r.getStatus()});
            }
        }
    }
    private void clearRes(){ rRefId.setText(""); rCustomer.setText(""); rType.setSelectedIndex(0); }

    // ------------------------------------------------------------------
    // Service Status
    // ------------------------------------------------------------------
    private JPanel buildServiceStatus(){
        JPanel p=new JPanel(new BorderLayout(0,16));
        p.setBackground(LIGHT);
        p.setBorder(BorderFactory.createEmptyBorder(22,26,22,26));
        p.add(Widgets.pageHeading("Service Status","Overview of room, transport and cleanliness status.",
            "Dashboard / Service Status"), BorderLayout.NORTH);

        JPanel mid=new JPanel(new BorderLayout(0,16));
        mid.setOpaque(false);

        JPanel logsCard=Widgets.card("Service Logs");
        JButton addUpdate=Widgets.btn("+ Add Status Update",PURPLE);
        addUpdate.addActionListener(e->openAddStatusDialog());
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        top.setOpaque(false);
        JTextField logSearch=Widgets.searchBox(top,"\uD83D\uDD0E Search:");
        top.add(addUpdate);
        logSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){doSearchServiceLogs(logSearch.getText());}
            public void removeUpdate(javax.swing.event.DocumentEvent e){doSearchServiceLogs(logSearch.getText());}
            public void changedUpdate(javax.swing.event.DocumentEvent e){}
        });

        statusM=new DefaultTableModel(new String[]{"Date","Service","Status","Remarks","Updated By"},0){
            public boolean isCellEditable(int r,int c){return false;}};
        statusT=new JTable(statusM); Widgets.styleTable(statusT);
        Widgets.pillStatusColumn(statusT,2,Map.of("Good",GREEN,"Attention",ORANGE,"Maintenance",RED));
        JScrollPane sp=new JScrollPane(statusT);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER,1));
        JPanel body=new JPanel(new BorderLayout(0,10)); body.setOpaque(false);
        body.add(top,BorderLayout.NORTH); body.add(sp,BorderLayout.CENTER);
        logsCard.add(body,BorderLayout.CENTER);

        mid.add(logsCard, BorderLayout.CENTER);
        p.add(mid, BorderLayout.CENTER);

        refreshStatus();
        return p;
    }

    /** Recomputes live room/transport status from hotel/vehicle data (used to
     *  auto-seed the first log entries) and reloads the service log table
     *  from the database. */
    private void refreshStatus(){
        if (statusM == null) return;
        List<Hotel> hotels = hc.getAll();
        List<Vehicle> vehicles = vc.getAll();

        boolean roomIssue = hotels.stream().anyMatch(h -> "Closed".equalsIgnoreCase(h.getStatus()));
        boolean transportIssue = vehicles.stream().anyMatch(v -> "Maintenance".equalsIgnoreCase(v.getStatus()));
        String roomStatus = roomIssue ? "Attention" : "Good";
        String transportStatus = transportIssue ? "Maintenance" : "Good";

        List<Object[]> logs = spc.getServiceLogs();
        if (logs.isEmpty()) {
            spc.addServiceLog("Room Service", roomStatus,
                roomIssue ? "One or more rooms marked Closed." : "All rooms in good condition.", displayName());
            spc.addServiceLog("Transport Service", transportStatus,
                transportIssue ? "One or more vehicles under maintenance." : "All vehicles available.", displayName());
            logs = spc.getServiceLogs();
        }

        statusM.setRowCount(0);
        for (Object[] row : logs) statusM.addRow(row);
    }
    private void doSearchServiceLogs(String q){
        if (statusM == null) return;
        statusM.setRowCount(0);
        List<Object[]> rows = q.isBlank() ? spc.getServiceLogs() : spc.searchServiceLogs(q);
        for (Object[] row : rows) statusM.addRow(row);
    }

    private void openAddStatusDialog(){
        JComboBox<String> service = new JComboBox<>(new String[]{"Room Service","Transport Service","Cleanliness"});
        JComboBox<String> status = new JComboBox<>(new String[]{"Good","Attention","Maintenance"});
        JTextField remarks = Widgets.field();
        Widgets.styleCombo(service); Widgets.styleCombo(status);

        JPanel form=new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        Widgets.addField(form,0,"Service:",service);
        Widgets.addField(form,1,"Status:",status);
        Widgets.addField(form,2,"Remarks:",remarks);
        form.setPreferredSize(new Dimension(340,140));

        int res = JOptionPane.showConfirmDialog(this, form, "Add Status Update",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            spc.addServiceLog((String) service.getSelectedItem(), (String) status.getSelectedItem(),
                remarks.getText().trim().isEmpty() ? "-" : remarks.getText().trim(), displayName());
            refreshStatus();
        }
    }
}
