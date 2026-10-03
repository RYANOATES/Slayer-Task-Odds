# Slayer Task Odds

Select a Slayer task or boss in the sidebar to compare the eight supported
masters. Calculations run locally from a bundled snapshot of the OSRS Wiki
calculator's task tables and eligibility rules (3 October 2026).

## Automatic requirements

The plugin reads unboosted skill levels (including Sailing), combat level,
quest progress, all task unlock flags, the active Fossil Island Wyvern block,
and conditional Frost dragon weighting from the logged-in character. Dragon
Slayer I and Desert Treasure I require being started, as specified by the Wiki
calculator; the other listed quests require completion. God Wars access follows
the calculator's Strength-or-Agility rule. Ancient Cavern access uses the
Barbarian pyre-training flag, and Abyss access uses its miniquest state.

Brittle-key access is detected from the unlocked roof or a key in inventory or
bank, then remembered per RuneScape profile. If you obtained a key before using
the plugin and have not unlocked the roof, open your bank once to make the key
visible to the client. There are no manual eligibility checkboxes.

Open **Account requirements** to see the automatic checks. Each result shows
the eligible weight denominator; boss results multiply the main assignment
chance by the eligible boss subtable chance. The included checks compare the
Java engine against the original Wiki Lua over 210 account scenarios.

## Block synchronization

Open **Slayer rewards > Tasks** or the task-list screen. The plugin reads
`SLAYER_MASTER_IN_FOCUS` and the selected master's seven block varbits (six
standard slots plus the diary slot), resolving task IDs through the live client
DB table. All seven slots must resolve before saved block settings are changed.
Success is confirmed only after saving and reading back the values. The **Post sync messages
in chat** setting controls the success notice and per-slot messages; sync failures
are always reported. Missing task data causes retries, unchanged screens do not
repeatedly post success, and a changed block or selected master is synced again.

Saved dropdowns can also be used to compare planned block lists. Open each
master's screen to replace that master's saved list with its current game list.
The former eighth slot is no longer used because it is not an in-game block slot.

## Build and verification

Use Java 11 or later. Run `gradlew.bat build` to compile and run the Wiki comparison
and block-reader checks, or `gradlew.bat verifyWiki` for those checks alone.
`gradlew.bat run` launches the development client.

See [reference attribution and regeneration](tools/wiki-reference/README.md).
Source data is CC BY-NC-SA 3.0; it retains those terms separately from plugin code.
The reference calculator is https://oldschool.runescape.wiki/w/Calculator:Slayer/Slayer_task_weight.
