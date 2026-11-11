package slayertaskodds;

public enum SlayerPanelTheme
{
    PARCHMENT("Parchment"),
    MAROON_GOLD("Maroon and gold");

    private final String label;

    SlayerPanelTheme(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
