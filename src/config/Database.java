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
            stmt.execute("CREATE TABLE IF NOT EXISTS penyakit (" +
                    "id_penyakit TEXT PRIMARY KEY," +
                    "nama_penyakit TEXT," +
                    "kategori TEXT," +
                    "deskripsi TEXT," +
                    "pencegahan TEXT," +
                    "obat TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS gejala (" +
                    "id_gejala TEXT PRIMARY KEY," +
                    "organ TEXT," +
                    "nama_gejala TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS aturan (" +
                    "id_penyakit TEXT," +
                    "id_gejala TEXT," +
                    "PRIMARY KEY (id_penyakit, id_gejala)," +
                    "FOREIGN KEY (id_penyakit) REFERENCES penyakit(id_penyakit)," +
                    "FOREIGN KEY (id_gejala) REFERENCES gejala(id_gejala))");

            stmt.execute("CREATE TABLE IF NOT EXISTS history (" +
                    "id_history INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nama_history TEXT," +
                    "tanggal TEXT," +
                    "hasil_diagnosis TEXT," +
                    "persentase REAL," +
                    "gejala_dipilih TEXT)");
        }
    }

    private static void seedKnowledgeBaseIfNeeded() throws SQLException {
        boolean shouldSeed = false;

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM gejala");
            if (rs.next() && rs.getInt(1) != ExpertSystemEngine.getQuestions().size()) {
                shouldSeed = true;
            }
        }

        try (PreparedStatement pstmt = connection.prepareStatement("SELECT nama_penyakit FROM penyakit WHERE id_penyakit = '22'")) {
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next() || !"Influenza".equals(rs.getString(1))) {
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
                    "INSERT INTO penyakit (id_penyakit, nama_penyakit, kategori, deskripsi, pencegahan, obat) VALUES (?, ?, ?, ?, ?, ?)");
                 PreparedStatement gejalaStmt = connection.prepareStatement(
                         "INSERT INTO gejala (id_gejala, organ, nama_gejala) VALUES (?, ?, ?)");
                 PreparedStatement aturanStmt = connection.prepareStatement(
                         "INSERT INTO aturan (id_penyakit, id_gejala) VALUES (?, ?)") ) {

                for (ExpertSystemEngine.Disease disease : ExpertSystemEngine.getDiseases()) {
                    penyakitStmt.setString(1, disease.id());
                    penyakitStmt.setString(2, disease.name());
                    penyakitStmt.setString(3, disease.category());
                    penyakitStmt.setString(4, disease.description());
                    penyakitStmt.setString(5, disease.prevention());
                    penyakitStmt.setString(6, "Observasi klinis dan penanganan medis sesuai hasil diagnosis.");
                    penyakitStmt.addBatch();
                }
                penyakitStmt.executeBatch();

                for (ExpertSystemEngine.Question question : ExpertSystemEngine.getQuestions()) {
                    gejalaStmt.setString(1, question.id());
                    gejalaStmt.setString(2, categoryForQuestion(question.id()));
                    gejalaStmt.setString(3, question.text());
                    gejalaStmt.addBatch();
                }
                gejalaStmt.executeBatch();

                for (ExpertSystemEngine.Disease disease : ExpertSystemEngine.getDiseases()) {
                    Set<String> leafQuestions = ExpertSystemEngine.expandQuestionDependenciesForDisease(disease.id());
                    for (String questionId : leafQuestions) {
                        aturanStmt.setString(1, disease.id());
                        aturanStmt.setString(2, questionId);
                        aturanStmt.addBatch();
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

    private static String categoryForQuestion(String questionId) {
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
}
