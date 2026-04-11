package form;

import javax.swing.*;
import java.awt.*;

public class MainForm extends JFrame {
    private JDesktopPane desktopPane;

    public MainForm() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Sistem Pakar identifikasi infeksi dan non-fiksi dengan metode Rule-Based dan Bobot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        desktopPane = new JDesktopPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(67, 108, 146);
                Color color2 = new Color(44, 78, 113);
                GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);

                g2d.setColor(new Color(255, 255, 255, 22));
                g2d.fillOval(-120, -60, 500, 900);
                g2d.fillOval(150, -140, 700, 760);
                g2d.fillOval(width - 480, -100, 620, 900);

                g2d.setColor(new Color(255, 255, 255, 18));
                g2d.fillRoundRect(width / 2 - 20, height / 2 - 40, 12, 80, 8, 8);
                g2d.fillRoundRect(width / 2 + 8, height / 2 - 40, 12, 80, 8, 8);

                g2d.setColor(new Color(255, 255, 255, 12));
                for (int i = 0; i < width; i += 180) {
                    g2d.drawLine(i, 0, i - 120, height);
                }
            }
        };
        setContentPane(desktopPane);
        setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Styling for Menu and MenuItems would usually require a custom UI or individual styling
        // For simplicity with standard Swing, we use basic menus
        
        JMenuItem itemPenyakit = new JMenuItem("Data Penyakit");
        styleMenuItem(itemPenyakit);
        itemPenyakit.addActionListener(e -> {
            DataPenyakitForm form = new DataPenyakitForm();
            desktopPane.add(form);
            form.setVisible(true);
        });
        JMenuItem itemGejala = new JMenuItem("Data Gejala");
        styleMenuItem(itemGejala);
        itemGejala.addActionListener(e -> {
            DataGejalaForm form = new DataGejalaForm();
            desktopPane.add(form);
            form.setVisible(true);
        });
        JMenu menuMaster = createMenu("Data Master");
        menuMaster.add(itemPenyakit);
        menuMaster.add(itemGejala);

        JMenuItem itemGejalaPenyakit = new JMenuItem("Data Gejala Penyakit");
        styleMenuItem(itemGejalaPenyakit);
        itemGejalaPenyakit.addActionListener(e -> {
            DataGejalaPenyakitForm form = new DataGejalaPenyakitForm();
            desktopPane.add(form);
            form.setVisible(true);
        });
        JMenu menuRule = createMenu("Rule");
        menuRule.add(itemGejalaPenyakit);

        JMenuItem itemMulaiKonsultasi = new JMenuItem("Mulai Konsultasi");
        styleMenuItem(itemMulaiKonsultasi);
        itemMulaiKonsultasi.addActionListener(e -> {
            KonsultasiForm form = new KonsultasiForm();
            desktopPane.add(form);
            form.setVisible(true);
        });
        JMenuItem itemHistory = new JMenuItem("Riwayat Konsultasi");
        styleMenuItem(itemHistory);
        itemHistory.addActionListener(e -> {
            HistoryForm form = new HistoryForm();
            desktopPane.add(form);
            form.setVisible(true);
        });
        JMenu menuKonsultasi = createMenu("Konsultasi");
        menuKonsultasi.add(itemMulaiKonsultasi);
        menuKonsultasi.add(itemHistory);
        menuBar.add(menuMaster);
        menuBar.add(menuRule);
        menuBar.add(menuKonsultasi);

        return menuBar;
    }

    private JMenu createMenu(String title) {
        JMenu menu = new JMenu(title);
        menu.setForeground(Color.BLACK);
        menu.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return menu;
    }

    private void styleMenuItem(JMenuItem item) {
        item.setForeground(Color.BLACK);
        item.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }
}
