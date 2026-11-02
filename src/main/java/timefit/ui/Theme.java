package timefit.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import timefit.model.Schedule.*;

/** Shared Swing appearance and Korean display labels. */
public final class Theme {
    private Theme() { }
    public static final Color BG = new Color(245,247,251), BLUE = new Color(37,99,235), INK = new Color(30,41,59), MUTED = new Color(100,116,139);
    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
        String family = java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains("맑은 고딕") ? "맑은 고딕" : "NanumGothic";
        Font font = new Font(family, Font.PLAIN, 14);
        for (Object key : UIManager.getDefaults().keySet()) if (UIManager.get(key) instanceof Font) UIManager.put(key, font);
        UIManager.put("Panel.background", BG); UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.margin", new Insets(6,8,6,8)); UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("Button.background", Color.WHITE); UIManager.put("Button.foreground", INK);
        UIManager.put("TabbedPane.selected", Color.WHITE);
    }
    public static JButton button(String text, Runnable action) {
        JButton b = new JButton(text); b.setFocusPainted(false); b.setMargin(new Insets(8,13,8,13));
        b.addActionListener(e -> action.run()); return b;
    }
    public static JButton primary(String text, Runnable action) {
        JButton b = button(text, action); b.setBackground(BLUE); b.setForeground(Color.WHITE); b.setOpaque(true); return b;
    }
    public static JLabel label(String text, int size, boolean bold) {
        JLabel l = new JLabel(text); l.setFont(l.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, (float)size)); return l;
    }
    public static JPanel card() {
        JPanel p = new JPanel(); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(222,228,237)), new EmptyBorder(16,16,16,16))); return p;
    }
    public static String duration(long minutes) { return (minutes / 60) + "시간 " + (minutes % 60) + "분"; }
    public static String display(Object value) {
        if (value instanceof Status s) return switch(s) { case PLANNED -> "예정"; case IN_PROGRESS -> "진행 중"; case COMPLETED -> "완료"; };
        if (value instanceof Importance i) return switch(i) { case LOW -> "낮음"; case NORMAL -> "보통"; case HIGH -> "높음"; };
        if (value instanceof Kind k) return k == Kind.ALL_DAY ? "하루종일" : "일반";
        return String.valueOf(value);
    }
    public static <T> JComboBox<T> combo(T[] values) {
        JComboBox<T> c = new JComboBox<>(values);
        c.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            return super.getListCellRendererComponent(list, value == null ? "전체" : display(value), index, selected, focus);
        }}); return c;
    }
    public static String html(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    public static void error(Component owner, Exception e) { JOptionPane.showMessageDialog(owner, e.getMessage(), "확인해 주세요", JOptionPane.WARNING_MESSAGE); }
}
