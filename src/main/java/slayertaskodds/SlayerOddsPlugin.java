package slayertaskodds;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(name = "Slayer Task Odds", description = "Find the best Slayer master for your chosen task with account-aware odds that include your levels, quests, unlocks, and synced block lists.", tags = {"slayer", "tasks", "odds", "blocks"})
public class SlayerOddsPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private EventBus eventBus;
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
    private SlayerMaster lastSyncedMaster;
    private long lastSyncedAt;
    private String lastFailure;
    private SlayerMaster lastFailureMaster;
    private int syncAttempts;
    private SlayerMaster activeSyncMaster;
    private int ticks;
    private Integer lastRender;
    private String lastRoutedMonster;
    private SlayerMaster lastRoutedMaster;

    @Override
    protected void startUp()
    {
        tasks = TaskData.load(gson);
        selectedMonster = null;
        loadLastSyncedStatus();
        running = true;
        lastRender = null;
        migrateBlockSettings();
        SwingUtilities.invokeLater(() -> {
            if (!running) return;
            panel = new SlayerOddsPanel(monster -> { selectedMonster = monster; refresh(); }, this::saveBlock,
                this::requestSync);
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
            if (event.getGameState() == GameState.LOGIN_SCREEN)
            {
                resetRouteSelection();
            }
        }
        refresh();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        migrateBlockSettings();
        loadLastSyncedStatus();
        resetSync();
        resetRouteSelection();
        lastFailureMaster = null;
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
        if (SlayerOddsConfig.GROUP.equals(event.getGroup()) && !syncing)
        {
            if ("routeToBestMaster".equals(event.getKey()) && !config.routeToBestMaster())
            {
                resetRouteSelection();
            }
            refresh();
        }
    }

    private void resetRouteSelection()
    {
        lastRoutedMonster = null;
        lastRoutedMaster = null;
    }

    private void updateBestMasterRoute(SlayerMaster master, double chance)
    {
        if (!config.routeToBestMaster() || master == null || chance <= 0)
        {
            resetRouteSelection();
            return;
        }
        if (master == lastRoutedMaster && Objects.equals(selectedMonster, lastRoutedMonster)) return;
        Map<String, Object> data = new HashMap<>();
        data.put("target", master.destination);
        lastRoutedMaster = master;
        lastRoutedMonster = selectedMonster;
        eventBus.post(new PluginMessage("shortestpath", "path", data));
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
        if (master != activeSyncMaster)
        {
            activeSyncMaster = master;
            lastSync = null;
            syncAttempts = 0;
            if (lastFailureMaster != master)
            {
                lastFailure = null;
                lastFailureMaster = null;
            }
        }
        try
        {
            if (master == null) throw new IllegalStateException("could not identify the selected master");
            syncMaster(master, false);
        }
        catch (RuntimeException ex)
        {
            recordSyncFailure(master, ex);
        }
    }

    private void requestSync(boolean manual)
    {
        if (!running) return;
        clientThread.invokeLater(() -> {
            if (!running) return;
            if (!blockScreenOpen())
            {
                lastFailure = "Open a Slayer master's Rewards > Tasks screen first.";
                lastFailureMaster = null;
                refresh();
                return;
            }
            SlayerMaster master = screenMaster();
            if (master == null)
            {
                lastFailure = "Could not identify the selected Slayer master.";
                lastFailureMaster = null;
                refresh();
                return;
            }
            try { syncMaster(master, true); }
            catch (RuntimeException ex) { recordSyncFailure(master, ex); }
        });
    }

    private void syncMaster(SlayerMaster master, boolean force)
    {
            // Read and validate every slot before changing any saved settings.
            List<SlayerTaskChoice> slots = BlockSync.read(client, master);
            String signature = master.name() + slots.toString();
            boolean blockSettingsMatch = true;
            for (int i = 0; i < slots.size(); i++)
                blockSettingsMatch &= slots.get(i).name().equals(configManager.getConfiguration(
                    SlayerOddsConfig.GROUP, master.key + "Block" + (i + 1)));
            if (!force && signature.equals(lastSync) && lastSyncedMaster == master && blockSettingsMatch
                && lastFailure == null) return;
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
            lastSyncedMaster = master;
            lastSyncedAt = System.currentTimeMillis();
            configManager.setConfiguration(SlayerOddsConfig.GROUP, "lastSyncedMaster", master.key);
            configManager.setConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt", Long.toString(lastSyncedAt));
            lastFailure = null;
            lastFailureMaster = null;
            syncAttempts = 0;
            if (config.postSyncChatMessages())
            {
                chat("Slayer Task Odds: " + master.displayName + " block list synced.");
                for (int i = 0; i < slots.size(); i++) chat("Slot " + (i + 1) + ": " + slots.get(i));
            }
            refresh();
    }

    private void recordSyncFailure(SlayerMaster master, RuntimeException ex)
    {
            String detail = ex.getMessage() == null ? "Unknown sync error" : ex.getMessage();
            lastFailure = detail;
            lastFailureMaster = master;
            // Allow screen data to settle, retry failures, and avoid chat spam.
            if (++syncAttempts == 3)
            {
                String message = "Slayer Task Odds: " + (master == null ? "Block list" : master.displayName + " block list")
                    + " sync failed: " + detail + ".";
                chat(message);
            }
            refresh();
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
            resetRouteSelection();
            lastRender = null;
            SlayerMaster syncedMaster = lastSyncedMaster;
            long syncedAt = lastSyncedAt;
            SwingUtilities.invokeLater(() -> { if (running && panel == target) target.showLoggedOut(syncedMaster, syncedAt); });
            return;
        }
        PlayerState state = PlayerStateReader.read(client, configManager, tasks);
        SlayerMaster detected = screenMaster();
        boolean hasSavedBlocks = lastSyncedMaster != null && lastSyncedAt > 0;
        boolean savedBlocksStale = hasSavedBlocks && System.currentTimeMillis() - lastSyncedAt > 24L * 60L * 60L * 1000L;
        if (!hasSavedBlocks && detected == null && lastFailure == null)
            lastFailure = "Not yet synced. Open a Slayer Rewards > Tasks screen to read your block list.";
        else if (savedBlocksStale && detected == null && lastFailure == null)
            lastFailure = "Saved block data may be stale. Reopen a Slayer Rewards > Tasks screen to refresh it.";
        List<String> monsters = TaskData.monsters(tasks);
        List<MasterOdds> ranked = new ArrayList<>();
        List<String> allBlocks = new ArrayList<>();
        for (SlayerMaster master : SlayerMaster.values())
        {
            List<String> blocks = readBlocks(master);
            allBlocks.addAll(blocks);
            if (selectedMonster != null)
                ranked.add(new MasterOdds(master, SlayerOdds.calculate(tasks.get(master), selectedMonster,
                    SlayerOdds.blocks(blocks.toArray(new String[0])), state)));
        }
        int fingerprint = Objects.hash(state.stats, state.quests, state.unlocks, state.other, state.currentTask,
            allBlocks, selectedMonster,
            detected, lastSyncedMaster, lastSyncedAt, lastFailure, syncing, config.hideZeroChanceMasters(),
            config.showOddsTooltips(), config.routeToBestMaster());
        if (lastRender != null && lastRender == fingerprint) return;
        lastRender = fingerprint;
        ranked.sort(Comparator.comparingDouble((MasterOdds item) -> item.result.chance()).reversed()
            .thenComparing(item -> item.master.displayName));
        MasterOdds best = ranked.isEmpty() ? null : ranked.get(0);
        updateBestMasterRoute(best == null ? null : best.master,
            best == null ? 0 : best.result.chance());
        List<SlayerMaster> masters = new ArrayList<>();
        List<SlayerOdds.Result> results = new ArrayList<>();
        for (MasterOdds row : ranked) { masters.add(row.master); results.add(row.result); }
        Map<SlayerMaster, List<String>> savedBlocks = new EnumMap<>(SlayerMaster.class);
        for (SlayerMaster master : SlayerMaster.values()) savedBlocks.put(master, readBlocks(master));
        String selection = selectedMonster;
        SlayerMaster syncedMaster = lastSyncedMaster;
        long syncedAt = lastSyncedAt;
        String syncFailure = lastFailure;
        boolean currentlySyncing = syncing;
        SwingUtilities.invokeLater(() -> {
            if (running && panel == target)
            {
                target.showResults(monsters, selection, detected, masters, results, state,
                    config.hideZeroChanceMasters(), config.showOddsTooltips());
                target.showSyncStatus(syncedMaster, syncedAt, syncFailure, detected != null, currentlySyncing);
                target.editBlocks(savedBlocks);
            }
        });
    }

    private void loadLastSyncedStatus()
    {
        lastSyncedMaster = SlayerMaster.byKey(configManager.getConfiguration(
            SlayerOddsConfig.GROUP, "lastSyncedMaster"));
        String timestamp = configManager.getConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt");
        try { lastSyncedAt = timestamp == null ? 0 : Long.parseLong(timestamp); }
        catch (NumberFormatException ex) { lastSyncedAt = 0; }
        if (lastSyncedMaster == null) lastSyncedAt = 0;
    }

    private void saveBlock(SlayerMaster master, int slot)
    {
        if (master == null || panel == null) return;
        List<String> values = panel.blockValues(master);
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
