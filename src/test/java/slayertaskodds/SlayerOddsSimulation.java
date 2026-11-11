package slayertaskodds;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.mockito.Mockito;

/** Development-only simulation of profile settings, sync failure and refresh scheduling. */
final class SlayerOddsSimulation
{
    static void verify() throws Exception
    {
        SlayerOddsPlugin plugin = new SlayerOddsPlugin();
        Map<String, String> global = new HashMap<>();
        Map<String, Map<String, String>> accounts = new HashMap<>();
        String[] account = {"rsprofile.a"};
        List<Runnable> queue = new ArrayList<>();
        Client client = Mockito.mock(Client.class);
        Mockito.when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        ClientThread thread = Mockito.mock(ClientThread.class);
        Mockito.doAnswer(call -> { queue.add(call.getArgument(0)); return null; }).when(thread).invokeLater(Mockito.any(Runnable.class));
        SlayerOddsConfig settings = Mockito.mock(SlayerOddsConfig.class);
        ConfigManager manager = Mockito.mock(ConfigManager.class, call -> {
            String name = call.getMethod().getName();
            Object[] args = call.getArguments();
            Map<String, String> local = accounts.computeIfAbsent(account[0], key -> new HashMap<>());
            if (name.equals("getRSProfileKey")) return account[0];
            if (name.equals("getConfiguration") && args.length == 2) return global.get(args[1]);
            if (name.equals("getRSProfileConfiguration") && args.length >= 2)
            {
                String value = local.get(args[1]);
                return args.length == 3 && args[2] == boolean.class ? value == null ? null : Boolean.valueOf(value) : value;
            }
            if ((name.equals("setConfiguration") || name.equals("setRSProfileConfiguration")) && args.length == 3)
            {
                Map<String, String> data = name.equals("setConfiguration") ? global : local;
                String key = (String) args[1], value = String.valueOf(args[2]);
                String old = data.put(key, value);
                if (!java.util.Objects.equals(old, value)) event(plugin, account[0], name, key, old, value);
                return null;
            }
            if (name.equals("unsetRSProfileConfiguration"))
            {
                String key = (String) args[1];
                String old = local.remove(key);
                if (old != null) event(plugin, account[0], name, key, old, null);
                return null;
            }
            return Mockito.RETURNS_DEFAULTS.answer(call);
        });
        inject(plugin, "client", client);
        inject(plugin, "clientThread", thread);
        inject(plugin, "configManager", manager);
        inject(plugin, "config", settings);
        inject(plugin, "running", true);
        global.put("nieveBlock1", "GREATER_DEMONS");
        call(plugin, "ensureAccountLoaded");
        require("GREATER_DEMONS".equals(accounts.get(account[0]).get("nieveBlock1")), "Legacy blocks migrate to first character");
        account[0] = "rsprofile.b";
        call(plugin, "ensureAccountLoaded");
        require("NONE".equals(global.get("nieveBlock1")), "Second character starts with its own list");
        account[0] = "rsprofile.a";
        call(plugin, "ensureAccountLoaded");
        require("GREATER_DEMONS".equals(global.get("nieveBlock1")), "Switching back restores the first list");

        inject(plugin, "selectedMonster", "Abyssal demons");
        call(plugin, "warnUnsynced", new Class<?>[]{java.util.Set.class}, java.util.Collections.singleton(SlayerMaster.NIEVE));
        Mockito.verify(client, Mockito.never()).addChatMessage(Mockito.eq(net.runelite.api.ChatMessageType.GAMEMESSAGE), Mockito.eq(""), Mockito.contains("~ masters"), Mockito.eq(""));
        Mockito.when(settings.postSyncChatMessages()).thenReturn(true);
        call(plugin, "warnUnsynced", new Class<?>[]{java.util.Set.class}, java.util.Collections.singleton(SlayerMaster.NIEVE));
        call(plugin, "warnUnsynced", new Class<?>[]{java.util.Set.class}, java.util.Collections.emptySet());
        call(plugin, "warnUnsynced", new Class<?>[]{java.util.Set.class}, java.util.Collections.singleton(SlayerMaster.DURADEL));
        Mockito.verify(client, Mockito.times(1)).addChatMessage(Mockito.eq(net.runelite.api.ChatMessageType.GAMEMESSAGE), Mockito.eq(""), Mockito.contains("~ masters"), Mockito.eq(""));
        Mockito.when(settings.postSyncChatMessages()).thenReturn(false);

        inject(plugin, "openTaskMaster", SlayerMaster.NIEVE);
        manager.setConfiguration(SlayerOddsConfig.GROUP, "nieveBlock1", "ABYSSAL_DEMONS");
        require("GREATER_DEMONS".equals(global.get("nieveBlock1")), "Open master screen rejects manual edits");
        require("GREATER_DEMONS".equals(accounts.get(account[0]).get("nieveBlock1")), "Rejected edit does not change character data");
        inject(plugin, "openTaskMaster", null);
        manager.setConfiguration(SlayerOddsConfig.GROUP, "nieveBlock1", "ABYSSAL_DEMONS");
        require("ABYSSAL_DEMONS".equals(accounts.get(account[0]).get("nieveBlock1")), "Closed screen permits manual edits");
        inject(plugin, "loadedAccount", null);
        manager.setConfiguration(SlayerOddsConfig.GROUP, "nieveBlock1", "GREATER_DEMONS");
        require("ABYSSAL_DEMONS".equals(global.get("nieveBlock1")), "Logged-out edits are reverted");
        inject(plugin, "loadedAccount", account[0]);

        queue.clear();
        // Drain/reset any pending refresh from account loading before counting a burst.
        inject(plugin, "refreshPending", new java.util.concurrent.atomic.AtomicBoolean());
        call(plugin, "refresh");
        call(plugin, "refresh");
        call(plugin, "refresh");
        require(queue.size() == 1, "Refresh bursts are coalesced");
        queue.remove(0).run();
        call(plugin, "refresh");
        require(queue.size() == 1, "Refresh after a completed refresh is not lost");
        queue.clear();
        inject(plugin, "refreshPending", new java.util.concurrent.atomic.AtomicBoolean());

        call(plugin, "syncMaster", new Class<?>[]{SlayerMaster.class, boolean.class}, SlayerMaster.NIEVE, true);
        String syncedAt = accounts.get(account[0]).get("nieveSyncedAt");
        require(syncedAt != null, "Successful sync saves timestamp");
        call(plugin, "recordSyncFailure", new Class<?>[]{SlayerMaster.class, RuntimeException.class}, SlayerMaster.NIEVE, new IllegalStateException("test failure"));
        require(syncedAt.equals(accounts.get(account[0]).get("nieveSyncedAt")), "Failure preserves the last successful sync");
        require("true".equals(accounts.get(account[0]).get("nieveSyncFailed")), "Failure status survives reload");
        require(queue.size() == 1, "Sync writes and failure coalesce to one queued refresh");
        call(plugin, "syncMaster", new Class<?>[]{SlayerMaster.class, boolean.class}, SlayerMaster.NIEVE, true);
        require(field(plugin, "lastFailure") == null, "A successful retry clears the failure");
        require(!accounts.get(account[0]).containsKey("nieveSyncFailed"), "Successful retry clears stored failure");
    }

    private static void event(SlayerOddsPlugin plugin, String profile, String name, String key, String old, String value)
    {
        ConfigChanged event = new ConfigChanged();
        event.setGroup(SlayerOddsConfig.GROUP);
        event.setProfile(name.equals("setConfiguration") ? null : profile);
        event.setKey(key);
        event.setOldValue(old);
        event.setNewValue(value);
        plugin.onConfigChanged(event);
    }

    private static void inject(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object field(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void call(Object target, String name) throws Exception { call(target, name, new Class<?>[0]); }
    private static void call(Object target, String name, Class<?>[] types, Object... args) throws Exception
    {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static void require(boolean result, String message)
    {
        if (!result) throw new AssertionError(message);
    }
}
