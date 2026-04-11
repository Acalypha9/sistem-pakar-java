package form;

import config.Database;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class HistoryForm extends JInternalFrame {
    private JTable tblHistory;
    private DefaultTableModel model;

    public HistoryForm() {
        super("Riwayat Konsultasi", true, true, true, true);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadHistory();
    }

    private void initComponents() {
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel();
        UIStyle.styleHeaderPanel(headerPanel, "RIWAYAT KONSULTASI");
        add(headerPanel, BorderLayout.NORTH);

        // Table Panel with padding
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("Data history menyimpan hasil diagnosa dan prosentase dari setiap percobaan.");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(60, 60, 60));
        infoLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        centerPanel.add(infoLabel, BorderLayout.NORTH);

        String[] columns = {"ID", "Nama History", "Tanggal", "Hasil Diagnosis", "Persentase"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblHistory = new JTable(model);
        UIStyle.styleTable(tblHistory);
        tblHistory.setBackground(Color.WHITE);
        tblHistory.getColumnModel().getColumn(0).setMaxWidth(50);
        JScrollPane scrollPane = new JScrollPane(tblHistory);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setOpaque(false);

        JButton btnView = new JButton("View Details");
        UIStyle.styleButton(btnView, ""); // Using default style first
        btnView.setText("Lihat Detail");
        btnView.addActionListener(e -> viewDetails());

        JButton btnUpdate = new JButton("Update Name");
        UIStyle.styleButton(btnUpdate, "");
        btnUpdate.setText("Ubah Nama");
        btnUpdate.addActionListener(e -> updateName());
        
        JButton btnDelete = new JButton("Delete");
        UIStyle.styleButton(btnDelete, "");
        btnDelete.setText("Hapus");
        btnDelete.setBackground(new Color(220, 38, 38));
        btnDelete.setForeground(Color.BLACK);
        btnDelete.addActionListener(e -> deleteHistory());

        buttonPanel.add(btnView);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadHistory() {
        model.setRowCount(0);
        try {
            Connection conn = Database.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id_history, nama_history, tanggal, hasil_diagnosis, persentase FROM history ORDER BY id_history DESC");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_history"),
                    rs.getString("nama_history"),
                    rs.getString("tanggal"),
                    rs.getString("hasil_diagnosis"),
                    String.format("%.2f%%", rs.getDouble("persentase"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading history: " + e.getMessage());
        }
    }

    private void viewDetails() {
        int row = tblHistory.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data history terlebih dahulu.");
            return;
        }

        int idHistory = (int) model.getValueAt(row, 0);
        try {
            Connection conn = Database.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM history WHERE id_history = ?");
            pstmt.setInt(1, idHistory);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String gejalaIds = rs.getString("gejala_dipilih");
                String diseaseName = rs.getString("hasil_diagnosis");
                double percentage = rs.getDouble("persentase");
                
                StringBuilder detail = new StringBuilder();
                detail.append("Nama: ").append(rs.getString("nama_history")).append("\n");
                detail.append("Tanggal: ").append(rs.getString("tanggal")).append("\n");
                detail.append("Hasil: ").append(diseaseName).append("\n");
                detail.append("Persentase: ").append(String.format("%.2f%%", percentage)).append("\n\n");
                detail.append("Gejala yang dipilih:\n");

                if (gejalaIds != null && !gejalaIds.isEmpty()) {
                    String[] ids = gejalaIds.split(",");
                    for (String id : ids) {
                        PreparedStatement pstG = conn.prepareStatement("SELECT nama_gejala FROM gejala WHERE id_gejala = ?");
                        pstG.setString(1, id);
                        ResultSet rsG = pstG.executeQuery();
                        if (rsG.next()) {
                            detail.append("- ").append(rsG.getString("nama_gejala")).append("\n");
                        }
                    }
                }

                JTextArea textArea = new JTextArea(detail.toString());
                textArea.setEditable(false);
                textArea.setBackground(Color.WHITE);
                textArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(this, scrollPane, "Detail History", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching details: " + e.getMessage());
        }
    }

    private void updateName() {
        int row = tblHistory.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data history terlebih dahulu.");
            return;
        }

        int idHistory = (int) model.getValueAt(row, 0);
        String oldName = (String) model.getValueAt(row, 1);
        String newName = JOptionPane.showInputDialog(this, "Masukkan nama baru:", oldName);

        if (newName != null && !newName.trim().isEmpty()) {
            try {
                Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("UPDATE history SET nama_history = ? WHERE id_history = ?");
                pstmt.setString(1, newName);
                pstmt.setInt(2, idHistory);
                pstmt.executeUpdate();
                loadHistory();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating name: " + e.getMessage());
            }
        }
    }

    private void deleteHistory() {
        int row = tblHistory.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data history terlebih dahulu.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Hapus history ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int idHistory = (int) model.getValueAt(row, 0);
            try {
                Connection conn = Database.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM history WHERE id_history = ?");
                pstmt.setInt(1, idHistory);
                pstmt.executeUpdate();
                loadHistory();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting history: " + e.getMessage());
            }
        }
    }
}
