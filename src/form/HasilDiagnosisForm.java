package form;

import config.Database;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HasilDiagnosisForm extends JDialog {
    private JLabel lblHeaderTitle;
    private JLabel lblDisease;
    private JLabel lblPercentage;
    private JTextArea txtDescription;
    private JTextArea txtPrevention;

    private JLabel lblCategory;

    public HasilDiagnosisForm(String idPenyakit, double percentage) {
        setTitle("Hasil Diagnosis");
        setModal(true);
        setSize(760, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        initComponents();
        lblPercentage.setText(String.format("Tingkat Kecocokan: %.2f%%", percentage));
        loadDiseaseData(idPenyakit);
    }

    private void initComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(180, 28, 12));
        headerPanel.setPreferredSize(new Dimension(0, 34));

        lblHeaderTitle = new JLabel("Hasil Diagnosis", SwingConstants.LEFT);
        lblHeaderTitle.setForeground(Color.WHITE);
        lblHeaderTitle.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblHeaderTitle.setBorder(new EmptyBorder(0, 8, 0, 8));
        headerPanel.add(lblHeaderTitle, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(14, 12, 14, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        lblDisease = new JLabel("Penyakit: Sedang memuat...");
        lblDisease.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblDisease.setForeground(Color.DARK_GRAY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(lblDisease, gbc);

        lblCategory = new JLabel("Kategori: -");
        lblCategory.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblCategory.setForeground(new Color(0, 102, 153));
        gbc.gridy = 1;
        contentPanel.add(lblCategory, gbc);

        lblPercentage = new JLabel("Tingkat Kecocokan: 0%");
        lblPercentage.setFont(new Font("Tahoma", Font.PLAIN, 12));
        lblPercentage.setForeground(new Color(15, 118, 110));
        gbc.gridy = 2;
        contentPanel.add(lblPercentage, gbc);

        JLabel lblDesc = new JLabel("Deskripsi Penyakit");
        lblDesc.setFont(UIStyle.FONT_LABEL);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(lblDesc, gbc);

        txtDescription = new JTextArea(6, 28);
        txtDescription.setEditable(false);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtDescription.setBackground(Color.WHITE);
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(descScroll, gbc);

        JLabel lblPrev = new JLabel("Cara Pencegahan");
        lblPrev.setFont(UIStyle.FONT_LABEL);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(lblPrev, gbc);

        txtPrevention = new JTextArea(6, 28);
        txtPrevention.setEditable(false);
        txtPrevention.setLineWrap(true);
        txtPrevention.setWrapStyleWord(true);
        txtPrevention.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtPrevention.setBackground(Color.WHITE);
        JScrollPane prevScroll = new JScrollPane(txtPrevention);
        prevScroll.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(prevScroll, gbc);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        JButton btnOk = new JButton("OK");
        UIStyle.styleButton(btnOk, "");
        btnOk.addActionListener(e -> dispose());
        buttonPanel.add(btnOk);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadDiseaseData(String idPenyakit) {
        try {
            Connection conn = Database.getConnection();
            String sql = "SELECT nama_penyakit, kategori, deskripsi, pencegahan FROM penyakit WHERE id_penyakit = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, idPenyakit);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String diseaseName = rs.getString("nama_penyakit");
                String kategori = rs.getString("kategori");
                String jenisLabel = "Infeksi".equals(kategori) ? "Infeksi (Menular)" : "Non-Infeksi (Tidak Menular)";
                lblHeaderTitle.setText("Hasil Diagnosis : anda terkena penyakit " + diseaseName + " (" + idPenyakit + ")");
                lblDisease.setText("Penyakit: " + diseaseName);
                lblCategory.setText("Kategori: " + jenisLabel);
                txtDescription.setText(rs.getString("deskripsi"));
                txtPrevention.setText(rs.getString("pencegahan"));
            } else {
                lblDisease.setText("Penyakit Tidak Ditemukan");
                txtDescription.setText("Informasi tidak tersedia untuk ID: " + idPenyakit);
                txtPrevention.setText("Informasi tidak tersedia.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading disease details: " + e.getMessage());
        }
    }
}
