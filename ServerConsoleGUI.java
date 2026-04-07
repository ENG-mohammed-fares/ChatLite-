package server;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerConsoleGUI {

    public static ServerConsoleGUI instance;

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

    static final Font F_MONO      = new Font("Courier New", Font.PLAIN, 12);
    static final Font F_MONO_SM   = new Font("Courier New", Font.PLAIN, 10);
    static final Font F_MONO_B    = new Font("Courier New", Font.BOLD,  12);
    static final Font F_MONO_B_SM = new Font("Courier New", Font.BOLD,  10);

    JLabel  uptimeLabel;
    JLabel  statusDot;
    JLabel  statusValueLabel;
    long    startTime = System.currentTimeMillis();

    DefaultTableModel        sessionsModel;
    DefaultTableModel        mailboxModel;
    DefaultTableModel        logsTableModel;
    DefaultListModel<String> usersListModel;
    JTextArea                logArea;

    private final java.util.List<String> allLogLines = new java.util.ArrayList<>();

    ServerSocket serverSocket;
    boolean      serverRunning = false;

    public ServerConsoleGUI() {
        instance = this;

        JFrame frame = new JFrame("ChatLite Server Console - [RUNNING]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLayout(new BorderLayout());

        frame.add(makeTopBar(),    BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(6, 0));
        body.setBackground(C_BG_PRIMARY);
        body.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        body.add(makeLeftPanel(),   BorderLayout.WEST);
        body.add(makeCenterPanel(), BorderLayout.CENTER);
        body.add(makeRightPanel(),  BorderLayout.EAST);
        frame.add(body, BorderLayout.CENTER);

        frame.add(makeBottomLogPanel(), BorderLayout.SOUTH);

        frame.setVisible(true);

        new javax.swing.Timer(1000, e -> updateUptime()).start();
        new javax.swing.Timer(3000, e -> refreshSessions()).start();
    }

    public void logMessage(String from, String room, String content) {
        appendLog("MSG", from + " → [" + room + "]: " + content);
    }

    public void logEvent(String type, String message) {
        appendLog(type, message);
    }

    void startServer(int port) {
        UserManager.reset();
        RoomManager.reset();
        serverRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                SwingUtilities.invokeLater(() -> {
                    statusDot.setForeground(C_GREEN);
                    statusValueLabel.setForeground(C_GREEN);
                    statusValueLabel.setText("ONLINE");
                });
                appendLog("INFO", "Server started on TCP:" + port);
                while (serverRunning) {
                    Socket client = serverSocket.accept();
                    String ip = client.getInetAddress().getHostAddress();
                    appendLog("AUTH", "New connection attempt from " + ip);
                    new ClientHandler(client).start();
                }
            } catch (Exception e) {
                if (serverRunning) appendLog("ERROR", "Server error: " + e.getMessage());
            }
        }).start();
    }

    void refreshSessions() {
        SwingUtilities.invokeLater(() -> {
            sessionsModel.setRowCount(0);
            logsTableModel.setRowCount(0);
            usersListModel.clear();
            for (ClientSession s : UserManager.getAllUsers()) {
                sessionsModel.addRow(new Object[]{
                        s.getUsername(),
                        s.getStatus().toUpperCase(),
                        s.getAssignedIp()
                });
                logsTableModel.addRow(new Object[]{
                        s.getUsername(),
                        s.getInbox(),
                        s.getSent(),
                        "0 (0 KB)"
                });
            }
            for (String name : UserManager.getAllRegistered()) {
                usersListModel.addElement(name);
            }
        });
    }

    JPanel makeTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_BG_SECONDARY);
        bar.setPreferredSize(new Dimension(0, 44));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(0, 14, 0, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(C_BG_SECONDARY);
        left.add(trafficDot(new Color(0xFF, 0x5F, 0x57)));
        left.add(trafficDot(new Color(0xFE, 0xBC, 0x2E)));
        left.add(trafficDot(new Color(0x28, 0xC8, 0x40)));
        JLabel title = new JLabel("ChatLite Server Console");
        title.setFont(F_MONO_B);
        title.setForeground(C_TEXT_MAIN);
        title.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        left.add(title);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        center.setBackground(C_BG_SECONDARY);

        JLabel srvLbl = new JLabel("SERVER STATUS:");
        srvLbl.setFont(F_MONO_B_SM);
        srvLbl.setForeground(C_TEXT_MUTED);
        center.add(srvLbl);

        statusDot = new JLabel("●");
        statusDot.setFont(new Font("Courier New", Font.PLAIN, 11));
        statusDot.setForeground(C_RED);
        center.add(statusDot);

        statusValueLabel = new JLabel("STARTING…");
        statusValueLabel.setFont(F_MONO_B);
        statusValueLabel.setForeground(C_AMBER);
        center.add(statusValueLabel);

        center.add(makeSep());
        center.add(infoChip("PORT (TCP):", "5000"));
        center.add(makeSep());
        center.add(infoChip("PORT (UDP):", "5001"));
        center.add(makeSep());

        JLabel uptimeTxt = new JLabel("Uptime:");
        uptimeTxt.setFont(F_MONO_SM);
        uptimeTxt.setForeground(C_TEXT_MUTED);
        center.add(uptimeTxt);

        uptimeLabel = new JLabel("00:00:00");
        uptimeLabel.setFont(F_MONO_B);
        uptimeLabel.setForeground(C_ACCENT);
        center.add(uptimeLabel);

        bar.add(center, BorderLayout.CENTER);
        return bar;
    }

    JPanel infoChip(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(C_BG_SECONDARY);
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_MONO_B_SM);
        lbl.setForeground(C_TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(F_MONO_B);
        val.setForeground(C_TEXT_MAIN);
        p.add(lbl);
        p.add(val);
        return p;
    }

    JLabel trafficDot(Color c) {
        JLabel d = new JLabel("●");
        d.setFont(new Font("Courier New", Font.PLAIN, 10));
        d.setForeground(c);
        return d;
    }

    JLabel makeSep() {
        JLabel s = new JLabel("|");
        s.setFont(F_MONO_SM);
        s.setForeground(C_TEXT_MUTED);
        return s;
    }

    JPanel makeLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(210, 0));
        panel.setBackground(C_BG_SECONDARY);
        panel.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(C_BG_SECONDARY);

        inner.add(makeSectionHeader("USER MANAGEMENT"));

        JPanel addPanel = new JPanel();
        addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.Y_AXIS));
        addPanel.setBackground(C_BG_SECONDARY);
        addPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        addPanel.add(fieldRow("Username:", usernameField));
        addPanel.add(Box.createVerticalStrut(6));
        addPanel.add(fieldRow("Password:", passwordField));
        addPanel.add(Box.createVerticalStrut(8));

        JButton createBtn = makeAccentButton("Create User");
        createBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        createBtn.addActionListener(e -> {
            String uname = usernameField.getText().trim();
            String pass  = new String(passwordField.getPassword()).trim();
            if (uname.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (pass.length() < 8) {
                JOptionPane.showMessageDialog(null, "Password must be at least 8 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (UserManager.isRegistered(uname)) {
                JOptionPane.showMessageDialog(null, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            UserManager.registerUsername(uname, pass);
            usersListModel.addElement(uname);
            appendLog("AUTH", "User '" + uname + "' created by admin.");
            usernameField.setText("");
            passwordField.setText("");
        });
        addPanel.add(createBtn);
        inner.add(addPanel);

        inner.add(makeThinSep());

        JLabel existingLbl = new JLabel("Existing Users:");
        existingLbl.setFont(F_MONO_SM);
        existingLbl.setForeground(C_TEXT_MUTED);
        existingLbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 4, 10));
        inner.add(existingLbl);

        usersListModel = new DefaultListModel<>();
        JList<String> usersList = new JList<>(usersListModel);
        usersList.setFont(F_MONO);
        usersList.setBackground(C_BG_SECONDARY);
        usersList.setForeground(C_TEXT_MAIN);
        usersList.setFixedCellHeight(24);
        usersList.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        usersList.setCellRenderer((list, value, idx, sel, focus) -> {
            boolean online = UserManager.getUser(value.toString()) != null;
            JLabel lbl = new JLabel((online ? "●  " : "○  ") + value);
            lbl.setFont(F_MONO);
            lbl.setForeground(online ? C_GREEN : (sel ? C_ACCENT : C_TEXT_MAIN));
            lbl.setBackground(sel ? C_BG_PRIMARY : C_BG_SECONDARY);
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            return lbl;
        });

        JScrollPane usersScroll = new JScrollPane(usersList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        usersScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        usersScroll.setPreferredSize(new Dimension(210, 100));
        styleScrollPane(usersScroll);
        inner.add(usersScroll);

        JPanel btnRow = new JPanel(new GridLayout(2, 1, 0, 6));
        btnRow.setBackground(C_BG_SECONDARY);
        btnRow.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JButton deleteBtn = makeDangerButton("Delete User");
        deleteBtn.addActionListener(e -> {
            String sel = usersList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(null, "Select a user first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Delete user '" + sel + "'?\nThis removes the account permanently.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                ClientSession activeSession = UserManager.getUser(sel);
                if (activeSession != null) {
                    try {
                        PrintWriter kickOut = new PrintWriter(
                                activeSession.getConnectionSocket().getOutputStream(), true);
                        kickOut.println("221 BYE");
                        activeSession.getConnectionSocket().close();
                    } catch (Exception ignored) {}
                    UserManager.removeUser(sel);
                }
                UserManager.unregisterUsername(sel);
                usersListModel.removeElement(sel);
                appendLog("AUTH", "User '" + sel + "' account deleted by admin.");
            }
        });

        JButton resetBtn = makeWideButton("Reset Password");
        resetBtn.addActionListener(e -> {
            String sel = usersList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(null, "Select a user first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String newPass = JOptionPane.showInputDialog(null,
                    "New password for '" + sel + "' (min 8 chars):",
                    "Reset Password", JOptionPane.PLAIN_MESSAGE);
            if (newPass == null) return;
            if (newPass.trim().length() < 8) {
                JOptionPane.showMessageDialog(null, "Password must be at least 8 characters.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            UserManager.registerUsername(sel, newPass.trim());
            ClientSession activeSession = UserManager.getUser(sel);
            if (activeSession != null) {
                try {
                    PrintWriter kickOut = new PrintWriter(
                            activeSession.getConnectionSocket().getOutputStream(), true);
                    kickOut.println("421 PASSWORD_RESET");
                    activeSession.getConnectionSocket().close();
                } catch (Exception ignored) {}
                UserManager.removeUser(sel);
            }
            appendLog("AUTH", "Password reset for '" + sel + "' — session invalidated, account kept.");
            JOptionPane.showMessageDialog(null, "Password updated. User must reconnect.",
                    "Done", JOptionPane.INFORMATION_MESSAGE);
        });

        btnRow.add(deleteBtn);
        btnRow.add(resetBtn);
        inner.add(btnRow);

        JPanel dupCheck = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        dupCheck.setBackground(C_BG_SECONDARY);
        JLabel warn = new JLabel("!");
        warn.setFont(F_MONO_B);
        warn.setForeground(C_AMBER);
        JLabel dupLbl = new JLabel("Duplicate check enabled");
        dupLbl.setFont(F_MONO_SM);
        dupLbl.setForeground(C_TEXT_MUTED);
        dupCheck.add(warn);
        dupCheck.add(dupLbl);
        dupCheck.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        inner.add(dupCheck);

        panel.add(inner, BorderLayout.NORTH);
        return panel;
    }

    JPanel fieldRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(C_BG_SECONDARY);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_MONO_SM);
        lbl.setForeground(C_TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(68, 22));
        field.setFont(F_MONO_SM);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    JPanel makeCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(C_BG_PRIMARY);
        panel.add(makeSessionsPanel(),        BorderLayout.CENTER);
        panel.add(makeSystemLogsTablePanel(), BorderLayout.SOUTH);
        return panel;
    }

    JPanel makeSessionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_BG_PRIMARY);
        panel.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        panel.add(makeSectionHeader("ACTIVE SESSIONS & ROSTER"), BorderLayout.NORTH);

        sessionsModel = new DefaultTableModel(
                new String[]{"Username", "Status", "IP Address"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable sessionsTable = new JTable(sessionsModel);
        sessionsTable.setFont(F_MONO);
        sessionsTable.setRowHeight(26);
        sessionsTable.setBackground(C_BG_PRIMARY);
        sessionsTable.setGridColor(new Color(0, 0, 0, 12));
        sessionsTable.setShowVerticalLines(false);
        sessionsTable.setIntercellSpacing(new Dimension(0, 0));
        sessionsTable.getTableHeader().setFont(F_MONO_B_SM);
        sessionsTable.getTableHeader().setBackground(C_BG_SECONDARY);
        sessionsTable.getTableHeader().setForeground(C_TEXT_MUTED);
        sessionsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        sessionsTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        sessionsTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        sessionsTable.setDefaultRenderer(Object.class, new SessionRenderer());

        JScrollPane scroll = new JScrollPane(sessionsTable);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        btnRow.setBackground(C_BG_SECONDARY);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton kickBtn = makeDangerButton("Kick User");
        kickBtn.addActionListener(e -> {
            int row = sessionsTable.getSelectedRow();
            if (row < 0) return;
            String user = (String) sessionsModel.getValueAt(row, 0);
            ClientSession session = UserManager.getUser(user);
            if (session != null) {
                try {
                    PrintWriter kickOut = new PrintWriter(
                            session.getConnectionSocket().getOutputStream(), true);
                    kickOut.println("221 BYE");
                    session.getConnectionSocket().close();
                } catch (Exception ignored) {}
                UserManager.removeUser(user);
            }
            sessionsModel.removeRow(row);
            appendLog("KICK", "User '" + user + "' session ended by admin (account preserved).");
        });
        btnRow.add(kickBtn);

        JButton broadcastBtn = makeAccentButton("Send Broadcast Msg...  v");
        broadcastBtn.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog(null, "Broadcast message:",
                    "Send Broadcast", JOptionPane.PLAIN_MESSAGE);
            if (msg != null && !msg.isBlank()) {
                String stamp = new SimpleDateFormat("hh:mm:ssa").format(new Date());
                int roomCount = 0;
                int userCount = 0;

                for (String room : RoomManager.getRooms().keySet()) {
                    ClientHandler.saveMessage(room, "Admin", stamp, "[BROADCAST] " + msg);
                    RoomManager.broadcast(room, "Admin", "[BROADCAST] " + msg);
                    roomCount++;
                }

                for (ClientSession s : UserManager.getAllUsers()) {
                    userCount++;
                }

                appendLog("BROADCAST", "Admin broadcast sent to " + userCount + " user(s) across "
                        + roomCount + " room(s): " + msg);
            }
        });
        btnRow.add(broadcastBtn);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    JPanel makeSystemLogsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(0, 180));
        panel.setBackground(C_BG_PRIMARY);
        panel.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        panel.add(makeSectionHeader("SYSTEM LOGS (Live Stream)"), BorderLayout.NORTH);

        logsTableModel = new DefaultTableModel(
                new String[]{"User", "Inbox", "Sent", "Archv Size"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable logTable = new JTable(logsTableModel);
        logTable.setFont(F_MONO);
        logTable.setRowHeight(24);
        logTable.setBackground(C_BG_PRIMARY);
        logTable.setGridColor(new Color(0, 0, 0, 12));
        logTable.setShowVerticalLines(false);
        logTable.setIntercellSpacing(new Dimension(0, 0));
        logTable.getTableHeader().setFont(F_MONO_B_SM);
        logTable.getTableHeader().setBackground(C_BG_SECONDARY);
        logTable.getTableHeader().setForeground(C_TEXT_MUTED);

        JScrollPane scroll = new JScrollPane(logTable);
        styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    JPanel makeRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBackground(C_BG_PRIMARY);
        panel.add(makeSettingsPanel(), BorderLayout.NORTH);
        return panel;
    }

    JPanel makeSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(260, 160));
        panel.setBackground(C_BG_SECONDARY);
        panel.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        panel.add(makeSectionHeader("SYSTEM LOGS"), BorderLayout.NORTH);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(C_BG_SECONDARY);
        inner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel maxMsgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        maxMsgRow.setBackground(C_BG_SECONDARY);
        maxMsgRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel maxLbl = new JLabel("Max Message Size:");
        maxLbl.setFont(F_MONO_SM);
        maxLbl.setForeground(C_TEXT_MUTED);
        JComboBox<String> maxSize = new JComboBox<>(new String[]{"16 KB", "32 KB", "64 KB", "128 KB"});
        maxSize.setSelectedItem("64 KB");
        maxSize.setFont(F_MONO_SM);
        maxSize.setBackground(C_BG_PRIMARY);
        maxMsgRow.add(maxLbl);
        maxMsgRow.add(maxSize);
        inner.add(maxMsgRow);
        inner.add(Box.createVerticalStrut(8));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setBackground(C_BG_SECONDARY);
        filterRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setFont(F_MONO_SM);
        filterLbl.setForeground(C_TEXT_MUTED);
        JComboBox<String> filterCombo = new JComboBox<>(
                new String[]{"All", "INFO", "AUTH", "MSG", "PM", "ROOM", "ERROR", "KICK", "BROADCAST"});
        filterCombo.setFont(F_MONO_SM);
        filterCombo.setBackground(C_BG_PRIMARY);
        filterCombo.setPreferredSize(new Dimension(130, 24));
        filterRow.add(filterLbl);
        filterRow.add(filterCombo);
        inner.add(filterRow);
        inner.add(Box.createVerticalStrut(10));

        JButton applyBtn = makeAccentButton("Apply Settings");
        applyBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyBtn.addActionListener(e -> {
            String selected = (String) filterCombo.getSelectedItem();
            logArea.setText("");
            for (String line : allLogLines) {
                if (selected == null || selected.equals("All") || line.contains("] " + selected + ":")) {
                    logArea.append(line + "\n");
                }
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
            appendLog("INFO", "Settings applied — max size: " + maxSize.getSelectedItem()
                    + ", filter: " + selected);
        });
        inner.add(applyBtn);

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    JPanel makeBottomLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xFF, 0xFF, 0xFF, 15)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JLabel title = new JLabel("SYSTEM LOGS (Live Stream)");
        title.setFont(F_MONO_B_SM);
        title.setForeground(new Color(0xC8, 0xC8, 0xE8));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        panel.add(title, BorderLayout.NORTH);

        logArea = new JTextArea(5, 0);
        logArea.setFont(F_MONO_SM);
        logArea.setBackground(new Color(0x12, 0x12, 0x24));
        logArea.setForeground(new Color(0xC0, 0xC0, 0xD8));
        logArea.setCaretColor(C_GREEN);
        logArea.setEditable(false);
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane logScroll = new JScrollPane(logArea);
        styleScrollPane(logScroll);
        logScroll.setPreferredSize(new Dimension(0, 90));
        panel.add(logScroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnRow.setBackground(C_DARK);

        JButton clearBtn = makeWideButton("Clear Logs");
        clearBtn.addActionListener(e -> {
            logArea.setText("");
            allLogLines.clear();
        });

        JButton saveBtn = makeWideButton("Save Logs to .txt");
        saveBtn.addActionListener(e -> saveLogs());

        btnRow.add(clearBtn);
        btnRow.add(saveBtn);
        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    JPanel makeSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(F_MONO_B);
        lbl.setForeground(C_TEXT_MAIN);
        lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 5, 10));
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG_SECONDARY);
        p.add(lbl, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        return p;
    }

    JPanel makeThinSep() {
        JPanel sep = new JPanel();
        sep.setBackground(C_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(0, 1));
        return sep;
    }

    JButton makeAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_B_SM);
        btn.setBackground(C_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_ACCENT_DARK); }
            public void mouseExited (MouseEvent e) { btn.setBackground(C_ACCENT); }
        });
        return btn;
    }

    JButton makeWideButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_SM);
        btn.setBackground(C_BG_PRIMARY);
        btn.setForeground(C_TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(C_BG_SECONDARY); }
            public void mouseExited (MouseEvent e) { btn.setBackground(C_BG_PRIMARY); }
        });
        return btn;
    }

    JButton makeDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(F_MONO_SM);
        btn.setBackground(new Color(0xFA, 0xEC, 0xE7));
        btn.setForeground(C_RED);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD8, 0x5A, 0x30, 80), 1),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xF0, 0xD8, 0xD0)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(new Color(0xFA, 0xEC, 0xE7)); }
        });
        return btn;
    }

    void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setBackground(C_BG_SECONDARY);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
    }

    void appendLog(String type, String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + time + "] " + type + ": " + message;
        SwingUtilities.invokeLater(() -> {
            allLogLines.add(line);
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    void updateUptime() {
        long sec = (System.currentTimeMillis() - startTime) / 1000;
        uptimeLabel.setText(String.format("%02d:%02d:%02d",
                sec / 3600, (sec % 3600) / 60, sec % 60));
    }

    void saveLogs() {
        try {
            String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File file = new File("server_logs_" + time + ".txt");
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.print(logArea.getText());
            }
            appendLog("INFO", "Logs saved to " + file.getName());
        } catch (Exception ex) {
            appendLog("ERROR", "Failed to save logs: " + ex.getMessage());
        }
    }

    class SessionRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable tbl, Object value, boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(tbl, value, sel, focus, row, col);
            c.setFont(col == 1 ? F_MONO_B_SM : F_MONO);
            c.setBackground(sel ? new Color(0xEE, 0xED, 0xFE)
                    : row % 2 == 0 ? C_BG_PRIMARY : new Color(0xFA, 0xFA, 0xF8));
            if (col == 1 && value != null) {
                String st = value.toString();
                if      (st.equals("ACTIVE")) c.setForeground(C_GREEN);
                else if (st.equals("BUSY"))   c.setForeground(C_AMBER);
                else if (st.equals("AWAY"))   c.setForeground(C_TEXT_MUTED);
                else                          c.setForeground(C_TEXT_MAIN);
            } else {
                c.setForeground(sel ? C_ACCENT_DARK : C_TEXT_MAIN);
            }
            ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
            return c;
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            ServerConsoleGUI gui = new ServerConsoleGUI();
            gui.startServer(5000);
        });
    }
}