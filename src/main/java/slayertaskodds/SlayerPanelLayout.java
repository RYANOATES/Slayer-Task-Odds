package slayertaskodds;

public enum SlayerPanelLayout
{
    SCROLL("New (scroll)"),
    CARDS("Legacy"),
    ARCHIVE("Fractured Archive");

    private final String label;
    SlayerPanelLayout(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
