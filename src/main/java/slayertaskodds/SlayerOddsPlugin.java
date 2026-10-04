package slayertaskodds;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(name = "Slayer Task Odds", description = "Find the best Slayer master for your chosen task with account-aware odds that include your levels, quests, unlocks, and synced block lists.", tags = {"slayer", "tasks", "odds", "blocks"})
public class SlayerOddsPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configManager;
    @Inject private SlayerOddsConfig config;
    @Inject private Gson gson;
    private volatile SlayerOddsPanel panel;
    private NavigationButton navigation;
    private Map<SlayerMaster, List<SlayerTask>> tasks;
    private volatile String selectedMonster;
    private volatile boolean running;
    private boolean syncing;
    private String lastSync;
    private String lastFailure;
    private int syncAttempts;
    private int ticks;
    private Integer lastRender;

    @Override
    protected void startUp()
    {
        tasks = TaskData.load(gson);
        running = true;
        lastRender = null;
        migrateBlockSettings();
        SwingUtilities.invokeLater(() -> {
            if (!running) return;
            panel = new SlayerOddsPanel(monster -> { selectedMonster = monster; refresh(); }, this::saveBlock);
            BufferedImage icon = loadIcon();
            navigation = NavigationButton.builder().tooltip("Slayer Task Odds").icon(icon).priority(8).panel(panel).build();
            toolbar.addNavigation(navigation);
            refresh();
        });
    }

    private BufferedImage loadIcon()
    {
        try (InputStream stream = SlayerOddsPlugin.class.getResourceAsStream("/slayertaskodds/icon.png"))
        {
            if (stream == null)
                throw new IllegalStateException("Missing Slayer Task Odds sidebar icon");
            BufferedImage icon = ImageIO.read(stream);
            if (icon == null)
                throw new IllegalStateException("Invalid Slayer Task Odds sidebar icon");
            return icon;
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to load Slayer Task Odds sidebar icon", ex);
        }
    }

    @Override
    protected void shutDown()
    {
        running = false;
        resetSync();
        SwingUtilities.invokeLater(() -> {
            if (navigation != null) toolbar.removeNavigation(navigation);
            navigation = null;
            panel = null;
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            resetSync();
            lastRender = null;
        }
        refresh();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        migrateBlockSettings();
        resetSync();
        lastRender = null;
        refresh();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        syncScreen();
        if (++ticks % 5 == 0) refresh();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (SlayerOddsConfig.GROUP.equals(event.getGroup()) && !syncing) refresh();
    }

    private boolean visible(int component)
    {
        Widget widget = client.getWidget(component);
        return widget != null && !widget.isHidden();
    }

    private boolean blockScreenOpen()
    {
        return client.getGameState() == GameState.LOGGED_IN &&
            (visible(InterfaceID.SlayerRewards.TASKS) || visible(InterfaceID.SlayerRewardsTaskList.UNIVERSE));
    }

    private SlayerMaster screenMaster()
    {
        return blockScreenOpen() ? SlayerMaster.byGameId(client.getVarbitValue(VarbitID.SLAYER_MASTER_IN_FOCUS)) : null;
    }

    private void resetSync()
    {
        lastSync = null;
        lastFailure = null;
        syncAttempts = 0;
    }

    private void syncScreen()
    {
        if (!blockScreenOpen())
        {
            boolean wasOpen = lastSync != null || lastFailure != null;
            resetSync();
            if (wasOpen) refresh();
            return;
        }
        SlayerMaster master = screenMaster();
        try
        {
            if (master == null) throw new IllegalStateException("could not identify the selected master");
            // Read and validate every slot before changing any saved settings.
            List<SlayerTaskChoice> slots = BlockSync.read(client, master);
            String signature = master.name() + slots.toString();
            if (signature.equals(lastSync)) return;
            syncing = true;
            try
            {
                for (int i = 0; i < slots.size(); i++)
                    configManager.setConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + (i + 1), slots.get(i).name());
                for (int i = 0; i < slots.size(); i++)
                    if (!slots.get(i).name().equals(configManager.getConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + (i + 1))))
                        throw new IllegalStateException("could not save slot " + (i + 1));
            }
            finally { syncing = false; }
            lastSync = signature;
            lastFailure = null;
            syncAttempts = 0;
            if (config.postSyncChatMessages())
            {
                chat("Slayer Task Odds: " + master.displayName + " block list synced.");
                for (int i = 0; i < slots.size(); i++) chat("Slot " + (i + 1) + ": " + slots.get(i));
            }
            refresh();
        }
        catch (RuntimeException ex)
        {
            String message = "Slayer Task Odds: " + (master == null ? "Block list" : master.displayName + " block list")
                + " sync failed: " + ex.getMessage() + ".";
            // Allow screen data to settle, retry failures, and avoid chat spam.
            if (++syncAttempts >= 3 && !message.equals(lastFailure))
            {
                chat(message);
                lastFailure = message;
            }
        }
    }

    private void chat(String text)
    {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, "");
    }

    private void refresh() { clientThread.invokeLater(this::refreshOnClientThread); }

    private void refreshOnClientThread()
    {
        SlayerOddsPanel target = panel;
        if (!running || target == null) return;
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            lastRender = null;
            SwingUtilities.invokeLater(() -> { if (running && panel == target) target.showLoggedOut(); });
            return;
        }
        PlayerState state = PlayerStateReader.read(client, configManager, tasks);
        SlayerMaster detected = screenMaster();
        List<String> monsters = TaskData.monsters(tasks);
        if (selectedMonster == null && !monsters.isEmpty()) selectedMonster = monsters.get(0);
        List<MasterOdds> ranked = new ArrayList<>();
        List<String> allBlocks = new ArrayList<>();
        for (SlayerMaster master : SlayerMaster.values())
        {
            List<String> blocks = readBlocks(master);
            allBlocks.addAll(blocks);
            ranked.add(new MasterOdds(master, SlayerOdds.calculate(tasks.get(master), selectedMonster,
                SlayerOdds.blocks(blocks.toArray(new String[0])), state)));
        }
        int fingerprint = Objects.hash(state.stats, state.quests, state.unlocks, state.other, allBlocks, selectedMonster, detected);
        if (lastRender != null && lastRender == fingerprint) return;
        lastRender = fingerprint;
        ranked.sort(Comparator.comparingDouble((MasterOdds item) -> item.result.chance()).reversed()
            .thenComparing(item -> item.master.displayName));
        List<SlayerMaster> masters = new ArrayList<>();
        List<SlayerOdds.Result> results = new ArrayList<>();
        for (MasterOdds row : ranked) { masters.add(row.master); results.add(row.result); }
        List<String> currentBlocks = detected == null ? new ArrayList<>() : readBlocks(detected);
        String selection = selectedMonster;
        SwingUtilities.invokeLater(() -> {
            if (running && panel == target)
            {
                target.showResults(monsters, selection, detected, masters, results, state);
                target.editBlocks(detected, monsters, currentBlocks);
            }
        });
    }

    private void saveBlock(SlayerMaster master, int slot)
    {
        if (master == null || panel == null) return;
        List<String> values = panel.blockValues();
        if (slot < 0 || slot >= values.size()) return;
        SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(values.get(slot));
        if (choice != null) configManager.setConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + (slot + 1), choice.name());
        refresh();
    }

    private List<String> readBlocks(SlayerMaster master)
    {
        List<String> blocks = new ArrayList<>();
        for (int slot = 1; slot <= BlockSync.SLOT_COUNT; slot++)
        {
            String value = configManager.getConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + slot);
            SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(value);
            blocks.add(choice == null ? value : choice.toString());
        }
        return blocks;
    }

    private void migrateBlockSettings()
    {
        for (SlayerMaster master : SlayerMaster.values())
            for (int slot = 1; slot <= BlockSync.SLOT_COUNT; slot++)
            {
                String key = master.key + "Block" + slot;
                String value = configManager.getConfiguration(SlayerOddsConfig.GROUP, key);
                SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(value);
                if (value != null && choice != null && !choice.name().equals(value))
                    configManager.setConfiguration(SlayerOddsConfig.GROUP, key, choice.name());
            }
    }

    @Provides
    SlayerOddsConfig provideConfig(ConfigManager manager) { return manager.getConfig(SlayerOddsConfig.class); }

    private static final class MasterOdds
    {
        private final SlayerMaster master;
        private final SlayerOdds.Result result;
        private MasterOdds(SlayerMaster master, SlayerOdds.Result result) { this.master = master; this.result = result; }
    }
}
