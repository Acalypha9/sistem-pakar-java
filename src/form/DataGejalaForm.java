package form;

import config.Database;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DataGejalaForm extends JInternalFrame {
    private JComboBox<String> cmbDiagnosisType;
    private JTextField txtId;
    private JComboBox<String> cmbOrgan;
    private JTextField txtNama;
    private JTable table;
    private DefaultTableModel tableModel;

    public DataGejalaForm() {
        super("Data Gejala", true, true, true, true);
        initComponents();
        loadData();
    }

    private void initComponents() {
        setSize(1000, 600);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel();
        UIStyle.styleHeaderPanel(headerPanel, "DATA GEJALA");
        add(headerPanel, BorderLayout.NORTH);

        // Split Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(265);
        splitPane.setBorder(new EmptyBorder(6, 6, 6, 6));
        splitPane.setOpaque(false);

        // Form Panel (Left)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Jenis Diagnosis
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblType = new JLabel("Jenis Diagnosis :");
        lblType.setFont(UIStyle.FONT_LABEL);
        lblType.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbDiagnosisType = new JComboBox<>(new String[]{"infeksi", "gastrousus"});
        cmbDiagnosisType.setPreferredSize(new Dimension(0, 24));
        formPanel.add(cmbDiagnosisType, gbc);

        // ID
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblId = new JLabel("Kode Gejala :");
        lblId.setFont(UIStyle.FONT_LABEL);
        lblId.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblId, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtId = new JTextField();
        txtId.setPreferredSize(new Dimension(0, 24));
        formPanel.add(txtId, gbc);

        // Organ
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel lblOrgan = new JLabel("Organ :");
        lblOrgan.setFont(UIStyle.FONT_LABEL);
        lblOrgan.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblOrgan, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbOrgan = new JComboBox<>(new String[]{"Pernafasan", "Infeksi Virus", "Metabolik", "Kardiovaskular", "Gastrousus", "Riwayat Konsumsi"});
        cmbOrgan.setPreferredSize(new Dimension(0, 24));
        formPanel.add(cmbOrgan, gbc);

        // Nama Gejala
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        JLabel lblNama = new JLabel("Nama Gejala :");
        lblNama.setFont(UIStyle.FONT_LABEL);
        lblNama.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblNama, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNama = new JTextField();
        txtNama.setPreferredSize(new Dimension(0, 24));
        formPanel.add(txtNama, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.setOpaque(false);
        JButton btnSimpan = new JButton("Simpan");
        JButton btnHapus = new JButton("Hapus");
        UIStyle.styleButton(btnSimpan, "");
        UIStyle.styleButton(btnHapus, "");
        btnSimpan.addActionListener(e -> simpanData());
        btnHapus.addActionListener(e -> hapusData());
        buttonPanel.add(btnSimpan);
        buttonPanel.add(btnHapus);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 0, 0);
        formPanel.add(buttonPanel, gbc);

        JPanel wrapperForm = new JPanel(new BorderLayout());
        wrapperForm.setOpaque(false);
        wrapperForm.add(formPanel, BorderLayout.NORTH);
        splitPane.setLeftComponent(new JScrollPane(wrapperForm));

        // Table Panel (Right)
        tableModel = new DefaultTableModel(new String[]{"Jenis", "Kode", "Organ", "Nama Gejala"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIStyle.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                cmbDiagnosisType.setSelectedItem(table.getValueAt(row, 0).toString());
                txtId.setText(table.getValueAt(row, 1).toString());
                cmbOrgan.setSelectedItem(table.getValueAt(row, 2).toString());
                txtNama.setText(table.getValueAt(row, 3).toString());
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        splitPane.setRightComponent(tableScroll);

        add(splitPane, BorderLayout.CENTER);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        try {
            Connection conn = Database.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT diagnosis_type, id_gejala, organ, nama_gejala FROM gejala ORDER BY diagnosis_type, CAST(id_gejala AS INTEGER), id_gejala");
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("diagnosis_type"));
                row.add(rs.getString("id_gejala"));
                row.add(rs.getString("organ"));
                row.add(rs.getString("nama_gejala"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error load data: " + e.getMessage());
        }
    }

    private void simpanData() {
        String diagnosisType = cmbDiagnosisType.getSelectedItem().toString();
        String id = txtId.getText();
        String organ = cmbOrgan.getSelectedItem().toString();
        String nama = txtNama.getText();

        if (id.isEmpty() || nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode dan Nama tidak boleh kosong!");
            return;
        }

        try {
            Connection conn = Database.getConnection();
            String checkSql = "SELECT id_gejala FROM gejala WHERE diagnosis_type = ? AND id_gejala = ?";
            PreparedStatement pstmtCheck = conn.prepareStatement(checkSql);
            pstmtCheck.setString(1, diagnosisType);
            pstmtCheck.setString(2, id);
            ResultSet rs = pstmtCheck.executeQuery();

            if (rs.next()) {
                // Update
                String sql = "UPDATE gejala SET organ=?, nama_gejala=? WHERE diagnosis_type=? AND id_gejala=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, organ);
                pstmt.setString(2, nama);
                pstmt.setString(3, diagnosisType);
                pstmt.setString(4, id);
                pstmt.executeUpdate();
            } else {
                // Insert
                String sql = "INSERT INTO gejala (diagnosis_type, id_gejala, organ, nama_gejala) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, diagnosisType);
                pstmt.setString(2, id);
                pstmt.setString(3, organ);
                pstmt.setString(4, nama);
                pstmt.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
            loadData();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error simpan: " + e.getMessage());
        }
    }

    private void hapusData() {
        String diagnosisType = cmbDiagnosisType.getSelectedItem().toString();
        String id = txtId.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus data ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = Database.getConnection();
                String sql = "DELETE FROM gejala WHERE diagnosis_type=? AND id_gejala=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, diagnosisType);
                pstmt.setString(2, id);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
                loadData();
                clearFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error hapus: " + e.getMessage());
            }
        }
    }

    private void clearFields() {
        cmbDiagnosisType.setSelectedIndex(0);
        txtId.setText("");
        cmbOrgan.setSelectedIndex(0);
        txtNama.setText("");
        table.clearSelection();
    }
}
