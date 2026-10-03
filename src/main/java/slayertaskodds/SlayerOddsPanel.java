package slayertaskodds;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

final class SlayerOddsPanel extends PluginPanel
{
    private static final Color ACCENT = new Color(255, 190, 80);
    private static final Color MUTED = new Color(174, 178, 185);
    private static final Color CARD = new Color(42, 45, 51);

    private final JComboBox<String> monster = new JComboBox<>();
    private final JPanel resultList = new JPanel();
    private final JPanel blockEditor = new JPanel();
    private final JLabel status = new JLabel("Open Slayer rewards > Tasks to sync blocks");
    private final JEditorPane requirements = new JEditorPane("text/html", "");
    private final List<JComboBox<SlayerTaskChoice>> blockBoxes = new ArrayList<>();
    private final BiConsumer<SlayerMaster, Integer> blockChange;
    private final JToggleButton blockToggle = new JToggleButton("Saved block list");
    private final JToggleButton requirementsToggle = new JToggleButton("Account requirements");
    private boolean changing;

    SlayerOddsPanel(Consumer<String> selection, BiConsumer<SlayerMaster, Integer> blockChange)
    {
        this.blockChange = blockChange;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //setBorder(BorderFactory.createEmptyBorder(12, 10, 14, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Slayer Task Odds", SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                title.getPreferredSize().height));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(ACCENT);
        add(title);
        
        JLabel subtitle = new JLabel("Compare your assignment chance", SwingConstants.CENTER);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(9f));
        add(Box.createVerticalStrut(3));
        add(subtitle);

        JLabel description = new JLabel(
            "<html><center>Odds use your account levels, unlocks,<br>and each master's synced block list.</center></html>",
            SwingConstants.CENTER);
        description.setAlignmentX(CENTER_ALIGNMENT);
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        description.setForeground(new Color(205, 208, 214));
        description.setFont(description.getFont().deriveFont(11f));
        add(Box.createVerticalStrut(5));
        add(description);
        add(Box.createVerticalStrut(12));

