package form;

import config.Database;
import model.ExpertSystemEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class KonsultasiForm extends JInternalFrame {
    private JTable tblGejala;
    private DefaultTableModel model;
    private JButton btnDiagnosis;
    private JComboBox<String> cbMetode;
    private JComboBox<String> cbThreshold;
    private JTextArea txtPersentase;
    private JEditorPane txtRule;
    private CardLayout rightPanelLayout;
    private JPanel rightPanelCards;
    private JLabel lblResult;

    public KonsultasiForm() {
        super("Konsultasi Gejala", true, true, true, true);
        setSize(900, 560);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        applyStyles();
        loadSymptoms();
        updateDiagnosisPreview();
    }

    private void initComponents() {
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);

        JPanel headerPanel = new JPanel();
        UIStyle.styleHeaderPanel(headerPanel, "DAFTAR PERTANYAAN");
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel infoLabel = new JLabel("<html><div style='text-align:center;'>Silahkan jawab pertanyaan berikut ini untuk melakukan konsultasi guna mengetahui penyakit yang dialami<br>(beri tanda centang untuk jawaban YA)</div></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
        infoLabel.setForeground(new Color(60, 60, 60));
        infoLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        centerWrapper.add(infoLabel, BorderLayout.NORTH);

        String[] columns = {"No", "Pertanyaan", "Jawaban (Ya/Tidak)", "ID"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };

        tblGejala = new JTable(model);
        UIStyle.styleTable(tblGejala);
        tblGejala.getTableHeader().setReorderingAllowed(false);
        tblGejala.getColumnModel().getColumn(0).setMaxWidth(40);
        tblGejala.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblGejala.getColumnModel().getColumn(3).setMinWidth(0);
        tblGejala.getColumnModel().getColumn(3).setMaxWidth(0);

        JScrollPane leftScroll = new JScrollPane(tblGejala);
        leftScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tblGejala.setBackground(new Color(255, 255, 200));
        leftScroll.getViewport().setBackground(new Color(255, 255, 200));

        txtPersentase = new JTextArea();
        txtPersentase.setEditable(false);
        txtPersentase.setFont(new Font("Tahoma", Font.PLAIN, 12));
        txtPersentase.setBackground(Color.WHITE);
        txtPersentase.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane percentageScroll = new JScrollPane(txtPersentase);
        percentageScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        txtRule = new JEditorPane();
        txtRule.setEditable(false);
        txtRule.setContentType("text/html");
        txtRule.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        txtRule.setBackground(Color.WHITE);
        txtRule.setText(buildEmptyRuleHtml());
        JScrollPane ruleScroll = new JScrollPane(txtRule);
        ruleScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        rightPanelLayout = new CardLayout();
        rightPanelCards = new JPanel(rightPanelLayout);
        rightPanelCards.add(ruleScroll, "RULE");
        rightPanelCards.add(percentageScroll, "PERCENTAGE");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightPanelCards);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerLocation(560);
        splitPane.setBorder(null);
        centerWrapper.add(splitPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(5, 10, 5, 5));

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultPanel.setOpaque(false);
        JLabel lblResultPrefix = new JLabel("Anda terdiagnosis : ");
        lblResultPrefix.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblResultPrefix.setForeground(new Color(60, 60, 60));
        resultPanel.add(lblResultPrefix);
        lblResult = new JLabel("\u2014");
        lblResult.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblResult.setForeground(Color.RED);
        resultPanel.add(lblResult);
        
        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        leftWrapper.add(resultPanel, gbc);

        bottomPanel.add(leftWrapper, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JLabel lblMetode = new JLabel("Metode diagnosis : ");
        lblMetode.setFont(UIStyle.FONT_LABEL);
        actionPanel.add(lblMetode);
        cbMetode = new JComboBox<>(new String[]{"Rule-Based", "Bobot / Prosentase"});
        cbMetode.setPreferredSize(new Dimension(140, 26));
        cbMetode.addActionListener(e -> updateDiagnosisPreview());
        actionPanel.add(cbMetode);

        JLabel lblThreshold = new JLabel("Threshold : ");
        lblThreshold.setFont(UIStyle.FONT_LABEL);
        actionPanel.add(lblThreshold);
        cbThreshold = new JComboBox<>(new String[]{"50", "60", "70", "80", "90", "100"});
        cbThreshold.setSelectedItem("80");
        cbThreshold.setPreferredSize(new Dimension(65, 26));
        cbThreshold.addActionListener(e -> updateDiagnosisPreview());
        actionPanel.add(cbThreshold);

        btnDiagnosis = new JButton("Proses");
        UIStyle.styleButton(btnDiagnosis, "");
        btnDiagnosis.setPreferredSize(new Dimension(90, 28));
        btnDiagnosis.setBackground(new Color(165, 35, 24));
        btnDiagnosis.setForeground(Color.BLACK);
        btnDiagnosis.addActionListener(e -> runDiagnosis());
        actionPanel.add(btnDiagnosis);

        bottomPanel.add(actionPanel, BorderLayout.EAST);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 2 || e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    updateDiagnosisPreview();
                }
            }
        });

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void applyStyles() {
        getContentPane().setBackground(UIStyle.COLOR_BACKGROUND);
    }

    private void loadSymptoms() {
        model.setRowCount(0);
        Database.getConnection();
        int number = 1;
        for (ExpertSystemEngine.Question question : ExpertSystemEngine.getQuestions()) {
            model.addRow(new Object[]{String.valueOf(number), question.text(), false, question.id()});
            number++;
        }
    }

    private void runDiagnosis() {
        Set<String> selectedQuestionIds = getSelectedQuestionIds();
        String gejalaStr = getSelectedQuestionIdsAsString();

        if (selectedQuestionIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Silakan pilih minimal satu pertanyaan dengan jawaban YA.");
            return;
        }

        boolean isRuleBased = cbMetode.getSelectedIndex() == 0;
        double threshold = Double.parseDouble(String.valueOf(cbThreshold.getSelectedItem()));
        List<ExpertSystemEngine.DiagnosisResult> results = isRuleBased
                ? ExpertSystemEngine.diagnoseRuleBased(selectedQuestionIds)
                : ExpertSystemEngine.diagnosePercentageBased(selectedQuestionIds, threshold);

        String mainDiseaseName = results.isEmpty() ? "Sehat" : results.get(0).diseaseName();
        double mainPercentage = results.isEmpty() ? 0.0 : results.get(0).percentage();
        
        saveToHistory(mainDiseaseName, mainPercentage, gejalaStr);

        if (!results.isEmpty()) {
            HasilDiagnosisForm resultForm = new HasilDiagnosisForm(results.get(0).diseaseId(), results.get(0).percentage());
            resultForm.setVisible(true);
            return;
        }

        String modeInfo = isRuleBased
                ? "Metode rule-based tidak menemukan rule yang terpenuhi seluruhnya."
                : String.format("Metode prosentase tidak menghasilkan nilai yang memenuhi threshold %.0f%%.", threshold);

        JOptionPane.showMessageDialog(
                this,
                "Tidak terdeteksi penyakit berdasarkan jawaban anda.\n\n" + modeInfo,
                "Message",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void saveToHistory(String diseaseName, double percentage, String gejalaIds) {
        try {
            Connection conn = Database.getConnection();
            int nextN = 1;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM history");
            if (rs.next()) {
                nextN = rs.getInt(1) + 1;
            }

            String sql = "INSERT INTO history (nama_history, tanggal, hasil_diagnosis, persentase, gejala_dipilih) VALUES (?, datetime('now'), ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "Percobaan-" + nextN);
            pstmt.setString(2, diseaseName);
            pstmt.setDouble(3, percentage);
            pstmt.setString(4, gejalaIds);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

    private Set<String> getSelectedQuestionIds() {
        Set<String> selectedQuestionIds = new LinkedHashSet<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 2)) {
                selectedQuestionIds.add(String.valueOf(model.getValueAt(i, 3)));
            }
        }
        return selectedQuestionIds;
    }

    private String getSelectedQuestionIdsAsString() {
        StringBuilder gejalaStr = new StringBuilder();
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 2)) {
                if (gejalaStr.length() > 0) {
                    gejalaStr.append(",");
                }
                gejalaStr.append(model.getValueAt(i, 3));
            }
        }
        return gejalaStr.toString();
    }

    private void updateDiagnosisPreview() {
        boolean isRuleBased = cbMetode.getSelectedIndex() == 0;
        
        cbThreshold.setEnabled(!isRuleBased);
        
        double threshold = Double.parseDouble(String.valueOf(cbThreshold.getSelectedItem()));
        rightPanelLayout.show(rightPanelCards, isRuleBased ? "RULE" : "PERCENTAGE");

        Set<String> selectedIds = getSelectedQuestionIds();
        if (selectedIds.isEmpty()) {
            txtPersentase.setText("");
            txtRule.setText(buildEmptyRuleHtml());
            lblResult.setText("\u2014");
            return;
        }

        List<ExpertSystemEngine.DiseaseScore> scores = ExpertSystemEngine.calculateDiseaseScores(selectedIds);

        if (isRuleBased) {
            List<ExpertSystemEngine.DiagnosisResult> results = ExpertSystemEngine.diagnoseRuleBased(selectedIds);
            txtRule.setText(buildRulePreviewHtml(selectedIds, scores, results));
            txtRule.setCaretPosition(0);

            if (!results.isEmpty()) {
                StringBuilder sb = new StringBuilder("<html>");
                for (int i = 0; i < results.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(results.get(i).diseaseName()).append(" [").append(results.get(i).category()).append("]");
                }
                sb.append("</html>");
                lblResult.setText(sb.toString());
            } else {
                lblResult.setText("\u2014");
            }
            return;
        }

        List<ExpertSystemEngine.DiagnosisResult> results = ExpertSystemEngine.diagnosePercentageBased(selectedIds, threshold);
        txtPersentase.setText(formatScores(scores));
        txtPersentase.setCaretPosition(0);
        if (!results.isEmpty()) {
            StringBuilder sb = new StringBuilder("<html>");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(results.get(i).diseaseName()).append(" [").append(results.get(i).category()).append("]");
            }
            sb.append("</html>");
            lblResult.setText(sb.toString());
        } else {
            lblResult.setText("\u2014");
        }
    }

    private String formatScores(List<ExpertSystemEngine.DiseaseScore> scores) {
        scores.sort((a, b) -> Double.compare(b.percentage(), a.percentage()));
        StringBuilder sb = new StringBuilder();
        for (ExpertSystemEngine.DiseaseScore score : scores) {
            sb.append(score.diseaseName())
                    .append(" : ")
                    .append(String.format("%.2f%%", score.percentage()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildEmptyRuleHtml() {
        return "<html><body style='font-family:Tahoma,sans-serif;background:#fff;padding:12px;'>"
                + "<div style='font-size:16px;font-weight:bold;color:#a14f00;margin-bottom:6px;'>Penyelesaian dengan aturan (rules)</div>"
                + "<div style='height:3px;background:#ffcf33;margin-bottom:2px;'></div>"
                + "<div style='height:3px;background:#8cc63f;margin-bottom:14px;'></div>"
                + "<div style='font-size:12px;color:#666;'>Pilih gejala pada tabel kiri untuk melihat rule yang relevan.</div>"
                + "</body></html>";
    }

    private String buildRulePreviewHtml(Set<String> selectedIds, List<ExpertSystemEngine.DiseaseScore> scores, List<ExpertSystemEngine.DiagnosisResult> results) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Tahoma,sans-serif;background:#fff;padding:12px;'>");
        html.append("<div style='font-size:16px;font-weight:bold;color:#a14f00;margin-bottom:6px;'>Penyelesaian dengan aturan (rules)</div>");
        html.append("<div style='height:3px;background:#ffcf33;margin-bottom:2px;'></div>");
        html.append("<div style='height:3px;background:#8cc63f;margin-bottom:14px;'></div>");

        boolean anyRelevant = false;
        for (ExpertSystemEngine.DiseaseScore score : scores) {
            if (score.matched() == 0) continue;
            anyRelevant = true;
            appendRuleBlock(html, score, selectedIds);
        }

        if (!anyRelevant) {
            html.append("<div style='font-size:12px;color:#666;'>Tidak ada rule yang relevan dengan gejala yang dipilih.</div>");
        }

        boolean hasDetected = !results.isEmpty();
        html.append("<div style='margin-top:14px;padding:8px;background:")
                .append(hasDetected ? "#e6ffe6;border:1px solid #4a4" : "#fff3e0;border:1px solid #e0a030")
                .append(";font-size:13px;'>");
        if (hasDetected) {
            StringBuilder sb = new StringBuilder();
            for (ExpertSystemEngine.DiagnosisResult r : results) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(esc(r.diseaseName())).append(" [").append(esc(r.category())).append("]");
            }
            html.append("<b>Hasil:</b> ").append(sb.toString()).append(" - SEMUA GEJALA TERPENUHI");
        } else {
            html.append("<b>Hasil:</b> Tidak ada rule yang terpenuhi seluruhnya.");
        }
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private void appendRuleBlock(StringBuilder html, ExpertSystemEngine.DiseaseScore score, Set<String> selectedIds) {
        List<String> deps = ExpertSystemEngine.getDependencies(score.diseaseId());
        boolean allMatch = score.matched() == score.total();

        html.append("<div style='border:1px solid ").append(allMatch ? "#4a4" : "#ccc")
                .append(";background:").append(allMatch ? "#f0fff0" : "#fafafa")
                .append(";padding:8px;margin-bottom:8px;font-size:12px;line-height:1.6;'>");

        html.append("<div style='font-weight:bold;margin-bottom:4px;'>")
                .append(esc(score.diseaseName()))
                .append(" [").append(esc(score.category())).append("]</div>");

        html.append("<b>IF</b> ");
        for (int i = 0; i < deps.size(); i++) {
            if (i > 0) html.append(" <b>AND</b> ");
            String dep = deps.get(i);
            boolean matched = selectedIds.contains(dep);
            html.append("<span style='color:").append(matched ? "#008000" : "#999").append(";'>")
                    .append(matched ? "\u2713 " : "\u2717 ")
                    .append(esc(getRulePhrase(dep)))
                    .append("</span>");
        }

        html.append("<br><b>THEN</b> <span style='color:")
                .append(allMatch ? "#008000" : "#999")
                .append(";font-weight:bold;'>")
                .append(esc(score.diseaseName()))
                .append("</span>");

        if (allMatch) {
            html.append(" <span style='color:#008000;font-weight:bold;'>\u2714 TERPENUHI</span>");
        }

        html.append("</div>");
    }

    private String getRulePhrase(String nodeId) {
        return switch (nodeId) {
            case "1" -> "demam";
            case "2" -> "batuk";
            case "3" -> "pilek";
            case "4" -> "sakit tenggorokan";
            case "5" -> "demam tinggi";
            case "6" -> "nyeri sendi";
            case "7" -> "ruam kulit";
            case "8" -> "mual";
            case "9" -> "sering haus";
            case "10" -> "sering buang air kecil";
            case "11" -> "mudah lelah";
            case "12" -> "luka sulit sembuh";
            case "13" -> "sakit kepala";
            case "14" -> "pusing";
            case "15" -> "penglihatan kabur";
            case "16" -> "tekanan darah tinggi";
            case "17" -> "mimisan";
            default -> nodeId;
        };
    }

    private String esc(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
