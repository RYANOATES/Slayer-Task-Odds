package slayertaskodds;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads the same changelog that is published with the project. */
final class ReleaseNotes
{
    private ReleaseNotes() { }

    static String latest()
    {
        try (InputStream stream = ReleaseNotes.class.getResourceAsStream("/slayertaskodds/CHANGELOG.md"))
        {
            if (stream == null) return "Release notes are unavailable.";
            String changelog = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            int start = changelog.indexOf("## ");
            if (start < 0) return "Release notes are unavailable.";
            int end = changelog.indexOf("\n## ", start + 3);
            return changelog.substring(start, end < 0 ? changelog.length() : end).trim();
        }
        catch (IOException ex) { return "Release notes are unavailable."; }
    }
}
