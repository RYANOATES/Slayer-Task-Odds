package slayertaskodds;

import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
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
        fallback = read("/slayertaskodds/portrait-fallback.png");
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
        if (image == null)
        {
            image = fallback;
        }
        if (image != null) image = fitPortrait(image);
        cache.put(key, image);
        return image;
    }

    private static ImageIcon fitPortrait(ImageIcon icon)
    {
        BufferedImage image = (BufferedImage) icon.getImage();
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
        }
        if (right < left) return icon;
        BufferedImage content = image.getSubimage(left, top, right - left + 1, bottom - top + 1);
        double scale = Math.min(68.0 / content.getWidth(), 80.0 / content.getHeight());
        int width = Math.max(1, (int) Math.floor(content.getWidth() * scale));
        int height = Math.max(1, (int) Math.floor(content.getHeight() * scale));
        return new ImageIcon(content.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
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
