package slayertaskodds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

final class PlayerStateReader
{
    private static final Map<String, Integer> UNLOCKS = new HashMap<>();
    static
    {
        UNLOCKS.put("Seeing red", VarbitID.SLAYER_UNLOCK_REDDRAGONS);
        UNLOCKS.put("I hope you mith me", VarbitID.SLAYER_UNLOCK_MITHRILDRAGONS);
        UNLOCKS.put("Watch the birdie", VarbitID.SLAYER_UNLOCK_AVIANSIES);
        UNLOCKS.put("Hot stuff", VarbitID.SLAYER_UNLOCK_TZHAAR);
        UNLOCKS.put("Reptile got ripped", VarbitID.SLAYER_UNLOCK_LIZARDMEN);
        UNLOCKS.put("Like a boss", VarbitID.SLAYER_UNLOCK_BOSSES);
        UNLOCKS.put("Stop the Wyvern", VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK);
        UNLOCKS.put("Basilocked", VarbitID.SLAYER_UNLOCK_BASILISK);
        UNLOCKS.put("Unlock vampyres", VarbitID.SLAYER_UNLOCK_VAMPYRES);
        UNLOCKS.put("I Wildy More Slayer", VarbitID.SLAYER_UNLOCK_WILDY_EXTRATASKS);
        UNLOCKS.put("Warped Reality", VarbitID.SLAYER_UNLOCK_WARPED_CREATURES);
        UNLOCKS.put("Wings Spread", VarbitID.SLAYER_UNLOCK_GRYPHONS);
        UNLOCKS.put("Lured In", VarbitID.SLAYER_UNLOCK_AQUANITES);
        UNLOCKS.put("Chance of Heavy Frost", VarbitID.SLAYER_WEIGHTED_LONGER_FROST_DRAGONS);
    }

    static PlayerState read(Client client, ConfigManager configManager, Map<SlayerMaster, List<SlayerTask>> tables)
    {
        PlayerState state = new PlayerState();
        for (Skill skill : Skill.values())
            if (skill != Skill.OVERALL) state.stats.put(skill.getName(), client.getRealSkillLevel(skill));
        state.stats.put("Combat", client.getLocalPlayer().getCombatLevel());
        Set<String> requiredQuests = new HashSet<>();
        for (List<SlayerTask> tasks : tables.values()) collectQuests(tasks, requiredQuests);
        for (String name : requiredQuests)
        {
            Quest quest = quest(name);
            QuestState progress = quest.getState(client);
            boolean started = name.equals("Dragon Slayer") || name.equals("Desert Treasure");
            if (progress == QuestState.FINISHED || started && progress == QuestState.IN_PROGRESS)
                state.quests.add(name);
        }
        UNLOCKS.forEach((name, varbit) -> {
            if (client.getVarbitValue(varbit) > 0) state.unlocks.add(name);
        });
        if (client.getVarbitValue(VarbitID.SLAYER_TOGGLEOFF_FOSSILWYVERNBLOCK) > 0)
            state.unlocks.remove("Stop the Wyvern");
        if (client.getVarbitValue(VarbitID.BRUT_CRAFT_SHIP) >= 1)
            state.other.add("Ancient Cavern");
        if (state.stats.getOrDefault("Strength", 0) >= 60 || state.stats.getOrDefault("Agility", 0) >= 60)
            state.other.add("Access GWD");
        // Remember a key observed in inventory/bank for this RS account, even if later banked.
        boolean keyKnown = client.getVarbitValue(VarbitID.GARGBOSS_UNLOCKED_ROOF) > 0
            || containsKey(client.getItemContainer(InventoryID.INVENTORY))
            || containsKey(client.getItemContainer(InventoryID.BANK));
        boolean keyRemembered = Boolean.TRUE.equals(configManager.getRSProfileConfiguration(SlayerOddsConfig.GROUP, "brittleKeySeen", boolean.class));
        if (keyKnown && !keyRemembered) configManager.setRSProfileConfiguration(SlayerOddsConfig.GROUP, "brittleKeySeen", true);
        if (keyKnown || keyRemembered)
            state.other.add("Brittle Key");
        if (Quest.ENTER_THE_ABYSS.getState(client) == QuestState.FINISHED) state.other.add("Access Abyss");
        for (String name : requiredQuests) state.checks.put(name + (name.equals("Dragon Slayer") || name.equals("Desert Treasure") ? " started" : " completed"), state.quests.contains(name));
        for (String name : UNLOCKS.keySet()) state.checks.put(name, state.unlocks.contains(name));
        for (String name : new String[] {"Ancient Cavern", "Access GWD", "Brittle Key", "Access Abyss"})
            state.checks.put(name, state.other.contains(name));
        state.checks.put("Thieving at least 23", state.stats.getOrDefault("Thieving", 0) >= 23);
        state.checks.put("Firemaking at least 33", state.stats.getOrDefault("Firemaking", 0) >= 33);
        state.checks.put("Magic at least 50", state.stats.getOrDefault("Magic", 0) >= 50);
        return state;
    }

    private static boolean containsKey(ItemContainer container)
    {
        if (container == null) return false;
        for (Item item : container.getItems()) if (item.getId() == ItemID.BRITTLE_KEY) return true;
        return false;
    }

    private static void collectQuests(List<SlayerTask> tasks, Set<String> quests)
    {
        for (SlayerTask task : tasks)
        {
            quests.addAll(task.quests);
            if (task.subtasks != null) collectQuests(task.subtasks, quests);
        }
    }

    static Quest quest(String name)
    {
        switch (name)
        {
            case "Dragon Slayer": return Quest.DRAGON_SLAYER_I;
            case "Desert Treasure": return Quest.DESERT_TREASURE_I;
            case "Desert Treasure 2": return Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE;
            default: return Quest.valueOf(name.toUpperCase(Locale.ROOT).replace("!", "").replace(' ', '_'));
        }
    }
}
