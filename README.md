# Slayer Task Odds

Compare your chance of receiving a Slayer task from each master. Slayer Task Odds
uses your account’s levels, quest progress, unlocks, and saved block lists to rank
the available masters for the task you select.

## Install

In RuneLite, open the **Plugin Hub**, search for **Slayer Task Odds**, and install
it. Open its red skull and odds icon in the sidebar to use the panel.

## Use the panel

1. Choose a monster from the **Slayer Task** dropdown.
2. Compare the masters, ranked from the highest assignment chance to the lowest.
3. Open **Account requirements** to see which levels, quests, unlocks, or access
   checks affect your eligible task pool.

The percentage is the chance that a master assigns the selected task, based on
the task weights that apply to your account and that master’s saved block list.
Boss-task odds also account for the eligible boss subtable. The result is a
probability, not a promise about the next assignment.

## Keep your block lists current

Open **Slayer rewards > Tasks** or the Slayer task-list screen while logged in.
The plugin detects the selected master and syncs its six regular block slots and
diary slot. It only updates the saved list after it has read and verified all
seven slots.

Use **Saved block list** in the panel or the master sections in Plugin
Configuration to review or edit saved blocks. This also lets you compare planned
block lists. Lists are kept separately for each Slayer master.

By default, a successful sync posts a confirmation and each slot in chat. To
silence those success messages, turn off **Post sync messages in chat** under
**Notifications** in Plugin Configuration. Sync failures are still reported.

## Calculation and account data

Task weights and eligibility rules are based on the [OSRS Wiki Slayer task weight
calculator](https://oldschool.runescape.wiki/w/Calculator:Slayer/Slayer_task_weight).
The plugin calculates locally from task data bundled with the plugin and account
information already available in RuneLite. It does not send your account state to
an external service.

The **Account requirements** section shows the levels, quests, unlocks, and access
checks used for the selected account. Some access checks depend on information
RuneLite has observed; if an older item unlock is not detected, open your bank so
the client can see the relevant item.

## Credits and licensing

The Java plugin code is licensed under the BSD 2-Clause License. The bundled
Slayer task data is derived from OSRS Wiki content and is covered separately by
CC BY-NC-SA 3.0. Source references, attribution, and data-generation notes are in
[`tools/wiki-reference/README.md`](tools/wiki-reference/README.md).

## Support

Report a bug or suggest an improvement on the [GitHub issue tracker](https://github.com/RYANOATES/Slayer-Task-Odds/issues).
