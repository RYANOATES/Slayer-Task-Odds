package slayertaskodds;

import java.util.List;
import java.util.Map;

final class SlayerTask
{
    String name;
    int weight;
    Map<String, Integer> stats;
    List<String> quests;
    String unlock;
    String other;
    String bonusUnlock;
    int bonusWeight;
    List<SlayerTask> subtasks;
}
