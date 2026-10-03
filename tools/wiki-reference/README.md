# OSRS Wiki reference snapshots

Retrieved 3 October 2026 from the OSRS Wiki, authored by its community contributors:

- `wiki-calculator.lua`: https://oldschool.runescape.wiki/w/Module:Slayer_weight_calculator
- `wiki-library.lua`: https://oldschool.runescape.wiki/w/Module:Slayer_task_library
- `wiki-consts.lua`: https://oldschool.runescape.wiki/w/Module:SlayerConsts
- `wiki-masters.lua`: https://oldschool.runescape.wiki/w/Module:SlayerConsts/MasterTables

These snapshots and their derived task data retain the Wiki's CC BY-NC-SA 3.0 terms:
https://creativecommons.org/licenses/by-nc-sa/3.0/
They are not covered by the plugin's code license. Changes: the generator converts
Lua tables to JSON, removes wiki link markup, and generates account fixtures.

Install `lupa` in a development Python environment, then run
`python tools/generate-wiki-data.py` from the project root. The generator executes
the original Lua requirement and probability functions to generate independent
expected results. Runtime calculations are Java and require no network or Lua.

The snapshots are frozen for reproducibility. Updating requires refreshing all
four sources together, reviewing new requirements, regenerating, and running
`gradlew verifyWiki`.
