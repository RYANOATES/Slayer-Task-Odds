package slayertaskodds;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SlayerOdds
{
    private SlayerOdds() { }

    static Result calculateForMaster(SlayerMaster master, Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state)
    {
        List<String> missing = new ArrayList<>();
        int combatRequired;
        switch (master)
        {
            case MAZCHNA: combatRequired = 20; break;
            case VANNAKA: combatRequired = 40; break;
            case CHAELDAR: combatRequired = 70; break;
            case KONAR: combatRequired = 75; break;
            case NIEVE: combatRequired = 85; break;
            case DURADEL:
            case MORTIMER: combatRequired = 100; break;
            default: combatRequired = 0;
        }
        int slayer = state.stats.getOrDefault("Slayer", 0);
        int combat = state.stats.getOrDefault("Combat", 0);
        if (combat < combatRequired && slayer < 99)
            missing.add("Combat level " + combatRequired + " (you have " + combat + ")");
        if (master == SlayerMaster.DURADEL && slayer < 50)
            missing.add("Slayer level 50 (you have " + slayer + ")");
        if (master == SlayerMaster.MORTIMER && slayer < 70)
            missing.add("Slayer level 70 (you have " + slayer + ")");
        if (master == SlayerMaster.CHAELDAR && !state.quests.contains("Lost City"))
            missing.add("quest: Lost City");
        if (master == SlayerMaster.MAZCHNA && !state.morytaniaAccess)
            missing.add("Morytania access (Priest in Peril)");
        if (master == SlayerMaster.DURADEL && !state.quests.contains("Shilo Village"))
            missing.add("quest: Shilo Village");
        if (master == SlayerMaster.MORTIMER && !state.mortimerAccess)
            missing.add("Mortimer access");
        if (!missing.isEmpty())
            return new Result(0, 0, 0, 0, "", "Requirements not met: " + String.join(", ", missing) + ".");

        Result result = master == SlayerMaster.MORTIMER
            ? calculateMortimer(tasks, monster, blocked, state) : calculate(master, tasks, monster, blocked, state);
        if (master == SlayerMaster.KRYSTILIA && !state.slayerCompletionObserved)
        {
            String note = " Krystilia requires a completed Slayer task; your completion history could not be confirmed.";
            return new Result(result.targetWeight, result.totalWeight, result.count, result.chance(),
                result.formula(), result.explanation() + note);
        }
        if (combat < combatRequired && slayer >= 99)
        {
            String note = " Show your Slayer cape to the master to bypass the combat requirement.";
            String tooltip = result.tooltip == null ? null : result.tooltip.replace("</html>", "<br>" + note.trim() + "</html>");
            return new Result(result.targetWeight, result.totalWeight, result.count, result.chance(),
                result.formula(), result.explanation() + note, tooltip);
        }
        return result;
    }

    static Result calculateMortimer(Iterable<SlayerTask> tasks, String monster, Set<String> blocked, PlayerState state)
    {
        if (state.stats.getOrDefault("Slayer", 0) < 70 ||
            (state.stats.getOrDefault("Slayer", 0) < 99 && state.stats.getOrDefault("Combat", 0) < 100) || !state.mortimerAccess)
            return new Result(0, 0, 0, 0, "", "Requirements not met: Mortimer access, 70 Slayer and 100 Combat (or 99 Slayer). ");
        int target = 0, total = 0, count = 0;
        List<Integer> others = new ArrayList<>();
        for (SlayerTask task : tasks)
        {
            if (blocked.contains(SlayerTaskChoice.identity(task.name)) || !state.eligible(task)) continue;
            int weight = state.weight(task);
            total += weight;
            count++;
            if (SlayerTaskChoice.identity(task.name).equals(SlayerTaskChoice.identity(monster))) target += weight;
            else others.add(weight);
        }
        int[] weights = others.stream().mapToInt(Integer::intValue).toArray();
        double chance = MortimerOdds.chance(target, weights, state.mortimerChoices);
        String explanation = chance == 0 ? unavailableReason(tasks, monster, blocked, state, true)
            : "Chance this task appears among Mortimer's " + state.mortimerChoices + " choices. Slayer cape repeat offers are separate.";
        String tooltip = String.format(Locale.ROOT,
            "<html>%s<br>Single draw: %.2f%%<br>Two offers: %.2f%%%s<br>Three offers: %.2f%%%s</html>",
            explanation, MortimerOdds.chance(target, weights, 1) * 100,
            MortimerOdds.chance(target, weights, 2) * 100, state.mortimerChoices == 2 ? " (your offers)" : "",
            MortimerOdds.chance(target, weights, 3) * 100, state.mortimerChoices == 3 ? " (your offers)" : "");
        return new Result(target, total, count, chance, target + " / " + total + " | " + state.mortimerChoices + " choices", explanation, tooltip);
    }

    static Result calculate(Iterable<SlayerTask> tasks, String monster, Set<String> blocked, PlayerState state)
    {
        return calculate(null, tasks, monster, blocked, state);
    }

    private static Result calculate(SlayerMaster master, Iterable<SlayerTask> tasks, String monster, Set<String> blocked, PlayerState state)
    {
        Result weighted = calculateWeighted(tasks, monster, blocked, state);
        if (state.slayerCapePerkActive && master != null && state.previousTaskMaster == null && state.currentTask != null)
            return new Result(weighted.targetWeight, weighted.totalWeight, weighted.count, weighted.chance(),
                weighted.formula(), weighted.explanation() + " Previous task master is unknown; cape repeat is not included.");
        if (!state.slayerCapePerkActive || master == null || master != state.previousTaskMaster || state.currentTask == null
            || !isAvailable(tasks, state.currentTask, blocked, state))
            return weighted;

        if (state.isCurrentTask(monster))
            return new Result(weighted.targetWeight, weighted.totalWeight, weighted.count, 0.10,
                "10% Slayer cape repeat", "The Slayer cape offers a 10% chance to repeat your previous task from the same master, if you accept the repeat.");

        if (weighted.chance() == 0)
            return weighted;

        return new Result(weighted.targetWeight, weighted.totalWeight, weighted.count,
            weighted.chance() * 0.90, weighted.formula() + " x 90%",
            "If you accept a Slayer cape repeat when offered, 10% is reserved for the previous task from this master.");
    }

    private static Result calculateWeighted(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state)
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
            Result sub = calculateWeighted(task.subtasks, monster, blocked, state);
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

    private static boolean isAvailable(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state)
    {
        return isAvailable(tasks, monster, blocked, state, false, true);
    }

    private static boolean isAvailable(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state, boolean blockedParent, boolean eligibleParent)
    {
        for (SlayerTask task : tasks)
        {
            boolean blockedHere = blockedParent || blocked.contains(SlayerTaskChoice.identity(task.name));
            boolean eligibleHere = eligibleParent && state.eligible(task);
            if (SlayerTaskChoice.identity(task.name).equals(SlayerTaskChoice.identity(monster)))
                return !blockedHere && eligibleHere;
            if (task.subtasks != null && isAvailable(task.subtasks, monster, blocked, state,
                blockedHere, eligibleHere))
                return true;
        }
        return false;
    }

    private static String unavailableReason(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state)
    {
        return unavailableReason(tasks, monster, blocked, state, false);
    }

    private static String unavailableReason(Iterable<SlayerTask> tasks, String monster,
        Set<String> blocked, PlayerState state, boolean allowRepeat)
    {
        if (!allowRepeat && state.isCurrentTask(monster))
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
        private final String tooltip;
        Result(int targetWeight, int totalWeight, int count, double probability, String formula, String explanation)
        { this(targetWeight, totalWeight, count, probability, formula, explanation, null); }
        Result(int targetWeight, int totalWeight, int count, double probability, String formula, String explanation, String tooltip)
        { this.targetWeight = targetWeight; this.totalWeight = totalWeight; this.count = count; this.probability = probability; this.formula = formula; this.explanation = explanation; this.tooltip = tooltip; }
        double chance() { return probability; }
        String formula() { return formula; }
        String explanation() { return explanation; }
        String tooltip() { return tooltip; }
    }
}
