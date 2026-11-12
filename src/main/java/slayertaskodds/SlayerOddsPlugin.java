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
import java.util.Set;
import java.util.EnumSet;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
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
    private volatile boolean syncing;
    private volatile SlayerMaster openTaskMaster;
    private final java.util.concurrent.atomic.AtomicBoolean refreshPending = new java.util.concurrent.atomic.AtomicBoolean();
    private final Set<SlayerMaster> failedSyncs = EnumSet.noneOf(SlayerMaster.class);
    private String lastSync;
    private SlayerMaster lastSyncedMaster;
    private long lastSyncedAt;
    private String lastFailure;
    private SlayerMaster lastFailureMaster;
    private int syncAttempts;
    private SlayerMaster activeSyncMaster;
    private boolean syncScreenWasOpen;
    private int ticks;
    private Integer lastRender;
    private String lastRoutedMonster;
    private SlayerMaster lastRoutedMaster;
    private WorldPoint lastRoutedDestination;
    private WorldPoint mortimerDestination;
    private long mortimerLocationAt;
    private volatile String loadedAccount;
    private int nextSyncAttemptTick;
    private boolean warnedUnsynced;
    private static final int RETRY_TICKS = 10;
    private static final int SLOW_RETRY_TICKS = 50;
    private static final int SYNC_CHECK_TICKS = 5;
    static final long BLOCK_STALE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    @Override
    protected void startUp()
    {
        tasks = TaskData.load(gson);
        loadedAccount = null;
        failedSyncs.clear();
        warnedUnsynced = false;
        mortimerDestination = null;
        selectedMonster = null;
        lastSyncedMaster = null;
        lastSyncedAt = 0;
        running = true;
        lastRender = null;
        migrateBlockSettings();
        SwingUtilities.invokeLater(() -> {
            if (!running) return;
            createPanel();
        });
    }

    private void createPanel()
    {
        SlayerOddsPanel previous = panel;
        SlayerOddsPanel replacement = new SlayerOddsPanel(monster -> { selectedMonster = monster; refresh(); }, this::saveBlock,
            this::requestSync, config.panelTheme(), config.panelLayout());
        if (previous != null) replacement.restoreExpansion(previous);
        NavigationButton replacementNavigation = NavigationButton.builder().tooltip("Slayer Task Odds")
            .icon(loadIcon()).priority(8).panel(replacement).build();
        if (navigation != null) toolbar.removeNavigation(navigation);
        panel = replacement;
        navigation = replacementNavigation;
        toolbar.addNavigation(navigation);
        clientThread.invokeLater(() -> {
            lastRender = null;
            refreshOnClientThread();
        });
    }

    private BufferedImage loadIcon()
    {
        try (InputStream stream = SlayerOddsPlugin.class.getResourceAsStream("/slayertaskodds/toolbar.png"))
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
                selectedMonster = null;
                loadedAccount = null;
                warnedUnsynced = false;
                failedSyncs.clear();
                mortimerDestination = null;
                resetRouteSelection();
            }
        }
        refresh();
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        migrateBlockSettings();
        loadedAccount = null;
        warnedUnsynced = false;
        failedSyncs.clear();
        selectedMonster = null;
        mortimerDestination = null;
        lastFailure = null;
        resetSync();
        resetRouteSelection();
        lastFailureMaster = null;
        lastRender = null;
        refresh();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        ticks++;
        ensureAccountLoaded();
        syncScreen();
        if (ticks % 5 == 0) refresh();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (SlayerOddsConfig.GROUP.equals(event.getGroup()) && !syncing)
        {
            boolean blockSetting = event.getProfile() == null && event.getKey().matches("(turael|krystilia|mazchna|vannaka|chaeldar|konar|nieve|duradel|mortimer)Block[1-7]");
            if (blockSetting)
            {
                String masterKey = event.getKey().substring(0, event.getKey().indexOf("Block"));
                SlayerMaster edited = SlayerMaster.byKey(masterKey);
                if (loadedAccount == null || edited == openTaskMaster)
                {
                    syncing = true;
                    try { configManager.setConfiguration(SlayerOddsConfig.GROUP, event.getKey(), event.getOldValue() == null ? SlayerTaskChoice.NONE.name() : event.getOldValue()); }
                    finally { syncing = false; }
                    if (loadedAccount != null)
                        clientThread.invokeLater(() -> chat("Slayer Task Odds: Close this master's task screen to edit blocks manually."));
                    else if (running && SwingUtilities.isEventDispatchThread())
                        SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(null,
                            "Log in to edit this character's blocks.", "Slayer Task Odds", javax.swing.JOptionPane.INFORMATION_MESSAGE));
                    SlayerOddsPanel target = panel;
                    if (target != null) SwingUtilities.invokeLater(target::invalidateBlockEditor);
                    lastRender = null;
                    refresh();
                    return;
                }
            }
            if ("panelTheme".equals(event.getKey()) || "panelLayout".equals(event.getKey()))
            {
                SwingUtilities.invokeLater(() -> { if (running) createPanel(); });
                return;
            }
            if ("hideZeroChanceMasters".equals(event.getKey()))
            {
                SlayerOddsPanel target = panel;
                if (target != null) SwingUtilities.invokeLater(() -> {
                    if (running && panel == target) target.collapseZeroChanceMasters();
                });
            }
            if ("routeToBestMaster".equals(event.getKey()) && !config.routeToBestMaster())
            {
                resetRouteSelection();
            }
            if (blockSetting && loadedAccount != null)
            {
                String value = configManager.getConfiguration(SlayerOddsConfig.GROUP, event.getKey());
                if (value != null)
                {
                    configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, event.getKey(), value);
                    String masterKey = event.getKey().substring(0, event.getKey().indexOf("Block"));
                    SlayerMaster editedMaster = SlayerMaster.byKey(masterKey);
                    if (editedMaster != null && readSyncTime(editedMaster) > 0)
                        for (int i = 1; i <= editedMaster.blockSlots(); i++)
                            configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, masterKey + "Block" + i + "Manual", true);
                    configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, event.getKey() + "Manual", true);
                    configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, masterKey + "ManualAt", Long.toString(System.currentTimeMillis()));
                    configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, masterKey + "SyncedAt");
                    if (lastSyncedMaster != null && lastSyncedMaster.key.equals(masterKey))
                    {
                        lastSyncedMaster = null;
                        lastSyncedAt = 0;
                        configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, "lastSyncedMaster");
                        configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt");
                    }
                }
            }
            refresh();
        }
    }

    private void resetRouteSelection()
    {
        lastRoutedMonster = null;
        lastRoutedMaster = null;
        lastRoutedDestination = null;
    }

    private void updateBestMasterRoute(SlayerMaster master, double chance)
    {
        if (!config.routeToBestMaster() || master == null || chance <= 0)
        {
            resetRouteSelection();
            return;
        }
        WorldPoint destination = master == SlayerMaster.MORTIMER ? mortimerDestination : master.destination;
        if (destination == null) return;
        if (master == lastRoutedMaster && Objects.equals(selectedMonster, lastRoutedMonster)
            && destination.equals(lastRoutedDestination)) return;
        Map<String, Object> data = new HashMap<>();
        data.put("target", destination);
        lastRoutedMaster = master;
        lastRoutedMonster = selectedMonster;
        lastRoutedDestination = destination;
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
        activeSyncMaster = null;
        syncScreenWasOpen = false;
        openTaskMaster = null;
        nextSyncAttemptTick = 0;
    }

    private void syncScreen()
    {
        if (client.getGameState() == GameState.LOGGED_IN && loadedAccount == null) return;
        if (!blockScreenOpen())
        {
            boolean wasOpen = syncScreenWasOpen;
            resetSync();
            if (wasOpen) refresh();
            return;
        }
        syncScreenWasOpen = true;
        SlayerMaster master = screenMaster();
        openTaskMaster = master;
        if (master != activeSyncMaster)
        {
            activeSyncMaster = master;
            lastSync = null;
            syncAttempts = 0;
            nextSyncAttemptTick = 0;
            if (lastFailureMaster != master)
            {
                lastFailure = null;
                lastFailureMaster = null;
            }
        }
        if (ticks < nextSyncAttemptTick) return;
        try
        {
            if (master == null) throw new IllegalStateException("could not identify the selected master");
            syncMaster(master, false);
            nextSyncAttemptTick = ticks + SYNC_CHECK_TICKS;
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
            lastSync = signature;
            lastSyncedMaster = master;
            lastSyncedAt = System.currentTimeMillis();
            configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "SyncedAt", Long.toString(lastSyncedAt));
            configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "SyncFailed");
            configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "ManualAt");
            for (int i = 0; i < slots.size(); i++)
            {
                configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + (i + 1), slots.get(i).name());
                configManager.unsetRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + (i + 1) + "Manual");
            }
            configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "lastSyncedMaster", master.key);
            configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt", Long.toString(lastSyncedAt));
            configManager.setConfiguration(SlayerOddsConfig.GROUP, "lastSyncedMaster", master.key);
            configManager.setConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt", Long.toString(lastSyncedAt));
            }
            finally { syncing = false; }
            lastFailure = null;
            lastFailureMaster = null;
            failedSyncs.remove(master);
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
            if (master != null)
            {
                failedSyncs.add(master);
                configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "SyncFailed", true);
            }
            nextSyncAttemptTick = ticks + (syncAttempts >= 2 ? SLOW_RETRY_TICKS : RETRY_TICKS);
            // Allow screen data to settle, retry failures, and avoid chat spam.
            if (++syncAttempts == 3 && config.postSyncChatMessages())
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

    private void refresh()
    {
        if (!refreshPending.compareAndSet(false, true)) return;
        clientThread.invokeLater(() -> {
            refreshPending.set(false);
            refreshOnClientThread();
        });
    }

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
        ensureAccountLoaded();
        if (loadedAccount == null)
        {
            SwingUtilities.invokeLater(() -> { if (running && panel == target) target.showAccountPending(); });
            return;
        }
        PlayerState state = PlayerStateReader.read(client, configManager, tasks);
        if (mortimerDestination != null && System.currentTimeMillis() - mortimerLocationAt > 24L * 60L * 60L * 1000L)
            mortimerDestination = null;
        for (NPC npc : client.getNpcs())
            if (npc.getName() != null && npc.getName().equalsIgnoreCase("Mortimer"))
            {
                WorldPoint location = npc.getWorldLocation();
                if (location != null && !location.equals(mortimerDestination))
                {
                    mortimerDestination = location;
                    mortimerLocationAt = System.currentTimeMillis();
                    configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "mortimerLocation",
                        location.getX() + "," + location.getY() + "," + location.getPlane());
                    configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "mortimerLocationAt", Long.toString(mortimerLocationAt));
                }
            }
        SlayerMaster detected = screenMaster();
        boolean hasSavedBlocks = lastSyncedMaster != null && lastSyncedAt > 0;
        if (!hasSavedBlocks && detected == null && lastFailure == null)
            lastFailure = Boolean.TRUE.equals(configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "legacyBlocksImported", boolean.class))
                ? "Not yet synced. Older saved blocks were imported; check and sync each master for this account."
                : "Not yet synced. Open a Slayer Rewards > Tasks screen to read your block list.";
        List<String> monsters = TaskData.monsters(tasks);
        List<MasterOdds> ranked = new ArrayList<>();
        List<String> allBlocks = new ArrayList<>();
        Map<SlayerMaster, Long> syncTimes = new EnumMap<>(SlayerMaster.class);
        Map<SlayerMaster, Long> manualTimes = new EnumMap<>(SlayerMaster.class);
        boolean legacyImported = Boolean.TRUE.equals(configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "legacyBlocksImported", boolean.class));
        Map<SlayerMaster, List<Boolean>> manualSlots = new EnumMap<>(SlayerMaster.class);
        Set<SlayerMaster> unsynced = EnumSet.noneOf(SlayerMaster.class);
        Set<SlayerMaster> enabledMasters = EnumSet.noneOf(SlayerMaster.class);
        List<Boolean> staleMasters = new ArrayList<>();
        for (SlayerMaster master : SlayerMaster.values())
        {
            boolean enabled = masterEnabled(config, master);
            if (enabled) enabledMasters.add(master);
            List<String> blocks = readBlocks(master);
            allBlocks.addAll(blocks);
            long syncedAt = readSyncTime(master);
            syncTimes.put(master, syncedAt);
            long manualAt = readTimestamp(master.key + "ManualAt");
            manualTimes.put(master, manualAt);
            List<Boolean> manual = new ArrayList<>();
            for (int slot = 1; slot <= master.blockSlots(); slot++)
                manual.add(Boolean.TRUE.equals(configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "Block" + slot + "Manual", boolean.class)));
            manualSlots.put(master, manual);
            long updatedAt = Math.max(syncedAt, manualAt);
            staleMasters.add(updatedAt == 0 || System.currentTimeMillis() - updatedAt > BLOCK_STALE_MILLIS);
            if ((syncedAt <= 0 || failedSyncs.contains(master) || System.currentTimeMillis() - updatedAt > BLOCK_STALE_MILLIS)
                && enabled) unsynced.add(master);
            if (selectedMonster != null && enabled)
                ranked.add(new MasterOdds(master, SlayerOdds.calculateForMaster(master, tasks.get(master),
                    selectedMonster, effectiveBlocks(blocks, syncedAt, manual, legacyImported), state)));
        }
        warnUnsynced(unsynced);
        int fingerprint = Objects.hash(state.stats, state.quests, state.unlocks, state.other, state.currentTask, state.previousTaskMaster,
            state.slayerCapePerkActive, state.mortimerAccess, state.morytaniaAccess, state.slayerCompletionObserved, state.mortimerChoices, state.mortimerTasksCompleted, mortimerDestination,
            allBlocks, syncTimes, manualTimes, manualSlots, staleMasters, failedSyncs, legacyImported, enabledMasters, selectedMonster,
            detected, lastSyncedMaster, lastSyncedAt, lastFailure, syncing, config.hideZeroChanceMasters(),
            config.showOddsTooltips(), config.routeToBestMaster(), config.showMortimer(), config.routeToMortimer(), config.expandMortimer(), config.showMortimerModifiers());
        if (lastRender != null && lastRender == fingerprint) return;
        lastRender = fingerprint;
        ranked.sort(Comparator.comparingDouble((MasterOdds item) -> item.result.chance()).reversed()
            .thenComparingInt(item -> item.master.ordinal()));
        MasterOdds best = ranked.stream()
            .filter(row -> row.master != SlayerMaster.MORTIMER || (config.routeToMortimer() && mortimerDestination != null))
            .max(Comparator.comparingDouble(row -> row.result.chance())).orElse(null);
        updateBestMasterRoute(best == null ? null : best.master,
            best == null ? 0 : best.result.chance());
        List<SlayerMaster> masters = new ArrayList<>();
        List<SlayerOdds.Result> results = new ArrayList<>();
        for (MasterOdds row : ranked) { masters.add(row.master); results.add(row.result); }
        Map<SlayerMaster, List<String>> savedBlocks = new EnumMap<>(SlayerMaster.class);
        for (SlayerMaster master : SlayerMaster.values())
        {
            savedBlocks.put(master, readBlocks(master));
        }
        String selection = selectedMonster;
        SlayerMaster syncedMaster = lastSyncedMaster;
        long syncedAt = lastSyncedAt;
        String syncFailure = lastFailure;
        boolean currentlySyncing = syncing;
        Set<SlayerMaster> failures = failedSyncs.isEmpty() ? EnumSet.noneOf(SlayerMaster.class) : EnumSet.copyOf(failedSyncs);
        SwingUtilities.invokeLater(() -> {
            if (running && panel == target)
            {
                target.showResults(monsters, selection, detected, masters, results, state,
                    config.hideZeroChanceMasters(), config.showOddsTooltips(), config.showMortimer(), config.expandMortimer(), config.showMortimerModifiers(), unsynced, failures);
                target.showSyncStatus(syncedMaster, syncedAt, syncFailure, detected != null, currentlySyncing);
                target.editBlocks(savedBlocks, syncTimes, manualTimes, manualSlots, legacyImported);
            }
        });
    }

    private void loadLastSyncedStatus()
    {
        lastSyncedMaster = SlayerMaster.byKey(configManager.getRSProfileConfiguration(
            SlayerOddsConfig.GROUP, "lastSyncedMaster"));
        String timestamp = configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "lastSyncedAt");
        try { lastSyncedAt = timestamp == null ? 0 : Long.parseLong(timestamp); }
        catch (NumberFormatException ex) { lastSyncedAt = 0; }
        if (lastSyncedMaster == null) lastSyncedAt = 0;
    }

    private void warnUnsynced(Set<SlayerMaster> unsynced)
    {
        if (selectedMonster == null || unsynced.isEmpty() || warnedUnsynced || !config.postSyncChatMessages()) return;
        chat("Slayer Task Odds: ~ masters use saved blocks that need checking. Sync their task lists to confirm.");
        warnedUnsynced = true;
    }

    private long readSyncTime(SlayerMaster master)
    {
        return readTimestamp(master.key + "SyncedAt");
    }

    static boolean masterEnabled(SlayerOddsConfig config, SlayerMaster master)
    {
        switch (master)
        {
            case TURAEL: return config.showTurael();
            case KRYSTILIA: return config.showKrystilia();
            case MAZCHNA: return config.showMazchna();
            case VANNAKA: return config.showVannaka();
            case CHAELDAR: return config.showChaeldar();
            case KONAR: return config.showKonar();
            case NIEVE: return config.showNieve();
            case DURADEL: return config.showDuradel();
            case MORTIMER: return config.showMortimer();
            default: return false;
        }
    }

    private long readTimestamp(String key)
    {
        String timestamp = configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, key);
        try { return timestamp == null ? 0 : Long.parseLong(timestamp); }
        catch (NumberFormatException ex) { return 0; }
    }

    static Set<String> effectiveBlocks(List<String> blocks, long syncedAt, List<Boolean> manual, boolean legacyImported)
    {
        if (syncedAt > 0 || legacyImported) return SlayerOdds.blocks(blocks.toArray(new String[0]));
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < blocks.size() && i < manual.size(); i++)
            if (Boolean.TRUE.equals(manual.get(i))) selected.add(blocks.get(i));
        return SlayerOdds.blocks(selected.toArray(new String[0]));
    }

    private void ensureAccountLoaded()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        String account = configManager.getRSProfileKey();
        if (account == null || account.equals(loadedAccount)) return;
        boolean importLegacy = configManager.getConfiguration(SlayerOddsConfig.GROUP, "accountBlocksMigrated") == null;
        boolean importedBlocks = false;
        syncing = true;
        try
        {
            for (SlayerMaster master : SlayerMaster.values())
                for (int slot = 1; slot <= master.blockSlots(); slot++)
                {
                    String key = master.key + "Block" + slot;
                    String value = configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, key);
                    if (value == null && importLegacy)
                    {
                        value = configManager.getConfiguration(SlayerOddsConfig.GROUP, key);
                        if (value != null)
                        {
                            SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(value);
                            if (choice != null && choice != SlayerTaskChoice.NONE) importedBlocks = true;
                            configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, key, value);
                        }
                    }
                    configManager.setConfiguration(SlayerOddsConfig.GROUP, key, value == null ? SlayerTaskChoice.NONE.name() : value);
                }
            if (importLegacy)
            {
                if (importedBlocks) configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "legacyBlocksImported", true);
                configManager.setConfiguration(SlayerOddsConfig.GROUP, "accountBlocksMigrated", "true");
            }
        }
        finally { syncing = false; }
        loadedAccount = account;
        failedSyncs.clear();
        for (SlayerMaster master : SlayerMaster.values())
            if (Boolean.TRUE.equals(configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, master.key + "SyncFailed", boolean.class)))
                failedSyncs.add(master);
        warnedUnsynced = false;
        selectedMonster = null;
        resetRouteSelection();
        resetSync();
        lastFailure = null;
        lastFailureMaster = null;
        loadLastSyncedStatus();
        mortimerDestination = null;
        mortimerLocationAt = 0;
        String location = configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "mortimerLocation");
        String locationAt = configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "mortimerLocationAt");
        if (location != null)
        {
            try {
                String[] parts = location.split(",");
                mortimerDestination = new WorldPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                mortimerLocationAt = Long.parseLong(locationAt);
            } catch (RuntimeException ignored) { mortimerDestination = null; }
        }
        lastRender = null;
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
        for (int slot = 1; slot <= master.blockSlots(); slot++)
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
            for (int slot = 1; slot <= master.blockSlots(); slot++)
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