        add(sectionHeading("SLAYER TASK", true));
        monster.setPreferredSize(new Dimension(210, 30));
        monster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        monster.setAlignmentX(CENTER_ALIGNMENT);
        monster.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                // The arrow occupies the right edge of the closed combo box; offset only
                // its selected value so it appears centered in the full control.
                if (index < 0)
                    label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                return label;
            }
        });
        monster.addActionListener(event -> {
            if (!changing && monster.getSelectedItem() != null)
                selection.accept(monster.getSelectedItem().toString());
        });
        add(monster);
        add(Box.createVerticalStrut(7));
        status.setForeground(new Color(144, 196, 220));
        status.setFont(status.getFont().deriveFont(11f));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setAlignmentX(CENTER_ALIGNMENT);
        status.setPreferredSize(new Dimension(210, 34));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        status.setText("<html><center>Open Slayer rewards &gt; Tasks<br>to sync blocks</center></html>");
        add(status);
        add(Box.createVerticalStrut(14));

        add(sectionHeading("BEST MASTER FOR THIS TASK", true));
        resultList.setLayout(new BoxLayout(resultList, BoxLayout.Y_AXIS));
        resultList.setOpaque(false);
        resultList.setAlignmentX(CENTER_ALIGNMENT);
        resultList.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(resultList);
        add(Box.createVerticalStrut(10));

        styleDetailButton(blockToggle);
        blockToggle.setAlignmentX(CENTER_ALIGNMENT);
        blockToggle.setHorizontalAlignment(SwingConstants.CENTER);
        blockToggle.addActionListener(event -> blockEditor.setVisible(blockToggle.isSelected()));
        add(blockToggle);
        blockEditor.setLayout(new BoxLayout(blockEditor, BoxLayout.Y_AXIS));
        blockEditor.setOpaque(false);
        blockEditor.setVisible(false);
        add(blockEditor);

        styleDetailButton(requirementsToggle);
        requirementsToggle.setAlignmentX(CENTER_ALIGNMENT);
        requirementsToggle.setHorizontalAlignment(SwingConstants.CENTER);
        requirementsToggle.addActionListener(event -> requirements.setVisible(requirementsToggle.isSelected()));
        add(Box.createVerticalStrut(5));
        add(requirementsToggle);
        requirements.setEditable(false);
        requirements.setOpaque(false);
        requirements.setForeground(Color.WHITE);
        requirements.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 4));
        requirements.setVisible(false);
        add(requirements);
    }

    private void styleDetailButton(JToggleButton button)
    {
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(4, 9, 4, 6));
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(210, 30));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    }

    private JLabel sectionHeading(String text, boolean centered)
    {
        JLabel heading = new JLabel(text, centered ? SwingConstants.CENTER : SwingConstants.LEFT);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        heading.setPreferredSize(new Dimension(210, 24));
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        heading.setForeground(MUTED);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 1, 5, 0));
        return heading;
    }

    void showLoggedOut()
    {
        resultList.removeAll();
        JLabel login = new JLabel("Log in to see task odds.", SwingConstants.CENTER);
        login.setAlignmentX(CENTER_ALIGNMENT);
        resultList.add(login);
        status.setText("<html><center>Open Slayer rewards &gt; Tasks<br>to sync blocks</center></html>");
        requirements.setText("");
        blockEditor.removeAll();
        blockBoxes.clear();
        revalidate();
        repaint();
    }

    void showResults(List<String> allMonsters, String selected, SlayerMaster detected,
        List<SlayerMaster> masters, List<SlayerOdds.Result> results, PlayerState state)
    {
        changing = true;
        try
        {
            monster.setModel(new DefaultComboBoxModel<>(allMonsters.toArray(new String[0])));
            monster.setSelectedItem(selected);
        }
        finally { changing = false; }

        status.setText(detected == null
            ? "<html><center>Open Slayer rewards &gt; Tasks<br>to sync blocks</center></html>"
            : "<html><center>Detected: " + detected.displayName + "</center></html>");
        resultList.removeAll();
        for (int i = 0; i < masters.size(); i++)
        {
            SlayerOdds.Result result = results.get(i);
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setOpaque(true);
            row.setBackground(i == 0 ? new Color(66, 55, 36) : CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(i == 0 ? new Color(128, 94, 47) : new Color(58, 61, 68)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            row.setPreferredSize(new Dimension(210, 65));

            JLabel master = new JLabel(masters.get(i).displayName);
            master.setForeground(i == 0 ? ACCENT : Color.WHITE);
            master.setFont(master.getFont().deriveFont(i == 0 ? Font.BOLD : Font.PLAIN, 12f));
            master.setHorizontalAlignment(SwingConstants.CENTER);
            master.setAlignmentX(CENTER_ALIGNMENT);
            row.add(master);

            JLabel detail = new JLabel("<html><small>" + result.formula() + "  |  " + result.count + " tasks</small></html>");
            detail.setForeground(MUTED);
            detail.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            detail.setHorizontalAlignment(SwingConstants.CENTER);
            detail.setAlignmentX(CENTER_ALIGNMENT);
            row.add(detail);

            JLabel chance = new JLabel(String.format("%.1f%%", result.chance() * 100.0), SwingConstants.CENTER);
            chance.setForeground(i == 0 ? ACCENT : new Color(220, 224, 230));
            chance.setFont(chance.getFont().deriveFont(Font.BOLD, 15f));
            chance.setAlignmentX(CENTER_ALIGNMENT);
            row.add(chance);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
            row.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(row);
            resultList.add(Box.createVerticalStrut(4));
        }

        StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif;font-size:10px'>");
        html.append("<b>Levels</b><br>");
        new TreeMap<>(state.stats).forEach((name, level) ->
            html.append(name).append(": ").append(level).append("<br>"));
        html.append("<br><b>Quests, unlocks and access</b><br>");
        new TreeMap<>(state.checks).forEach((name, met) -> html
            .append("<font color='").append(met ? "#91d39a" : "#d99595").append("'>")
            .append(met ? "&#10003; " : "&#10007; ").append(name).append("</font><br>"));
        html.append("</body></html>");
        requirements.setText(html.toString());
        requirements.setBackground(UIManager.getColor("Panel.background"));
        revalidate();
        repaint();
    }

    void editBlocks(SlayerMaster master, List<String> allTasks, List<String> selectedBlocks)
    {
        changing = true;
        try
        {
            blockEditor.removeAll();
            blockBoxes.clear();
            if (master == null)
            {
                blockToggle.setText("Saved block list");
                blockToggle.setEnabled(false);
                return;
            }
            blockToggle.setEnabled(true);
            blockToggle.setText("Saved " + master.displayName + " block list");
            blockEditor.setBorder(BorderFactory.createEmptyBorder(6, 3, 4, 3));
            for (int i = 0; i < BlockSync.SLOT_COUNT; i++)
            {
                JLabel label = new JLabel(i == 6 ? "Diary slot" : "Slot " + (i + 1));
                label.setForeground(MUTED);
                label.setFont(label.getFont().deriveFont(10f));
                blockEditor.add(label);
                JComboBox<SlayerTaskChoice> box = new JComboBox<>(SlayerTaskChoice.values());
                box.setSelectedItem(SlayerTaskChoice.fromStoredValue(i < selectedBlocks.size() ? selectedBlocks.get(i) : null));
                final int slot = i;
                box.addActionListener(event -> { if (!changing) blockChange.accept(master, slot); });
                box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
                blockBoxes.add(box);
                blockEditor.add(box);
                blockEditor.add(Box.createVerticalStrut(4));
            }
        }
        finally { changing = false; revalidate(); repaint(); }
    }

    List<String> blockValues()
    {
        List<String> values = new ArrayList<>();
        for (JComboBox<SlayerTaskChoice> box : blockBoxes)
            values.add(String.valueOf(box.getSelectedItem()));
        return values;
    }
}
