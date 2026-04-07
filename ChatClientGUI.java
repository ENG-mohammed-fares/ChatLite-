package client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClientGUI {

    static final Color C_DARK         = new Color(0x1A, 0x1A, 0x2E);
    static final Color C_BG_PRIMARY   = new Color(0xFF, 0xFF, 0xFF);
    static final Color C_BG_SECONDARY = new Color(0xF5, 0xF5, 0xF0);
    static final Color C_ACCENT       = new Color(0x53, 0x4A, 0xB7);
    static final Color C_ACCENT_DARK  = new Color(0x3C, 0x34, 0x89);
    static final Color C_GREEN        = new Color(0x1D, 0x9E, 0x75);
    static final Color C_RED          = new Color(0xD8, 0x5A, 0x30);
    static final Color C_AMBER        = new Color(0xEF, 0x9F, 0x27);
    static final Color C_TEXT_MAIN    = new Color(0x1A, 0x1A, 0x1A);
    static final Color C_TEXT_MUTED   = new Color(0x88, 0x87, 0x80);
    static final Color C_BORDER       = new Color(0x00, 0x00, 0x00, 30);

    static final Font F_MONO     = new Font("Courier New", Font.PLAIN, 12);
    static final Font F_MONO_SM  = new Font("Courier New", Font.PLAIN, 10);
    static final Font F_MONO_B   = new Font("Courier New", Font.BOLD,  12);

    Socket         socket;
    BufferedReader in;
    PrintWriter    out;
    String         connectedUsername = "guest";
    String         connectedPassword = "";
    String         connectedHost     = "localhost";
    int            connectedPort     = 5000;
    String         currentRoom       = null;

    DefaultTableModel        tableModel;
    JTable                   table;
    JTextField               inputField;
    JTextField               privateField;
    JTextField               searchField;
    JComboBox<String>        userCombo;
    DefaultListModel<String> roomsModel;
    DefaultListModel<String> usersModel;
    JLabel                   statusDot;
    JLabel                   statusLabel;
    JLabel                   logLabel;
    JLabel                   uptimeLabel;
    JLabel                   connLabel;
    JLabel                   userBadgeLabel;
    JButton                  sendBtn;
    JList<String>            usersList;
    ButtonGroup              statusGroup;
    String                   myStatus      = "Active";
    long                     connectTime   = 0;
    boolean                  showRooms     = false;
    boolean                  showUsers     = false;
    String                   selectedUser  = null;
    java.util.Map<String,String> userStatuses = new java.util.concurrent.ConcurrentHashMap<>();
    javax.swing.Timer            roomsEndTimer = null;

    java.util.Map<String, java.util.List<Object[]>> ephemeralMsgs = new java.util.HashMap<>();

    private int lastExportedRowCount = 0;


    private javax.swing.Timer uptimeTimer = null;

    public ChatClientGUI() {
        JFrame frame = new JFrame("ChatLite Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1060, 660);
        frame.setLayout(new BorderLayout());

        frame.add(makeTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(C_BG_PRIMARY);
        body.add(makeLeftPanel(),   BorderLayout.WEST);
        body.add(makeCenterPanel(), BorderLayout.CENTER);
        body.add(makeRightPanel(),  BorderLayout.EAST);
        frame.add(body, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(makeSearchBar(), BorderLayout.CENTER);
        south.add(makeStatusBar(), BorderLayout.SOUTH);
        frame.add(south, BorderLayout.SOUTH);

        frame.setVisible(true);
        connect();
    }


    JPanel makeTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 44));
        bar.setBackground(C_BG_SECONDARY);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(C_BG_SECONDARY);
        left.add(trafficDot(new Color(0xFF, 0x5F, 0x57)));
        left.add(trafficDot(new Color(0xFE, 0xBC, 0x2E)));
        left.add(trafficDot(new Color(0x28, 0xC8, 0x40)));
        JLabel title = new JLabel("ChatLite Client");
        title.setFont(F_MONO_B);
        title.setForeground(C_TEXT_MAIN);
        title.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        center.setBackground(C_BG_SECONDARY);

        connLabel = new JLabel("CONNECTED TO: —");
        connLabel.setFont(F_MONO_B);
        connLabel.setForeground(C_TEXT_MAIN);
        center.add(connLabel);
        center.add(makeSep());

        JLabel uptimeTxt = new JLabel("Uptime:");
        uptimeTxt.setFont(F_MONO_SM);
        uptimeTxt.setForeground(C_TEXT_MUTED);
        center.add(uptimeTxt);

        uptimeLabel = new JLabel("00:00:00");
        uptimeLabel.setFont(F_MONO_B);
        uptimeLabel.setForeground(C_ACCENT);
        center.add(uptimeLabel);

        center.add(makeSep());

        JButton exportBtn = new JButton("⬇ EXPORT");
        exportBtn.setFont(F_MONO_SM);
        exportBtn.setBackground(C_BG_PRIMARY);
        exportBtn.setForeground(C_ACCENT);
        exportBtn.setFocusPainted(false);
        exportBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_ACCENT, 1),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.setToolTipText("Export current chat log to .txt file");
        exportBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                exportBtn.setBackground(C_ACCENT);
                exportBtn.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                exportBtn.setBackground(C_BG_PRIMARY);
                exportBtn.setForeground(C_ACCENT);
            }
        });
        exportBtn.addActionListener(e -> exportChat("manual export"));
        center.add(exportBtn);

        bar.add(center, BorderLayout.CENTER);

        userBadgeLabel = new JLabel("  ● —  ");
        userBadgeLabel.setFont(F_MONO_B);
        userBadgeLabel.setForeground(C_TEXT_MUTED);
        userBadgeLabel.setOpaque(true);
        userBadgeLabel.setBackground(C_BG_SECONDARY);
        userBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));
        bar.add(userBadgeLabel, BorderLayout.EAST);

        return bar;
    }

    JLabel makeSep() {
        JLabel sep = new JLabel("|");
        sep.setFont(F_MONO_SM);
        sep.setForeground(C_TEXT_MUTED);
        return sep;
    }

    JLabel trafficDot(Color c) {
        JLabel d = new JLabel("●");
        d.setFont(new Font("Courier New", Font.PLAIN, 10));
        d.setForeground(c);
        return d;
    }

    JPanel makeLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(190, 0));
        panel.setBackground(C_BG_SECONDARY);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(C_BG_SECONDARY);

        inner.add(makeSectionHeader("CHAT ROOMS"));
        roomsModel = new DefaultListModel<>();
        JList<String> roomsList = new JList<>(roomsModel);
        roomsList.setFont(F_MONO);
        roomsList.setBackground(C_BG_SECONDARY);
        roomsList.setForeground(C_TEXT_MUTED);
        roomsList.setSelectionBackground(C_BG_PRIMARY);
        roomsList.setSelectionForeground(C_ACCENT);
        roomsList.setFixedCellHeight(30);
        roomsList.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        roomsList.setCellRenderer(new RoomCellRenderer());
        roomsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = roomsList.getSelectedValue();
                if (sel != null) sendRaw("JOIN " + sel);
            }
        });
        JScrollPane roomsScroll = new JScrollPane(roomsList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        roomsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        roomsScroll.setPreferredSize(new Dimension(190, 110));
        styleScrollPane(roomsScroll);
        inner.add(roomsScroll);

        inner.add(makeSectionHeader("ONLINE USERS"));
        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        usersList.setFont(F_MONO);
        usersList.setBackground(C_BG_SECONDARY);
        usersList.setForeground(C_TEXT_MUTED);
        usersList.setFixedCellHeight(30);
        usersList.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        usersList.setCellRenderer(new UserCellRenderer());
        JScrollPane usersScroll = new JScrollPane(usersList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        usersScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        usersScroll.setPreferredSize(new Dimension(190, 110));
        styleScrollPane(usersScroll);
        inner.add(usersScroll);

        JButton refreshBtn = new JButton("Refresh Users");
        refreshBtn.setFont(F_MONO_SM);
        refreshBtn.setBackground(C_BG_PRIMARY);
        refreshBtn.setForeground(C_TEXT_MUTED);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> sendRaw("USERS"));
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        refreshPanel.setBackground(C_BG_SECONDARY);
        refreshPanel.add(refreshBtn);
        inner.add(refreshPanel);

        inner.add(makeSectionHeader("USER STATUS"));
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBackground(C_BG_SECONDARY);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(6, 14, 8, 14));

        statusGroup = new ButtonGroup();
        String[] statuses  = {"Active", "Busy", "Away"};
        Color[]  dotColors = {C_GREEN,  C_AMBER, C_RED};

        for (int i = 0; i < statuses.length; i++) {
            final String s = statuses[i];
            final Color  c = dotColors[i];
            JRadioButton rb = new JRadioButton(s);
            rb.setFont(F_MONO);
            rb.setBackground(C_BG_SECONDARY);
            rb.setForeground(C_TEXT_MAIN);
            rb.setFocusPainted(false);
            rb.setSelected(s.equals("Active"));
            rb.setIcon(radioIcon(c, false));
            rb.setSelectedIcon(radioIcon(c, true));
            rb.addActionListener(e -> {
                myStatus = s;
                sendRaw("STATUS " + s);
                setLog("[ " + ts() + " ] Status → " + s);
            });
            statusGroup.add(rb);
            statusPanel.add(rb);
            statusPanel.add(Box.createVerticalStrut(2));
        }
        inner.add(statusPanel);

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    Icon radioIcon(Color c, boolean selected) {
        return new Icon() {
            public int getIconWidth()  { return 14; }
            public int getIconHeight() { return 14; }
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? c : C_BG_PRIMARY);
                g2.fillOval(x + 1, y + 1, 11, 11);
                g2.setColor(selected ? c.darker() : C_TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, 11, 11);
            }
        };
    }

    JPanel makeCenterPanel() {
        tableModel = new DefaultTableModel(new String[]{"User", "Message", "Time"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(F_MONO);
        table.setRowHeight(24);
        table.setBackground(C_BG_PRIMARY);
        table.setForeground(C_TEXT_MAIN);
        table.setGridColor(new Color(0, 0, 0, 15));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(F_MONO_SM);
        table.getTableHeader().setBackground(C_BG_SECONDARY);
        table.getTableHeader().setForeground(C_TEXT_MUTED);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        table.setSelectionBackground(new Color(0xEE, 0xED, 0xFE));
        table.setSelectionForeground(C_ACCENT_DARK);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(0).setMaxWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setMaxWidth(80);
        table.setDefaultRenderer(Object.class, new MsgTableRenderer());

        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);

        inputField = makePlaceholderField("Type message here", F_MONO);
        inputField.addActionListener(e -> sendMessage());

        sendBtn = makeAccentButton("SEND");
        sendBtn.addActionListener(e -> sendMessage());

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setPreferredSize(new Dimension(0, 38));
        inputRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendBtn,    BorderLayout.EAST);

        JPanel inputSection = new JPanel(new BorderLayout());
        inputSection.add(makeSectionHeader("MESSAGE INPUT"), BorderLayout.NORTH);
        inputSection.add(inputRow, BorderLayout.CENTER);
        inputSection.setPreferredSize(new Dimension(0, 68));

        JPanel center = new JPanel(new BorderLayout());
        center.add(scroll,        BorderLayout.CENTER);
        center.add(inputSection,  BorderLayout.SOUTH);
        return center;
    }

    JPanel makeRightPanel() {
        userCombo = new JComboBox<>();
        userCombo.setFont(F_MONO_SM);
        userCombo.setBackground(C_BG_PRIMARY);
        userCombo.setForeground(C_TEXT_MAIN);

        privateField = makePlaceholderField("Message…", F_MONO_SM);
        privateField.setPreferredSize(new Dimension(0, 70));
        privateField.addActionListener(e -> sendPrivate());

        JButton pmSendBtn  = makeAccentButton("SEND");
        JButton pmClearBtn = makeDarkButton("CLEAR");
        pmSendBtn .addActionListener(e -> sendPrivate());
        pmClearBtn.addActionListener(e -> privateField.setText(""));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
        btnRow.setBackground(C_BG_SECONDARY);
        btnRow.add(pmSendBtn);
        btnRow.add(pmClearBtn);

        JPanel pm = new JPanel();
        pm.setLayout(new BoxLayout(pm, BoxLayout.Y_AXIS));
        pm.setBackground(C_BG_SECONDARY);
        pm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        userCombo   .setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        privateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        btnRow      .setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        pm.add(makeSectionLabel("To:"));
        pm.add(Box.createVerticalStrut(4));
        pm.add(userCombo);
        pm.add(Box.createVerticalStrut(8));
        pm.add(makeSectionLabel("Message:"));
        pm.add(Box.createVerticalStrut(4));
        pm.add(privateField);
        pm.add(Box.createVerticalStrut(8));
        pm.add(btnRow);

        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(210, 0));
        right.setBackground(C_BG_SECONDARY);
        right.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, C_BORDER));
        right.add(makeSectionHeader("PRIVATE MSG"), BorderLayout.NORTH);
        right.add(pm, BorderLayout.CENTER);
        return right;
    }

    JPanel makeSearchBar() {
        searchField = makePlaceholderField("SEARCH:  Find by user/message…", F_MONO_SM);
        searchField.setBackground(C_BG_SECONDARY);
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filterTable(); }
            public void removeUpdate(DocumentEvent e)  { filterTable(); }
            public void changedUpdate(DocumentEvent e) { filterTable(); }
        });

        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBackground(C_BG_SECONDARY);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        bar.add(searchField, BorderLayout.CENTER);
        return bar;
    }

    JPanel makeStatusBar() {
        statusDot = new JLabel("●");
        statusDot.setFont(new Font("Courier New", Font.PLAIN, 9));
        statusDot.setForeground(C_RED);

        statusLabel = new JLabel("Disconnected");
        statusLabel.setFont(F_MONO_SM);
        statusLabel.setForeground(new Color(0x88, 0x88, 0xA8));

        logLabel = new JLabel("");
        logLabel.setFont(F_MONO_SM);
        logLabel.setForeground(new Color(0x5F, 0x5E, 0x5A));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 30));
        bar.setBackground(C_DARK);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xFF, 0xFF, 0xFF, 15)),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        JPanel leftP = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftP.setBackground(C_DARK);
        leftP.add(statusDot);
        leftP.add(statusLabel);

        bar.add(leftP,    BorderLayout.WEST);
        bar.add(logLabel, BorderLayout.CENTER);
        return bar;
    }

    JPanel makeSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_MONO_B);
        lbl.setForeground(C_TEXT_MAIN);
        lbl.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG_SECONDARY);
        p.add(lbl, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        return p;
    }

    JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_MONO_SM);
        lbl.setForeground(C_TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    JButton makeAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_B);
        btn.setBackground(C_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(80, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_ACCENT_DARK); }
            public void mouseExited (MouseEvent e) { btn.setBackground(C_ACCENT); }
        });
        return btn;
    }

    JButton makeDarkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_B);
        btn.setBackground(C_DARK);
        btn.setForeground(new Color(0xC8, 0xC8, 0xE8));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(80, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_ACCENT); }
            public void mouseExited (MouseEvent e) { btn.setBackground(C_DARK); }
        });
        return btn;
    }

    void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setBackground(C_BG_SECONDARY);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
    }

    JTextField makePlaceholderField(String hint, Font f) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    Insets ins = getInsets();
                    int y = ins.top + (getHeight() - ins.top - ins.bottom + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(hint, ins.left + 4, y);
                }
            }
        };
        field.setFont(f);
        field.setBackground(C_BG_PRIMARY);
        field.setForeground(C_TEXT_MAIN);
        field.setCaretColor(C_ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return field;
    }


    void exportChat(String reason) {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            setLog("[ " + ts() + " ] Nothing to export.");
            return;
        }
        if (reason.startsWith("auto") && rowCount == lastExportedRowCount) return;

        String room      = (currentRoom != null && !currentRoom.isEmpty()) ? currentRoom : "session";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename  = "chat_log_" + room + "_" + timestamp + ".txt";
        File file = new File(System.getProperty("user.dir"), filename);

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            pw.println(room);
            pw.println();
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
            for (int i = 0; i < rowCount; i++) {
                String user    = String.valueOf(tableModel.getValueAt(i, 0));
                String msg     = String.valueOf(tableModel.getValueAt(i, 1));
                String timeRaw = String.valueOf(tableModel.getValueAt(i, 2));
                String dateStr = dateFmt.format(new Date());
                pw.println(user + ": " + msg + " <" + timeRaw + " " + dateStr + ">");
            }
        } catch (IOException ex) {
            setLog("[ " + ts() + " ] Export failed: " + ex.getMessage());
            return;
        }

        lastExportedRowCount = rowCount;
        setLog("[ " + ts() + " ] Chat saved → " + file.getAbsolutePath());

        if (reason.equals("manual export")) {
            JOptionPane.showMessageDialog(null,
                    "Chat exported to:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    void resetSessionState() {
        if (uptimeTimer != null) { uptimeTimer.stop(); uptimeTimer = null; }
        connectTime  = 0;
        currentRoom  = null;
        myStatus     = "Active";
        showRooms    = false;
        showUsers    = false;
        selectedUser = null;
        lastExportedRowCount = 0;
        userStatuses.clear();
        ephemeralMsgs.clear();

        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            roomsModel.clear();
            usersModel.clear();
            userCombo.removeAllItems();
            uptimeLabel.setText("00:00:00");
            connLabel.setText("CONNECTED TO: —");
            userBadgeLabel.setText("  ● —  ");
            userBadgeLabel.setForeground(C_TEXT_MUTED);
            statusDot.setForeground(C_RED);
            setStatus("Disconnected");
            setChatEnabled(true);
        });
    }

    void connect() {
        String[] creds = showLoginDialog();
        if (creds == null) System.exit(0);

        connectedUsername = creds[0];
        connectedPassword = creds[1];
        connectedHost     = creds[2];
        connectedPort     = 5000;

        try {
            socket = new Socket(connectedHost, connectedPort);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            sendRaw("HELLO " + connectedUsername + " " + connectedPassword);

            String firstLine = in.readLine();
            if (firstLine == null || !firstLine.startsWith("200")) {
                try { socket.close(); } catch (Exception ignored) {}
                String msg = "Server error. Try again.";
                if (firstLine != null && firstLine.startsWith("403")) msg = "Username not registered. Contact the admin.";
                else if (firstLine != null && firstLine.startsWith("401")) msg = "Wrong password. Try again.";
                else if (firstLine != null && firstLine.startsWith("409")) msg = "Username already connected.";
                JOptionPane.showMessageDialog(null, msg, "Login Failed", JOptionPane.ERROR_MESSAGE);
                connect();
                return;
            }

            SwingUtilities.invokeLater(() -> {
                addRow("C", "HELLO " + connectedUsername, "proto");
                handle(firstLine);
            });

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        final String l = line;
                        SwingUtilities.invokeLater(() -> handle(l));
                    }
                } catch (Exception ex) {

                    SwingUtilities.invokeLater(() -> {
                        exportChat("auto — connection lost");
                        resetSessionState();
                        setLog("[ " + ts() + " ] connection lost");
                    });
                }
            }).start();

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                statusDot.setForeground(C_RED);
                setStatus("Connection failed — " + connectedHost + ":" + connectedPort + " unreachable");
                setLog("[ " + ts() + " ] " + ex.getMessage());
            });
        }
    }


    String[] showLoginDialog() {
        JDialog dialog = new JDialog((Frame) null, "ChatLite Login", true);
        dialog.setSize(380, 340);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_DARK);
        header.setPreferredSize(new Dimension(0, 52));
        header.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        JLabel title    = new JLabel("ChatLite Client");
        title.setFont(F_MONO_B);
        title.setForeground(new Color(0xC8, 0xC8, 0xE8));
        JLabel subtitle = new JLabel("Enter your registered username and password");
        subtitle.setFont(F_MONO_SM);
        subtitle.setForeground(new Color(0x5F, 0x5E, 0x5A));
        JPanel headerText = new JPanel(new GridLayout(2, 1));
        headerText.setBackground(C_DARK);
        headerText.add(title);
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.CENTER);
        dialog.add(header, BorderLayout.NORTH);

        JButton btnSameDevice = new JButton("Same Device");
        JButton btnZeroTier   = new JButton("IP Address");
        btnSameDevice.setFont(F_MONO_SM);
        btnZeroTier  .setFont(F_MONO_SM);
        btnSameDevice.setFocusPainted(false);
        btnZeroTier  .setFocusPainted(false);

        JPanel tabRow = new JPanel(new GridLayout(1, 2, 0, 0));
        tabRow.setBackground(C_BG_SECONDARY);
        tabRow.add(btnSameDevice);
        tabRow.add(btnZeroTier);

        JTextField ipField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_MUTED); g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics(); Insets ins = getInsets();
                    g2.drawString("Server IP ", ins.left + 4,
                            ins.top + (getHeight()-ins.top-ins.bottom+fm.getAscent()-fm.getDescent())/2);
                }
            }
        };
        ipField.setFont(F_MONO);
        ipField.setBackground(C_BG_PRIMARY);
        ipField.setForeground(C_TEXT_MAIN);
        ipField.setCaretColor(C_ACCENT);
        ipField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ipField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JTextField usernameField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_MUTED); g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics(); Insets ins = getInsets();
                    g2.drawString("Username", ins.left + 4,
                            ins.top + (getHeight()-ins.top-ins.bottom+fm.getAscent()-fm.getDescent())/2);
                }
            }
        };
        usernameField.setFont(F_MONO);
        usernameField.setBackground(C_BG_PRIMARY);
        usernameField.setForeground(C_TEXT_MAIN);
        usernameField.setCaretColor(C_ACCENT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPasswordField passwordField = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_MUTED); g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics(); Insets ins = getInsets();
                    g2.drawString("Password", ins.left + 4,
                            ins.top + (getHeight()-ins.top-ins.bottom+fm.getAscent()-fm.getDescent())/2);
                }
            }
        };
        passwordField.setFont(F_MONO);
        passwordField.setBackground(C_BG_PRIMARY);
        passwordField.setForeground(C_TEXT_MAIN);
        passwordField.setCaretColor(C_ACCENT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel errorLbl = new JLabel(" ");
        errorLbl.setFont(F_MONO_SM);
        errorLbl.setForeground(C_RED);
        errorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginBtn = makeAccentButton("CONNECT");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(C_BG_SECONDARY);
        body.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        body.add(ipField);
        body.add(Box.createVerticalStrut(8));
        body.add(usernameField);
        body.add(Box.createVerticalStrut(8));
        body.add(passwordField);
        body.add(Box.createVerticalStrut(6));
        body.add(errorLbl);
        body.add(Box.createVerticalStrut(8));
        body.add(loginBtn);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(C_BG_SECONDARY);
        centerPanel.add(tabRow, BorderLayout.NORTH);
        centerPanel.add(body,   BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JLabel hint = new JLabel("Admin must register your username first.");
        hint.setFont(F_MONO_SM);
        hint.setForeground(C_TEXT_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(6, 24, 8, 24));
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(hint, BorderLayout.SOUTH);

        final boolean[] useRemoteIp = {false};

        Runnable activateSameDevice = () -> {
            useRemoteIp[0] = false;
            ipField.setVisible(false);
            btnSameDevice.setBackground(C_ACCENT);
            btnSameDevice.setForeground(Color.WHITE);
            btnZeroTier.setBackground(C_BG_PRIMARY);
            btnZeroTier.setForeground(C_TEXT_MAIN);
            dialog.revalidate();
            dialog.repaint();
        };

        Runnable activateZeroTier = () -> {
            useRemoteIp[0] = true;
            ipField.setVisible(true);
            btnZeroTier.setBackground(C_ACCENT);
            btnZeroTier.setForeground(Color.WHITE);
            btnSameDevice.setBackground(C_BG_PRIMARY);
            btnSameDevice.setForeground(C_TEXT_MAIN);
            dialog.revalidate();
            dialog.repaint();
        };

        btnSameDevice.addActionListener(e -> activateSameDevice.run());
        btnZeroTier  .addActionListener(e -> activateZeroTier.run());

        ipField.setVisible(false);
        btnSameDevice.setBackground(C_ACCENT);
        btnSameDevice.setForeground(Color.WHITE);
        btnZeroTier.setBackground(C_BG_PRIMARY);
        btnZeroTier.setForeground(C_TEXT_MAIN);

        final String[][] result = {null};

        Runnable tryLogin = () -> {
            String name = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            String host = useRemoteIp[0] ? ipField.getText().trim() : "localhost";

            if (name.isEmpty()) { errorLbl.setText("Username cannot be empty."); return; }
            if (pass.length() < 8) {
                errorLbl.setText("Password must be at least 8 characters.");
                passwordField.requestFocus();
                return;
            }
            if (useRemoteIp[0] && host.isEmpty()) {
                errorLbl.setText("Please enter the server IP address.");
                ipField.requestFocus();
                return;
            }

            try {
                Socket testSocket = new Socket(host, 5000);
                BufferedReader testIn  = new BufferedReader(new InputStreamReader(testSocket.getInputStream()));
                PrintWriter    testOut = new PrintWriter(testSocket.getOutputStream(), true);
                testOut.println("HELLO " + name + " " + pass);
                String response = testIn.readLine();
                testSocket.close();
                if (response != null && response.startsWith("200")) {
                    result[0] = new String[]{name, pass, host};
                    dialog.dispose();
                } else if (response != null && response.startsWith("403")) {
                    errorLbl.setText("Username not registered. Ask the admin.");
                } else if (response != null && response.startsWith("401")) {
                    errorLbl.setText("Wrong password. Try again.");
                    passwordField.setText(""); passwordField.requestFocus();
                } else if (response != null && response.startsWith("409")) {
                    errorLbl.setText("Username already in use.");
                } else {
                    errorLbl.setText("Server error. Try again.");
                }
            } catch (Exception ex) {
                errorLbl.setText("Cannot reach " + host + ":5000");
            }
        };

        loginBtn     .addActionListener(e -> tryLogin.run());
        usernameField.addActionListener(e -> passwordField.requestFocus());
        passwordField.addActionListener(e -> tryLogin.run());
        ipField      .addActionListener(e -> usernameField.requestFocus());

        dialog.setVisible(true);
        return result[0];
    }


    void sendMessage() {
        String raw = inputField.getText().trim();
        if (raw.isEmpty()) return;
        String up = raw.toUpperCase();

        if (up.startsWith("JOIN ")) {
            String room = raw.substring(5).trim();
            if (!room.isEmpty()) sendRaw("JOIN " + room);
        } else if (up.startsWith("LEAVE")) {
            String room = raw.length() > 5 ? raw.substring(5).trim() : "";
            if (room.isEmpty() && currentRoom != null) room = currentRoom;
            if (!room.isEmpty()) { addRow("C", "LEAVE " + room, "proto"); sendRaw("LEAVE " + room); }
        } else if (up.startsWith("PM ")) {
            sendRaw("PM " + raw.substring(3).trim());
        } else if (up.equals("USERS")) {
            addRow("C", "USERS", "proto"); showUsers = true; showRooms = false; sendRaw("USERS");
        } else if (up.equals("ROOMS")) {
            addRow("C", "ROOMS", "proto"); roomsModel.clear(); showRooms = true; showUsers = false; sendRaw("ROOMS");
        } else if (up.equals("QUIT")) {
            addRow("C", "QUIT", "proto"); sendRaw("QUIT");
        } else if (up.startsWith("MSG ")) {
            String rest = raw.substring(4).trim();
            int sp = rest.indexOf(' ');
            if (sp > 0) {
                sendRaw("MSG " + rest.substring(0, sp).trim() + " " + rest.substring(sp + 1).trim());
            } else {
                addRow("System", "Usage: MSG <room> <message>", "system");
            }
        } else if (up.startsWith("STATUS ")) {
            String st = raw.substring(7).trim();
            if (!st.isEmpty()) {
                String normalized = st.substring(0, 1).toUpperCase() + st.substring(1).toLowerCase();
                sendRaw("STATUS " + normalized);
                setLog("[ " + ts() + " ] Status → " + normalized);
            } else {
                addRow("System", "Usage: STATUS <Active|Busy|Away>", "system");
            }
        } else {
            if (currentRoom != null) {
                sendRaw("MSG " + currentRoom + " " + raw);
            } else {
                addRow("System", "⚠ Not in a room — click a room on the left or type JOIN <room>", "system");
            }
        }
        inputField.setText("");
    }

    void sendPrivate() {
        String user = (String) userCombo.getSelectedItem();
        String msg  = privateField.getText().trim();
        if (user == null || msg.isEmpty()) return;
        sendRaw("PM " + user + " " + msg);
        addRow("→ PM to " + user, msg, "pm");
        setLog("[ " + ts() + " ] PM sent to " + user);
        privateField.setText("");
    }

    void sendRaw(String msg) { if (out != null) out.println(msg); }


    void handle(String res) {

        if (res.startsWith("421")) {
            exportChat("auto — kicked: password reset by admin");
            addRow("System", "⚠ Your password was reset by the admin. Please sign in again.", "system");
            setLog("[ " + ts() + " ] 421 PASSWORD_RESET");
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            resetSessionState();
            int choice = JOptionPane.showConfirmDialog(null,
                    "Your password has been changed by the administrator.\nReconnect with your new credentials?",
                    "Password Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) connect();
            return;
        }


        if (res.startsWith("403")) {
            addRow("System", "Access denied — username not registered.", "system");
            statusDot.setForeground(C_RED); setStatus("Rejected — not registered");
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            return;
        }


        if (res.startsWith("200 STATUS ")) {
            setLog("[ " + ts() + " ] Status changed to " + res.substring(11).trim());
            return;
        }

        if (res.startsWith("200 WELCOME") || res.equals("200")) {
            addRow("S", "200 WELCOME", "proto");
            connectTime = System.currentTimeMillis();
            statusDot.setForeground(C_GREEN);
            setStatus("Connected as " + connectedUsername);
            connLabel.setText("CONNECTED TO: " + connectedHost + ":5000");
            userBadgeLabel.setText("  ● " + connectedUsername + "  ");
            userBadgeLabel.setForeground(C_ACCENT);
            setLog("[ " + ts() + " ] 200 WELCOME");
            sendRaw("ROOMS"); sendRaw("USERS");


            if (uptimeTimer != null) uptimeTimer.stop();
            uptimeTimer = new javax.swing.Timer(1000, e -> {
                if (connectTime == 0) return;
                long sec = (System.currentTimeMillis() - connectTime) / 1000;
                uptimeLabel.setText(String.format("%02d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60));
            });
            uptimeTimer.start();

            new javax.swing.Timer(3000, e -> { sendRaw("ROOMS"); sendRaw("USERS"); }).start();
            return;
        }

        if (res.startsWith("210")) {
            String room = res.contains("JOINED ") ? res.substring(res.indexOf("JOINED ")+7).trim() : "";
            currentRoom = room.isEmpty() ? currentRoom : room;
            tableModel.setRowCount(0);
            addRow("System", "You joined " + currentRoom, "joined");
            setStatus("Connected as " + connectedUsername + "  ·  " + currentRoom);
            sendRaw("HISTORY " + currentRoom);
            return;
        }

        if (res.startsWith("211")) { setLog("[ " + ts() + " ] 211 SENT"); return; }
        if (res.startsWith("212")) { setLog("[ " + ts() + " ] 212 PRIVATE SENT"); return; }

        if (res.equals("213 END") || res.equals("213END")) {
            if (selectedUser != null && userCombo.getItemCount() > 0)
                for (int i = 0; i < userCombo.getItemCount(); i++)
                    if (selectedUser.equals(userCombo.getItemAt(i))) { userCombo.setSelectedIndex(i); break; }
            selectedUser = null;
            if (showUsers) { addRow("S", "213 END", "proto"); showUsers = false; }
            setLog("[ " + ts() + " ] 213 END — " + usersModel.getSize() + " users");
            return;
        }
        if (res.startsWith("213U ") || res.startsWith("213U\t")) {
            String rest = res.substring(res.indexOf(' ')+1).trim();
            String[] tokens = rest.split(" ", 2);
            String user   = tokens[0].trim();
            String status = tokens.length > 1 ? tokens[1].trim() : "Active";
            if (showUsers) addRow("S", res.trim(), "proto");
            if (!user.isEmpty()) {
                userStatuses.put(user, status);
                if (!usersModel.contains(user)) { usersModel.addElement(user); userCombo.addItem(user); }
            }
            usersList.repaint();
            return;
        }
        if (res.equals("213") || (res.startsWith("213") && !res.startsWith("213U"))) {
            selectedUser = (String) userCombo.getSelectedItem();
            usersModel.clear(); userCombo.removeAllItems();
            if (showUsers) addRow("S", "213", "proto");
            return;
        }


        if (res.startsWith("214")) {
            String room = res.length() > 4 ? res.substring(4).trim() : "";
            if (showRooms) addRow("S", res.trim(), "proto");
            if (!room.isEmpty() && !roomsModel.contains(room)) roomsModel.addElement(room);
            if (roomsEndTimer != null) roomsEndTimer.stop();
            roomsEndTimer = new javax.swing.Timer(400, e -> { showRooms = false; roomsEndTimer = null; });
            roomsEndTimer.setRepeats(false); roomsEndTimer.start();
            return;
        }
        showRooms = false;

        if (res.startsWith("215")) {
            String left = res.length() > 8 ? res.substring(8).trim() : "";
            addRow("S", res.trim(), "proto");
            if (left.equals(currentRoom)) {
                exportChat("auto — left room: " + left);
                ephemeralMsgs.remove(left);
                currentRoom = null; tableModel.setRowCount(0);
                addRow("System", "You left " + left + ". Type JOIN <room> to join another.", "system");
                setStatus("Connected as " + connectedUsername + "  ·  no room");
            } else {
                addRow("System", "Left " + left + " (you remain in " + currentRoom + ").", "system");
            }
            return;
        }


        if (res.startsWith("221")) {
            exportChat("auto — quit");
            addRow("S", res.trim(), "proto");
            addRow("System", "Disconnected. Reconnecting…", "system");
            setLog("[ " + ts() + " ] 221 BYE — session ended, account preserved");

            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            resetSessionState();


            new javax.swing.Timer(600, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                connect();
            }).start();
            return;
        }


        if (res.equals("216 END") || res.equals("216END")) {
            if (tableModel.getRowCount() > 1) {
                addRow("System", " live messages ", "history_sep");
            }
            java.util.List<Object[]> stored = ephemeralMsgs.get(currentRoom);
            if (stored != null) {
                for (Object[] e : stored) {
                    tableModel.addRow(new Object[]{e[0], e[1], e[2]});
                    int row = tableModel.getRowCount() - 1;
                    table.putClientProperty("rowType:" + row, e[3]);
                }
                table.scrollRectToVisible(table.getCellRect(tableModel.getRowCount() - 1, 0, true));
            }
            setLog("[ " + ts() + " ] history loaded for " + currentRoom);
            return;
        }
        if (res.startsWith("216H ")) {
            String rest = res.substring(5).trim();
            int sp1 = rest.indexOf(' ');
            if (sp1 > 0) {
                String histUser  = rest.substring(0, sp1);
                String remainder = rest.substring(sp1 + 1).trim();
                int    sp2       = remainder.indexOf(' ');
                String histTime  = sp2 > 0 ? remainder.substring(0, sp2) : remainder;
                String histMsg   = sp2 > 0 ? remainder.substring(sp2 + 1).trim() : "";
                addHistoryRow(histUser, histMsg, histTime);
            }
            return;
        }
        if (res.startsWith("216")) {
            addRow("System", " message history ", "history_sep");
            return;
        }


        if (res.contains(":")) {
            String[] parts = res.split(":", 2);
            String user = parts[0].trim();
            String msg  = parts[1].trim();

            if (user.equals("System") && msg.contains("[BROADCAST]")) {
                addRow("Admin", msg.replace("[BROADCAST]", "").trim(), "broadcast");
                setLog("[ " + ts() + " ] Broadcast received");
                return;
            }

            if (user.equals("System") && msg.contains(" is now ")) {
                int idx = msg.indexOf(" is now ");
                userStatuses.put(msg.substring(0, idx).trim(), msg.substring(idx + 8).trim());
                usersList.repaint();
                if (currentRoom != null) {
                    String t = new SimpleDateFormat("hh:mm:ssa").format(new Date());
                    ephemeralMsgs
                            .computeIfAbsent(currentRoom, k -> new java.util.ArrayList<>())
                            .add(new Object[]{user, msg, t, "system"});
                }
            }

            String type = user.equalsIgnoreCase("System")
                    ? (msg.toLowerCase().contains("join") ? "joined"
                    :  msg.toLowerCase().contains("left") ? "left" : "system") : "normal";
            addRow(user, msg, type);
            return;
        }

        addRow("Server", res, "system");
        setLog("[ " + ts() + " ] " + res);
    }



    void addRow(String user, String msg, String type) {
        String time = new SimpleDateFormat("hh:mm:ssa").format(new Date());
        tableModel.addRow(new Object[]{user, msg, time});
        int row = tableModel.getRowCount() - 1;
        table.putClientProperty("rowType:" + row, type);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    void addHistoryRow(String user, String msg, String originalTime) {
        tableModel.addRow(new Object[]{user, msg, originalTime});
        int row = tableModel.getRowCount() - 1;
        table.putClientProperty("rowType:" + row, "history");
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    void filterTable() {
        String q = searchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        if (q.isEmpty()) { sorter.setRowFilter(null); return; }
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + q));
    }

    void setChatEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendBtn.setEnabled(enabled);
        inputField.setBackground(enabled ? C_BG_PRIMARY : C_BG_SECONDARY);
        sendBtn.setBackground(enabled ? C_ACCENT : C_TEXT_MUTED);
        if (!enabled) { tableModel.setRowCount(0); table.putClientProperty("rowTypeCount", 0); }
    }

    void setStatus(String txt) { if (statusLabel != null) statusLabel.setText(txt); }
    void setLog   (String txt) { if (logLabel    != null) logLabel.setText(txt); }
    String ts() { return new SimpleDateFormat("HH:mm:ss").format(new Date()); }



    class RoomCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int idx, boolean sel, boolean focus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
            lbl.setFont(F_MONO);
            if (sel) {
                lbl.setBackground(C_BG_PRIMARY); lbl.setForeground(C_ACCENT);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 2, 0, 0, C_ACCENT),
                        BorderFactory.createEmptyBorder(4, 10, 4, 12)));
            } else {
                lbl.setBackground(C_BG_SECONDARY); lbl.setForeground(C_TEXT_MUTED);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            }
            lbl.setText("⊟ " + value);
            return lbl;
        }
    }

    class UserCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int idx, boolean sel, boolean focus) {
            String name = value == null ? "" : value.toString();
            String initials = name.length() >= 2
                    ? ("" + name.charAt(0) + name.charAt(1)).toUpperCase() : name.toUpperCase();
            String status = userStatuses.getOrDefault(name, "Active");
            Color dotCol = status.equalsIgnoreCase("Busy")    ? C_AMBER
                    : status.equalsIgnoreCase("Away")    ? C_RED
                    : status.equalsIgnoreCase("Offline") ? C_TEXT_MUTED : C_GREEN;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
            row.setBackground(sel ? C_BG_PRIMARY : C_BG_SECONDARY);
            JLabel avatar = new JLabel(initials) {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_ACCENT.brighter()); g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                    super.paintComponent(g);
                }
            };
            avatar.setPreferredSize(new Dimension(22, 22));
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatar.setFont(new Font("Courier New", Font.BOLD, 8));
            avatar.setForeground(Color.WHITE); avatar.setOpaque(false);
            JLabel nameLbl = new JLabel(name + " (" + status + ")");
            nameLbl.setFont(F_MONO); nameLbl.setForeground(sel ? C_TEXT_MAIN : C_TEXT_MUTED);
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Courier New", Font.PLAIN, 8)); dot.setForeground(dotCol);
            row.add(avatar); row.add(nameLbl); row.add(dot);
            return row;
        }
    }

    class MsgTableRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable tbl, Object value, boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(tbl, value, sel, focus, row, col);
            c.setFont(col == 2 ? F_MONO_SM : F_MONO);
            String type = (String) tbl.getClientProperty("rowType:" + row);
            if (type == null) type = "normal";
            if (sel) { c.setBackground(new Color(0xEE,0xED,0xFE)); c.setForeground(C_ACCENT_DARK); return c; }
            c.setBackground(row % 2 == 0 ? C_BG_PRIMARY : new Color(0xFA,0xFA,0xF8));
            switch (type) {
                case "joined"    -> c.setForeground(col == 0 ? C_TEXT_MUTED : C_GREEN);
                case "left"      -> c.setForeground(col == 0 ? C_TEXT_MUTED : C_RED);
                case "pm"        -> { c.setForeground(C_RED); c.setBackground(new Color(0xFF,0xF0,0xEE)); }
                case "broadcast" -> {
                    c.setBackground(new Color(0xFF, 0xF8, 0xE1));
                    c.setForeground(col == 0 ? C_AMBER : new Color(0x7A, 0x5C, 0x00));
                    c.setFont(F_MONO_B);
                }
                case "system"    -> c.setForeground(C_TEXT_MUTED);
                case "proto"     -> {
                    c.setBackground(new Color(0xF0,0xF0,0xFF));
                    c.setForeground(col == 0 ? C_ACCENT : new Color(0x18,0x5F,0xA5));
                    c.setFont(F_MONO_SM);
                }
                case "history" -> {
                    c.setBackground(new Color(0xF8, 0xF8, 0xF0));
                    c.setForeground(col == 0 ? C_TEXT_MUTED : new Color(0x6A, 0x6A, 0x62));
                    c.setFont(F_MONO_SM);
                }
                case "history_sep" -> {
                    c.setBackground(new Color(0xED, 0xED, 0xE5));
                    c.setForeground(C_TEXT_MUTED);
                    c.setFont(F_MONO_SM);
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                default -> c.setForeground(col == 0 ? C_ACCENT : C_TEXT_MAIN);
            }
            if (col == 2) c.setForeground(C_TEXT_MUTED);
            ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, col == 0 ? 14 : 8, 0, 8));
            return c;
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }

}