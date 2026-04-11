package form;

import config.Database;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataGejalaPenyakitForm extends JInternalFrame {
    private JComboBox<DiseaseItem> cmbPenyakit;
    private JTextField txtNamaPenyakit;
    private JTable tblGejalaTersedia;
    private JTable tblGejalaTerpilih;
    private DefaultTableModel modelTersedia;
    private DefaultTableModel modelTerpilih;
    private JButton btnAdd, btnRemove, btnSave;

    private static class DiseaseItem {
        String id;
        String nama;

        public DiseaseItem(String id, String nama) {
            this.id = id;
            this.nama = nama;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public DataGejalaPenyakitForm() {
        super("Pemetaan Gejala ke Penyakit", true, true, true, true);
        setSize(800, 500);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadDiseases();
        loadAvailableSymptoms();
    }

    private void initComponents() {
        setSize(1000, 650);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel();
        UIStyle.styleHeaderPanel(headerPanel, "DATA GEJALA PENYAKIT (RULE)");
        add(headerPanel, BorderLayout.NORTH);

        // Main Content Container
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Panel: Disease Selection
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.insets = new Insets(6, 6, 6, 6);
        topGbc.anchor = GridBagConstraints.WEST;

        JLabel lblPenyakit = new JLabel("Kode Penyakit");
        lblPenyakit.setFont(UIStyle.FONT_LABEL);
        topGbc.gridx = 0;
        topGbc.gridy = 0;
        topPanel.add(lblPenyakit, topGbc);

        cmbPenyakit = new JComboBox<>();
        cmbPenyakit.setPreferredSize(new Dimension(180, 24));
        cmbPenyakit.addActionListener(e -> loadSelectedSymptoms());
        topGbc.gridx = 1;
        topPanel.add(cmbPenyakit, topGbc);

        JLabel lblNama = new JLabel("Nama Penyakit");
        lblNama.setFont(UIStyle.FONT_LABEL);
        topGbc.gridx = 0;
        topGbc.gridy = 1;
        topPanel.add(lblNama, topGbc);

        txtNamaPenyakit = new JTextField();
        txtNamaPenyakit.setEditable(false);
        txtNamaPenyakit.setPreferredSize(new Dimension(180, 24));
        topGbc.gridx = 1;
        topPanel.add(txtNamaPenyakit, topGbc);

        btnSave = new JButton("Simpan");
        UIStyle.styleButton(btnSave, "");
        btnSave.addActionListener(e -> saveMapping());
        topGbc.gridx = 1;
        topGbc.gridy = 2;
        topGbc.anchor = GridBagConstraints.CENTER;
        topPanel.add(btnSave, topGbc);

        mainContent.add(topPanel, BorderLayout.NORTH);

        // Center Panel: Dual List (Shuttle)
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        centerPanel.setOpaque(false);
        
        // Left: Available Symptoms
        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.setOpaque(false);
        JLabel lblTersedia = new JLabel("Daftar Gejala Tersedia", SwingConstants.CENTER);
        lblTersedia.setFont(UIStyle.FONT_LABEL);
        lblTersedia.setBorder(new EmptyBorder(0,0,10,0));
        leftWrapper.add(lblTersedia, BorderLayout.NORTH);
        
        modelTersedia = new DefaultTableModel(new Object[]{"ID", "Gejala Tersedia"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblGejalaTersedia = new JTable(modelTersedia);
        UIStyle.styleTable(tblGejalaTersedia);
        tblGejalaTersedia.getColumnModel().getColumn(0).setMinWidth(0);
        tblGejalaTersedia.getColumnModel().getColumn(0).setMaxWidth(0);
        JScrollPane scrollTersedia = new JScrollPane(tblGejalaTersedia);
        scrollTersedia.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        leftWrapper.add(scrollTersedia, BorderLayout.CENTER);
        centerPanel.add(leftWrapper);

        // Middle: Buttons
        JPanel btnPanel = new JPanel(new GridBagLayout());
        btnPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);

        btnAdd = new JButton(">>");
        btnAdd.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnAdd.setPreferredSize(new Dimension(60, 28));
        gbc.gridy = 0;
        btnPanel.add(btnAdd, gbc);

        btnRemove = new JButton("<<");
        btnRemove.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnRemove.setPreferredSize(new Dimension(60, 28));
        gbc.gridy = 1;
        btnPanel.add(btnRemove, gbc);
        centerPanel.add(btnPanel);

        // Right: Selected Symptoms
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        JLabel lblTerpilih = new JLabel("Daftar Gejala Terpilih", SwingConstants.CENTER);
        lblTerpilih.setFont(UIStyle.FONT_LABEL);
        lblTerpilih.setBorder(new EmptyBorder(0,0,10,0));
        rightWrapper.add(lblTerpilih, BorderLayout.NORTH);
        
        modelTerpilih = new DefaultTableModel(new Object[]{"ID", "Gejala Terpilih"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblGejalaTerpilih = new JTable(modelTerpilih);
        UIStyle.styleTable(tblGejalaTerpilih);
        tblGejalaTerpilih.getColumnModel().getColumn(0).setMinWidth(0);
        tblGejalaTerpilih.getColumnModel().getColumn(0).setMaxWidth(0);
        JScrollPane scrollTerpilih = new JScrollPane(tblGejalaTerpilih);
        scrollTerpilih.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        rightWrapper.add(scrollTerpilih, BorderLayout.CENTER);
        centerPanel.add(rightWrapper);

        mainContent.add(centerPanel, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Bottom Panel: Save
        // Basic Action Listeners
        btnAdd.addActionListener(e -> {
            int row = tblGejalaTersedia.getSelectedRow();
            if (row != -1) {
                Object id = modelTersedia.getValueAt(row, 0);
                Object nama = modelTersedia.getValueAt(row, 1);
                modelTerpilih.addRow(new Object[]{id, nama});
                modelTersedia.removeRow(row);
            }
        });

        btnRemove.addActionListener(e -> {
            int row = tblGejalaTerpilih.getSelectedRow();
            if (row != -1) {
                Object id = modelTerpilih.getValueAt(row, 0);
                Object nama = modelTerpilih.getValueAt(row, 1);
                modelTersedia.addRow(new Object[]{id, nama});
                modelTerpilih.removeRow(row);
            }
        });

    }

    private void loadDiseases() {
        cmbPenyakit.removeAllItems();
        try {
            Connection conn = Database.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id_penyakit, nama_penyakit FROM penyakit");
            while (rs.next()) {
                cmbPenyakit.addItem(new DiseaseItem(rs.getString("id_penyakit"), rs.getString("nama_penyakit")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading diseases: " + e.getMessage());
        }
    }

    private void loadAvailableSymptoms() {
        modelTersedia.setRowCount(0);
        try {
            Connection conn = Database.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id_gejala, nama_gejala FROM gejala");
            while (rs.next()) {
                modelTersedia.addRow(new Object[]{rs.getString("id_gejala"), rs.getString("nama_gejala")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading symptoms: " + e.getMessage());
        }
    }

    private void loadSelectedSymptoms() {
        modelTerpilih.setRowCount(0);
        loadAvailableSymptoms(); // Reset available list first

        DiseaseItem selected = (DiseaseItem) cmbPenyakit.getSelectedItem();
        if (selected == null) {
            txtNamaPenyakit.setText("");
            return;
        }
        txtNamaPenyakit.setText(selected.nama);

        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT g.id_gejala, g.nama_gejala FROM gejala g JOIN aturan a ON g.id_gejala = a.id_gejala WHERE a.id_penyakit = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, selected.id);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id_gejala");
                String nama = rs.getString("nama_gejala");
                modelTerpilih.addRow(new Object[]{id, nama});

                // Remove from available
                for (int i = 0; i < modelTersedia.getRowCount(); i++) {
                    if (modelTersedia.getValueAt(i, 0).equals(id)) {
                        modelTersedia.removeRow(i);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading rules: " + e.getMessage());
        }
    }

    private void saveMapping() {
        DiseaseItem selected = (DiseaseItem) cmbPenyakit.getSelectedItem();
        if (selected == null) return;

        try {
            Connection conn = Database.getConnection();
            conn.setAutoCommit(false);
            try {
                // Clear existing rules
                String deleteSql = "DELETE FROM aturan WHERE id_penyakit = ?";
                PreparedStatement pstmtDelete = conn.prepareStatement(deleteSql);
                pstmtDelete.setString(1, selected.id);
                pstmtDelete.executeUpdate();

                // Insert new rules
                String insertSql = "INSERT INTO aturan (id_penyakit, id_gejala) VALUES (?, ?)";
                PreparedStatement pstmtInsert = conn.prepareStatement(insertSql);
                for (int i = 0; i < modelTerpilih.getRowCount(); i++) {
                    pstmtInsert.setString(1, selected.id);
                    pstmtInsert.setString(2, modelTerpilih.getValueAt(i, 0).toString());
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
                
                conn.commit();
                JOptionPane.showMessageDialog(this, "Mapping untuk " + selected.nama + " berhasil disimpan!");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving mapping: " + e.getMessage());
        }
    }
}
