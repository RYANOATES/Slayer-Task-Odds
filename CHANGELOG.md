# Changelog

## 1.6.2 - 2026-10-19

- Added Slayer cape perk detection for the cape in inventory or equipment, with a 10% previous-task repeat chance reflected in task odds and Account requirements.

## 1.6.1 - 2026-10-19

- Corrected task odds to exclude the player's current Slayer assignment from the next-task roll, including immediately after completing it. This matches the in-game task list, where the current task is shown at 0.0% and locked until a different task is assigned.

## 1.6.0 - 2026-10-19

### Added

- New burgundy-and-gold side panel theme, with a matching scales-and-skull logo.
- Bundled local portraits for 147 Slayer tasks. Grouped assignments use a representative creature, and portraits do not require image downloads.
- Account-aware task explanations that identify blocked tasks, missing levels, quests, unlocks or access, tasks a master never assigns, and chances that round to 0.00%.
- An Account requirements panel with a completion summary, level overview, and separate quest and unlock/access cards. Longer lists can be expanded within each card.
- Optional routing to the highest-odds Slayer master through the Shortest Path plugin. Routing is off by default and follows the selected task's best master.
- A task selection prompt on startup so the panel does not immediately route to a task the player did not choose.
- Manual block editing in the side panel for every Slayer master. Expand a master to edit its saved block slots; each master's list remains separate.
- A panel hint explaining where to enable routing to the best master.
- A source and attribution list for bundled monster portraits.

### Improved

- Kept odds aligned with the Wiki calculator's two-decimal display, including clear explanations for very small chances that round to 0.00%.
- Made task, master, block, and account information easier to scan with centered layout, readable text, colored status indicators, and expandable sections.
- Preserved successful sync status and timestamp between client restarts, with clearer stale and failure states.
- Added a setting to control successful sync messages in chat, including the synced block slots.
- Expanded README guidance with panel graphics, installation and usage instructions, routing details, and data attribution.

### Fixed

- Corrected Slayer task block-name handling for plural **Suqahs**.
- Improved account requirement detection and calculator eligibility checks across task levels, quests, unlocks, and access conditions.
- Kept long account requirement names readable in the narrow side panel and added a missing-requirements notice when no master can assign the selected task.
