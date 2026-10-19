package slayertaskodds;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;
import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

final class SlayerOddsPanel extends PluginPanel
{
    private static final String SELECT_TASK = "Select a Slayer task...";
    private static final Color ACCENT = new Color(244, 207, 105);
    private static final Color MUTED = new Color(193, 175, 157);
    private static final Color BACKGROUND = new Color(27, 12, 17);
    private static final Color CARD = new Color(43, 19, 24);
    private static final Color EDGE = new Color(166, 124, 48);

    private final MonsterImages images = new MonsterImages();
    private final JLabel portrait = new JLabel("", SwingConstants.CENTER)
    {
        @Override protected void paintComponent(Graphics g)
        { paintSurface(g, this, CARD); super.paintComponent(g); }
    };

    private final JComboBox<String> monster = new JComboBox<>();
    private final JPanel resultList = new JPanel();
    private final JPanel blockEditor = new JPanel();
    private final JLabel status = new JLabel("Open Slayer rewards > Tasks to sync blocks");
    private final JLabel syncStatus = new JLabel("<html><center>Last block sync<br>Not yet synced</center></html>");
    private final JButton syncButton = new JButton("Sync open master's block list")
    {
        @Override protected void paintComponent(Graphics g)
        { paintSurface(g, this, getModel().isRollover() ? CARD.brighter() : CARD); super.paintComponent(g); }
    };
    private final JPanel requirements = roundedPanel();
    private final Map<SlayerMaster, List<JComboBox<SlayerTaskChoice>>> blockBoxes =
        new EnumMap<>(SlayerMaster.class);
    private final Set<SlayerMaster> expandedBlockMasters = new HashSet<>();
    private final BiConsumer<SlayerMaster, Integer> blockChange;
    private final Consumer<Boolean> syncRequest;
    private final JToggleButton blockToggle = roundedButton("Saved block lists");
    private final JToggleButton requirementsToggle = roundedButton("Account requirements");
    private final JToggleButton zeroToggle = roundedButton("Show 0.00% masters");
    private final JPanel zeroList = new JPanel();
    private String displayedTask;
    private boolean previouslyHiding;
    private boolean changing;
    private String lastRequirementsSnapshot;

    SlayerOddsPanel(Consumer<String> selection, BiConsumer<SlayerMaster, Integer> blockChange,
        Consumer<Boolean> syncRequest)
    {
        this.blockChange = blockChange;
        this.syncRequest = syncRequest;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

        JLabel title = new JLabel("Slayer Task Odds", SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                title.getPreferredSize().height));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        ImageIcon logo = images.forTask(null);
        if (logo != null)
            title.setIcon(new ImageIcon(logo.getImage().getScaledInstance(26, 26, java.awt.Image.SCALE_SMOOTH)));
        title.setIconTextGap(5);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        title.setForeground(ACCENT);
        add(title);
        
        JLabel subtitle = new JLabel("Compare your assignment chance", SwingConstants.CENTER);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        add(Box.createVerticalStrut(1));
        add(subtitle);

        JLabel description = new JLabel(
            "<html><center>Odds use your account levels, unlocks,<br>and each master's synced block list.</center></html>",
            SwingConstants.CENTER);
        description.setAlignmentX(CENTER_ALIGNMENT);
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        description.setForeground(new Color(225, 211, 190));
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        add(Box.createVerticalStrut(5));
        add(description);
        add(Box.createVerticalStrut(7));

