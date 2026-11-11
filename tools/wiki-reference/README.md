# OSRS Wiki reference snapshots

Retrieved 3 October 2026 from the OSRS Wiki, authored by its community contributors:

Verified 6 November 2026 against the Wiki's current raw module contents; all four
files were byte-for-byte identical to these snapshots. The corresponding Wiki
revision IDs were `15273165` (calculator), `15273964` (library), `15273962`
(constants), and `15302373` (master tables).

- `wiki-calculator.lua`: https://oldschool.runescape.wiki/w/Module:Slayer_weight_calculator
- `wiki-library.lua`: https://oldschool.runescape.wiki/w/Module:Slayer_task_library
- `wiki-consts.lua`: https://oldschool.runescape.wiki/w/Module:SlayerConsts
- `wiki-masters.lua`: https://oldschool.runescape.wiki/w/Module:SlayerConsts/MasterTables

These snapshots and their derived task data retain the Wiki's CC BY-NC-SA 3.0 terms:
https://creativecommons.org/licenses/by-nc-sa/3.0/
They are not covered by the plugin's code license. Changes: the generator converts
Lua tables to JSON, removes wiki link markup, and generates account fixtures.

`src/main/resources/slayertaskodds/mortimer-modifiers.tsv` contains the proposed
modifier ranges transcribed from the [OSRS Wiki Mortimer page](https://oldschool.runescape.wiki/w/Mortimer),
retrieved 9 November 2026. It is derived from Wiki community content under CC BY-NC-SA 3.0;
the stored task names and range formatting were adapted for the plugin. Modifier unlocks
and equal type probabilities were checked against [Jagex's Mortimer announcement](https://secure.runescape.com/m=news/meet-mortimer-your-newest-slayer-master?oldschool=1).
The Wiki describes the ranges as proposed, so they are displayed as estimates.

Install `lupa` in a development Python environment, then run
`python tools/generate-wiki-data.py` from the project root. The generator executes
the original Lua requirement and probability functions to generate independent
expected results. Runtime calculations are Java and require no network or Lua.

The snapshots are frozen for reproducibility. Updating requires refreshing all
four sources together, reviewing new requirements, regenerating, and running
`gradlew verifyWiki`.

Run `powershell -ExecutionPolicy Bypass -File tools/check-wiki-data.ps1` to compare all four snapshots
with the current Wiki modules without modifying any files. The command fails if
any source differs or the Wiki cannot be reached; it is a development check and
does not make the plugin download data while the game is running.
