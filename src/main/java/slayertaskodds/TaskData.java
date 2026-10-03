package slayertaskodds;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.EnumMap;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class TaskData
{
    private TaskData() { }

    static Map<SlayerMaster, List<SlayerTask>> load()
    {
        InputStream stream = TaskData.class.getResourceAsStream("/slayertaskodds/wiki-tasks.json");
        if (stream == null) throw new IllegalStateException("Missing Slayer task data");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            Gson gson = new Gson();
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            Map<SlayerMaster, List<SlayerTask>> tasks = new EnumMap<>(SlayerMaster.class);
            for (Map.Entry<String, JsonElement> entry : root.entrySet())
            {
                SlayerMaster master = SlayerMaster.valueOf(entry.getKey());
                List<SlayerTask> masterTasks = new ArrayList<>();
                for (JsonElement task : entry.getValue().getAsJsonArray())
                    masterTasks.add(gson.fromJson(task, SlayerTask.class));
                tasks.put(master, masterTasks);
            }
            return tasks;
        }
        catch (java.io.IOException ex) { throw new IllegalStateException("Cannot read Slayer task data", ex); }
    }

    static List<String> monsters(Map<SlayerMaster, List<SlayerTask>> tasks)
    {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (List<SlayerTask> table : tasks.values())
            for (SlayerTask task : table) { names.add(task.name); if (task.subtasks != null) for (SlayerTask sub : task.subtasks) names.add(sub.name); }
        return Collections.unmodifiableList(new ArrayList<>(names));
    }
}
