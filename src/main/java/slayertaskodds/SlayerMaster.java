package slayertaskodds;

enum SlayerMaster
{
	TURAEL("Turael / Aya", "turael"),
	KRYSTILIA("Krystilia", "krystilia"),
	MAZCHNA("Mazchna / Achtryn", "mazchna"),
	VANNAKA("Vannaka", "vannaka"),
	CHAELDAR("Chaeldar", "chaeldar"),
	KONAR("Konar quo Maten", "konar"),
	NIEVE("Nieve / Steve", "nieve"),
	DURADEL("Duradel / Kuradal", "duradel");

	final String displayName;
	final String key;

	SlayerMaster(String displayName, String key)
	{
		this.displayName = displayName;
		this.key = key;
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

