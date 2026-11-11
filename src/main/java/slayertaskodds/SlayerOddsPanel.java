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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
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
    private static final String SELECT_TASK = "Select a Slayer Task...";
    private static final String PAGE_FONT = new Font("Georgia", Font.PLAIN, 14).getFamily().equals("Dialog")
        ? Font.SERIF : "Georgia";
    private Color INK = new Color(250, 235, 204);
    private Color ACCENT = new Color(242, 190, 80);
    private Color MUTED = new Color(211, 186, 160);
    private Color BACKGROUND = new Color(48, 8, 20);
    private Color CARD = BACKGROUND;
    private Color ACTIVE_PAPER = new Color(78, 27, 36);
    private Color EDGE = new Color(177, 130, 58);
    private final boolean maroonTheme;
    private final boolean cardLayout;
    private final boolean archiveTheme;

    private final MonsterImages images = new MonsterImages();
    private final BufferedImage parchment = loadParchment();
    private final JLabel portrait = new JLabel("", SwingConstants.CENTER)
    {
        @Override protected void paintComponent(Graphics g)
        { paintSurface(g, this, CARD); super.paintComponent(g); }
    };

    private final JLabel taskName = new JLabel(SELECT_TASK);
    private JLabel taskHeading;
    private final JComboBox<String> monster = new JComboBox<>();
    private final JPanel resultList = new JPanel();
    private final JLabel approximationHint = new JLabel("<html><center>~ Blocks need checking;<br>hover to see why.</center></html>", SwingConstants.CENTER);
    private final JLabel explanationHint = new JLabel("Hover over 0.00% for details", SwingConstants.CENTER);
    private final JPanel mortimerList = new JPanel();
    private final JPanel mortimerSection = new JPanel();
    private final JPanel mortimerContent = new JPanel();
    private final JToggleButton mortimerToggle = roundedButton("Mortimer");
    private final JToggleButton modifierToggle = roundedButton("Task modifiers");
    private final JPanel modifierDetails = new JPanel();
    private final JPanel modifierGroup = new JPanel();
    private final JPanel blockEditor = new JPanel();
    private final JLabel status = new JLabel("Open Slayer rewards > Tasks to sync blocks");
    private final JLabel syncStatus = new JLabel("<html><center>Last block sync<br>Not yet synced</center></html>");
    private final JPanel syncBalance = new JPanel();
    private final JButton syncButton = new JButton("Sync open master's block list")
    {
        @Override protected void paintComponent(Graphics g)
        { paintSurface(g, this, getModel().isRollover() ? ACTIVE_PAPER : CARD); super.paintComponent(g); }
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
    private Boolean lastMortimerExpandPreference;
    private boolean mortimerExpandable = true;
    private boolean previouslyHiding;
    private boolean zeroTooltipsEnabled;
    private boolean changing;
    private String lastRequirementsSnapshot;
    private String lastBlockEditorSnapshot;

    SlayerOddsPanel(Consumer<String> selection, BiConsumer<SlayerMaster, Integer> blockChange,
        Consumer<Boolean> syncRequest, SlayerPanelTheme theme, SlayerPanelLayout layout)
    {
        archiveTheme = layout == SlayerPanelLayout.ARCHIVE;
        maroonTheme = archiveTheme || theme == SlayerPanelTheme.MAROON_GOLD;
        cardLayout = layout == SlayerPanelLayout.CARDS;
        if (!maroonTheme)
        {
            INK = new Color(61, 32, 22);
            ACCENT = new Color(118, 43, 25);
            MUTED = new Color(91, 61, 40);
            BACKGROUND = new Color(227, 194, 143);
            CARD = BACKGROUND;
            ACTIVE_PAPER = new Color(211, 175, 122);
            EDGE = new Color(164, 123, 81);
        }
        if (cardLayout)
        {
            if (maroonTheme)
            {
                INK = new Color(246, 230, 198);
                ACCENT = new Color(244, 207, 105);
                MUTED = new Color(193, 175, 157);
                EDGE = new Color(166, 124, 48);
            }
            BACKGROUND = maroonTheme ? new Color(27, 12, 17) : new Color(227, 212, 184);
            CARD = maroonTheme ? new Color(43, 19, 24) : new Color(237, 224, 198);
            ACTIVE_PAPER = maroonTheme ? new Color(65, 28, 34) : new Color(216, 195, 161);
        }
        if (archiveTheme)
        {
            INK = new Color(224, 235, 213);
            ACCENT = new Color(173, 211, 150);
            MUTED = new Color(164, 184, 169);
            BACKGROUND = new Color(18, 35, 32);
            CARD = BACKGROUND;
            ACTIVE_PAPER = new Color(41, 69, 57);
            EDGE = new Color(110, 151, 118);
        }
        requirements.setBackground(CARD);
        this.blockChange = blockChange;
        this.syncRequest = syncRequest;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));

        JLabel title = new JLabel("Slayer Task Odds", SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                title.getPreferredSize().height));
        title.setFont(pageFont(Font.BOLD, 20));
        JLabel emblem = new JLabel(scalesIcon());
        emblem.setAlignmentX(CENTER_ALIGNMENT);
        add(emblem);
        add(Box.createVerticalStrut(4));
        title.setIconTextGap(5);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        title.setForeground(INK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title);
        add(Box.createVerticalStrut(2));
        JPanel titleRule = new JPanel()
        {
            @Override protected void paintComponent(Graphics graphics)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    Color clear = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 0);
                    g.setPaint(new java.awt.LinearGradientPaint(0, 0, getWidth(), 0,
                        new float[] {0f, 0.3f, 0.7f, 1f}, new Color[] {clear, ACCENT, ACCENT, clear}));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                finally { g.dispose(); }
            }
        };
        titleRule.setOpaque(false);
        titleRule.setPreferredSize(new Dimension(125, 2));
        titleRule.setMaximumSize(new Dimension(125, 2));
        titleRule.setAlignmentX(CENTER_ALIGNMENT);
        add(titleRule);

        monster.setFont(pageFont(Font.BOLD, 13));
        monster.setPreferredSize(new Dimension(210, 30));
        monster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        monster.setAlignmentX(CENTER_ALIGNMENT);
        monster.setBackground(CARD);
        monster.setBorder(new RoundedBorder(EDGE));
        monster.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()
        {
            @Override public void paint(Graphics g, javax.swing.JComponent component)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                try
                {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int width = cardLayout ? component.getWidth() - 1 : Math.min(component.getWidth() - 1, 125);
                    g2.setColor(cardLayout ? CARD : archiveTheme ? ACTIVE_PAPER : maroonTheme ? new Color(83, 25, 35) : new Color(204, 173, 125));
                    g2.fillRoundRect(0, 2, width, component.getHeight() - 5, 5, 5);
                    g2.setColor(EDGE);
                    g2.drawRoundRect(0, 2, width, component.getHeight() - 5, 5, 5);
                    g2.setFont(monster.getFont());
                    g2.setColor(ACCENT);
                    Object selected = monster.getSelectedItem();
                    String text = selected == null || SELECT_TASK.equals(selected) ? "Choose a task" : "Change task";
                    java.awt.FontMetrics metrics = g2.getFontMetrics();
                    int baseline = (component.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                    int textX = Math.max(6, (width - metrics.stringWidth(text)) / 2);
                    g2.drawString(text, textX, baseline);
                    if (monster.hasFocus()) g2.drawLine(textX, baseline + 2, textX + metrics.stringWidth(text), baseline + 2);
                }
                finally { g2.dispose(); }
            }

            @Override protected void installDefaults()
            {
                super.installDefaults();
                squareButton = false;
            }

            @Override protected javax.swing.plaf.basic.ComboPopup createPopup()
            {
                return new javax.swing.plaf.basic.BasicComboPopup(comboBox)
                {
                    @Override protected java.awt.Rectangle computePopupBounds(int x, int y, int width, int height)
                    {
                        return super.computePopupBounds(x, y, Math.max(width, 210), height);
                    }
                };
            }

            @Override protected JButton createArrowButton()
            {
                JButton arrow = new JButton();
                arrow.setPreferredSize(new Dimension(0, 0));
                arrow.setBorder(BorderFactory.createEmptyBorder());
                return arrow;
            }
        });
        monster.setOpaque(false);
        monster.setPrototypeDisplayValue("Choose a task ▾");
        monster.getAccessibleContext().setAccessibleName("Change Slayer task");
        monster.addItem(SELECT_TASK);
        monster.setRenderer(new DefaultListCellRenderer()
        {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean selected, boolean focused)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    index < 0 ? "Change task ▾" : value, index, selected, focused);
                label.setBackground(index >= 0 && selected ? EDGE : BACKGROUND);
                label.setForeground(index < 0 ? ACCENT : INK);
                label.setFont(monster.getFont());
                label.setBorder(BorderFactory.createEmptyBorder(0, index < 0 ? 0 : 4, 0, 0));
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
        JPanel taskRow = new JPanel(new BorderLayout(4, 0))
        {
            @Override public Dimension getMaximumSize()
            {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        taskRow.setOpaque(false);
        JPanel taskInfo = new JPanel();
        taskInfo.setLayout(new BoxLayout(taskInfo, BoxLayout.Y_AXIS));
        taskInfo.setOpaque(false);
        taskInfo.setBorder(BorderFactory.createEmptyBorder(13, 0, 12, 0));
        taskHeading = sectionHeading("No task selected", false);
        taskHeading.setForeground(INK);
        taskHeading.setFont(pageFont(Font.BOLD, 13));
        taskHeading.setAlignmentX(LEFT_ALIGNMENT);
        taskInfo.add(taskHeading);
        taskInfo.add(Box.createVerticalStrut(3));
        monster.setBorder(BorderFactory.createEmptyBorder());
        taskName.setFont(pageFont(Font.BOLD, 17));
        taskName.setForeground(INK);
        taskName.setAlignmentX(LEFT_ALIGNMENT);
        taskInfo.add(taskName);
        taskInfo.add(Box.createVerticalStrut(5));
        monster.setAlignmentX(LEFT_ALIGNMENT);
        taskInfo.add(monster);
        taskRow.add(taskInfo, BorderLayout.CENTER);
        portrait.setPreferredSize(cardLayout ? new Dimension(210, 70) : new Dimension(48, 84));
        portrait.setBackground(BACKGROUND);
        if (cardLayout) portrait.setBorder(new RoundedBorder(EDGE));
        ImageIcon emptyLogo = images.forTask(null);
        if (emptyLogo != null)
            portrait.setIcon(new ImageIcon(emptyLogo.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
        taskRow.add(portrait, BorderLayout.EAST);
        add(Box.createVerticalStrut(10));
        add(taskRow);
        JPanel taskRule = new JPanel();
        taskRule.setBackground(EDGE);
        taskRule.setPreferredSize(new Dimension(210, 1));
        taskRule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        taskRule.setAlignmentX(CENTER_ALIGNMENT);
        add(taskRule);
        status.setForeground(MUTED);
        status.setFont(pageFont(Font.PLAIN, 12));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setAlignmentX(CENTER_ALIGNMENT);
        status.setPreferredSize(new Dimension(210, 20));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        status.setText("Open Slayer rewards to sync blocks");

        syncStatus.setForeground(MUTED);
        syncStatus.setFont(pageFont(Font.PLAIN, 12));
        syncStatus.setHorizontalAlignment(SwingConstants.CENTER);
        syncStatus.setAlignmentX(CENTER_ALIGNMENT);
        syncStatus.setPreferredSize(new Dimension(148, 26));
        syncStatus.setMinimumSize(new Dimension(0, 26));
        syncStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        add(Box.createVerticalStrut(2));
        syncButton.setAlignmentX(CENTER_ALIGNMENT);
        syncButton.setOpaque(false);
        syncButton.setContentAreaFilled(false);
        syncButton.setForeground(ACCENT);
        syncButton.setBorder(new RoundedBorder(EDGE));
        syncButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        syncButton.setText("↻");
        syncButton.setPreferredSize(new Dimension(28, 23));
        syncButton.setMinimumSize(new Dimension(28, 23));
        syncButton.setMaximumSize(new Dimension(28, 23));
        syncButton.setVisible(false);
        syncButton.getAccessibleContext().setAccessibleName("Sync open Slayer master's block list");
        syncButton.setToolTipText("Read and save the block slots from the currently open Slayer rewards task list.");
        syncButton.addActionListener(event -> this.syncRequest.accept(true));
        JPanel syncRow = new JPanel(new BorderLayout(3, 0));
        syncRow.setOpaque(false);
        syncRow.setAlignmentX(CENTER_ALIGNMENT);
        syncStatus.setHorizontalAlignment(SwingConstants.CENTER);
        syncBalance.setOpaque(false);
        syncBalance.setPreferredSize(new Dimension(28, 23));
        syncBalance.setVisible(false);
        syncRow.add(syncBalance, BorderLayout.WEST);
        syncRow.add(syncStatus, BorderLayout.CENTER);
        syncRow.add(syncButton, BorderLayout.EAST);
        syncRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        add(Box.createVerticalStrut(5));

        JLabel assignment = sectionHeading("Assignment chance", false);
        assignment.setFont(pageFont(Font.BOLD, 17));
        assignment.setForeground(INK);
        assignment.setBorder(BorderFactory.createEmptyBorder());
        int assignmentHeight = assignment.getFontMetrics(assignment.getFont()).getHeight() + 2;
        assignment.setPreferredSize(new Dimension(210, assignmentHeight));
        assignment.setMaximumSize(new Dimension(Integer.MAX_VALUE, assignmentHeight));
        approximationHint.setFont(pageFont(Font.PLAIN, 11));
        approximationHint.setForeground(MUTED);
        approximationHint.setAlignmentX(CENTER_ALIGNMENT);
        approximationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        approximationHint.setVisible(false);
        add(approximationHint);
        add(assignment);
        add(Box.createVerticalStrut(5));
        explanationHint.setAlignmentX(CENTER_ALIGNMENT);
        explanationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        explanationHint.setForeground(MUTED);
        explanationHint.setFont(pageFont(cardLayout ? Font.PLAIN : Font.ITALIC, cardLayout ? 12 : 11));
        if (!cardLayout)
        {
            explanationHint.setBorder(BorderFactory.createEmptyBorder(5, 0, 7, 0));
            explanationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        }
        explanationHint.setVisible(false);

        resultList.setLayout(new BoxLayout(resultList, BoxLayout.Y_AXIS));
        resultList.setOpaque(false);
        resultList.setAlignmentX(CENTER_ALIGNMENT);
        resultList.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        add(resultList);
        styleDetailButton(zeroToggle);
        zeroToggle.setVisible(false);
        explanationHint.setVisible(false);
        zeroList.setLayout(new BoxLayout(zeroList, BoxLayout.Y_AXIS));
        zeroList.setOpaque(false);
        zeroList.setAlignmentX(CENTER_ALIGNMENT);
        zeroList.setVisible(false);
        zeroToggle.addActionListener(event -> {
            zeroList.setVisible(zeroToggle.isSelected());
            explanationHint.setVisible(zeroToggle.isSelected() && zeroTooltipsEnabled);
            updateZeroToggle();
            revalidate();
            repaint();
        });
        add(zeroToggle);
        add(zeroList);
        add(explanationHint);
        mortimerSection.setLayout(new BoxLayout(mortimerSection, BoxLayout.Y_AXIS));
        mortimerSection.setOpaque(false);
        mortimerToggle.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        styleDetailButton(mortimerToggle);
        mortimerToggle.setHorizontalAlignment(SwingConstants.LEFT);
        mortimerContent.setLayout(new BoxLayout(mortimerContent, BoxLayout.Y_AXIS));
        mortimerContent.setOpaque(false);
        mortimerToggle.addActionListener(event -> {
            if (!mortimerExpandable) { mortimerToggle.setSelected(true); return; }
            mortimerContent.setVisible(mortimerToggle.isSelected());
            revalidate();
            repaint();
        });
        mortimerSection.add(mortimerToggle);
        mortimerSection.add(mortimerContent);
        add(mortimerSection);
        mortimerList.setLayout(new BoxLayout(mortimerList, BoxLayout.Y_AXIS));
        mortimerList.setOpaque(false);
        mortimerList.setAlignmentX(CENTER_ALIGNMENT);
        mortimerContent.add(mortimerList);
        styleDetailButton(modifierToggle);
        modifierToggle.setHorizontalAlignment(SwingConstants.LEFT);
        modifierToggle.setVisible(false);
        modifierToggle.addActionListener(event -> {
            modifierDetails.setVisible(modifierToggle.isSelected());
            revalidate();
            repaint();
        });
        modifierGroup.setLayout(new BoxLayout(modifierGroup, BoxLayout.Y_AXIS));
        modifierGroup.setOpaque(false);
        modifierGroup.setAlignmentX(CENTER_ALIGNMENT);
        modifierGroup.setVisible(false);
        modifierGroup.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 12, 0, 0),
            BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, EDGE),
                BorderFactory.createEmptyBorder(0, 6, 0, 0))));
        if (cardLayout) modifierToggle.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
        modifierToggle.setFont(pageFont(Font.BOLD, 12));
        modifierToggle.setPreferredSize(new Dimension(190, 28));
        modifierToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        modifierGroup.add(modifierToggle);
        modifierDetails.setLayout(new BoxLayout(modifierDetails, BoxLayout.Y_AXIS));
        modifierDetails.setOpaque(false);
        modifierDetails.setAlignmentX(CENTER_ALIGNMENT);
        modifierDetails.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        modifierDetails.setVisible(false);
        modifierGroup.add(modifierDetails);
        mortimerContent.add(modifierGroup);
        add(Box.createVerticalStrut(4));

        styleDetailButton(blockToggle);
        blockToggle.setAlignmentX(CENTER_ALIGNMENT);
        blockToggle.setHorizontalAlignment(SwingConstants.LEFT);
        blockToggle.addActionListener(event -> blockEditor.setVisible(blockToggle.isSelected()));
        add(blockToggle);
        blockEditor.setLayout(new BoxLayout(blockEditor, BoxLayout.Y_AXIS));
        blockEditor.setOpaque(false);
        blockEditor.setVisible(false);
        add(blockEditor);

        styleDetailButton(requirementsToggle);
        requirementsToggle.setAlignmentX(CENTER_ALIGNMENT);
        requirementsToggle.setHorizontalAlignment(SwingConstants.LEFT);
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
            "<html><center>Directions: enable routing<br>in plugin settings.</center></html>",
            SwingConstants.CENTER);
        routeHint.setFont(pageFont(Font.PLAIN, 12));
        routeHint.setForeground(MUTED);
        routeHint.setAlignmentX(CENTER_ALIGNMENT);
        routeHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        routeHint.setToolTipText("Side panel settings → Route to best master. Requires Shortest Path. Visit Mortimer once to record his location.");
        syncRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(syncRow);
        add(routeHint);
        add(status);
        if (cardLayout)
            arrangeLegacyLayout(title, taskInfo, taskRow, assignment, explanationHint, syncRow, routeHint);
        else if (archiveTheme)
            arrangeArchiveLayout(title, emblem, taskInfo, taskRow, assignment, syncRow, routeHint);
        JLabel themeIntro = new JLabel("Change theme in plugin settings", SwingConstants.CENTER);
        themeIntro.setFont(pageFont(Font.PLAIN, 10));
        themeIntro.setForeground(MUTED);
        themeIntro.setAlignmentX(CENTER_ALIGNMENT);
        themeIntro.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        themeIntro.setMaximumSize(new Dimension(Integer.MAX_VALUE, themeIntro.getPreferredSize().height));
        add(themeIntro, 0);
        JLabel themeHint = new JLabel(archiveTheme ? "<html><center>Theme: Fractured Archive<br>Change in Side panel settings</center></html>" : cardLayout ? "<html><center>Theme: Legacy · "
            + (maroonTheme ? "Maroon and gold" : "Parchment")
            + "<br>Change in plugin settings → Side panel</center></html>"
            : "<html><center>Theme: Scroll · " + (maroonTheme ? "Maroon &amp; gold" : "Parchment")
            + "<br>Change in Side panel settings</center></html>", SwingConstants.CENTER);
        themeHint.setAlignmentX(CENTER_ALIGNMENT);
        themeHint.setForeground(MUTED);
        themeHint.setFont(pageFont(Font.PLAIN, 11));
        themeHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        add(Box.createVerticalStrut(6));
        add(themeHint);
        JButton changes = new JButton("What's new");
        changes.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        changes.setFont(pageFont(Font.BOLD, 11));
        changes.setForeground(ACCENT);
        changes.setContentAreaFilled(false);
        changes.setFocusPainted(false);
        changes.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        changes.setAlignmentX(CENTER_ALIGNMENT);
        changes.setToolTipText("View the latest Slayer Task Odds changes");
        changes.addActionListener(event -> showReleaseNotes());
        javax.swing.Icon arrow = new javax.swing.Icon()
        {
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 10; }
            public void paintIcon(Component c, Graphics graphics, int x, int y)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(MUTED);
                    g.drawLine(x + 2, y + 5, x + 12, y + 5);
                    g.drawLine(x + 2, y + 5, x + 5, y + 2);
                    g.drawLine(x + 2, y + 5, x + 5, y + 8);
                }
                finally { g.dispose(); }
            }
        };
        JLabel clickHint = new JLabel("Click me!", arrow, SwingConstants.LEFT);
        clickHint.setFont(pageFont(Font.PLAIN, 10));
        clickHint.setForeground(MUTED);
        clickHint.setIconTextGap(3);
        JPanel changesRow = new JPanel(new BorderLayout());
        changesRow.setOpaque(false);
        changesRow.setAlignmentX(CENTER_ALIGNMENT);
        JPanel balance = new JPanel();
        balance.setOpaque(false);
        balance.setPreferredSize(clickHint.getPreferredSize());
        if (archiveTheme)
        {
            clickHint.setHorizontalAlignment(SwingConstants.CENTER);
            changesRow.add(changes, BorderLayout.CENTER);
            changesRow.add(clickHint, BorderLayout.SOUTH);
        }
        else
        {
            changesRow.add(balance, BorderLayout.WEST);
            changesRow.add(changes, BorderLayout.CENTER);
            changesRow.add(clickHint, BorderLayout.EAST);
        }
        changesRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, changes.getPreferredSize().height
            + (archiveTheme ? clickHint.getPreferredSize().height : 0)));
        add(changesRow);
    }

    private void arrangeLegacyLayout(JLabel title, JPanel taskInfo, JPanel taskRow, JLabel assignment,
        JLabel explanationHint, JPanel syncRow, JLabel routeHint)
    {
        removeAll();
        taskInfo.remove(monster);
        taskRow.remove(portrait);
        setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setForeground(ACCENT);
        ImageIcon logo = images.forTask(null);
        if (logo != null)
            title.setIcon(new ImageIcon(logo.getImage().getScaledInstance(26, 26, java.awt.Image.SCALE_SMOOTH)));
        add(title);
        JLabel subtitle = new JLabel("Compare your assignment chance", SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        add(subtitle);
        JLabel description = new JLabel("<html><center>Odds use your account levels, unlocks,<br>and each master's synced block list.</center></html>", SwingConstants.CENTER);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        description.setForeground(INK);
        description.setAlignmentX(CENTER_ALIGNMENT);
        add(Box.createVerticalStrut(5));
        add(description);
        add(Box.createVerticalStrut(7));
        JLabel taskLabel = sectionHeading("SLAYER TASK", true);
        taskLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        add(taskLabel);
        monster.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        monster.setUI(new javax.swing.plaf.basic.BasicComboBoxUI()
        {
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focused)
            { g.setColor(CARD); g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height); }
            @Override public void paintCurrentValue(Graphics g, Rectangle bounds, boolean focused)
            {
                Object selected = monster.getSelectedItem();
                JLabel value = new JLabel(selected == null ? SELECT_TASK : selected.toString(), SwingConstants.CENTER);
                value.setForeground(INK);
                value.setBackground(CARD);
                value.setOpaque(true);
                value.setFont(monster.getFont());
                currentValuePane.paintComponent(g, value, monster, bounds.x, bounds.y, bounds.width, bounds.height, true);
            }
            @Override protected JButton createArrowButton()
            {
                JButton arrow = new JButton()
                {
                    @Override protected void paintComponent(Graphics graphics)
                    {
                        Graphics2D g = (Graphics2D) graphics.create();
                        try
                        {
                            // Match the selector's value area so the transparent arrow has no visible button box.
                            g.setColor(CARD);
                            g.fillRect(0, 0, getWidth(), getHeight());
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g.setColor(isEnabled() ? ACCENT : MUTED);
                            int cx = getWidth() / 2;
                            int cy = getHeight() / 2;
                            g.fillPolygon(new int[] {cx - 5, cx + 5, cx}, new int[] {cy - 2, cy - 2, cy + 3}, 3);
                        }
                        finally { g.dispose(); }
                    }
                };
                arrow.setOpaque(false);
                arrow.setContentAreaFilled(false);
                arrow.setBorderPainted(false);
                arrow.setFocusPainted(false);
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setPreferredSize(new Dimension(24, 24));
                return arrow;
            }
        });
        monster.setBackground(CARD);
        monster.setBorder(new RoundedBorder(EDGE));
        monster.setAlignmentX(CENTER_ALIGNMENT);
        monster.setPreferredSize(new Dimension(210, 30));
        monster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(monster);
        add(Box.createVerticalStrut(4));
        portrait.setAlignmentX(CENTER_ALIGNMENT);
        portrait.setPreferredSize(new Dimension(210, 70));
        portrait.setMinimumSize(new Dimension(0, 70));
        portrait.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        add(portrait);
        add(Box.createVerticalStrut(2));
        add(status);
        syncRow.setBorder(BorderFactory.createEmptyBorder());
        syncStatus.setHorizontalAlignment(SwingConstants.CENTER);
        add(Box.createVerticalStrut(5));
        assignment.setText("BEST MASTER FOR THIS TASK");
        assignment.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        assignment.setHorizontalAlignment(SwingConstants.CENTER);
        assignment.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        assignment.setPreferredSize(new Dimension(210, 22));
        assignment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        approximationHint.setBorder(BorderFactory.createEmptyBorder(2, 2, 3, 2));
        approximationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        add(approximationHint);
        add(assignment);
        explanationHint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        explanationHint.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        explanationHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        add(resultList);
        add(zeroToggle);
        add(zeroList);
        add(explanationHint);
        add(mortimerSection);
        add(Box.createVerticalStrut(4));
        add(syncRow);
        blockToggle.setHorizontalAlignment(SwingConstants.CENTER);
        requirementsToggle.setHorizontalAlignment(SwingConstants.CENTER);
        add(blockToggle);
        add(blockEditor);
        add(Box.createVerticalStrut(3));
        add(requirementsToggle);
        add(requirements);
        add(Box.createVerticalStrut(5));
        routeHint.setText("<html><center>To auto-route to the best Slayer master,<br>enable Route to best master in settings.</center></html>");
        add(routeHint);
    }

    private void arrangeArchiveLayout(JLabel title, JLabel emblem, JPanel taskInfo, JPanel taskRow,
        JLabel assignment, JPanel syncRow, JLabel routeHint)
    {
        removeAll();
        setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        title.setText("SLAYER TASK ODDS");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        title.setForeground(INK);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        emblem.setAlignmentX(CENTER_ALIGNMENT);
        header.add(emblem);
        header.add(Box.createVerticalStrut(5));
        JLabel eyebrow = new JLabel("THE FRACTURED ARCHIVE");
        eyebrow.setForeground(ACCENT);
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(3));
        header.add(title);
        header.setAlignmentX(CENTER_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        add(header);
        add(Box.createVerticalStrut(12));

        taskRow.remove(portrait);
        taskInfo.remove(monster);
        taskInfo.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
        taskHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        taskName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        // Keep the portrait out of the text column: long task names need the full width.
        taskRow.add(portrait, BorderLayout.NORTH);
        taskRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 1, 1, EDGE),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)));
        add(taskRow);
        JPanel taskAction = new JPanel(new BorderLayout());
        taskAction.setOpaque(false);
        taskAction.setAlignmentX(CENTER_ALIGNMENT);
        taskAction.add(monster, BorderLayout.WEST);
        taskAction.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(Box.createVerticalStrut(4));
        add(taskAction);
        add(Box.createVerticalStrut(12));

        assignment.setText("MASTER ODDS");
        assignment.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        assignment.setForeground(ACCENT);
        add(approximationHint);
        add(assignment);
        add(Box.createVerticalStrut(6));
        add(resultList);
        add(zeroToggle);
        add(zeroList);
        add(explanationHint);
        add(Box.createVerticalStrut(8));
        add(mortimerSection);
        add(Box.createVerticalStrut(10));
        add(blockToggle);
        add(blockEditor);
        add(Box.createVerticalStrut(4));
        add(requirementsToggle);
        add(requirements);
        add(Box.createVerticalStrut(10));
        add(syncRow);
        add(routeHint);
        add(status);
    }

    private Font pageFont(int style, int size)
    {
        if (cardLayout || archiveTheme) return new Font(Font.SANS_SERIF, style, size);
        Font font = new Font(PAGE_FONT, style, size).deriveFont(java.awt.geom.AffineTransform.getScaleInstance(1.0, 1.12));
        java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>();
        attributes.put(java.awt.font.TextAttribute.KERNING, java.awt.font.TextAttribute.KERNING_ON);
        attributes.put(java.awt.font.TextAttribute.TRACKING, 0.005f);
        return font.deriveFont(attributes);
    }

    private void showReleaseNotes()
    {
        javax.swing.JDialog dialog = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(this),
            "Slayer Task Odds — What's new", java.awt.Dialog.ModalityType.MODELESS);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel highlights = new JLabel("<html><b>Slayer Task Odds 2.0.0</b><br><br>"
            + "• Added Mortimer and his task modifier breakdown<br>"
            + "• Character-specific block lists and clearer sync status<br>"
            + "• Per-master warnings for old or unconfirmed blocks<br>"
            + "• Master access and account requirement checks<br>"
            + "• Three panel themes, including the Fractured Archive<br>"
            + "• Parchment or maroon and gold for Legacy and Scroll<br>"
            + "• Mortimer task-choice odds and optional routing<br>"
            + "• More reliable syncing and automatic retries<br>"
            + "• Clearer layouts, portraits and performance improvements</html>");
        highlights.setForeground(INK);
        highlights.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        content.add(highlights, BorderLayout.NORTH);
        StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif;font-size:12pt;color:")
            .append(String.format("#%02x%02x%02x", INK.getRed(), INK.getGreen(), INK.getBlue()))
            .append("'>");
        for (String line : ReleaseNotes.latest().split("\n"))
        {
            if (line.equals("- Added this release-notes popup, with highlights and the complete changelist."))
                line = "- Added this release-notes popup (Hello!), with highlights and the complete changelist.";
            String clean = escapeHtml(line.replace("**", ""));
            if (line.startsWith("### ")) html.append("<br><b>").append(clean.substring(4)).append("</b><br>");
            else if (line.startsWith("## ")) html.append("<b>").append(clean.substring(3)).append("</b><br>");
            else if (line.startsWith("- ")) html.append("&#8226; ").append(clean.substring(2)).append("<br><br>");
        }
        html.append("</body></html>");
        javax.swing.JEditorPane full = new javax.swing.JEditorPane("text/html", html.toString());
        full.setEditable(false);
        full.setBackground(CARD);
        full.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(full);
        scroll.setPreferredSize(new Dimension(460, 340));
        scroll.setVisible(false);
        content.add(scroll, BorderLayout.CENTER);
        JButton expand = new JButton("Show full changelist ▾");
        expand.addActionListener(event -> {
            boolean show = !scroll.isVisible();
            scroll.setVisible(show);
            expand.setText(show ? "Hide full changelist ▴" : "Show full changelist ▾");
            dialog.pack();
        });
        content.add(expand, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    void restoreExpansion(SlayerOddsPanel previous)
    {
        expandedBlockMasters.addAll(previous.expandedBlockMasters);
        blockToggle.setSelected(previous.blockToggle.isSelected());
        blockEditor.setVisible(previous.blockToggle.isSelected());
        requirementsToggle.setSelected(previous.requirementsToggle.isSelected());
        requirements.setVisible(previous.requirementsToggle.isSelected());
        zeroToggle.setSelected(previous.zeroToggle.isSelected());
        mortimerToggle.setSelected(previous.mortimerToggle.isSelected());
        mortimerContent.setVisible(previous.mortimerContent.isVisible());
        modifierToggle.setSelected(previous.modifierToggle.isSelected());
        modifierDetails.setVisible(previous.modifierToggle.isSelected());
        lastMortimerExpandPreference = previous.lastMortimerExpandPreference;
        displayedTask = previous.displayedTask;
        previouslyHiding = previous.previouslyHiding;
    }

    private javax.swing.Icon scalesIcon()
    {
        return new javax.swing.Icon()
        {
            public int getIconWidth() { return 72; }
            public int getIconHeight() { return 55; }

            public void paintIcon(Component component, Graphics graphics, int x, int y)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    g.translate(x, y);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(ACCENT);
                    g.setStroke(new java.awt.BasicStroke(1.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    if (archiveTheme)
                    {
                        // A fractured vault seal, drawn at any display scale.
                        g.setColor(EDGE);
                        g.drawPolygon(new int[] {36, 59, 59, 36, 13, 13}, new int[] {3, 15, 40, 52, 40, 15}, 6);
                        g.drawPolygon(new int[] {36, 53, 53, 36, 19, 19}, new int[] {9, 18, 37, 46, 37, 18}, 6);
                        g.setColor(ACCENT);
                        g.drawLine(36, 12, 36, 40);
                        g.drawLine(25, 21, 47, 21);
                        g.drawLine(25, 21, 20, 32);
                        g.drawLine(25, 21, 30, 32);
                        g.drawLine(20, 32, 30, 32);
                        g.drawLine(47, 21, 42, 32);
                        g.drawLine(47, 21, 52, 32);
                        g.drawLine(42, 32, 52, 32);
                        g.drawLine(31, 42, 41, 42);
                        g.setColor(INK);
                        g.drawLine(36, 9, 33, 17);
                        g.drawLine(33, 17, 38, 23);
                        g.drawLine(38, 23, 34, 29);
                        return;
                    }
                    g.drawLine(36, 7, 36, 46);
                    g.drawLine(30, 48, 42, 48);
                    g.drawLine(27, 51, 45, 51);
                    g.drawLine(10, 14, 62, 14);
                    g.drawOval(33, 4, 6, 6);
                    for (int centre : new int[] {13, 59})
                    {
                        g.drawLine(centre, 14, centre - 9, 36);
                        g.drawLine(centre, 14, centre + 9, 36);
                        g.drawLine(centre - 10, 36, centre + 10, 36);
                        g.drawArc(centre - 10, 30, 20, 13, 180, 180);
                    }
                }
                finally { g.dispose(); }
            }
        };
    }

    private static BufferedImage loadParchment()
    {
        try (InputStream stream = SlayerOddsPanel.class.getResourceAsStream("/slayertaskodds/parchment.png"))
        {
            return stream == null ? null : ImageIO.read(stream);
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        if (archiveTheme)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            try
            {
                int w = getWidth(), h = getHeight();
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(31, 56, 48), w, 220, BACKGROUND));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(139, 187, 146, 35));
                g.drawLine(4, 4, w - 5, 4);
                g.drawLine(4, 4, 4, h - 5);
                g.drawLine(w - 5, 4, w - 5, h - 5);
                g.drawLine(4, h - 5, w - 5, h - 5);
                for (int y = 16; y < h; y += 120)
                {
                    g.drawLine(4, y, 9, y + 5);
                    g.drawLine(9, y + 5, 4, y + 10);
                    g.drawLine(w - 5, y, w - 10, y + 5);
                    g.drawLine(w - 10, y + 5, w - 5, y + 10);
                }
            }
            finally { g.dispose(); }
        }
        else if (!cardLayout && parchment != null)
        {
            // Trim the bright source-image edge so it does not stretch into a band at the top.
            graphics.drawImage(parchment, 0, 0, getWidth(), getHeight(),
                0, Math.min(5, parchment.getHeight() - 1), parchment.getWidth(), parchment.getHeight(), null);
            if (maroonTheme)
            {
                graphics.setColor(new Color(43, 1, 15, 232));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    @Override
    public void doLayout()
    {
        // RuneLite supplies its own text hints on Swing components, including newly built rows.
        applyPanelTextHints(this);
        super.doLayout();
    }

    private static final javax.swing.plaf.LabelUI PAGE_LABEL_UI = new javax.swing.plaf.basic.BasicLabelUI()
    {
        @Override protected void paintEnabledText(JLabel label, Graphics graphics, String text, int x, int y)
        {
            Graphics2D g = (Graphics2D) graphics.create();
            try
            {
                g.setFont(label.getFont());
                g.setColor(label.getForeground());
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.drawString(text, x, y);
            }
            finally { g.dispose(); }
        }
    };

    private static void applyPanelTextHints(java.awt.Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof javax.swing.JComponent)
            {
                javax.swing.JComponent component = (javax.swing.JComponent) child;
                if (component.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING)
                    != RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    component.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (component instanceof JLabel && component.getUI() != PAGE_LABEL_UI)
                {
                    JLabel label = (JLabel) component;
                    label.setUI(PAGE_LABEL_UI);
                }
                applyPanelTextHints(component);
            }
        }
    }

    @Override
    public void paint(Graphics graphics)
    {
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paint(g);
        }
        finally { g.dispose(); }
    }

    private void paintSurface(Graphics graphics, Component c, Color color)
    {
        if (archiveTheme && c == portrait) return;
        if (!cardLayout && !archiveTheme && CARD.equals(color)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
        g.dispose();
    }

    private JPanel roundedPanel()
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

    private JToggleButton roundedButton(String text)
    {
        return new JToggleButton(text)
        {
            @Override protected void paintComponent(Graphics g)
            {
                paintSurface(g, this, getModel().isRollover() || (!cardLayout && isSelected()) ? ACTIVE_PAPER : CARD);
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
        button.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        button.setFont(pageFont(Font.BOLD, 13));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(4, 9, 4, 6));
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(210, 34));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setBackground(CARD);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setForeground(cardLayout ? ACCENT : INK);
        if (button == blockToggle || button == requirementsToggle || button == zeroToggle || button == mortimerToggle || button == modifierToggle)
        {
            javax.swing.Icon disclosure = new javax.swing.Icon()
            {
                public int getIconWidth() { return 6; }
                public int getIconHeight() { return 6; }
                public void paintIcon(Component c, Graphics g, int x, int y)
                {
                    if (button == mortimerToggle && !mortimerExpandable) return;
                    g.setColor(MUTED);
                    if (button.isSelected())
                        g.fillPolygon(new int[]{x, x + 6, x + 3}, new int[]{y, y, y + 4}, 3);
                    else
                        g.fillPolygon(new int[]{x, x, x + 4}, new int[]{y, y + 6, y + 3}, 3);
                }
            };
            button.setIcon(disclosure);
        }
        javax.swing.border.Border border;
        if (cardLayout) border = new RoundedBorder(EDGE);
        else border = new AbstractBorder()
        {
            @Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                try
                {
                    if (button == blockToggle || button == requirementsToggle)
                    {
                        g.setColor(EDGE);
                        g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
                    }
                    if (button.isSelected())
                    {
                        g.setColor(ACCENT);
                        g.fillRect(x, y + 5, 2, Math.max(0, height - 10));
                    }
                }
                finally { g.dispose(); }
            }
        };
        button.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(4, 7, 4, 7)));
    }

    private JLabel sectionHeading(String text, boolean centered)
    {
        JLabel heading = new JLabel(text, centered ? SwingConstants.CENTER : SwingConstants.LEFT);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        heading.setPreferredSize(new Dimension(210, 20));
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        heading.setForeground(MUTED);
        heading.setFont(pageFont(Font.PLAIN, 13));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 1, 5, 0));
        return heading;
    }

    private void updateRequirements(PlayerState state)
    {
        String snapshot = new TreeMap<>(state.stats) + "|" + new TreeMap<>(state.checks)
            + "|slayerCapePerk=" + state.slayerCapePerkActive + "|choices=" + state.mortimerChoices
            + "|mortimerAccess=" + state.mortimerAccess + "|mortimerCompleted=" + state.mortimerTasksCompleted
            + "|width=" + getWidth();
        if (snapshot.equals(lastRequirementsSnapshot)) return;
        lastRequirementsSnapshot = snapshot;
        requirements.removeAll();

        long met = state.checks.values().stream().filter(Boolean.TRUE::equals).count();
        JPanel summary = requirementCard();
        requirementLabel(summary, "ACCOUNT CHECKS", ACCENT, Font.BOLD, 12);
        JPanel summaryLine = new JPanel(new BorderLayout(8, 0));
        summaryLine.setOpaque(false);
        summaryLine.setAlignmentX(LEFT_ALIGNMENT);
        JLabel statusIcon = met == state.checks.size() ? checkMark(true) : new JLabel("!", SwingConstants.CENTER);
        statusIcon.setFont(pageFont(Font.BOLD, 22));
        statusIcon.setForeground(met == state.checks.size()
            ? (maroonTheme ? new Color(145, 211, 154) : new Color(55, 95, 52)) : ACCENT);
        summaryLine.add(statusIcon, BorderLayout.WEST);
        JLabel count = new JLabel("<html><b>" + met + " of " + state.checks.size()
            + " checks met</b><br><font color='" + (maroonTheme ? "#d3baa0" : "#635e4d") + "'>Account snapshot</font></html>");
        count.setFont(pageFont(Font.PLAIN, 12));
        count.setForeground(INK);
        summaryLine.add(count, BorderLayout.CENTER);
        summary.setBackground(archiveTheme ? ACTIVE_PAPER : maroonTheme ? new Color(69, 22, 31) : new Color(228, 219, 195));
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

        JPanel capePerk = requirementCard();
        JLabel capeHeading = new JLabel("SLAYER CAPE PERK", SwingConstants.CENTER);
        capeHeading.setFont(pageFont(Font.BOLD, 12));
        capeHeading.setForeground(ACCENT);
        capeHeading.setAlignmentX(CENTER_ALIGNMENT);
        capePerk.add(capeHeading);
        capePerk.add(Box.createVerticalStrut(5));

        JLabel capeStatusIcon = checkMark(state.slayerCapePerkActive);
        capeStatusIcon.setAlignmentX(CENTER_ALIGNMENT);
        capePerk.add(capeStatusIcon);

        String capeStatus = state.slayerCapePerkActive
            ? "10% previous-task repeat chance active"
            : "Keep a Slayer cape in your inventory or wear it for a 10% previous-task repeat chance";
        JLabel capeStatusLabel = wrappedRequirement(capeStatus, true);
        capeStatusLabel.setFont(pageFont(Font.PLAIN, 12));
        capeStatusLabel.setForeground(INK);
        capeStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
        capeStatusLabel.setToolTipText(capeStatus);
        capePerk.add(capeStatusLabel);

        JLabel capeNote = wrappedRequirement("The repeat offer applies at the same master, if the previous task is available and not blocked. Odds assume you accept it.", true);
        capeNote.setFont(pageFont(Font.PLAIN, 12));
        capeNote.setForeground(MUTED);
        capeNote.setAlignmentX(CENTER_ALIGNMENT);
        capePerk.add(Box.createVerticalStrut(4));
        capePerk.add(capeNote);
        sizeRequirementCard(capePerk);
        requirements.add(capePerk);
        requirements.add(Box.createVerticalStrut(8));

        JPanel mortimer = requirementCard();
        requirementLabel(mortimer, "MORTIMER", ACCENT, Font.BOLD, 12);
        int slayer = state.stats.getOrDefault("Slayer", 0);
        requirementCheck(mortimer, "Mortimer access", state.mortimerAccess);
        requirementCheck(mortimer, "70 Slayer", slayer >= 70);
        requirementCheck(mortimer, "100 Combat or 99 Slayer", state.stats.getOrDefault("Combat", 0) >= 100 || slayer >= 99);
        requirementLevel(mortimer, "Tasks completed", state.mortimerTasksCompleted);
        requirementLabel(mortimer, "Tasks per offer: " + state.mortimerChoices, MUTED, Font.PLAIN, 12);
        sizeRequirementCard(mortimer);
        requirements.add(mortimer);
        requirements.add(Box.createVerticalStrut(8));

        List<Map.Entry<String, Boolean>> questChecks = new ArrayList<>();
        List<Map.Entry<String, Boolean>> unlockChecks = new ArrayList<>();
        for (Map.Entry<String, Boolean> check : new TreeMap<>(state.checks).entrySet())
        {
            String name = check.getKey();
            if (name.endsWith(" completed") || name.endsWith(" started"))
                questChecks.add(check);
            else if (!name.contains(" at least ") && !name.equals("Mortimer access"))
                unlockChecks.add(check);
        }
        JPanel quests = requirementGroup("QUESTS", questChecks, 4);
        JPanel unlocks = requirementGroup("UNLOCKS & ACCESS", unlockChecks, 5);
        requirements.add(quests);
        requirements.add(Box.createVerticalStrut(8));
        requirements.add(unlocks);
        requirements.add(Box.createVerticalStrut(8));
        JLabel note = wrappedRequirement("Odds use these account checks and saved blocks.", true);
        note.setFont(pageFont(Font.PLAIN, 12));
        note.setForeground(MUTED);
        note.setAlignmentX(CENTER_ALIGNMENT);
        requirements.add(note);
        requirements.setMaximumSize(new Dimension(Integer.MAX_VALUE, requirements.getPreferredSize().height));
        requirements.revalidate();
        requirements.repaint();
    }

    private JPanel requirementCard()
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
        requirementLabel(card, complete + " of " + checks.size() + " met", MUTED, Font.PLAIN, 12);
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
            JButton toggle = new JButton("Show all (" + checks.size() + ")");
            toggle.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            toggle.setFont(pageFont(Font.BOLD, 12));
            toggle.setForeground(ACCENT);
            toggle.setBackground(CARD);
            toggle.setContentAreaFilled(false);
            toggle.setFocusPainted(false);
            toggle.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
            toggle.setAlignmentX(LEFT_ALIGNMENT);
            toggle.addActionListener(event -> {
                extra.setVisible(!extra.isVisible());
                toggle.setText(extra.isVisible() ? "Show fewer" : "Show all (" + checks.size() + ")");
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

    private void requirementLevel(JPanel card, String name, int value)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel(name);
        JLabel number = new JLabel(String.valueOf(value), SwingConstants.RIGHT);
        label.setForeground(INK);
        number.setForeground(ACCENT);
        label.setFont(pageFont(Font.PLAIN, 12));
        number.setFont(pageFont(Font.BOLD, 12));
        row.add(label, BorderLayout.WEST);
        row.add(number, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
        card.add(Box.createVerticalStrut(5));
    }

    private JLabel wrappedRequirement(String text, boolean centered)
    {
        // Reserve the panel and card insets, scrollbar and the check column.
        int sidebar = getWidth() > 0 ? getWidth() : 220;
        int pixels = Math.max(90, sidebar - (centered ? 77 : 92));
        JLabel label = new JLabel("<html><div style='width:" + (pixels * 3 / 4)
            + "px;text-align:" + (centered ? "center" : "left") + "'>" + escapeHtml(text) + "</div></html>");
        label.setHorizontalAlignment(centered ? SwingConstants.CENTER : SwingConstants.LEFT);
        return label;
    }

    private JLabel checkMark(boolean met)
    {
        JLabel mark = new JLabel(new javax.swing.Icon()
        {
            public int getIconWidth() { return 12; }
            public int getIconHeight() { return 12; }
            public void paintIcon(Component c, Graphics graphics, int x, int y)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(met ? (maroonTheme ? new Color(145, 211, 154) : new Color(47, 94, 51))
                    : (maroonTheme ? new Color(235, 151, 143) : new Color(143, 55, 46)));
                g.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                if (met)
                    g.drawPolyline(new int[]{1, 4, 10}, new int[]{6, 9, 2}, 3);
                else
                {
                    g.drawLine(3, 3, 9, 9);
                    g.drawLine(9, 3, 3, 9);
                }
                g.dispose();
            }
        });
        mark.getAccessibleContext().setAccessibleName(met ? "Met" : "Not met");
        return mark;
    }

    private void requirementCheck(JPanel card, String text, boolean met)
    {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(checkMark(met), BorderLayout.WEST);
        JLabel label = wrappedRequirement(text, false);
        label.setFont(pageFont(Font.PLAIN, 12));
        label.setForeground(INK);
        row.add(label, BorderLayout.CENTER);
        row.setToolTipText(text);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
        card.add(Box.createVerticalStrut(5));
    }

    private static void requirementLabel(JPanel card, String text, Color color, int style, int size)
    {
        JLabel label = new JLabel(text);
        label.setFont(new Font(PAGE_FONT, style, size));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setToolTipText(text);
        card.add(label);
        card.add(Box.createVerticalStrut(5));
    }

    void showLoggedOut(SlayerMaster lastSynced, long syncedAt)
    {
        resultList.removeAll();
        approximationHint.setVisible(false);
        zeroList.removeAll();
        mortimerList.removeAll();
        mortimerList.setVisible(false);
        modifierToggle.setVisible(false);
        modifierGroup.setVisible(false);
        modifierDetails.setVisible(false);
        modifierDetails.removeAll();
        mortimerSection.setVisible(false);
        zeroList.setVisible(false);
        zeroToggle.setVisible(false);
        explanationHint.setVisible(false);
        taskName.setText("<html><div style='width:" + (archiveTheme ? 125 : 100) + "px'>" + escapeHtml(SELECT_TASK) + "</div></html>");
        taskHeading.setText("No task selected");
        taskName.setToolTipText(null);
        changing = true;
        try { monster.setSelectedItem(SELECT_TASK); }
        finally { changing = false; }
        portrait.setPreferredSize(cardLayout ? new Dimension(210, 70) : new Dimension(48, 84));
        ImageIcon emptyLogo = images.forTask(null);
        portrait.setIcon(emptyLogo == null ? null : new ImageIcon(emptyLogo.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
        JLabel login = new JLabel("Log in to see task odds.", SwingConstants.CENTER);
        login.setFont(pageFont(Font.PLAIN, 12));
        login.setForeground(MUTED);
        login.setAlignmentX(CENTER_ALIGNMENT);
        resultList.add(login);
        status.setText("Open Slayer rewards to sync blocks");
        if (!cardLayout) status.setVisible(false);
        requirements.removeAll();
        lastRequirementsSnapshot = null;
        blockEditor.removeAll();
        blockBoxes.clear();
        lastBlockEditorSnapshot = null;
        blockToggle.setEnabled(false);
        blockToggle.setToolTipText("Log in to edit this character's blocks.");
        portrait.setToolTipText(null);
        showSyncStatus(lastSynced, syncedAt);
        revalidate();
        repaint();
    }

    void showAccountPending()
    {
        showLoggedOut(null, 0);
        resultList.removeAll();
        JLabel waiting = new JLabel("Waiting for RuneLite account data…", SwingConstants.CENTER);
        waiting.setFont(pageFont(Font.PLAIN, 12));
        waiting.setForeground(MUTED);
        waiting.setAlignmentX(CENTER_ALIGNMENT);
        resultList.add(waiting);
        status.setText("Account data is not ready yet");
        status.setVisible(true);
        revalidate();
        repaint();
    }

    void showSyncStatus(SlayerMaster master, long syncedAt)
    {
        showSyncStatus(master, syncedAt, null, false, false);
    }

    void showSyncStatus(SlayerMaster master, long syncedAt, String failure, boolean screenOpen, boolean syncing)
    {
        syncButton.setVisible(screenOpen);
        syncBalance.setVisible(screenOpen);
        syncButton.setEnabled(screenOpen && !syncing);
        syncButton.setText(syncing ? "…" : "↻");
        if (master == null || syncedAt <= 0)
        {
            boolean informational = failure != null && (failure.startsWith("Not yet synced")
                || failure.startsWith("Saved block data may be stale"));
            boolean imported = informational && failure.contains("imported");
            syncStatus.setText(imported ? "<html><center>Old blocks · sync to confirm</center></html>" : failure == null || informational
                ? "<html><center>Block list: Not yet synced</center></html>"
                : "<html><center>Sync failed<br>Open task list and retry</center></html>");
            syncStatus.setToolTipText(imported ? failure : failure == null
                ? "Block data has not been synced yet. Open a Slayer master’s Rewards > Tasks screen and sync it."
                : "Last sync attempt failed: " + failure + ". Keep the Slayer Rewards > Tasks screen open and retry.");
            syncStatus.setForeground(failure == null || informational ? MUTED : new Color(235, 151, 143));
        }
        else
        {
            String time = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(syncedAt));
            long age = System.currentTimeMillis() - syncedAt;
            boolean stale = age > SlayerOddsPlugin.BLOCK_STALE_MILLIS;
            String freshness = stale ? " | saved data may be stale" : "";
            boolean failed = failure != null && !failure.startsWith("Saved block data may be stale")
                && !failure.startsWith("Not yet synced");
            String marker = failed ? "Retry sync" : stale ? "Stale" : "Synced";
            syncStatus.setText((failed || stale ? marker : "Last synced") + " · " + master.displayName);
            syncStatus.setToolTipText("Last successful sync: " + master.displayName + " at "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(new Date(syncedAt))
                + freshness + (failure == null ? "" : ". Latest sync failed: " + failure));
            syncStatus.setForeground(failure == null && !stale ? MUTED : ACCENT);
        }
        revalidate();
        repaint();
    }

    void showResults(List<String> allMonsters, String selected, SlayerMaster detected,
        List<SlayerMaster> masters, List<SlayerOdds.Result> results, PlayerState state,
        boolean hideZeroChanceMasters, boolean showOddsTooltips, boolean showMortimerSection, boolean expandMortimerSection, boolean showMortimerModifiers,
        Set<SlayerMaster> unsyncedMasters, Set<SlayerMaster> failedMasters)
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

        taskName.setText("<html><div style='width:" + (archiveTheme ? 125 : 100) + "px'>" + escapeHtml(selected == null ? SELECT_TASK : selected) + "</div></html>");
        taskHeading.setText(selected == null ? "No task selected" : "Selected task");
        taskName.setToolTipText(selected);
        portrait.setPreferredSize(cardLayout ? new Dimension(210, 70) : new Dimension(selected == null ? 48 : 72, 84));
        ImageIcon taskImage = images.forTask(selected);
        portrait.setIcon(selected == null && taskImage != null
            ? new ImageIcon(taskImage.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)) : taskImage);
        portrait.setToolTipText(selected == null ? null : selected + " — representative task image");
        portrait.getAccessibleContext().setAccessibleName(selected == null
            ? "Slayer Task Odds icon" : selected + " task portrait");

        status.setText(detected == null ? "Open Slayer rewards to sync blocks" : "Detected: " + detected.displayName);
        if (!cardLayout) status.setVisible(detected != null);
        approximationHint.setVisible(selected != null && !unsyncedMasters.isEmpty());
        approximationHint.setText(showOddsTooltips ? "<html><center>~ Blocks need checking;<br>hover to see why.</center></html>"
            : "<html><center>~ Marked blocks need checking.</center></html>");
        zeroTooltipsEnabled = showOddsTooltips;
        approximationHint.setToolTipText("Marked odds use an unconfirmed or old block list, or the last saved list after a sync failure. Each master syncs separately.");
        resultList.removeAll();
        zeroList.removeAll();
        mortimerSection.setVisible(showMortimerSection);
        mortimerToggle.setVisible(showMortimerSection && expandMortimerSection);
        mortimerExpandable = expandMortimerSection;
        mortimerToggle.repaint();
        if (!showMortimerSection || lastMortimerExpandPreference == null
            || lastMortimerExpandPreference != expandMortimerSection)
        {
            mortimerToggle.setSelected(true);
            mortimerContent.setVisible(true);
            lastMortimerExpandPreference = expandMortimerSection;
        }
        mortimerList.removeAll();
        mortimerList.setVisible(false);
        modifierToggle.setVisible(false);
        modifierGroup.setVisible(false);
        modifierDetails.setVisible(false);
        modifierDetails.removeAll();
        if (!java.util.Objects.equals(displayedTask, selected) || previouslyHiding != hideZeroChanceMasters)
            zeroToggle.setSelected(false);
        displayedTask = selected;
        previouslyHiding = hideZeroChanceMasters;
        int visibleRows = 0;
        boolean highlightedRegularMaster = false;
        double bestRegularChance = 0;
        for (int i = 0; i < masters.size(); i++)
            if (masters.get(i) != SlayerMaster.MORTIMER)
                bestRegularChance = Math.max(bestRegularChance, results.get(i).chance());
        if (selected == null)
        {
            JLabel prompt = new JLabel("Choose a task to compare masters.", SwingConstants.CENTER);
            prompt.setFont(pageFont(Font.PLAIN, 12));
            prompt.setForeground(MUTED);
            prompt.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(prompt);
        }
        else if (masters.isEmpty())
        {
            JLabel emptyMasters = new JLabel("Enable a Slayer master in settings.", SwingConstants.CENTER);
            emptyMasters.setFont(pageFont(Font.PLAIN, 12));
            emptyMasters.setForeground(MUTED);
            emptyMasters.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(emptyMasters);
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
                    requirementLabel(notice, "+ " + (missing.size() - 3) + " more requirements", MUTED, Font.PLAIN, 12);
                sizeRequirementCard(notice);
                resultList.add(notice);
                resultList.add(Box.createVerticalStrut(5));
            }
        }
        for (int i = 0; i < masters.size(); i++)
        {
            SlayerOdds.Result result = results.get(i);
            boolean displaysAsZero = result.chance() * 100.0 < 0.005;
            boolean isMortimer = masters.get(i) == SlayerMaster.MORTIMER;
            JPanel destination = isMortimer ? mortimerList : hideZeroChanceMasters && displaysAsZero ? zeroList : resultList;
            if (isMortimer)
            {
                mortimerList.setVisible(true);
                List<String> modifierRows = MortimerModifiers.breakdown(selected, state.mortimerTasksCompleted, result.chance());
                if (showMortimerModifiers && !modifierRows.isEmpty())
                {
                    modifierGroup.setVisible(true);
                    modifierToggle.setVisible(true);
                    modifierDetails.setVisible(modifierToggle.isSelected());
                    for (String modifier : modifierRows)
                    {
                        int separator = modifier.indexOf(" · ");
                        String title = separator < 0 ? modifier : modifier.substring(0, separator);
                        String detail = separator < 0 ? "" : modifier.substring(separator + 3).replace(" | ", " · ");
                        JPanel modifierRow = new JPanel();
                        modifierRow.setLayout(new BoxLayout(modifierRow, BoxLayout.Y_AXIS));
                        modifierRow.setOpaque(false);
                        modifierRow.setAlignmentX(LEFT_ALIGNMENT);
                        modifierRow.add(modifierLabel(title, INK, cardLayout ? Font.BOLD : Font.ITALIC));
                        if (!detail.isEmpty()) modifierRow.add(modifierLabel(detail, MUTED, cardLayout ? Font.PLAIN : Font.ITALIC));
                        modifierRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, modifierRow.getPreferredSize().height));
                        modifierDetails.add(modifierRow);
                        modifierDetails.add(Box.createVerticalStrut(7));
                    }
                    JLabel explanation = modifierLabel("Estimates assume equal modifier chances. Ranges are proposed. Task + modifier means both appear in one offer. Solo only; cape repeats are separate.", MUTED, Font.ITALIC);
                    modifierDetails.add(explanation);
                }
                boolean mortimerBest = result.chance() > 0 && result.chance() > bestRegularChance;
                mortimerList.setBorder(archiveTheme
                    ? BorderFactory.createEmptyBorder(2, expandMortimerSection ? 6 : 0, 2, 0)
                    : cardLayout
                    ? BorderFactory.createEmptyBorder(2, expandMortimerSection ? 6 : 0, 2, 0)
                    : BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, mortimerBest ? 3 : 0, 0, 0, ACCENT),
                        BorderFactory.createEmptyBorder(2, 6, 2, 0)));
            }
            JPanel row = roundedPanel();
            row.setLayout(new BorderLayout(0, 3));
            row.setOpaque(false);
            row.setBackground(CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(i == 0 ? ACCENT : EDGE),
                BorderFactory.createEmptyBorder(3, 2, 3, 2)));
            row.setPreferredSize(new Dimension(210, cardLayout ? 48 : displaysAsZero ? 50 : 32));

            JPanel titleRow = new JPanel();
            titleRow.setLayout(new BorderLayout(6, 0));
            titleRow.setOpaque(false);
            boolean highlighted = !isMortimer && !displaysAsZero && !highlightedRegularMaster && result.chance() == bestRegularChance;
            highlightedRegularMaster |= highlighted;
            if (!isMortimer && (!displaysAsZero || !hideZeroChanceMasters)) visibleRows++;
            row.setBackground(CARD);
            boolean bestRegularMaster = !isMortimer && result.chance() > 0 && result.chance() == bestRegularChance;
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, isMortimer ? 0 : 3, 0, 0,
                    bestRegularMaster ? ACCENT : new Color(0, 0, 0, 0)),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0,
                        (bestRegularMaster || (isMortimer && result.chance() > 0 && result.chance() > bestRegularChance))
                            ? ACCENT : EDGE),
                    BorderFactory.createEmptyBorder(3, 2, 3, 2))));
            if (cardLayout)
            {
                row.setBackground((bestRegularMaster || (isMortimer && result.chance() > 0 && result.chance() > bestRegularChance))
                    ? ACTIVE_PAPER : CARD);
                row.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(
                    bestRegularMaster ? ACCENT : EDGE), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            }
            else if (archiveTheme)
            {
                row.setBackground(bestRegularMaster ? new Color(47, 79, 64) : new Color(25, 48, 42));
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, bestRegularMaster ? ACCENT : EDGE),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            }
            JLabel master = new JLabel(cardLayout ? masters.get(i).displayName : masters.get(i).displayName.replace(" / ", "/"));
            master.setForeground(cardLayout && highlighted ? ACCENT : INK);
            boolean emphasizeMaster = highlighted || (isMortimer && result.chance() > 0 && result.chance() > bestRegularChance);
            master.setFont(pageFont(emphasizeMaster ? Font.BOLD : Font.PLAIN, cardLayout ? 12 : 14));
            master.setHorizontalAlignment(SwingConstants.LEFT);
            titleRow.add(master, BorderLayout.CENTER);
            JLabel chance = new JLabel((unsyncedMasters.contains(masters.get(i)) ? "~" : "")
                + String.format("%.2f%%", result.chance() * 100.0), SwingConstants.RIGHT);
            chance.setForeground(ACCENT);
            chance.setFont(pageFont(Font.BOLD, cardLayout ? 13 : 14));
            titleRow.add(chance, BorderLayout.EAST);
            row.add(titleRow, BorderLayout.NORTH);

            String brief = briefExplanation(result.explanation());
            String detailText = displaysAsZero ? brief : masters.get(i) == SlayerMaster.MORTIMER
                ? state.mortimerChoices + " choices  |  " + result.count + " eligible"
                : result.formula() + "  |  " + result.count + " eligible";
            JLabel detail = new JLabel(detailText, SwingConstants.CENTER);
            detail.setFont(pageFont(Font.PLAIN, cardLayout ? 11 : 12));
            detail.setForeground(MUTED);
            detail.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            detail.setHorizontalAlignment(SwingConstants.CENTER);
            detail.setAlignmentX(CENTER_ALIGNMENT);
            if (displaysAsZero || cardLayout) row.add(detail, BorderLayout.CENTER);
            String tooltip = result.explanation() + " Weight calculation: " + result.formula()
                + "; " + result.count + " eligible tasks.";
            if (isMortimer && result.tooltip() != null) tooltip = result.tooltip();
            String blockWarning = failedMasters.contains(masters.get(i))
                ? "Sync failed; using this master's last saved blocks. "
                : unsyncedMasters.contains(masters.get(i)) ? "Block list is unconfirmed or over seven days old. " : "";
            if (!blockWarning.isEmpty())
                tooltip = tooltip.startsWith("<html>") ? tooltip.replaceFirst("<html>", "<html>" + blockWarning + "<br>") : blockWarning + tooltip;
            if (!showOddsTooltips) tooltip = null;
            row.setToolTipText(tooltip);
            titleRow.setToolTipText(tooltip);
            master.setToolTipText(tooltip);
            detail.setToolTipText(tooltip);
            chance.setToolTipText(tooltip);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardLayout ? 48 : displaysAsZero ? 50 : archiveTheme ? 36 : 32));
            row.setAlignmentX(CENTER_ALIGNMENT);
            destination.add(row);
            if (isMortimer)
            {
                JPanel notes = new JPanel(new java.awt.GridLayout(0, 1));
                notes.setOpaque(false);
                notes.setAlignmentX(CENTER_ALIGNMENT);
                notes.setBorder(BorderFactory.createEmptyBorder(2, archiveTheme ? 11 : cardLayout ? 8 : 2, 0, 0));
                JLabel note = new JLabel("Chance among " + state.mortimerChoices + " offered tasks", SwingConstants.LEFT);
                note.setFont(pageFont(Font.ITALIC, 11));
                note.setForeground(MUTED);
                notes.add(note);
                if (result.chance() > 0 && showOddsTooltips)
                {
                    JLabel hoverHint = new JLabel("Hover for odds breakdown", SwingConstants.LEFT);
                    hoverHint.setFont(pageFont(Font.ITALIC, 11));
                    hoverHint.setForeground(MUTED);
                    hoverHint.setToolTipText(tooltip);
                    notes.add(hoverHint);
                }
                notes.setMaximumSize(new Dimension(Integer.MAX_VALUE, notes.getPreferredSize().height));
                destination.add(notes);
            }
            destination.add(Box.createVerticalStrut(3));
        }
        if (selected != null && !masters.isEmpty() && visibleRows == 0)
        {
            JLabel empty = new JLabel("No masters above 0.00%.", SwingConstants.CENTER);
            empty.setFont(pageFont(Font.PLAIN, 12));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            resultList.add(empty);
        }

        zeroToggle.setVisible(hideZeroChanceMasters && zeroList.getComponentCount() > 0);
        zeroList.setVisible(zeroToggle.isVisible() && zeroToggle.isSelected());
        explanationHint.setVisible(zeroList.isVisible() && showOddsTooltips);
        updateZeroToggle();

        updateRequirements(state);
        revalidate();
        repaint();
    }

    private JLabel modifierLabel(String text, Color color, int style)
    {
        int panelWidth = getWidth() > 0 ? getWidth() : 220;
        // Swing's HTML width is further affected by font scaling and nested insets.
        // Leave room for both so wrapped text stays inside the narrow sidebar.
        int width = Math.max(90, Math.min(135, panelWidth - 90));
        JLabel label = new JLabel("<html><div style='width:" + width + "px'>" + escapeHtml(text) + "</div></html>");
        label.setFont(pageFont(style, 11));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        return label;
    }

    private void updateZeroToggle()
    {
        zeroToggle.setText((zeroToggle.isSelected() ? "Hide" : "Show")
            + " 0.00% masters (" + (zeroList.getComponentCount() / 2) + ")");
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String briefExplanation(String explanation)
    {
        if (explanation.startsWith("Blocked")) return "Blocked in saved list";
        if (explanation.startsWith("Requirements not met")) return "Missing requirements";
        if (explanation.startsWith("Your current task")) return "Previous task cannot repeat";
        if (explanation.startsWith("This master never assigns")) return "Never assigned by this master";
        if (explanation.startsWith("This task is available")) return "Available; rounds to 0.00%";
        return "No eligible task weight";
    }

    void editBlocks(Map<SlayerMaster, List<String>> savedBlocks, Map<SlayerMaster, Long> syncTimes,
        Map<SlayerMaster, Long> manualTimes, Map<SlayerMaster, List<Boolean>> manualSlots, boolean legacyImported)
    {
        StringBuilder ages = new StringBuilder();
        for (SlayerMaster master : SlayerMaster.values())
        {
            long updatedAt = Math.max(syncTimes.getOrDefault(master, 0L), manualTimes.getOrDefault(master, 0L));
            ages.append(updatedAt == 0 || System.currentTimeMillis() - updatedAt > SlayerOddsPlugin.BLOCK_STALE_MILLIS);
        }
        String snapshot = String.valueOf(savedBlocks) + syncTimes + manualTimes + manualSlots + legacyImported + ages;
        if (snapshot.equals(lastBlockEditorSnapshot)) return;
        lastBlockEditorSnapshot = snapshot;
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
                masterToggle.setToolTipText(syncTimes.getOrDefault(master, 0L) <= 0
                    ? "Not synced for this character: saved blocks are approximate" : "Block list synced");
                styleDetailButton(masterToggle);
                masterToggle.setSelected(expandedBlockMasters.contains(master));
                masterToggle.setHorizontalAlignment(SwingConstants.LEFT);
                JPanel slots = new JPanel();
                slots.setLayout(new BoxLayout(slots, BoxLayout.Y_AXIS));
                slots.setOpaque(false);
                slots.setBorder(BorderFactory.createEmptyBorder(3, 7, 6, 3));
                slots.setVisible(expandedBlockMasters.contains(master));
                long syncedAt = syncTimes.getOrDefault(master, 0L);
                List<Boolean> manual = manualSlots.getOrDefault(master, java.util.Collections.emptyList());
                boolean hasManual = manual.contains(Boolean.TRUE);
                long manualAt = manualTimes.getOrDefault(master, 0L);
                boolean hasOldBlocks = false;
                if (legacyImported)
                    for (int i = 0; i < selectedBlocks.size(); i++)
                    {
                        SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(selectedBlocks.get(i));
                        if ((i >= manual.size() || !Boolean.TRUE.equals(manual.get(i))) && choice != null && choice != SlayerTaskChoice.NONE)
                            hasOldBlocks = true;
                    }
                boolean oldManual = manualAt > 0 && System.currentTimeMillis() - manualAt > SlayerOddsPlugin.BLOCK_STALE_MILLIS;
                String syncText = syncedAt <= 0 ? (hasOldBlocks ? "Old blocks; sync this character" : hasManual ? (oldManual ? "Manual blocks: over 7 days old" : "Manual blocks; not synced") : "Not synced; no blocks assumed")
                    : "Last synced: " + new SimpleDateFormat("d MMM, HH:mm").format(new Date(syncedAt))
                        + (System.currentTimeMillis() - syncedAt > SlayerOddsPlugin.BLOCK_STALE_MILLIS ? " (stale)" : "");
                JLabel syncLabel = archiveTheme ? wrappedRequirement(syncText, false) : new JLabel(syncText);
                syncLabel.setToolTipText(syncText);
                syncLabel.setForeground(MUTED);
                syncLabel.setFont(pageFont(Font.PLAIN, 11));
                slots.add(syncLabel);
                List<JComboBox<SlayerTaskChoice>> masterBoxes = new ArrayList<>();
                for (int i = 0; i < master.blockSlots(); i++)
                {
                    boolean imported = syncedAt <= 0 && (i >= manual.size() || !Boolean.TRUE.equals(manual.get(i)))
                        && i < selectedBlocks.size() && SlayerTaskChoice.fromStoredValue(selectedBlocks.get(i)) != SlayerTaskChoice.NONE;
                    JLabel label = new JLabel((i == 6 ? "Diary slot" : "Slot " + (i + 1)) + (imported && legacyImported ? " · old" : ""));
                    label.setForeground(MUTED);
                    label.setFont(pageFont(Font.PLAIN, 12));
                    slots.add(label);
                    JComboBox<SlayerTaskChoice> box = new JComboBox<>(SlayerTaskChoice.values());
                    box.setFont(pageFont(Font.PLAIN, 12));
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

    void invalidateBlockEditor()
    {
        lastBlockEditorSnapshot = null;
    }

    List<String> blockValues(SlayerMaster master)
    {
        List<String> values = new ArrayList<>();
        for (JComboBox<SlayerTaskChoice> box : blockBoxes.getOrDefault(master, new ArrayList<>()))
            values.add(String.valueOf(box.getSelectedItem()));
        return values;
    }
}
