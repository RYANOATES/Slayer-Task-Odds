package slayertaskodds;

import net.runelite.api.coords.WorldPoint;

enum SlayerMaster
{
	// Destinations checked against Slayer Simplified's SlayerMaster locations.
	// https://github.com/Wulfic/slayer-simplified/blob/master/src/main/java/com/slayersimplified/domain/SlayerMaster.java
	TURAEL("Turael / Aya", "turael", 2931, 3536),
	KRYSTILIA("Krystilia", "krystilia", 3109, 3514),
	MAZCHNA("Mazchna / Achtryn", "mazchna", 3510, 3507),
	VANNAKA("Vannaka", "vannaka", 3145, 9914),
	CHAELDAR("Chaeldar", "chaeldar", 2445, 4431),
	KONAR("Konar quo Maten", "konar", 1308, 3786),
	NIEVE("Nieve / Steve", "nieve", 2432, 3424),
	DURADEL("Duradel / Kuradal", "duradel", 2869, 2982);

	final String displayName;
	final String key;
	final WorldPoint destination;

	SlayerMaster(String displayName, String key, int x, int y)
	{
		this.displayName = displayName;
		this.key = key;
		this.destination = new WorldPoint(x, y, 0);
	}

	static SlayerMaster byGameId(int id)
    {
        switch (id)
        {
            case 1: return TURAEL;
            case 2: return MAZCHNA;
            case 3: return VANNAKA;
            case 4: return CHAELDAR;
            case 5: return DURADEL;
            case 6: return NIEVE;
            case 7: return KRYSTILIA;
            case 8: return KONAR;
            default: return null;
        }
    }

    static SlayerMaster byKey(String key)
	{
		for (SlayerMaster master : values())
		{
			if (master.key.equals(key)) return master;
		}
		return null;
	}
}

