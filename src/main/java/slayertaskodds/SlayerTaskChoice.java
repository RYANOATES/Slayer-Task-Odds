package slayertaskodds;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/** Choices displayed by RuneLite in each block slot. */
public enum SlayerTaskChoice
{
	NONE("None"),
	ABERRANT_SPECTRES("Aberrant spectres"),
	ABYSSAL_DEMONS("Abyssal demons"),
	ADAMANT_DRAGONS("Adamant dragons"),
	ANKOU("Ankou"),
	AQUANITES("Aquanites"),
	ARAXYTES("Araxytes"),
	AVIANSIES("Aviansies"),
	BANDITS("Bandits"),
	BANSHEES("Banshees"),
	BASILISKS("Basilisks"),
	BATS("Bats"),
	BEARS("Bears"),
	BIRDS("Birds"),
	BLACK_DEMONS("Black demons"),
	BLACK_DRAGONS("Black dragons"),
	BLACK_KNIGHTS("Black knights"),
	BLOODVELD("Bloodveld"),
	BLUE_DRAGONS("Blue dragons"),
	BOSSES("Bosses"),
	BRINE_RATS("Brine rats"),
	BRONZE_DRAGONS("Bronze dragons"),
	CATABLEPON("Catablepon"),
	CAVE_BUGS("Cave bugs"),
	CAVE_CRAWLERS("Cave crawlers"),
	CAVE_HORRORS("Cave horrors"),
	CAVE_KRAKEN("Cave kraken"),
	CAVE_SLIMES("Cave slimes"),
	CHAOS_DRUIDS("Chaos druids"),
	COCKATRICES("Cockatrices"),
	COWS("Cows"),
	CRABS("Crabs"),
	CRAWLING_HANDS("Crawling hands"),
	CROCODILES("Crocodiles"),
	CUSTODIAN_STALKERS("Custodian stalkers"),
	DAGANNOTH("Dagannoth"),
	DARK_BEASTS("Dark beasts"),
	DARK_WARRIORS("Dark warriors"),
	DOGS("Dogs"),
	DRAKES("Drakes"),
	DUST_DEVILS("Dust devils"),
	DWARVES("Dwarves"),
	EARTH_WARRIORS("Earth warriors"),
	ELVES("Elves"),
	ENTS("Ents"),
	FEVER_SPIDERS("Fever spiders"),
	FIRE_GIANTS("Fire giants"),
	FLESH_CRAWLERS("Flesh crawlers"),
	FOSSIL_ISLAND_WYVERNS("Fossil Island Wyverns"),
	FROST_DRAGONS("Frost dragons"),
	GARGOYLES("Gargoyles"),
	GHOSTS("Ghosts"),
	GHOULS("Ghouls"),
	GOBLINS("Goblins"),
	GREATER_DEMONS("Greater demons"),
	GREEN_DRAGONS("Green dragons"),
	GRYPHONS("Gryphons"),
	HARPIE_BUG_SWARMS("Harpie bug swarms"),
	HELLHOUNDS("Hellhounds"),
	HILL_GIANTS("Hill giants"),
	HOBGOBLINS("Hobgoblins"),
	HYDRAS("Hydras"),
	ICE_GIANTS("Ice giants"),
	ICE_WARRIORS("Ice warriors"),
	ICEFIENDS("Icefiends"),
	INFERNAL_MAGES("Infernal mages"),
	IRON_DRAGONS("Iron dragons"),
	JELLIES("Jellies"),
	JUNGLE_HORRORS("Jungle horrors"),
	KALPHITES("Kalphites"),
	KILLERWATTS("Killerwatts"),
	KURASKS("Kurasks"),
	LAVA_DRAGONS("Lava dragons"),
	LESSER_DEMONS("Lesser demons"),
	LESSER_NAGUAS("Lesser naguas"),
	LIZARDMEN("Lizardmen"),
	LIZARDS("Lizards"),
	MAGIC_AXES("Magic axes"),
	MAMMOTHS("Mammoths"),
	METAL_DRAGONS("Metal dragons"),
	MINIONS_OF_SCABARAS("Minions of Scabaras"),
	MINOTAURS("Minotaurs"),
	MITHRIL_DRAGONS("Mithril dragons"),
	MOGRES("Mogres"),
	MOLANISKS("Molanisks"),
	MONKEYS("Monkeys"),
	MOSS_GIANTS("Moss giants"),
	MUTATED_ZYGOMITES("Mutated zygomites"),
	NECHRYAEL("Nechryael"),
	OGRES("Ogres"),
	OTHERWORLDLY_BEINGS("Otherworldly beings"),
	PIRATES("Pirates"),
	PYREFIENDS("Pyrefiends"),
	RATS("Rats"),
	RED_DRAGONS("Red dragons"),
	REVENANTS("Revenants"),
	ROCKSLUGS("Rockslugs"),
	ROGUES("Rogues"),
	RUNE_DRAGONS("Rune dragons"),
	SCORPIONS("Scorpions"),
	SEA_SNAKES("Sea snakes"),
	SHADES("Shades"),
	SHADOW_WARRIORS("Shadow warriors"),
	SKELETAL_WYVERNS("Skeletal Wyverns"),
	SKELETONS("Skeletons"),
	SMOKE_DEVILS("Smoke devils"),
	SOURHOGS("Sourhogs"),
	SPIDERS("Spiders"),
	SPIRITUAL_CREATURES("Spiritual creatures"),
	STEEL_DRAGONS("Steel dragons"),
	SUQAH("Suqah"),
	TERROR_DOGS("Terror dogs"),
	TROLLS("Trolls"),
	TUROTH("Turoth"),
	TZHAAR("TzHaar"),
	VAMPYRES("Vampyres"),
	WALL_BEASTS("Wall beasts"),
	WARPED_CREATURES("Warped creatures"),
	WATERFIENDS("Waterfiends"),
	WEREWOLVES("Werewolves"),
	WOLVES("Wolves"),
	WYRMS("Wyrms"),
	ZOMBIES("Zombies");

    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z0-9]");
    private static final Map<String, SlayerTaskChoice> LOOKUP = new HashMap<>();
    static
    {
        for (SlayerTaskChoice choice : values())
        {
            LOOKUP.put(normalize(choice.name()), choice);
            LOOKUP.put(normalize(choice.label), choice);
        }
    }
	private final String label;
	SlayerTaskChoice(String label) { this.label = label; }
	@Override public String toString() { return label; }

	static SlayerTaskChoice fromStoredValue(String value)
	{
		if (value == null || value.trim().isEmpty()) return NONE;
		return LOOKUP.get(normalize(value));
	}

	static String identity(String value)
	{
		SlayerTaskChoice choice = fromStoredValue(value);
		return choice == null ? normalize(value) : choice.name();
	}

	private static String normalize(String value)
	{
		String key = NON_LETTERS.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
		switch (key)
		{
			case "custodianstalker": return "custodianstalkers";
			case "boss": return "bosses";
			case "aviansie": return "aviansies";
			case "basilisk": return "basilisks";
			case "caveslime": return "caveslimes";
			case "cockatrice": return "cockatrices";
			case "kurask": return "kurasks";
			case "molanisk": return "molanisks";
			case "monkey": return "monkeys";
			case "waterfiend": return "waterfiends";
			default: return key;
		}
	}
}
