package slayertaskodds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Proposed per-task ranges from the OSRS Wiki; no network access is used at runtime. */
final class MortimerModifiers
{
    private static final Map<String, String[]> RANGES = load();
    private static final String[] LABELS = {"Task quantity", "Slayer points", "Clue rate", "Slayer XP", "Superior unique rate"};
    private static final int[] UNLOCKS = {0, 0, 15, 40, 25};

    private MortimerModifiers() { }

    static List<String> breakdown(String task, int completed, double taskChance)
    {
        if (task == null) return Collections.emptyList();
        String[] ranges = RANGES.get(SlayerTaskChoice.identity(task));
        if (ranges == null) return Collections.emptyList();
        int available = 0;
        for (int i = 0; i < LABELS.length; i++)
            if (!ranges[i].isEmpty() && completed >= UNLOCKS[i]) available++;
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++)
        {
            if (ranges[i].isEmpty())
            {
                lines.add(LABELS[i] + ": not available for this task");
                continue;
            }
            String range = ranges[i].replace("+ ", "+").replace('-', '–');
            if (i == 0 && ranges[i].startsWith("-")) range = "−15 to −30";
            if (i == 1) range += " points";
            if (i == 0) range += " kills";
            if (i >= 2) range = "+" + range;
            if (completed < UNLOCKS[i])
                lines.add(LABELS[i] + ": " + range + " · unlocks at " + UNLOCKS[i] + " tasks");
            else
                lines.add(String.format(Locale.ROOT, "%s: %s · %.2f%% modifier | %.2f%% task + modifier",
                    LABELS[i], range, 100.0 / available, 100.0 * taskChance / available));
        }
        return lines;
    }

    private static Map<String, String[]> load()
    {
        InputStream stream = MortimerModifiers.class.getResourceAsStream("/slayertaskodds/mortimer-modifiers.tsv");
        if (stream == null) return Collections.emptyMap();
        Map<String, String[]> data = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] columns = line.split("\\|", -1);
                if (columns.length == 6)
                    data.put(SlayerTaskChoice.identity(columns[0]), new String[]{columns[1], columns[2], columns[3], columns[4], columns[5]});
            }
        }
        catch (IOException ex) { return Collections.emptyMap(); }
        return Collections.unmodifiableMap(data);
    }
}
