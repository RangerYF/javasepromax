

package com.company.View;

import com.company.Control.AnalyzePackage;
import com.company.Control.CapturePackage;
import com.company.Model.HandlerInfo;
import com.company.Control.NetworkCard;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Icmp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppFrame extends JFrame {
    // 菜单条
    private JMenuBar menuBar;
    // 菜单
    private JMenu networkMenu, protocolMenu;
    // 菜单项
    private JMenuItem[] menuItems;
    private JButton srcButton, desButton, searchButton, trackButton, refreshButton, exportButton, darkModeButton;
    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JTable table;
    private DefaultTableModel tableModel;
    private HandlerInfo handlerInfo;
    private boolean isDarkMode = false;

    public AppFrame() {
        setTitle("网络嗅探器");
        setBounds(250, 150, 1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 菜单条
        menuBar = new JMenuBar();
        networkMenu = new JMenu("网卡");
        protocolMenu = new JMenu("协议");

        // 协议菜单项
        String[] protocols = {"Ethernet II", "IP", "ICMP", "ARP", "UDP", "TCP", "HTTP"};
        for (String protocol : protocols) {
            JMenuItem item = new JMenuItem(protocol);
            item.addActionListener(e -> {
                handlerInfo.setFilterProtocol(protocol);
                handlerInfo.ShowAfterFilter();
            });
            protocolMenu.add(item);
        }

        // 过滤按钮
        srcButton = createButton("源IP", e -> filterBySourceIP());
        desButton = createButton("目的IP", e -> filterByDestinationIP());
        searchButton = createButton("查找", e -> filterByKeyword());
        trackButton = createButton("流追踪", e -> traceFlow());
        refreshButton = createButton("刷新", e -> refreshTable());
        exportButton = createButton("导出数据", e -> exportData());
        darkModeButton = createButton("暗黑模式", e -> toggleDarkMode());

        // 添加到菜单条
        menuBar.add(networkMenu);
        menuBar.add(protocolMenu);
        menuBar.add(srcButton);
        menuBar.add(desButton);
        menuBar.add(searchButton);
        menuBar.add(trackButton);
        menuBar.add(refreshButton);
        menuBar.add(exportButton);
        menuBar.add(darkModeButton);
        setJMenuBar(menuBar);

        // 表格设置
        String[] head = {"时间", "源IP/MAC", "目的IP/MAC", "协议", "长度"};
        tableModel = new DefaultTableModel(new Object[][]{}, head);
        table = new JTable(tableModel) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(25);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 25));
        header.setFont(new Font("楷体", Font.PLAIN, 14));
        scrollPane = new JScrollPane(table);

        // 主面板设置
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
        pack();
        setResizable(false);
        setVisible(true);

        // 表格双击显示详细信息
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent ev) {
                if (ev.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    showPacketDetails(row);
                }
            }
        });
    }

    private JButton createButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.setFont(new Font("", Font.PLAIN, 16));
        button.addActionListener(actionListener);
        return button;
    }

    private void filterBySourceIP() {
        String srcIP = JOptionPane.showInputDialog("请输入源IP：");
        if (srcIP != null) {
            handlerInfo.setFilterSrcip(srcIP);
            handlerInfo.ShowAfterFilter();
        }
    }

    private void filterByDestinationIP() {
        String destIP = JOptionPane.showInputDialog("请输入目的IP：");
        if (destIP != null) {
            handlerInfo.setFilterDesip(destIP);
            handlerInfo.ShowAfterFilter();
        }
    }

    private void filterByKeyword() {
        String keyword = JOptionPane.showInputDialog("请输入关键字：");
        if (keyword != null) {
            handlerInfo.setFilterKey(keyword);
            handlerInfo.ShowAfterFilter();
        }
    }

    private void traceFlow() {
        String ipPort = JOptionPane.showInputDialog("请输入IP地址和端口（格式：IP:端口）：");
        if (ipPort != null && !ipPort.isEmpty()) {
            String[] parts = ipPort.split(":");
            if (parts.length == 2) {
                handlerInfo.setTraceIP(parts[0]);
                handlerInfo.setTracePort(parts[1]);
                handlerInfo.ShowAfterFilter();
            }
        }
    }

    private void refreshTable() {
        handlerInfo.ShowAfterFilter();
    }

    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存为CSV文件");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile() + ".csv")) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i) + ",");
                }
                writer.write("\n");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.write(tableModel.getValueAt(i, j).toString() + ",");
                    }
                    writer.write("\n");
                }
                JOptionPane.showMessageDialog(this, "数据导出成功！");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "导出时发生错误：" + ex.getMessage());
            }
        }
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            mainPanel.setBackground(Color.DARK_GRAY);
            table.setBackground(Color.BLACK);
            table.setForeground(Color.WHITE);
        } else {
            mainPanel.setBackground(Color.WHITE);
            table.setBackground(Color.WHITE);
            table.setForeground(Color.BLACK);
        }
    }

    /*private void showPacketDetails(int row) {
        JFrame frame = new JFrame("详细信息");
        JPanel panel = new JPanel();
        final JTextArea info = new JTextArea(32, 42);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        frame.add(panel);
        panel.add(new JScrollPane(info));
        JButton save = new JButton("保存到本地");
        save.addActionListener(
                e -> {
                    String text = info.getText();
                    Date date = new Date(System.currentTimeMillis());
                    DateFormat df = new SimpleDateFormat("HH点mm秒ss");
                    String name = df.format(date);
                    try {
                        FileOutputStream fos = new FileOutputStream("C:\\Users\\Desktop\\捕获包详情_" + name + ".txt");
                        fos.write(text.getBytes());
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
        panel.add(save);
        frame.setBounds(150, 150, 500, 600);
        frame.setVisible(true);
        frame.setResizable(false);
        // 获取数据包并展示详细信息
        ArrayList<PcapPacket> packetlist = handlerInfo.analyzePacketlist;
        if (row < packetlist.size()) {
            PcapPacket packet = packetlist.get(row);
            AnalyzePackage analyzePackage = new AnalyzePackage(packet);
            Map<String, String> hm = analyzePackage.Analyzed();
            info.append("协议: " + hm.get("协议") + "\n");
            info.append("源IP: " + hm.get("源IP4") + "\n");
            info.append("目的IP: " + hm.get("目的IP4") + "\n");
            info.append("源端口: " + hm.get("源端口") + "\n");
            info.append("目的端口: " + hm.get("目的端口") + "\n");
            info.append("包内容: " + hm.get("包内容") + "\n");
        }
    }*/
    private void showPacketDetails(int row) {
        JFrame frame = new JFrame("详细信息");
        JPanel panel = new JPanel();
        final JTextArea info = new JTextArea(32, 42);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        frame.add(panel);
        panel.add(new JScrollPane(info));
        JButton save = new JButton("保存到本地");
        save.addActionListener(
                e -> {
                    String text = info.getText();
                    Date date = new Date(System.currentTimeMillis());
                    DateFormat df = new SimpleDateFormat("HH点mm秒ss");
                    String name = df.format(date);
                    try {
                        FileOutputStream fos = new FileOutputStream("C:\\Users\\Desktop\\捕获包详情_" + name + ".txt");
                        fos.write(text.getBytes());
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
        panel.add(save);
        frame.setBounds(150, 150, 500, 600);
        frame.setVisible(true);
        frame.setResizable(false);
        // 获取数据包并展示详细信息
        ArrayList<PcapPacket> packetlist = handlerInfo.analyzePacketlist;
        if (row < packetlist.size()) {
            PcapPacket packet = packetlist.get(row);
            AnalyzePackage analyzePackage = new AnalyzePackage(packet);
            Map<String, String> hm = analyzePackage.Analyzed();
            // 显示每个层的详细信息
            info.append("链路层:\n");
            info.append("源MAC: " + hm.get("源MAC") + "\n");
            info.append("目的MAC: " + hm.get("目的MAC") + "\n");
            info.append("类型: " + hm.get("以太网类型") + "\n\n");
            info.append("网络层:\n");
            info.append("源IP: " + hm.get("源IP4") + "\n");
            info.append("目的IP: " + hm.get("目的IP4") + "\n");
            info.append("协议类型: " + hm.get("协议") + "\n\n");
            info.append("传输层:\n");
            info.append("源端口: " + hm.get("源端口") + "\n");
            info.append("目的端口: " + hm.get("目的端口") + "\n\n");
            info.append("应用层:\n");
            info.append("数据内容: " + hm.get("包内容") + "\n");
        }
    }




    public void dataInjection() {
        java.util.List<PcapIf> alldevs = new NetworkCard().getAlldevs();
        menuItems = new JMenuItem[alldevs != null ? alldevs.size() : 0];
        int i = 0;
        for (PcapIf device : alldevs) {
            String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
            menuItems[i] = new JMenuItem(device.getName() + " [" + description + "]");
            menuItems[i].addActionListener(new CardActionListener(device));
            networkMenu.add(menuItems[i]);
            i++;
        }
        capturePackage = new CapturePackage();
        handlerInfo = new HandlerInfo();
        handlerInfo.setTablemodel(tableModel);
    }

    private class CardActionListener implements ActionListener {
        private PcapIf device;

        CardActionListener(PcapIf device) {
            this.device = device;
        }

        public void actionPerformed(ActionEvent e) {
            if (capturePackage == null) {
                capturePackage = new CapturePackage();
            }
            if (capthread == null) {
                capturePackage.setDevice(device);
                capturePackage.setHandlerInfo(handlerInfo);
                capthread = new Thread(capturePackage);
                capthread.start();
            } else {
                capturePackage.setDevice(device);
                handlerInfo.clearAllpackets();
                while (tableModel.getRowCount() > 0) {
                    tableModel.removeRow(0);
                }
            }
        }
    }

    private Thread capthread = null;
    private CapturePackage capturePackage;
}



