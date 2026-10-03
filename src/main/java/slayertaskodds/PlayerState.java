package slayertaskodds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class PlayerState
{
    final Map<String, Boolean> checks = new HashMap<>();
    final Map<String, Integer> stats = new HashMap<>();
    final Set<String> quests = new HashSet<>();
    final Set<String> unlocks = new HashSet<>();
    final Set<String> other = new HashSet<>();

    boolean eligible(SlayerTask task)
    {
        for (Map.Entry<String, Integer> requirement : task.stats.entrySet())
            if (stats.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) return false;
        if (!quests.containsAll(task.quests)) return false;
        if (task.other != null && !other.contains(task.other)) return false;
        if ("Stop the Wyvern".equals(task.unlock)) return !unlocks.contains(task.unlock);
        return task.unlock == null || unlocks.contains(task.unlock);
    }

    int weight(SlayerTask task)
    {
        return task.bonusUnlock != null && unlocks.contains(task.bonusUnlock) ? task.bonusWeight : task.weight;
    }
}
