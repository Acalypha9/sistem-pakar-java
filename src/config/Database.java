package config;

import model.ExpertSystemEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

public class Database {
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:sistem_pakar.db");
                createTables();
                seedKnowledgeBaseIfNeeded();
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("CREATE TABLE IF NOT EXISTS history (" +
                    "id_history INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nama_history TEXT," +
                    "tanggal TEXT," +
                    "hasil_diagnosis TEXT," +
                    "persentase REAL," +
                    "gejala_dipilih TEXT)");

            if (requiresKnowledgeBaseSchemaMigration()) {
                stmt.execute("DROP TABLE IF EXISTS aturan");
                stmt.execute("DROP TABLE IF EXISTS gejala");
                stmt.execute("DROP TABLE IF EXISTS penyakit");
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS penyakit (" +
                    "id_penyakit TEXT PRIMARY KEY," +
                    "diagnosis_type TEXT NOT NULL," +
                    "nama_penyakit TEXT," +
                    "kategori TEXT," +
                    "deskripsi TEXT," +
                    "pencegahan TEXT," +
                    "obat TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS gejala (" +
                    "diagnosis_type TEXT NOT NULL," +
                    "id_gejala TEXT NOT NULL," +
                    "organ TEXT," +
                    "nama_gejala TEXT," +
                    "PRIMARY KEY (diagnosis_type, id_gejala))");

            stmt.execute("CREATE TABLE IF NOT EXISTS aturan (" +
                    "diagnosis_type TEXT NOT NULL," +
                    "id_penyakit TEXT," +
                    "id_gejala TEXT," +
                    "PRIMARY KEY (diagnosis_type, id_penyakit, id_gejala)," +
                    "FOREIGN KEY (id_penyakit) REFERENCES penyakit(id_penyakit)," +
                    "FOREIGN KEY (diagnosis_type, id_gejala) REFERENCES gejala(diagnosis_type, id_gejala))");
        }
    }

    private static void seedKnowledgeBaseIfNeeded() throws SQLException {
        boolean shouldSeed = false;
        int expectedQuestionCount = getExpectedQuestionCount();
        int expectedDiseaseCount = getExpectedDiseaseCount();

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM gejala");
            if (rs.next() && rs.getInt(1) != expectedQuestionCount) {
                shouldSeed = true;
            }
        }

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM penyakit");
            if (rs.next() && rs.getInt(1) != expectedDiseaseCount) {
                shouldSeed = true;
            }
        }

        try (PreparedStatement pstmt = connection.prepareStatement("SELECT nama_penyakit FROM penyakit WHERE id_penyakit = '22'")) {
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next() || !"Influenza".equals(rs.getString(1))) {
                shouldSeed = true;
            }
        }

        try (PreparedStatement pstmt = connection.prepareStatement("SELECT nama_penyakit FROM penyakit WHERE id_penyakit = '33'")) {
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next() || !"Keracunan Staphylococcus aureus".equals(rs.getString(1))) {
                shouldSeed = true;
            }
        }

        if (!shouldSeed) {
            return;
        }

        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM aturan");
            stmt.executeUpdate("DELETE FROM gejala");
            stmt.executeUpdate("DELETE FROM penyakit");

            try (PreparedStatement penyakitStmt = connection.prepareStatement(
                    "INSERT INTO penyakit (id_penyakit, diagnosis_type, nama_penyakit, kategori, deskripsi, pencegahan, obat) VALUES (?, ?, ?, ?, ?, ?, ?)");
                 PreparedStatement gejalaStmt = connection.prepareStatement(
                         "INSERT INTO gejala (diagnosis_type, id_gejala, organ, nama_gejala) VALUES (?, ?, ?, ?)");
                 PreparedStatement aturanStmt = connection.prepareStatement(
                         "INSERT INTO aturan (diagnosis_type, id_penyakit, id_gejala) VALUES (?, ?, ?)") ) {

                for (String type : ExpertSystemEngine.getSupportedTypes()) {
                    for (ExpertSystemEngine.Disease disease : ExpertSystemEngine.getDiseases(type)) {
                        penyakitStmt.setString(1, disease.id());
                        penyakitStmt.setString(2, type);
                        penyakitStmt.setString(3, disease.name());
                        penyakitStmt.setString(4, disease.category());
                        penyakitStmt.setString(5, disease.description());
                        penyakitStmt.setString(6, disease.prevention());
                        penyakitStmt.setString(7, "Observasi klinis dan penanganan medis sesuai hasil diagnosis.");
                        penyakitStmt.addBatch();
                    }
                }
                penyakitStmt.executeBatch();

                for (String type : ExpertSystemEngine.getSupportedTypes()) {
                    for (ExpertSystemEngine.Question question : ExpertSystemEngine.getQuestions(type)) {
                        gejalaStmt.setString(1, type);
                        gejalaStmt.setString(2, question.id());
                        gejalaStmt.setString(3, categoryForQuestion(type, question.id()));
                        gejalaStmt.setString(4, question.text());
                        gejalaStmt.addBatch();
                    }
                }
                gejalaStmt.executeBatch();

                for (String type : ExpertSystemEngine.getSupportedTypes()) {
                    for (ExpertSystemEngine.Disease disease : ExpertSystemEngine.getDiseases(type)) {
                        Set<String> leafQuestions = ExpertSystemEngine.expandQuestionDependenciesForDisease(type, disease.id());
                        for (String questionId : leafQuestions) {
                            aturanStmt.setString(1, type);
                            aturanStmt.setString(2, disease.id());
                            aturanStmt.setString(3, questionId);
                            aturanStmt.addBatch();
                        }
                    }
                }
                aturanStmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static boolean requiresKnowledgeBaseSchemaMigration() throws SQLException {
        return !tableHasColumn("penyakit", "diagnosis_type")
                || !tableHasColumn("gejala", "diagnosis_type")
                || !tableHasColumn("aturan", "diagnosis_type");
    }

    private static boolean tableHasColumn(String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getExpectedQuestionCount() {
        int total = 0;
        for (String type : ExpertSystemEngine.getSupportedTypes()) {
            total += ExpertSystemEngine.getQuestions(type).size();
        }
        return total;
    }

    private static int getExpectedDiseaseCount() {
        int total = 0;
        for (String type : ExpertSystemEngine.getSupportedTypes()) {
            total += ExpertSystemEngine.getDiseases(type).size();
        }
        return total;
    }

    private static String categoryForQuestion(String type, String questionId) {
        if (ExpertSystemEngine.TYPE_GASTROUSUS.equals(type)) {
            return classifyGastroususQuestion(questionId);
        }

        int id = Integer.parseInt(questionId);
        if (id >= 1 && id <= 4) {
            return "Pernafasan";
        }
        if (id >= 5 && id <= 8) {
            return "Infeksi Virus";
        }
        if (id >= 9 && id <= 12) {
            return "Metabolik";
        }
        return "Kardiovaskular";
    }

    private static String classifyGastroususQuestion(String questionId) {
        int id = Integer.parseInt(questionId);
        if (id >= 1 && id <= 13) {
            return "Gastrousus";
        }
        return "Riwayat Konsumsi";
    }
}
