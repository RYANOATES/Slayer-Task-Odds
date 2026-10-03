package slayertaskodds;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(SlayerOddsConfig.GROUP)
public interface SlayerOddsConfig extends Config
{
	String GROUP = "slayerodds";

	@ConfigSection(name = "Notifications", description = "Slayer block list synchronization messages", position = 1, closedByDefault = false)
	String notifications = "notifications";
	@ConfigItem(keyName = "postSyncChatMessages", name = "Post sync messages in chat", description = "Show a chat confirmation and the synced block slots when a Slayer master's block list is synchronized", section = SlayerOddsConfig.notifications, position = 1)
	default boolean postSyncChatMessages() { return true; }

	@ConfigSection(name = "Turael / Aya", description = "Turael / Aya's blocked tasks", position = 2, closedByDefault = true)
	String turael = "turael";
	@ConfigSection(name = "Krystilia", description = "Krystilia's blocked tasks", position = 3, closedByDefault = true)
	String krystilia = "krystilia";
	@ConfigSection(name = "Mazchna / Achtryn", description = "Mazchna / Achtryn's blocked tasks", position = 4, closedByDefault = true)
	String mazchna = "mazchna";
	@ConfigSection(name = "Vannaka", description = "Vannaka's blocked tasks", position = 5, closedByDefault = true)
	String vannaka = "vannaka";
	@ConfigSection(name = "Chaeldar", description = "Chaeldar's blocked tasks", position = 6, closedByDefault = true)
	String chaeldar = "chaeldar";
	@ConfigSection(name = "Konar quo Maten", description = "Konar quo Maten's blocked tasks", position = 7, closedByDefault = true)
	String konar = "konar";
	@ConfigSection(name = "Nieve / Steve", description = "Nieve / Steve's blocked tasks", position = 8, closedByDefault = true)
	String nieve = "nieve";
	@ConfigSection(name = "Duradel / Kuradal", description = "Duradel / Kuradal's blocked tasks", position = 9, closedByDefault = true)
	String duradel = "duradel";

	@ConfigItem(keyName = "turaelBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.turael, position = 1)
	default SlayerTaskChoice turaelBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.turael, position = 2)
	default SlayerTaskChoice turaelBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.turael, position = 3)
	default SlayerTaskChoice turaelBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.turael, position = 4)
	default SlayerTaskChoice turaelBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.turael, position = 5)
	default SlayerTaskChoice turaelBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.turael, position = 6)
	default SlayerTaskChoice turaelBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "turaelBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.turael, position = 7)
	default SlayerTaskChoice turaelBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.krystilia, position = 1)
	default SlayerTaskChoice krystiliaBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.krystilia, position = 2)
	default SlayerTaskChoice krystiliaBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.krystilia, position = 3)
	default SlayerTaskChoice krystiliaBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.krystilia, position = 4)
	default SlayerTaskChoice krystiliaBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.krystilia, position = 5)
	default SlayerTaskChoice krystiliaBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.krystilia, position = 6)
	default SlayerTaskChoice krystiliaBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "krystiliaBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.krystilia, position = 7)
	default SlayerTaskChoice krystiliaBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.mazchna, position = 1)
	default SlayerTaskChoice mazchnaBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.mazchna, position = 2)
	default SlayerTaskChoice mazchnaBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.mazchna, position = 3)
	default SlayerTaskChoice mazchnaBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.mazchna, position = 4)
	default SlayerTaskChoice mazchnaBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.mazchna, position = 5)
	default SlayerTaskChoice mazchnaBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.mazchna, position = 6)
	default SlayerTaskChoice mazchnaBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mazchnaBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.mazchna, position = 7)
	default SlayerTaskChoice mazchnaBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.vannaka, position = 1)
	default SlayerTaskChoice vannakaBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.vannaka, position = 2)
	default SlayerTaskChoice vannakaBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.vannaka, position = 3)
	default SlayerTaskChoice vannakaBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.vannaka, position = 4)
	default SlayerTaskChoice vannakaBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.vannaka, position = 5)
	default SlayerTaskChoice vannakaBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.vannaka, position = 6)
	default SlayerTaskChoice vannakaBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "vannakaBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.vannaka, position = 7)
	default SlayerTaskChoice vannakaBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.chaeldar, position = 1)
	default SlayerTaskChoice chaeldarBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.chaeldar, position = 2)
	default SlayerTaskChoice chaeldarBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.chaeldar, position = 3)
	default SlayerTaskChoice chaeldarBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.chaeldar, position = 4)
	default SlayerTaskChoice chaeldarBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.chaeldar, position = 5)
	default SlayerTaskChoice chaeldarBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.chaeldar, position = 6)
	default SlayerTaskChoice chaeldarBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "chaeldarBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.chaeldar, position = 7)
	default SlayerTaskChoice chaeldarBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.konar, position = 1)
	default SlayerTaskChoice konarBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.konar, position = 2)
	default SlayerTaskChoice konarBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.konar, position = 3)
	default SlayerTaskChoice konarBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.konar, position = 4)
	default SlayerTaskChoice konarBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.konar, position = 5)
	default SlayerTaskChoice konarBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.konar, position = 6)
	default SlayerTaskChoice konarBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "konarBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.konar, position = 7)
	default SlayerTaskChoice konarBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.nieve, position = 1)
	default SlayerTaskChoice nieveBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.nieve, position = 2)
	default SlayerTaskChoice nieveBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.nieve, position = 3)
	default SlayerTaskChoice nieveBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.nieve, position = 4)
	default SlayerTaskChoice nieveBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.nieve, position = 5)
	default SlayerTaskChoice nieveBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.nieve, position = 6)
	default SlayerTaskChoice nieveBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "nieveBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.nieve, position = 7)
	default SlayerTaskChoice nieveBlock7() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock1", name = "Block 1", description = "Blocked task slot 1", section = SlayerOddsConfig.duradel, position = 1)
	default SlayerTaskChoice duradelBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock2", name = "Block 2", description = "Blocked task slot 2", section = SlayerOddsConfig.duradel, position = 2)
	default SlayerTaskChoice duradelBlock2() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock3", name = "Block 3", description = "Blocked task slot 3", section = SlayerOddsConfig.duradel, position = 3)
	default SlayerTaskChoice duradelBlock3() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock4", name = "Block 4", description = "Blocked task slot 4", section = SlayerOddsConfig.duradel, position = 4)
	default SlayerTaskChoice duradelBlock4() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock5", name = "Block 5", description = "Blocked task slot 5", section = SlayerOddsConfig.duradel, position = 5)
	default SlayerTaskChoice duradelBlock5() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock6", name = "Block 6", description = "Blocked task slot 6", section = SlayerOddsConfig.duradel, position = 6)
	default SlayerTaskChoice duradelBlock6() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "duradelBlock7", name = "Block 7", description = "Blocked task slot 7", section = SlayerOddsConfig.duradel, position = 7)
	default SlayerTaskChoice duradelBlock7() { return SlayerTaskChoice.NONE; }
}



