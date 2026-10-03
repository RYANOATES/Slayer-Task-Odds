package slayertaskodds;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class SlayerOddsLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(SlayerOddsPlugin.class);
        RuneLite.main(args);
    }
}
