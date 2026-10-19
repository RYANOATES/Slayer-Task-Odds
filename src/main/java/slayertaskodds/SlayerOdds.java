package slayertaskodds;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class SlayerOdds
{
    private SlayerOdds() { }

    static Result calculate(Iterable<SlayerTask> tasks, String monster, Set<String> blocked, PlayerState state)
    {
        int total = 0, target = 0, count = 0;
        for (SlayerTask task : tasks)
        {
            if (blocked.contains(SlayerTaskChoice.identity(task.name)) || state.isCurrentTask(task.name)
                || !state.eligible(task)) continue;
            int weight = state.weight(task);
            total += weight;
            count++;
            if (SlayerTaskChoice.identity(task.name).equals(SlayerTaskChoice.identity(monster))) target += weight;
        }
        double probability = total == 0 ? 0 : (double) target / total;
        String formula = target + " / " + total + " weight";
        for (SlayerTask task : tasks)
        {
            if (task.subtasks == null || !state.eligible(task) || state.isCurrentTask(task.name)
                || blocked.contains(SlayerTaskChoice.identity(task.name))) continue;
            Result sub = calculate(task.subtasks, monster, blocked, state);
            if (total > 0 && sub.chance() > 0)
            {
                probability += (double) state.weight(task) / total * sub.chance();
                formula = state.weight(task) + " / " + total + " x " + sub.formula();
            }
        }
        String explanation = probability == 0
            ? unavailableReason(tasks, monster, blocked, state)
            : probability * 100.0 < 0.005
                ? "This task is available, but its chance is below 0.005% and rounds to 0.00%."
                : "This task is available from this master.";
        return new Result(target, total, count, probability, formula, explanation);
    }

    private static String unavailableReason(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state)
    {
        if (state.isCurrentTask(monster))
            return "Your current task cannot be assigned again until you receive a different task.";
        Availability availability = new Availability();
        inspect(tasks, monster, blocked, state, new ArrayList<>(), false, availability);
        if (!availability.found) return "This master never assigns this task.";
        if (!availability.unblocked) return "Blocked in this master's saved block list.";
        if (!availability.missingRequirements.isEmpty())
        {
            List<String> missing = new ArrayList<>(availability.missingRequirements);
            Collections.sort(missing, String.CASE_INSENSITIVE_ORDER);
            return "Requirements not met: " + String.join(", ", missing) + ".";
        }
        return "This task has no eligible weight for this master and account.";
    }

    private static void inspect(Iterable<SlayerTask> tasks, String monster, Set<String> blocked,
        PlayerState state, List<SlayerTask> parents, boolean blockedParent, Availability result)
    {
        for (SlayerTask task : tasks)
        {
            boolean blockedHere = blockedParent || blocked.contains(SlayerTaskChoice.identity(task.name));
            parents.add(task);
            if (SlayerTaskChoice.identity(task.name).equals(SlayerTaskChoice.identity(monster)))
            {
                result.found = true;
                if (!blockedHere)
                {
                    result.unblocked = true;
                    for (SlayerTask requirement : parents) collectMissing(requirement, state, result.missingRequirements);
                }
            }
            if (task.subtasks != null)
                inspect(task.subtasks, monster, blocked, state, parents, blockedHere, result);
            parents.remove(parents.size() - 1);
        }
    }

    private static void collectMissing(SlayerTask task, PlayerState state, Set<String> missing)
    {
        task.stats.forEach((skill, required) -> {
            int actual = state.stats.getOrDefault(skill, 0);
            if (actual < required) missing.add(skill + " level " + required + " (you have " + actual + ")");
        });
        for (String quest : task.quests)
            if (!state.quests.contains(quest)) missing.add("quest: " + quest);
        boolean hasUnlock = task.unlock != null && state.unlocks.contains(task.unlock);
        if (task.unlock != null && ("Stop the Wyvern".equals(task.unlock) ? hasUnlock : !hasUnlock))
            missing.add(task.unlock.equals("Stop the Wyvern")
                ? "the Stop the Wyvern toggle must be off" : "unlock: " + task.unlock);
        if (task.other != null && !state.other.contains(task.other)) missing.add(task.other);
    }

    private static final class Availability
    {
        private boolean found;
        private boolean unblocked;
        private final Set<String> missingRequirements = new LinkedHashSet<>();
    }

    static Set<String> blocks(String... values)
    {
        Set<String> result = new HashSet<>();
        for (String value : values)
            if (value != null && SlayerTaskChoice.fromStoredValue(value) != SlayerTaskChoice.NONE)
                result.add(SlayerTaskChoice.identity(value));
        return result;
    }

    static final class Result
    {
        final int targetWeight, totalWeight, count;
        private final double probability;
        private final String formula;
        private final String explanation;
        Result(int targetWeight, int totalWeight, int count, double probability, String formula, String explanation)
        { this.targetWeight = targetWeight; this.totalWeight = totalWeight; this.count = count; this.probability = probability; this.formula = formula; this.explanation = explanation; }
        double chance() { return probability; }
        String formula() { return formula; }
        String explanation() { return explanation; }
    }
}
