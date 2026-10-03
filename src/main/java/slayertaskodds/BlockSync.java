package slayertaskodds;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;

final class BlockSync
{
    static final int SLOT_COUNT = 7;
    private static final Map<SlayerMaster, int[]> VARBITS = new EnumMap<>(SlayerMaster.class);
    static
    {
        VARBITS.put(SlayerMaster.TURAEL, new int[] {VarbitID.SLAYER_BLOCKED_TURAEL_1, VarbitID.SLAYER_BLOCKED_TURAEL_2, VarbitID.SLAYER_BLOCKED_TURAEL_3, VarbitID.SLAYER_BLOCKED_TURAEL_4, VarbitID.SLAYER_BLOCKED_TURAEL_5, VarbitID.SLAYER_BLOCKED_TURAEL_6, VarbitID.SLAYER_BLOCKED_TURAEL_DIARY});
        VARBITS.put(SlayerMaster.KRYSTILIA, new int[] {VarbitID.SLAYER_BLOCKED_KRYSTILIA_1, VarbitID.SLAYER_BLOCKED_KRYSTILIA_2, VarbitID.SLAYER_BLOCKED_KRYSTILIA_3, VarbitID.SLAYER_BLOCKED_KRYSTILIA_4, VarbitID.SLAYER_BLOCKED_KRYSTILIA_5, VarbitID.SLAYER_BLOCKED_KRYSTILIA_6, VarbitID.SLAYER_BLOCKED_KRYSTILIA_DIARY});
        VARBITS.put(SlayerMaster.MAZCHNA, new int[] {VarbitID.SLAYER_BLOCKED_MAZCHNA_1, VarbitID.SLAYER_BLOCKED_MAZCHNA_2, VarbitID.SLAYER_BLOCKED_MAZCHNA_3, VarbitID.SLAYER_BLOCKED_MAZCHNA_4, VarbitID.SLAYER_BLOCKED_MAZCHNA_5, VarbitID.SLAYER_BLOCKED_MAZCHNA_6, VarbitID.SLAYER_BLOCKED_MAZCHNA_DIARY});
        VARBITS.put(SlayerMaster.VANNAKA, new int[] {VarbitID.SLAYER_BLOCKED_VANNAKA_1, VarbitID.SLAYER_BLOCKED_VANNAKA_2, VarbitID.SLAYER_BLOCKED_VANNAKA_3, VarbitID.SLAYER_BLOCKED_VANNAKA_4, VarbitID.SLAYER_BLOCKED_VANNAKA_5, VarbitID.SLAYER_BLOCKED_VANNAKA_6, VarbitID.SLAYER_BLOCKED_VANNAKA_DIARY});
        VARBITS.put(SlayerMaster.CHAELDAR, new int[] {VarbitID.SLAYER_BLOCKED_CHAELDAR_1, VarbitID.SLAYER_BLOCKED_CHAELDAR_2, VarbitID.SLAYER_BLOCKED_CHAELDAR_3, VarbitID.SLAYER_BLOCKED_CHAELDAR_4, VarbitID.SLAYER_BLOCKED_CHAELDAR_5, VarbitID.SLAYER_BLOCKED_CHAELDAR_6, VarbitID.SLAYER_BLOCKED_CHAELDAR_DIARY});
        VARBITS.put(SlayerMaster.KONAR, new int[] {VarbitID.SLAYER_BLOCKED_KONAR_1, VarbitID.SLAYER_BLOCKED_KONAR_2, VarbitID.SLAYER_BLOCKED_KONAR_3, VarbitID.SLAYER_BLOCKED_KONAR_4, VarbitID.SLAYER_BLOCKED_KONAR_5, VarbitID.SLAYER_BLOCKED_KONAR_6, VarbitID.SLAYER_BLOCKED_KONAR_DIARY});
        VARBITS.put(SlayerMaster.NIEVE, new int[] {VarbitID.SLAYER_BLOCKED_NIEVE_1, VarbitID.SLAYER_BLOCKED_NIEVE_2, VarbitID.SLAYER_BLOCKED_NIEVE_3, VarbitID.SLAYER_BLOCKED_NIEVE_4, VarbitID.SLAYER_BLOCKED_NIEVE_5, VarbitID.SLAYER_BLOCKED_NIEVE_6, VarbitID.SLAYER_BLOCKED_NIEVE_DIARY});
        VARBITS.put(SlayerMaster.DURADEL, new int[] {VarbitID.SLAYER_BLOCKED_DURADEL_1, VarbitID.SLAYER_BLOCKED_DURADEL_2, VarbitID.SLAYER_BLOCKED_DURADEL_3, VarbitID.SLAYER_BLOCKED_DURADEL_4, VarbitID.SLAYER_BLOCKED_DURADEL_5, VarbitID.SLAYER_BLOCKED_DURADEL_6, VarbitID.SLAYER_BLOCKED_DURADEL_DIARY});
    }

    static List<SlayerTaskChoice> read(Client client, SlayerMaster master)
    {
        List<SlayerTaskChoice> slots = new ArrayList<>();
        for (int varbit : VARBITS.get(master))
        {
            int taskId = client.getVarbitValue(varbit);
            if (taskId == 0) { slots.add(SlayerTaskChoice.NONE); continue; }
            List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
            if (rows == null || rows.isEmpty()) throw new IllegalStateException("Unknown blocked task ID " + taskId);
            Object[] names = client.getDBTableField(rows.get(0), DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
            if (names == null || names.length == 0) throw new IllegalStateException("Task names are not ready");
            SlayerTaskChoice choice = SlayerTaskChoice.fromStoredValue(String.valueOf(names[0]));
            if (choice == null) throw new IllegalStateException("Unsupported blocked task: " + names[0]);
            slots.add(choice);
        }
        return slots;
    }
}
