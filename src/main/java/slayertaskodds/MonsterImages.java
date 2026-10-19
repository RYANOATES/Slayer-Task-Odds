package slayertaskodds;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Small, bundled Wiki portraits. Accessed only on the Swing event thread. */
final class MonsterImages
{
    private final Properties catalog = new Properties();
    private final Map<String, ImageIcon> cache = new HashMap<>();
    private final ImageIcon fallback;

    MonsterImages()
    {
        fallback = read("/slayertaskodds/icon.png");
        try (InputStream stream = MonsterImages.class.getResourceAsStream("/slayertaskodds/monsters/catalog.properties"))
        {
            if (stream != null) catalog.load(stream);
        }
        catch (IOException ignored)
        {
            // A missing portrait must never prevent the calculator from opening.
        }
    }

    ImageIcon forTask(String task)
    {
        if (task == null) return fallback;
        String key = task.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (cache.containsKey(key)) return cache.get(key);
        String file = catalog.getProperty(key);
        ImageIcon image = file == null ? null : read("/slayertaskodds/monsters/" + file);
        if (image == null) image = fallback;
        cache.put(key, image);
        return image;
    }

    private static ImageIcon read(String path)
    {
        try (InputStream stream = MonsterImages.class.getResourceAsStream(path))
        {
            if (stream == null) return null;
            java.awt.image.BufferedImage image = ImageIO.read(stream);
            return image == null ? null : new ImageIcon(image);
        }
        catch (IOException ex) { return null; }
    }
}
