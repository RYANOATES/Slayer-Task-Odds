# Monster portraits

The bundled portraits in `src/main/resources/slayertaskodds/monsters/` are
Old School RuneScape game artwork by Jagex, obtained from the OSRS Wiki.
They are separate from the BSD-licensed plugin source code. Copyright in the
depicted game artwork remains with Jagex; the BSD code license does not grant
rights to these images.

The source file page, original image URL, thumbnail URL, and available Wiki
metadata for each portrait are recorded in `tools/monster-image-sources.json`.
Consult each linked file page for its media notice and contributor history.
Wiki text and data have their own licensing terms; do not assume those terms
replace the copyright notice attached to a game image.

Retrieved 17 October 2026. Images are Wiki-generated PNG thumbnails fitted
within 80 by 80 pixels, with original aspect ratios preserved. Grouped tasks
use a representative creature (for example, an iron dragon for metal dragons).
The generic Boss task uses the plugin's existing Slayer skull icon.

`tools/update-monster-images.ps1` is a development-only importer. It records
explicit group mappings, resolves variant portraits through Wiki page metadata,
and creates the bundled image catalog. The installed plugin reads these local
resources and does not contact the Wiki for images.
