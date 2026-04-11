package form;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class UIStyle {
    // Colors based on requirement: "exact dark blue for headers"
    public static final Color COLOR_HEADER = new Color(0, 51, 102);
    public static final Color COLOR_HEADER_TEXT = Color.WHITE;
    public static final Color COLOR_BACKGROUND = new Color(210, 214, 219);
    public static final Color COLOR_PRIMARY = new Color(210, 214, 219);
    public static final Color COLOR_ACCENT = new Color(100, 140, 180);
    public static final Color COLOR_ROW_ALT = new Color(245, 245, 245);
    public static final Color COLOR_THEORY_BACKGROUND = new Color(255, 252, 210);
    public static final Color COLOR_THEORY_TITLE = new Color(141, 76, 14);
    public static final Color COLOR_THEORY_LINE_TOP = new Color(255, 196, 0);
    public static final Color COLOR_THEORY_LINE_BOTTOM = new Color(125, 197, 0);
    
    // Fonts
    public static final Font FONT_TITLE = new Font("Tahoma", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 18);
    public static final Font FONT_LABEL = new Font("Tahoma", Font.PLAIN, 12);
    public static final Font FONT_BUTTON = new Font("Tahoma", Font.PLAIN, 12);
    public static final Font FONT_THEORY_TITLE = new Font("SansSerif", Font.BOLD, 34);

    public static void styleHeaderPanel(JPanel panel, String title) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(COLOR_HEADER);
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 46));

        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(FONT_HEADER);
        label.setForeground(COLOR_HEADER_TEXT);
        label.setBorder(new EmptyBorder(0, 10, 0, 10));
        panel.add(label, BorderLayout.CENTER);
    }

    public static void styleTheoryHeaderPanel(JPanel panel, String title) {
        panel.setLayout(new BorderLayout());
        panel.setBackground(COLOR_THEORY_BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 18, 12, 18));

        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(FONT_THEORY_TITLE);
        label.setForeground(COLOR_THEORY_TITLE);
        label.setBorder(new EmptyBorder(0, 0, 12, 0));
        panel.add(label, BorderLayout.NORTH);

        JPanel lines = new JPanel();
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setOpaque(false);

        JPanel topLine = new JPanel();
        topLine.setBackground(COLOR_THEORY_LINE_TOP);
        topLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        topLine.setPreferredSize(new Dimension(0, 6));

        JPanel bottomLine = new JPanel();
        bottomLine.setBackground(COLOR_THEORY_LINE_BOTTOM);
        bottomLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        bottomLine.setPreferredSize(new Dimension(0, 6));

        lines.add(topLine);
        lines.add(Box.createVerticalStrut(4));
        lines.add(bottomLine);
        panel.add(lines, BorderLayout.CENTER);
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(21);
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(200, 200, 200));
        table.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(238, 238, 238));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Tahoma", Font.PLAIN, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 22));

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : COLOR_ROW_ALT);
                }
                return c;
            }
        });
    }

    public static void styleButton(JButton button, String iconEmoji) {
        button.setFont(FONT_BUTTON);
        button.setPreferredSize(new Dimension(90, 26));
        if (button.getText().toLowerCase().contains("simpan") || button.getText().toLowerCase().contains("save")) {
            button.setBackground(null);
            button.setForeground(Color.BLACK);
            button.setIcon(createSaveIcon());
        } else if (button.getText().toLowerCase().contains("hapus") || button.getText().toLowerCase().contains("delete")) {
            button.setBackground(null);
            button.setForeground(Color.BLACK);
            button.setIcon(createDeleteIcon());
        } else if (button.getText().toLowerCase().contains("tambah") || button.getText().toLowerCase().contains("add")) {
            button.setBackground(null);
            button.setForeground(Color.BLACK);
        } else if (button.getText().toLowerCase().contains("clear") || button.getText().toLowerCase().contains("batal")) {
            button.setBackground(null);
            button.setForeground(Color.BLACK);
        }
        button.setIconTextGap(4);
        button.setMargin(new Insets(2, 6, 2, 6));
        button.setFocusPainted(false);
    }

    private static Icon createSaveIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(70, 110, 170));
                g2.fill(new RoundRectangle2D.Double(x, y, 14, 14, 2, 2));
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 3, y + 2, 8, 4);
                g2.fillRect(x + 4, y + 8, 6, 4);
                g2.setColor(new Color(40, 60, 90));
                g2.drawRect(x, y, 13, 13);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return 14; }

            @Override
            public int getIconHeight() { return 14; }
        };
    }

    private static Icon createDeleteIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(230, 90, 90));
                g2.fillOval(x, y, 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillRect(x + 3, y + 6, 8, 2);
                g2.setColor(new Color(170, 50, 50));
                g2.drawOval(x, y, 13, 13);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return 14; }

            @Override
            public int getIconHeight() { return 14; }
        };
    }
    
    public static void applyPadding(JComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
            component.getBorder(),
            new EmptyBorder(10, 10, 10, 10)
        ));
    }
}
