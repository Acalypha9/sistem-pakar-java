package form;

import config.Database;
import model.ExpertSystemEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
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
    private final String diagnosisType;
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
        this(ExpertSystemEngine.TYPE_INFEKSI);
    }

    public KonsultasiForm(String type) {
        super("Konsultasi Gejala", true, true, true, true);
        diagnosisType = ExpertSystemEngine.normalizeType(type);
        setTitle("Konsultasi Gejala - " + ExpertSystemEngine.getDisplayName(diagnosisType));
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

        JLabel infoLabel = new JLabel("<html><div style='text-align:center;'>Silahkan jawab pertanyaan berikut ini untuk melakukan konsultasi "
                + ExpertSystemEngine.getDisplayName(diagnosisType)
                + "<br>(beri tanda centang untuk jawaban YA)</div></html>");
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
        configureQuestionColumnWrap();
        tblGejala.getTableHeader().setReorderingAllowed(false);
        tblGejala.getColumnModel().getColumn(0).setMaxWidth(40);
        tblGejala.getColumnModel().getColumn(1).setPreferredWidth(360);
        tblGejala.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblGejala.getColumnModel().getColumn(3).setMinWidth(0);
        tblGejala.getColumnModel().getColumn(3).setMaxWidth(0);

        JScrollPane leftScroll = new JScrollPane(tblGejala);
        leftScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tblGejala.setBackground(new Color(255, 255, 200));
        leftScroll.getViewport().setBackground(new Color(255, 255, 200));

        JPanel leftArea = new JPanel(new BorderLayout());
        leftArea.setOpaque(false);
        leftArea.add(leftScroll, BorderLayout.CENTER);

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

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftArea, rightPanelCards);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerLocation(560);
        splitPane.setBorder(null);
        centerWrapper.add(splitPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(2, 10, 10, 5));

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultPanel.setOpaque(false);
        resultPanel.setBorder(new EmptyBorder(18, 0, 0, 0));
        JLabel lblResultPrefix = new JLabel("Anda terdiagnosis : ");
        lblResultPrefix.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblResultPrefix.setForeground(new Color(60, 60, 60));
        resultPanel.add(lblResultPrefix);
        lblResult = new JLabel("\u2014");
        lblResult.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblResult.setForeground(Color.RED);
        resultPanel.add(lblResult);

        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.add(resultPanel, BorderLayout.NORTH);
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
                SwingUtilities.invokeLater(() -> updateQuestionRowHeights());
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
        for (ExpertSystemEngine.Question question : ExpertSystemEngine.getQuestions(diagnosisType)) {
            model.addRow(new Object[]{String.valueOf(number), question.text(), false, question.id()});
            number++;
        }
        SwingUtilities.invokeLater(() -> updateQuestionRowHeights());
    }

    private void runDiagnosis() {
        Set<String> selectedQuestionIds = getSelectedQuestionIds();
        String gejalaStr = diagnosisType + "|" + getSelectedQuestionIdsAsString();

        if (selectedQuestionIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Silakan pilih minimal satu pertanyaan dengan jawaban YA.");
            return;
        }

        boolean isRuleBased = cbMetode.getSelectedIndex() == 0;
        double threshold = Double.parseDouble(String.valueOf(cbThreshold.getSelectedItem()));
        List<ExpertSystemEngine.DiagnosisResult> results = isRuleBased
                ? ExpertSystemEngine.diagnoseRuleBased(diagnosisType, selectedQuestionIds)
                : ExpertSystemEngine.diagnosePercentageBased(diagnosisType, selectedQuestionIds, threshold);

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

        Set<String> inferredFacts = ExpertSystemEngine.inferFacts(diagnosisType, selectedIds);
        List<ExpertSystemEngine.DiseaseScore> scores = ExpertSystemEngine.calculateDiseaseScores(diagnosisType, selectedIds);

        if (isRuleBased) {
            List<ExpertSystemEngine.DiagnosisResult> results = ExpertSystemEngine.diagnoseRuleBased(diagnosisType, selectedIds);
            txtRule.setText(buildRulePreviewHtml(inferredFacts, scores, results));
            txtRule.setCaretPosition(0);

            lblResult.setText(formatDiagnosisResults(results));
            return;
        }

        List<ExpertSystemEngine.DiagnosisResult> results = ExpertSystemEngine.diagnosePercentageBased(diagnosisType, selectedIds, threshold);
        txtPersentase.setText(formatScores(scores));
        txtPersentase.setCaretPosition(0);
        lblResult.setText(formatDiagnosisResults(results));
    }

    private String formatDiagnosisResults(List<ExpertSystemEngine.DiagnosisResult> results) {
        if (results.isEmpty()) {
            return "\u2014";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(results.get(i).diseaseName());
            if (!ExpertSystemEngine.TYPE_GASTROUSUS.equals(diagnosisType)) {
                sb.append(" [")
                        .append(results.get(i).category())
                        .append("]");
            }
        }
        return "<html>" + esc(sb.toString()) + "</html>";
    }

    private void configureQuestionColumnWrap() {
        tblGejala.getColumnModel().getColumn(1).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea(value == null ? "" : value.toString());
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setFont(table.getFont());
                textArea.setBorder(new EmptyBorder(6, 8, 6, 8));
                textArea.setOpaque(true);

                if (isSelected) {
                    textArea.setBackground(table.getSelectionBackground());
                    textArea.setForeground(table.getSelectionForeground());
                } else {
                    textArea.setBackground(table.getBackground());
                    textArea.setForeground(table.getForeground());
                }

                return textArea;
            }
        });
    }

    private void updateQuestionRowHeights() {
        int questionColumn = 1;
        int width = tblGejala.getColumnModel().getColumn(questionColumn).getWidth();
        if (width <= 0) {
            width = 360;
        }

        for (int row = 0; row < tblGejala.getRowCount(); row++) {
            TableCellRenderer renderer = tblGejala.getCellRenderer(row, questionColumn);
            Component component = tblGejala.prepareRenderer(renderer, row, questionColumn);

            if (component instanceof JTextArea textArea) {
                textArea.setSize(width, Short.MAX_VALUE);
                int preferredHeight = Math.max(30, textArea.getPreferredSize().height + 4);
                if (tblGejala.getRowHeight(row) != preferredHeight) {
                    tblGejala.setRowHeight(row, preferredHeight);
                }
            }
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        SwingUtilities.invokeLater(this::updateQuestionRowHeights);
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

    private String buildRulePreviewHtml(Set<String> inferredFacts, List<ExpertSystemEngine.DiseaseScore> scores, List<ExpertSystemEngine.DiagnosisResult> results) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Tahoma,sans-serif;background:#fff;padding:12px;'>");
        html.append("<div style='font-size:16px;font-weight:bold;color:#a14f00;margin-bottom:6px;'>Penyelesaian dengan aturan (rules)</div>");
        html.append("<div style='height:3px;background:#ffcf33;margin-bottom:2px;'></div>");
        html.append("<div style='height:3px;background:#8cc63f;margin-bottom:14px;'></div>");

        boolean anyRelevant = false;
        for (ExpertSystemEngine.DiseaseScore score : scores) {
            if (score.matched() == 0) continue;
            anyRelevant = true;
            appendRuleBlock(html, score, inferredFacts);
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

    private void appendRuleBlock(StringBuilder html, ExpertSystemEngine.DiseaseScore score, Set<String> inferredFacts) {
        List<String> deps = ExpertSystemEngine.getDependencies(diagnosisType, score.diseaseId());
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
            boolean matched = inferredFacts.contains(dep);
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
        return ExpertSystemEngine.getNodeLabel(diagnosisType, nodeId);
    }

    private String esc(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
