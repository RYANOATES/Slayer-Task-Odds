package slayertaskodds;

/** Probability of a task appearing in a weighted choice without replacement. */
final class MortimerOdds
{
    private MortimerOdds() { }

    static double chance(int targetWeight, int[] otherWeights, int choices)
    {
        if (targetWeight <= 0 || choices <= 0) return 0;
        int total = targetWeight;
        for (int weight : otherWeights) total += Math.max(0, weight);
        return 1.0 - miss(targetWeight, otherWeights, total, choices, new java.util.BitSet(otherWeights.length));
    }

    private static double miss(int target, int[] weights, int total, int remaining, java.util.BitSet selected)
    {
        if (remaining == 0) return 1.0;
        if (total <= target) return 0.0;
        double probability = 0.0;
        for (int i = 0; i < weights.length; i++)
        {
            if (weights[i] <= 0 || selected.get(i)) continue;
            selected.set(i);
            probability += (double) weights[i] / total
                * miss(target, weights, total - weights[i], remaining - 1, selected);
            selected.clear(i);
        }
        return probability;
    }
}
