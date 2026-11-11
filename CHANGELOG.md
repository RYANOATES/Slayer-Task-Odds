# Changelog

## 2.0.0 - 2026-11-11

### Main Updates

- Added Mortimer.
- Added Mortimer's task pool and calculated the chance of a task appearing among his weighted choices.
- Detect whether the character receives two or three Mortimer choices from their completed task count.
- Added Mortimer access, Slayer level, Combat level and completed-task checks to Account requirements.
- Added a separate Mortimer section, with a breakdown for one, two and three offers in its hover tooltip.
- Added Mortimer's expandable task-modifier breakdown, showing completion unlocks, proposed effect ranges, estimated modifier odds and combined task-and-modifier chances.
- Added optional Shortest Path routing to Mortimer after his location has been observed in game.
- Moved saved block lists and sync information to character-specific storage.
- Kept existing saved blocks during the move to character-specific data, marking them approximate until confirmed for the character.
- Added per-master sync status, saved-list dates and approximate markers for unconfirmed, old or failed-sync data.
- Limited the block-check chat reminder to once per login, even when changing enabled masters.
- Retained manual blocks in calculations; where no saved or manual list exists, odds assume no blocks.
- Added a seven-day reminder for synced and manually edited block lists; old lists remain in use.
- Added master access checks and explanations for missing Combat, Slayer, quest and area requirements.
- Added this release-notes popup, with highlights and the complete changelist.

### Settings Changes

- Added **Panel Theme**, choosing Legacy cards, Scroll or the distinct Fractured Archive layout.
- Added **Theme Colours**, choosing parchment or maroon and gold for Legacy and Scroll. Fractured Archive has its own exclusive colours. New settings default to legacy maroon and gold; existing choices are preserved.
- Added **Enable/Disable Masters**, with individual switches to include each master in the comparison and automatic routing. All masters are enabled by default.
- **Post sync messages in chat** controls sync confirmations, failures and block-check reminders.
- Added **Collapsible Mortimer section**, controlling whether Mortimer's section can be collapsed; off by default, so his card stays visible without a separate header.
- Added **Show Mortimer task modifiers**, controlling the expandable Mortimer modifier breakdown; enabled by default.
- Added **Route to Mortimer**, allowing the optional best-master route to select him when his location is known; this is off by default.
- Added Mortimer's two block slots to his own settings section.
- Grouped routing controls under General and presentation controls under Side panel.
- Kept each master's block settings in its own clearly named expandable Block List section, ordered by master progression.
- Moved Enable/Disable Masters between General and Side panel, and made it expanded by default.

### Cosmetic Updates

- Added a scroll layout with a textured background, scales header and clearer selected-task display.
- Added a Fractured Archive layout with a custom vault seal, framed task record and exclusive green palette.
- Restored the full legacy card layout as a separate style.
- Added parchment and maroon-and-gold palettes for both layouts.
- Refined typography, spacing, dividers, selection controls and the scroll footer.
- Added a per-master approximate marker and clarified which results need their blocks checked.
- Added a hover reminder beneath Mortimer's odds when a tooltip is available.
- Improved portrait sizing and centring, and added a sharper fallback image for tasks without a portrait.
- Updated the README with a five-theme banner, a Mortimer overview and a side-by-side preview of all five theme and colour combinations.

### Performance Updates

- Combined overlapping refresh requests so bursts of changes do not queue unnecessary updates.
- Batched sync-related settings writes before requesting a panel refresh.
- Reduced repeated reads while a task screen remains open.
- Added automatic retry backoff after repeated sync failures.
- Avoided rebuilding the saved-block controls for unrelated panel updates.
- Added development-only simulations for character switching, sync recovery, edit guards and refresh coalescing.
- Added a development check for changes to the bundled Wiki reference modules.

### Bug Fixes

- Prevented one character's saved block list being applied to another character using the same RuneLite profile.
- Kept the last successfully synced blocks and timestamp after a failed sync, and marked that master's odds until a retry succeeds.
- Preserved a failed-sync warning across client restarts until that master's next successful sync.
- Prevented a manual edit from being overwritten by the currently open master's task screen; the edit is rejected with a chat explanation.
- Rejected character-block edits while logged out instead of silently losing them on login.
- Cleared the selected task when changing characters to avoid routing for the previous character's selection.
- Updated a Mortimer route when his observed destination changes.
- Expired old Mortimer locations so routing does not indefinitely rely on a previous observation.
- Kept Mortimer separate from the regular-master count when no regular master has a visible chance.
- Corrected the explanation for an unavailable task when the Slayer cape repeat perk is active.
- Restricted Slayer cape repeat odds to the master that assigned the previous task; the displayed odds assume the repeat offer is accepted.
- Kept the regular-master ranking ordered from highest to lowest chance.
- Fixed Mortimer's expandable control and kept its label colour consistent when the setting changes.
- Fixed task portraits being clipped, off-centre or displayed too small.
- Improved task and requirement text wrapping in the narrow side panel.
- Fixed the theme setting's RuneLite config-proxy access so the panel can start correctly.
- Preserved panel expansion state when switching styles or colours.
- Kept the existing navigation panel until a replacement theme panel is ready.
- Fixed unnecessary repeated refreshes after the task screen closes.
- Fixed logged-out message contrast on the parchment palette.

### Minor Changes

- Added Mortimer's task-completion count to the account overview.
- Added clearer task-selection and logged-out states.
- Added sync and routing guidance and a theme indicator to the side panel.
- Preserved the last sync failure separately from the last successful block data.
- Verified the bundled Wiki calculator, library, constants and master tables against their online modules.

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