        add(sectionHeading("SLAYER TASK", true));
        monster.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        monster.setPreferredSize(new Dimension(210, 30));
        monster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        monster.setAlignmentX(CENTER_ALIGNMENT);
        monster.setBackground(CARD);
        monster.setBorder(new RoundedBorder(EDGE));
        monster.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()
        {
            @Override public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean focused)
            {
                g.setColor(CARD);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override public void paintCurrentValue(Graphics graphics, java.awt.Rectangle bounds, boolean focused)
            {
                Object selected = monster.getSelectedItem();
                JLabel value = new JLabel(selected == null ? SELECT_TASK : selected.toString(), SwingConstants.CENTER);
                value.setOpaque(true);
                value.setBackground(CARD);
                value.setForeground(new Color(246, 230, 198));
                value.setFont(monster.getFont());
                value.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                currentValuePane.paintComponent(graphics, value, monster,
                    bounds.x, bounds.y, bounds.width, bounds.height, true);
            }

            @Override protected JButton createArrowButton()
            {
                return new javax.swing.plaf.basic.BasicArrowButton(SwingConstants.SOUTH,
                    CARD, CARD, ACCENT, CARD);
            }
        });
        // Installing the combo UI may restore its default background.
        monster.setBackground(CARD);
        monster.setForeground(new Color(246, 230, 198));
        monster.addItem(SELECT_TASK);
        monster.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBackground(isSelected ? CARD.brighter() : CARD);
                label.setForeground(new Color(246, 230, 198));
                // The arrow occupies the right edge of the closed combo box; offset only
                // its selected value so it appears centered in the full control.
                if (index < 0)
                    label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                return label;
            }
        });
        monster.addActionListener(event -> {
            if (!changing && monster.getSelectedItem() != null)
            {
                String choice = monster.getSelectedItem().toString();
                selection.accept(SELECT_TASK.equals(choice) ? null : choice);
            }
        });
        add(monster);
        add(Box.createVerticalStrut(4));
        portrait.setAlignmentX(CENTER_ALIGNMENT);
        portrait.setPreferredSize(new Dimension(210, 70));
        portrait.setMinimumSize(new Dimension(0, 70));
        portrait.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        portrait.setOpaque(false);
        portrait.setBackground(CARD);
        portrait.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(EDGE), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        portrait.setIcon(images.forTask(null));
        portrait.getAccessibleContext().setAccessibleName("Selected Slayer task portrait");
        add(portrait);
        add(Box.createVerticalStrut(2));
        status.setForeground(new Color(193, 175, 157));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setAlignmentX(CENTER_ALIGNMENT);
        status.setPreferredSize(new Dimension(210, 20));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        status.setText("Open a Slayer task list to sync");
        add(status);
        syncStatus.setForeground(MUTED);
        syncStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        syncStatus.setHorizontalAlignment(SwingConstants.CENTER);
        syncStatus.setAlignmentX(CENTER_ALIGNMENT);
        syncStatus.setPreferredSize(new Dimension(148, 32));
        syncStatus.setMinimumSize(new Dimension(0, 32));
        syncStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        add(Box.createVerticalStrut(2));
        syncButton.setAlignmentX(CENTER_ALIGNMENT);
        syncButton.setOpaque(false);
        syncButton.setContentAreaFilled(false);
        syncButton.setForeground(ACCENT);
        syncButton.setBorder(new RoundedBorder(EDGE));
        syncButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 19));
        syncButton.setText("↻");
        syncButton.setPreferredSize(new Dimension(28, 23));
        syncButton.setMinimumSize(new Dimension(28, 23));
        syncButton.setMaximumSize(new Dimension(28, 23));
        syncButton.getAccessibleContext().setAccessibleName("Sync open Slayer master's block list");
        syncButton.setToolTipText("Read and save the block slots from the currently open Slayer rewards task list.");
        syncButton.addActionListener(event -> this.syncRequest.accept(true));
        JPanel syncRow = new JPanel();
        syncRow.setLayout(new BoxLayout(syncRow, BoxLayout.X_AXIS));
        syncRow.setOpaque(false);
        syncRow.setAlignmentX(CENTER_ALIGNMENT);
        // Balance the refresh button and its gap so the text stays centered in the panel.
        syncRow.add(Box.createHorizontalStrut(31));
        syncRow.add(syncStatus);
        syncRow.add(Box.createHorizontalStrut(3));
        syncRow.add(syncButton);
        syncRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        add(syncRow);
        add(Box.createVerticalStrut(5));

        add(sectionHeading("BEST MASTER FOR THIS TASK", true));
        JLabel explanationHint = new JLabel("0.00% cards show why", SwingConstants.CENTER);
        explanationHint.setAlignmentX(CENTER_ALIGNMENT);
        explanationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        explanationHint.setForeground(MUTED);
        explanationHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        add(explanationHint);
        resultList.setLayout(new BoxLayout(resultList, BoxLayout.Y_AXIS));
        resultList.setOpaque(false);
        resultList.setAlignmentX(CENTER_ALIGNMENT);
        resultList.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(resultList);
        styleDetailButton(zeroToggle);
        zeroToggle.setVisible(false);
        zeroList.setLayout(new BoxLayout(zeroList, BoxLayout.Y_AXIS));
        zeroList.setOpaque(false);
        zeroList.setAlignmentX(CENTER_ALIGNMENT);
        zeroList.setVisible(false);
        zeroToggle.addActionListener(event -> {
            zeroList.setVisible(zeroToggle.isSelected());
            updateZeroToggle();
            revalidate();
            repaint();
        });
        add(zeroToggle);
        add(zeroList);
        add(Box.createVerticalStrut(4));

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
        requirementsToggle.addActionListener(event -> {
            boolean expanded = requirementsToggle.isSelected();
            requirements.setVisible(expanded);
            revalidate();
            repaint();
            if (expanded)
                SwingUtilities.invokeLater(() -> {
                    if (requirements.isVisible())
                        requirements.scrollRectToVisible(new Rectangle(0, 0, 1,
                            Math.min(120, requirements.getHeight())));
                });
        });
        add(Box.createVerticalStrut(3));
        add(requirementsToggle);
        requirements.setLayout(new BoxLayout(requirements, BoxLayout.Y_AXIS));
        requirements.setAlignmentX(CENTER_ALIGNMENT);
        requirements.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        requirements.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(EDGE),
            BorderFactory.createEmptyBorder(9, 8, 9, 8)));
        requirements.setVisible(false);
        add(requirements);
        add(Box.createVerticalStrut(5));
        JLabel routeHint = new JLabel(
            "<html><center>To auto-route to the best Slayer master,<br>enable Route to best master in settings.</center></html>",
            SwingConstants.CENTER);
        routeHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        routeHint.setForeground(MUTED);
        routeHint.setAlignmentX(CENTER_ALIGNMENT);
        routeHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        routeHint.setToolTipText("Side panel settings → Route to best master. Requires the Shortest Path plugin.");
        add(routeHint);
    }

    private static void paintSurface(Graphics graphics, Component c, Color color)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
        g.dispose();
    }

    private static JPanel roundedPanel()
    {
        JPanel panel = new JPanel()
        {
            @Override protected void paintComponent(Graphics g)
            { paintSurface(g, this, getBackground()); super.paintComponent(g); }
        };
        panel.setOpaque(false);
        panel.setBackground(CARD);
        return panel;
    }

    private static JToggleButton roundedButton(String text)
    {
        return new JToggleButton(text)
        {
            @Override protected void paintComponent(Graphics g)
            {
                paintSurface(g, this, isSelected() || getModel().isRollover() ? CARD.brighter() : CARD);
                super.paintComponent(g);
            }
        };
    }

    private static final class RoundedBorder extends AbstractBorder
    {
        private final Color color;
        private RoundedBorder(Color color) { this.color = color; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
        @Override public Insets getBorderInsets(Component c, Insets insets)
        { insets.set(2, 2, 2, 2); return insets; }
        @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            g.dispose();
        }
    }

    private void styleDetailButton(JToggleButton button)
    {
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(4, 9, 4, 6));
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(210, 30));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        button.setBackground(CARD);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setForeground(ACCENT);
        button.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(EDGE), BorderFactory.createEmptyBorder(4, 7, 4, 7)));
    }

    private JLabel sectionHeading(String text, boolean centered)
    {
        JLabel heading = new JLabel(text, centered ? SwingConstants.CENTER : SwingConstants.LEFT);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        heading.setPreferredSize(new Dimension(210, 20));
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        heading.setForeground(MUTED);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 1, 5, 0));
        return heading;
    }

    private void updateRequirements(PlayerState state)
    {
        String snapshot = new TreeMap<>(state.stats) + "|" + new TreeMap<>(state.checks);
        if (snapshot.equals(lastRequirementsSnapshot)) return;
        lastRequirementsSnapshot = snapshot;
        requirements.removeAll();

        long met = state.checks.values().stream().filter(Boolean.TRUE::equals).count();
        JPanel summary = requirementCard();
        requirementLabel(summary, "ACCOUNT REQUIREMENTS", ACCENT, Font.BOLD, 12);
        JPanel summaryLine = new JPanel(new BorderLayout(8, 0));
        summaryLine.setOpaque(false);
        summaryLine.setAlignmentX(LEFT_ALIGNMENT);
        JLabel statusIcon = new JLabel(met == state.checks.size() ? "✓" : "!", SwingConstants.CENTER);
        statusIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        statusIcon.setForeground(met == state.checks.size() ? new Color(145, 211, 154) : ACCENT);
        summaryLine.add(statusIcon, BorderLayout.WEST);
        JLabel count = new JLabel("<html><b>" + met + " of " + state.checks.size()
            + " checks met</b><br><font color='#c1af9d'>Account snapshot</font></html>");
        count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        count.setForeground(new Color(246, 230, 198));
        summaryLine.add(count, BorderLayout.CENTER);
        summary.setBackground(new Color(53, 27, 28));
        summary.add(summaryLine);
        sizeRequirementCard(summary);
        requirements.add(summary);
        requirements.add(Box.createVerticalStrut(8));

        JPanel levels = requirementCard();
        requirementLabel(levels, "LEVELS", ACCENT, Font.BOLD, 12);
        for (String name : new String[] {"Combat", "Slayer", "Sailing"})
            if (state.stats.containsKey(name))
                requirementLevel(levels, name, state.stats.get(name));
        for (Map.Entry<String, Boolean> check : new TreeMap<>(state.checks).entrySet())
            if (check.getKey().contains(" at least "))
                requirementCheck(levels, check.getKey(), check.getValue());
        sizeRequirementCard(levels);
        requirements.add(levels);
        requirements.add(Box.createVerticalStrut(8));

        List<Map.Entry<String, Boolean>> questChecks = new ArrayList<>();
        List<Map.Entry<String, Boolean>> unlockChecks = new ArrayList<>();
        for (Map.Entry<String, Boolean> check : new TreeMap<>(state.checks).entrySet())
        {
            String name = check.getKey();
            if (name.endsWith(" completed") || name.endsWith(" started"))
                questChecks.add(check);
            else if (!name.contains(" at least "))
                unlockChecks.add(check);
        }
        JPanel quests = requirementGroup("QUESTS", questChecks, 4);
        JPanel unlocks = requirementGroup("UNLOCKS & ACCESS", unlockChecks, 5);
        requirements.add(quests);
        requirements.add(Box.createVerticalStrut(8));
        requirements.add(unlocks);
        requirements.add(Box.createVerticalStrut(8));
        JLabel note = new JLabel("<html><div style='width:160px;text-align:center'>Odds use these account checks and saved blocks.</div></html>", SwingConstants.CENTER);
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        note.setForeground(MUTED);
        note.setAlignmentX(CENTER_ALIGNMENT);
        requirements.add(note);
        requirements.setMaximumSize(new Dimension(Integer.MAX_VALUE, requirements.getPreferredSize().height));
        requirements.revalidate();
        requirements.repaint();
    }

    private static JPanel requirementCard()
    {
        JPanel card = roundedPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(CENTER_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(EDGE),
            BorderFactory.createEmptyBorder(9, 9, 9, 9)));
        return card;
    }

    private static void sizeRequirementCard(JPanel card)
    {
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
    }

    private JPanel requirementGroup(String title, List<Map.Entry<String, Boolean>> checks, int previewCount)
    {
        JPanel card = requirementCard();
        long complete = checks.stream().filter(entry -> Boolean.TRUE.equals(entry.getValue())).count();
        requirementLabel(card, title, ACCENT, Font.BOLD, 12);
        requirementLabel(card, complete + " of " + checks.size() + " met", MUTED, Font.PLAIN, 11);
        checks.sort((a, b) -> Boolean.compare(a.getValue(), b.getValue()));
        JPanel extra = new JPanel();
        extra.setLayout(new BoxLayout(extra, BoxLayout.Y_AXIS));
        extra.setOpaque(false);
        extra.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < checks.size(); i++)
        {
            Map.Entry<String, Boolean> check = checks.get(i);
            requirementCheck(i < previewCount ? card : extra, check.getKey(), check.getValue());
        }
        if (checks.size() > previewCount)
        {
            extra.setVisible(false);
            card.add(extra);
            JButton toggle = new JButton("Show all " + title.toLowerCase(Locale.ROOT) + " (" + checks.size() + ")");
            toggle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            toggle.setForeground(ACCENT);
            toggle.setBackground(CARD);
            toggle.setContentAreaFilled(false);
            toggle.setFocusPainted(false);
            toggle.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
            toggle.setAlignmentX(LEFT_ALIGNMENT);
            toggle.addActionListener(event -> {
                extra.setVisible(!extra.isVisible());
                toggle.setText((extra.isVisible() ? "Show fewer " : "Show all ")
                    + title.toLowerCase(Locale.ROOT) + " (" + checks.size() + ")");
                sizeRequirementCard(card);
                requirements.setMaximumSize(new Dimension(Integer.MAX_VALUE, requirements.getPreferredSize().height));
                requirements.revalidate();
                requirements.repaint();
            });
            card.add(toggle);
        }
        sizeRequirementCard(card);
        return card;
    }

    private static void requirementLevel(JPanel card, String name, int value)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel(name);
        JLabel number = new JLabel(String.valueOf(value), SwingConstants.RIGHT);
        label.setForeground(new Color(246, 230, 198));
        number.setForeground(ACCENT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        number.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        row.add(label, BorderLayout.WEST);
        row.add(number, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
        card.add(Box.createVerticalStrut(5));
    }

    private static void requirementCheck(JPanel card, String text, boolean met)
    {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel icon = new JLabel(met ? "✓" : "✕");
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        icon.setForeground(met ? new Color(145, 211, 154) : new Color(217, 149, 149));
        row.add(icon, BorderLayout.WEST);
        JLabel label = new JLabel("<html><div style='width:155px'>" + escapeHtml(text) + "</div></html>");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(new Color(246, 230, 198));
        row.add(label, BorderLayout.CENTER);
        row.setToolTipText(text);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
        card.add(Box.createVerticalStrut(5));
    }

    private static void requirementLabel(JPanel card, String text, Color color, int style, int size)
    {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setToolTipText(text);
        card.add(label);
        card.add(Box.createVerticalStrut(5));
    }

    void showLoggedOut(SlayerMaster lastSynced, long syncedAt)
    {
        resultList.removeAll();
        zeroList.removeAll();
        zeroList.setVisible(false);
        zeroToggle.setVisible(false);
        JLabel login = new JLabel("Log in to see task odds.", SwingConstants.CENTER);
        login.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        login.setAlignmentX(CENTER_ALIGNMENT);
        resultList.add(login);
        status.setText("Open a Slayer task list to sync");
        requirements.removeAll();
        lastRequirementsSnapshot = null;
        blockEditor.removeAll();
        blockBoxes.clear();
        portrait.setIcon(images.forTask(null));
        portrait.setToolTipText(null);
        showSyncStatus(lastSynced, syncedAt);
        revalidate();
        repaint();
    }

    void showSyncStatus(SlayerMaster master, long syncedAt)
    {
        showSyncStatus(master, syncedAt, null, false, false);
    }

    void showSyncStatus(SlayerMaster master, long syncedAt, String failure, boolean screenOpen, boolean syncing)
    {
        syncButton.setEnabled(screenOpen && !syncing);
        syncButton.setText(syncing ? "…" : "↻");
        if (master == null || syncedAt <= 0)
        {
            boolean informational = failure != null && (failure.startsWith("Not yet synced")
                || failure.startsWith("Saved block data may be stale"));
            syncStatus.setText(failure == null || informational
                ? "<html><center>Block list: Not yet synced</center></html>"
                : "<html><center>Sync failed<br>Open task list and retry</center></html>");
            syncStatus.setToolTipText(failure == null
                ? "Block data has not been synced yet. Open a Slayer master’s Rewards > Tasks screen and sync it."
                : "Last sync attempt failed: " + failure + ". Keep the Slayer Rewards > Tasks screen open and retry.");
            syncStatus.setForeground(failure == null || informational ? MUTED : new Color(235, 143, 132));
        }
        else
        {
            String time = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(syncedAt));
            long age = System.currentTimeMillis() - syncedAt;
            boolean stale = age > 24L * 60L * 60L * 1000L;
            String freshness = stale ? " | saved data may be stale" : "";
            boolean failed = failure != null && !failure.startsWith("Saved block data may be stale")
                && !failure.startsWith("Not yet synced");
            String marker = failed ? "Retry sync" : stale ? "Stale" : "Synced";
            syncStatus.setText("<html><center>" + master.displayName
                + "<br>" + marker + " · " + time + "</center></html>");
            syncStatus.setToolTipText("Last successful sync: " + master.displayName + " at "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(new Date(syncedAt))
                + freshness + (failure == null ? "" : ". Latest sync failed: " + failure));
            syncStatus.setForeground(failure == null && !stale ? MUTED : new Color(235, 184, 114));
        }
        revalidate();
        repaint();
    }

    void showResults(List<String> allMonsters, String selected, SlayerMaster detected,
        List<SlayerMaster> masters, List<SlayerOdds.Result> results, PlayerState state,
        boolean hideZeroChanceMasters, boolean showOddsTooltips)
    {
        changing = true;
        try
        {
            List<String> choices = new ArrayList<>(allMonsters.size() + 1);
            choices.add(SELECT_TASK);
            choices.addAll(allMonsters);
            monster.setModel(new DefaultComboBoxModel<>(choices.toArray(new String[0])));
            monster.setSelectedItem(selected == null ? SELECT_TASK : selected);
        }
        finally { changing = false; }

        portrait.setIcon(images.forTask(selected));
        portrait.setToolTipText(selected == null ? null : selected + " — representative task image");
        portrait.getAccessibleContext().setAccessibleName(selected == null
            ? "Slayer Task Odds icon" : selected + " task portrait");

        status.setText(detected == null ? "Open a Slayer task list to sync" : "Detected: " + detected.displayName);
        resultList.removeAll();
        zeroList.removeAll();
        if (!java.util.Objects.equals(displayedTask, selected) || previouslyHiding != hideZeroChanceMasters)
            zeroToggle.setSelected(false);
        displayedTask = selected;
        previouslyHiding = hideZeroChanceMasters;
        int visibleRows = 0;
        if (selected == null)
        {
            JLabel prompt = new JLabel("Choose a task to compare masters.", SwingConstants.CENTER);
            prompt.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            prompt.setForeground(MUTED);
            prompt.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(prompt);
        }
        else if (results.stream().noneMatch(result -> result.chance() > 0))
        {
            LinkedHashSet<String> missing = new LinkedHashSet<>();
            for (SlayerOdds.Result result : results)
                if (result.explanation().startsWith("Requirements not met: "))
                {
                    String reason = result.explanation().substring("Requirements not met: ".length());
                    if (reason.endsWith(".")) reason = reason.substring(0, reason.length() - 1);
                    for (String item : reason.split(", ")) missing.add(item);
                }
            if (!missing.isEmpty())
            {
                JPanel notice = requirementCard();
                requirementLabel(notice, "WHY NO MASTER IS AVAILABLE", ACCENT, Font.BOLD, 11);
                int shown = 0;
                for (String item : missing)
                {
                    if (shown++ == 3) break;
                    requirementCheck(notice, item, false);
                }
                if (missing.size() > 3)
                    requirementLabel(notice, "+ " + (missing.size() - 3) + " more requirements", MUTED, Font.PLAIN, 11);
                sizeRequirementCard(notice);
                resultList.add(notice);
                resultList.add(Box.createVerticalStrut(5));
            }
        }
        for (int i = 0; i < masters.size(); i++)
        {
            SlayerOdds.Result result = results.get(i);
            boolean displaysAsZero = result.chance() * 100.0 < 0.005;
            JPanel destination = hideZeroChanceMasters && displaysAsZero ? zeroList : resultList;
            JPanel row = roundedPanel();
            row.setLayout(new BorderLayout(0, 3));
            row.setOpaque(false);
            row.setBackground(i == 0 ? new Color(65, 35, 25) : CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(i == 0 ? ACCENT : EDGE),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
            row.setPreferredSize(new Dimension(210, 48));

            JPanel titleRow = new JPanel();
            titleRow.setLayout(new BorderLayout(6, 0));
            titleRow.setOpaque(false);
            boolean highlighted = !displaysAsZero && visibleRows == 0;
            if (destination == resultList) visibleRows++;
            row.setBackground(highlighted ? new Color(65, 35, 25) : CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(highlighted ? ACCENT : EDGE),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
            JLabel master = new JLabel(masters.get(i).displayName);
            master.setForeground(highlighted ? ACCENT : new Color(246, 230, 198));
            master.setFont(new Font(Font.SANS_SERIF, highlighted ? Font.BOLD : Font.PLAIN, 12));
            master.setHorizontalAlignment(SwingConstants.LEFT);
            titleRow.add(master, BorderLayout.CENTER);
            JLabel chance = new JLabel(String.format("%.2f%%", result.chance() * 100.0), SwingConstants.RIGHT);
            chance.setForeground(highlighted ? ACCENT : new Color(244, 207, 105));
            chance.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            titleRow.add(chance, BorderLayout.EAST);
            row.add(titleRow, BorderLayout.NORTH);

            String brief = briefExplanation(result.explanation());
            String detailText = displaysAsZero ? brief : result.formula() + "  |  " + result.count + " eligible";
            JLabel detail = new JLabel(detailText, SwingConstants.CENTER);
            detail.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            detail.setForeground(MUTED);
            detail.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            detail.setHorizontalAlignment(SwingConstants.CENTER);
            detail.setAlignmentX(CENTER_ALIGNMENT);
            row.add(detail, BorderLayout.CENTER);
            String tooltip = result.explanation() + " Weight calculation: " + result.formula()
                + "; " + result.count + " eligible tasks.";
            if (!showOddsTooltips) tooltip = null;
            row.setToolTipText(tooltip);
            titleRow.setToolTipText(tooltip);
            master.setToolTipText(tooltip);
            detail.setToolTipText(tooltip);
            chance.setToolTipText(tooltip);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            row.setAlignmentX(CENTER_ALIGNMENT);
            destination.add(row);
            destination.add(Box.createVerticalStrut(3));
        }
        if (selected != null && visibleRows == 0)
        {
            JLabel empty = new JLabel("No masters above 0.00%.", SwingConstants.CENTER);
            empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(empty);
        }

        zeroToggle.setVisible(hideZeroChanceMasters && zeroList.getComponentCount() > 0);
        zeroList.setVisible(zeroToggle.isVisible() && zeroToggle.isSelected());
        updateZeroToggle();

        updateRequirements(state);
        revalidate();
        repaint();
    }

    private void updateZeroToggle()
    {
        zeroToggle.setText((zeroToggle.isSelected() ? "Hide" : "Show")
            + " 0.00% masters (" + zeroList.getComponentCount() / 2 + ")");
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String briefExplanation(String explanation)
    {
        if (explanation.startsWith("Blocked")) return "Blocked in saved list";
        if (explanation.startsWith("Requirements not met")) return "Missing requirements";
        if (explanation.startsWith("This master never assigns")) return "Never assigned by this master";
        if (explanation.startsWith("This task is available")) return "Available; rounds to 0.00%";
        return "No eligible task weight";
    }

    void editBlocks(Map<SlayerMaster, List<String>> savedBlocks)
    {
        changing = true;
        try
        {
            blockEditor.removeAll();
            blockBoxes.clear();
            if (savedBlocks == null || savedBlocks.isEmpty())
            {
                blockToggle.setText("Saved block lists");
                blockToggle.setEnabled(false);
                return;
            }
            blockToggle.setEnabled(true);
            blockToggle.setText("Saved block lists");
            blockToggle.setToolTipText("Expand a master to view or change its saved block slots");
            blockEditor.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(EDGE),
                BorderFactory.createEmptyBorder(6, 5, 6, 5)));
            for (SlayerMaster master : SlayerMaster.values())
            {
                List<String> selectedBlocks = savedBlocks.getOrDefault(master, new ArrayList<>());
                JToggleButton masterToggle = roundedButton((expandedBlockMasters.contains(master) ? "▼  " : "▶  ")
                    + master.displayName);
                styleDetailButton(masterToggle);
                masterToggle.setHorizontalAlignment(SwingConstants.LEFT);
                JPanel slots = new JPanel();
                slots.setLayout(new BoxLayout(slots, BoxLayout.Y_AXIS));
                slots.setOpaque(false);
                slots.setBorder(BorderFactory.createEmptyBorder(3, 7, 6, 3));
                slots.setVisible(expandedBlockMasters.contains(master));
                List<JComboBox<SlayerTaskChoice>> masterBoxes = new ArrayList<>();
                for (int i = 0; i < BlockSync.SLOT_COUNT; i++)
                {
                    JLabel label = new JLabel(i == 6 ? "Diary slot" : "Slot " + (i + 1));
                    label.setForeground(MUTED);
                    label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                    slots.add(label);
                    JComboBox<SlayerTaskChoice> box = new JComboBox<>(SlayerTaskChoice.values());
                    box.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    box.setBackground(CARD);
                    box.setForeground(ACCENT);
                    box.setSelectedItem(SlayerTaskChoice.fromStoredValue(
                        i < selectedBlocks.size() ? selectedBlocks.get(i) : null));
                    final int slot = i;
                    box.addActionListener(event -> { if (!changing) blockChange.accept(master, slot); });
                    box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
                    masterBoxes.add(box);
                    slots.add(box);
                    slots.add(Box.createVerticalStrut(4));
                }
                blockBoxes.put(master, masterBoxes);
                masterToggle.addActionListener(event -> {
                    boolean expanded = masterToggle.isSelected();
                    slots.setVisible(expanded);
                    if (expanded) expandedBlockMasters.add(master);
                    else expandedBlockMasters.remove(master);
                    masterToggle.setText((expanded ? "▼  " : "▶  ") + master.displayName);
                    revalidate();
                    repaint();
                });
                blockEditor.add(masterToggle);
                blockEditor.add(slots);
            }
        }
        finally { changing = false; revalidate(); repaint(); }
    }

    List<String> blockValues(SlayerMaster master)
    {
        List<String> values = new ArrayList<>();
        for (JComboBox<SlayerTaskChoice> box : blockBoxes.getOrDefault(master, new ArrayList<>()))
            values.add(String.valueOf(box.getSelectedItem()));
        return values;
    }
}
