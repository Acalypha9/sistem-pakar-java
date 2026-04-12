package form;

import config.Database;
import model.ExpertSystemEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DataPenyakitForm extends JInternalFrame {
    private JTextField txtId;
    private JTextField txtNama;
    private JComboBox<String> cmbKategori;
    private JTextArea txtDeskripsi;
    private JTextArea txtPencegahan;
    private JTextArea txtObat;
    private JTable table;
    private DefaultTableModel tableModel;

    public DataPenyakitForm() {
        super("Data Penyakit", true, true, true, true);
        initComponents();
        loadData();
    }

    private void initComponents() {
        setSize(1000, 650);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel();
        UIStyle.styleHeaderPanel(headerPanel, "DATA PENYAKIT");
        add(headerPanel, BorderLayout.NORTH);

        // Split Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(330);
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

        // ID
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblId = new JLabel("ID Penyakit :");
        lblId.setFont(UIStyle.FONT_LABEL);
        lblId.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblId, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtId = new JTextField();
        txtId.setPreferredSize(new Dimension(0, 24));
        formPanel.add(txtId, gbc);

        // Nama
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblNama = new JLabel("Nama Penyakit :");
        lblNama.setFont(UIStyle.FONT_LABEL);
        lblNama.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblNama, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNama = new JTextField();
        txtNama.setPreferredSize(new Dimension(0, 24));
        formPanel.add(txtNama, gbc);

        // Kategori
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel lblKategori = new JLabel("Kategori :");
        lblKategori.setFont(UIStyle.FONT_LABEL);
        lblKategori.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblKategori, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbKategori = new JComboBox<>(new String[]{"Infeksi", "Non-Infeksi", "Gastrousus"});
        cmbKategori.setPreferredSize(new Dimension(0, 24));
        formPanel.add(cmbKategori, gbc);

        // Deskripsi
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        JLabel lblDesk = new JLabel("Deskripsi :");
        lblDesk.setFont(UIStyle.FONT_LABEL);
        lblDesk.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblDesk, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtDeskripsi = new JTextArea(4, 20);
        txtDeskripsi.setLineWrap(true);
        txtDeskripsi.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(txtDeskripsi), gbc);

        // Pencegahan
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        JLabel lblPencegahan = new JLabel("Pencegahan :");
        lblPencegahan.setFont(UIStyle.FONT_LABEL);
        lblPencegahan.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblPencegahan, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtPencegahan = new JTextArea(4, 20);
        txtPencegahan.setLineWrap(true);
        txtPencegahan.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(txtPencegahan), gbc);

        // Obat
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        JLabel lblObat = new JLabel("Obat :");
        lblObat.setFont(UIStyle.FONT_LABEL);
        lblObat.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblObat, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtObat = new JTextArea(4, 20);
        txtObat.setLineWrap(true);
        txtObat.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(txtObat), gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.setOpaque(false);
        JButton btnSimpan = new JButton("Simpan");
        JButton btnHapus = new JButton("Hapus");
        UIStyle.styleButton(btnSimpan, "");
        UIStyle.styleButton(btnHapus, "");
        btnSimpan.addActionListener(e -> simpanData());
        btnHapus.addActionListener(e -> hapusData());
        buttonPanel.add(btnSimpan);
        buttonPanel.add(btnHapus);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 0, 0);
        formPanel.add(buttonPanel, gbc);

        JPanel wrapperForm = new JPanel(new BorderLayout());
        wrapperForm.setOpaque(false);
        wrapperForm.add(formPanel, BorderLayout.NORTH);
        splitPane.setLeftComponent(new JScrollPane(wrapperForm));

        // Table Panel (Right)
        tableModel = new DefaultTableModel(new String[]{"ID", "Nama", "Kategori", "Deskripsi", "Pencegahan", "Obat"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIStyle.styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                txtId.setText(table.getValueAt(row, 0).toString());
                txtNama.setText(table.getValueAt(row, 1).toString());
                cmbKategori.setSelectedItem(table.getValueAt(row, 2).toString());
                txtDeskripsi.setText(table.getValueAt(row, 3).toString());
                txtPencegahan.setText(table.getValueAt(row, 4).toString());
                txtObat.setText(table.getValueAt(row, 5).toString());
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
            ResultSet rs = stmt.executeQuery("SELECT * FROM penyakit");
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("id_penyakit"));
                row.add(rs.getString("nama_penyakit"));
                row.add(rs.getString("kategori"));
                row.add(rs.getString("deskripsi"));
                row.add(rs.getString("pencegahan"));
                row.add(rs.getString("obat"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error load data: " + e.getMessage());
        }
    }

    private void simpanData() {
        String id = txtId.getText();
        String nama = txtNama.getText();
        String kategori = cmbKategori.getSelectedItem().toString();
        String diagnosisType = "Gastrousus".equals(kategori)
                ? ExpertSystemEngine.TYPE_GASTROUSUS
                : ExpertSystemEngine.TYPE_INFEKSI;
        String deskripsi = txtDeskripsi.getText();
        String pencegahan = txtPencegahan.getText();
        String obat = txtObat.getText();

        if (id.isEmpty() || nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID dan Nama tidak boleh kosong!");
            return;
        }

        try {
            Connection conn = Database.getConnection();
            String checkSql = "SELECT id_penyakit FROM penyakit WHERE id_penyakit = ?";
            PreparedStatement pstmtCheck = conn.prepareStatement(checkSql);
            pstmtCheck.setString(1, id);
            ResultSet rs = pstmtCheck.executeQuery();

            if (rs.next()) {
                // Update
                String sql = "UPDATE penyakit SET diagnosis_type=?, nama_penyakit=?, kategori=?, deskripsi=?, pencegahan=?, obat=? WHERE id_penyakit=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, diagnosisType);
                pstmt.setString(2, nama);
                pstmt.setString(3, kategori);
                pstmt.setString(4, deskripsi);
                pstmt.setString(5, pencegahan);
                pstmt.setString(6, obat);
                pstmt.setString(7, id);
                pstmt.executeUpdate();
            } else {
                // Insert
                String sql = "INSERT INTO penyakit (id_penyakit, diagnosis_type, nama_penyakit, kategori, deskripsi, pencegahan, obat) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, id);
                pstmt.setString(2, diagnosisType);
                pstmt.setString(3, nama);
                pstmt.setString(4, kategori);
                pstmt.setString(5, deskripsi);
                pstmt.setString(6, pencegahan);
                pstmt.setString(7, obat);
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
        String id = txtId.getText();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus data ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = Database.getConnection();
                String sql = "DELETE FROM penyakit WHERE id_penyakit=?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, id);
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
        txtId.setText("");
        txtNama.setText("");
        cmbKategori.setSelectedIndex(0);
        txtDeskripsi.setText("");
        txtPencegahan.setText("");
        txtObat.setText("");
        table.clearSelection();
    }
}
