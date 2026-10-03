package slayertaskodds;

import java.util.HashSet;
import java.util.Set;

final class SlayerOdds
{
    private SlayerOdds() { }

    static Result calculate(Iterable<SlayerTask> tasks, String monster, Set<String> blocked, PlayerState state)
    {
        int total = 0, target = 0, count = 0;
        for (SlayerTask task : tasks)
        {
            if (blocked.contains(SlayerTaskChoice.identity(task.name)) || !state.eligible(task)) continue;
            int weight = state.weight(task);
            total += weight;
            count++;
            if (SlayerTaskChoice.identity(task.name).equals(SlayerTaskChoice.identity(monster))) target += weight;
        }
        double probability = total == 0 ? 0 : (double) target / total;
        String formula = target + " / " + total + " weight";
        for (SlayerTask task : tasks)
        {
            if (task.subtasks == null || !state.eligible(task) || blocked.contains(SlayerTaskChoice.identity(task.name))) continue;
            Result sub = calculate(task.subtasks, monster, blocked, state);
            if (total > 0 && sub.chance() > 0)
            {
                probability += (double) state.weight(task) / total * sub.chance();
                formula = state.weight(task) + " / " + total + " x " + sub.formula();
            }
        }
        return new Result(target, total, count, probability, formula);
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
        Result(int targetWeight, int totalWeight, int count, double probability, String formula)
        { this.targetWeight = targetWeight; this.totalWeight = totalWeight; this.count = count; this.probability = probability; this.formula = formula; }
        double chance() { return probability; }
        String formula() { return formula; }
    }
}
