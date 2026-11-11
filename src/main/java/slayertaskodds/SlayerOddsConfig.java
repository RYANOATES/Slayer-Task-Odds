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
	@ConfigSection(name = "General", description = "General Slayer Odds behavior", position = 2, closedByDefault = false)
	String general = "general";
	@ConfigItem(keyName = "postSyncChatMessages", name = "Post sync messages in chat", description = "Show block-sync confirmations, failures and reminders in chat", section = SlayerOddsConfig.notifications, position = 1)
	default boolean postSyncChatMessages() { return true; }
	@ConfigSection(name = "Side panel", description = "Master odds display and explanation tooltips", position = 4, closedByDefault = false)
	String display = "display";
	@ConfigSection(name = "Enable/Disable Masters", description = "Choose which masters appear in odds and routing", position = 3, closedByDefault = false)
	String enabledMasters = "enabledMasters";
	@ConfigItem(keyName = "panelLayout", name = "Panel Theme", description = "Choose legacy cards, scroll, or Fractured Archive", section = SlayerOddsConfig.display, position = -1)
	default SlayerPanelLayout panelLayout() { return SlayerPanelLayout.CARDS; }
	@ConfigItem(keyName = "panelTheme", name = "Theme Colours", description = "Choose colours for Legacy and Scroll. Fractured Archive has its own fixed colours.", section = SlayerOddsConfig.display, position = 0)
	default SlayerPanelTheme panelTheme() { return SlayerPanelTheme.MAROON_GOLD; }
	@ConfigItem(keyName = "hideZeroChanceMasters", name = "Hide 0.00% masters", description = "Hide masters whose displayed chance rounds to 0.00% in the side panel", section = SlayerOddsConfig.display, position = 1)
	default boolean hideZeroChanceMasters() { return false; }
	@ConfigItem(keyName = "showOddsTooltips", name = "Show explanation tooltips", description = "Show task availability reasons and weight calculations when hovering over master cards", section = SlayerOddsConfig.display, position = 2)
	default boolean showOddsTooltips() { return true; }
	@ConfigItem(keyName = "routeToBestMaster", name = "Route to best master", description = "When the selected task's best master changes, ask Shortest Path to route to that Slayer master", section = SlayerOddsConfig.general, position = 1)
	default boolean routeToBestMaster() { return false; }
	@ConfigItem(keyName = "showMortimer", name = "Mortimer", description = "Include Mortimer in the comparison", section = SlayerOddsConfig.enabledMasters, position = 9)
	default boolean showMortimer() { return true; }
	@ConfigItem(keyName = "showTurael", name = "Turael / Aya", description = "Include Turael / Aya in the comparison", section = SlayerOddsConfig.enabledMasters, position = 1)
	default boolean showTurael() { return true; }
	@ConfigItem(keyName = "showKrystilia", name = "Krystilia", description = "Include Krystilia in the comparison", section = SlayerOddsConfig.enabledMasters, position = 2)
	default boolean showKrystilia() { return true; }
	@ConfigItem(keyName = "showMazchna", name = "Mazchna / Achtryn", description = "Include Mazchna / Achtryn in the comparison", section = SlayerOddsConfig.enabledMasters, position = 3)
	default boolean showMazchna() { return true; }
	@ConfigItem(keyName = "showVannaka", name = "Vannaka", description = "Include Vannaka in the comparison", section = SlayerOddsConfig.enabledMasters, position = 4)
	default boolean showVannaka() { return true; }
	@ConfigItem(keyName = "showChaeldar", name = "Chaeldar", description = "Include Chaeldar in the comparison", section = SlayerOddsConfig.enabledMasters, position = 5)
	default boolean showChaeldar() { return true; }
	@ConfigItem(keyName = "showKonar", name = "Konar quo Maten", description = "Include Konar in the comparison", section = SlayerOddsConfig.enabledMasters, position = 6)
	default boolean showKonar() { return true; }
	@ConfigItem(keyName = "showNieve", name = "Nieve / Steve", description = "Include Nieve / Steve in the comparison", section = SlayerOddsConfig.enabledMasters, position = 7)
	default boolean showNieve() { return true; }
	@ConfigItem(keyName = "showDuradel", name = "Duradel / Kuradal", description = "Include Duradel / Kuradal in the comparison", section = SlayerOddsConfig.enabledMasters, position = 8)
	default boolean showDuradel() { return true; }
	@ConfigItem(keyName = "expandMortimer", name = "Collapsible Mortimer section", description = "Allow Mortimer's odds section to be collapsed; when disabled, show the card without a section header", section = SlayerOddsConfig.display, position = 4)
	default boolean expandMortimer() { return false; }
	@ConfigItem(keyName = "showMortimerModifiers", name = "Show Mortimer task modifiers", description = "Show the expandable task-modifier breakdown under Mortimer", section = SlayerOddsConfig.display, position = 5)
	default boolean showMortimerModifiers() { return true; }
	@ConfigItem(keyName = "routeToMortimer", name = "Route to Mortimer", description = "Allow Route to best master to choose Mortimer when he is shown, his location is known, and he has the highest chance", section = SlayerOddsConfig.general, position = 2)
	default boolean routeToMortimer() { return false; }

    @ConfigSection(name = "Mortimer Block List", description = "Mortimer blocked tasks", position = 13, closedByDefault = true)
	String mortimer = "mortimer";
	@ConfigItem(keyName = "mortimerBlock1", name = "Block 1", description = "Mortimer blocked task slot 1", section = SlayerOddsConfig.mortimer, position = 1)
	default SlayerTaskChoice mortimerBlock1() { return SlayerTaskChoice.NONE; }
	@ConfigItem(keyName = "mortimerBlock2", name = "Block 2", description = "Mortimer blocked task slot 2", section = SlayerOddsConfig.mortimer, position = 2)
	default SlayerTaskChoice mortimerBlock2() { return SlayerTaskChoice.NONE; }

    @ConfigSection(name = "Turael / Aya Block List", description = "Turael / Aya blocked tasks", position = 5, closedByDefault = true)
	String turael = "turael";
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
    @ConfigSection(name = "Krystilia Block List", description = "Krystilia blocked tasks", position = 6, closedByDefault = true)
	String krystilia = "krystilia";
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
    @ConfigSection(name = "Mazchna / Achtryn Block List", description = "Mazchna / Achtryn blocked tasks", position = 7, closedByDefault = true)
	String mazchna = "mazchna";
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
    @ConfigSection(name = "Vannaka Block List", description = "Vannaka blocked tasks", position = 8, closedByDefault = true)
	String vannaka = "vannaka";
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
    @ConfigSection(name = "Chaeldar Block List", description = "Chaeldar blocked tasks", position = 9, closedByDefault = true)
	String chaeldar = "chaeldar";
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
    @ConfigSection(name = "Konar quo Maten Block List", description = "Konar quo Maten blocked tasks", position = 10, closedByDefault = true)
	String konar = "konar";
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
    @ConfigSection(name = "Nieve / Steve Block List", description = "Nieve / Steve blocked tasks", position = 11, closedByDefault = true)
	String nieve = "nieve";
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
    @ConfigSection(name = "Duradel / Kuradal Block List", description = "Duradel / Kuradal blocked tasks", position = 12, closedByDefault = true)
	String duradel = "duradel";
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



