"""Generate bundled tables and comparison fixtures by executing the Wiki Lua.

Requires Python and lupa (development only). No network calls at runtime or here.
Source snapshots and attribution are in tools/wiki-reference/README.md.
"""
from pathlib import Path
import copy
import json
import re
from lupa import LuaRuntime

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / 'tools/wiki-reference'
lua = LuaRuntime(unpack_returned_tuples=True)
modules = lua.table()
lua.globals().modules = modules
lua.execute('function require(name) return modules[name] end')
consts = lua.execute((REF / 'wiki-consts.lua').read_text())
modules['Module:SlayerConsts'] = consts
masters = lua.execute((REF / 'wiki-masters.lua').read_text())
modules['Module:SlayerConsts/MasterTables'] = masters
library = lua.execute((REF / 'wiki-library.lua').read_text())
modules['Module:Slayer_task_library'] = library
modules['Module:Paramtest'] = lua.table()
calculator = lua.execute((REF / 'wiki-calculator.lua').read_text())
MASTER_NAMES = ['Turael', 'Krystilia', 'Mazchna', 'Vannaka', 'Chaeldar', 'Konar', 'Nieve', 'Duradel']


def name(task_id, task):
    return consts.get_monster_name(task_id) or re.sub(r'\[\[(?:[^\]|]*\|)?([^\]]+)\]\]', r'\1', task['name'])


def convert(task_id, task):
    data = dict(name=name(task_id, task), weight=task['weight'], stats={}, quests=[])
    for key, value in task['requirements'].items():
        if key == 'Quest':
            values = list(value.values()) if hasattr(value, 'items') else [value]
            data['quests'] = [consts.get_quest_name(v) for v in values]
        elif key == 'Unlock':
            data['unlock'] = consts.get_unlock_name(value)
        elif key == 'Other':
            data['other'] = consts.get_other_name(value)
        else:
            data['stats'][key] = value
    if task['unlockWeight']:
        data['bonusUnlock'] = consts.get_unlock_name(task['unlockWeight']['unlock'])
        data['bonusWeight'] = task['unlockWeight']['weight']
    if task['subtable']:
        data['subtasks'] = [convert(k, v) for k, v in sorted(task['subtable'].items())]
    return data


tables = {m.upper(): sorted([convert(k, v) for k, v in masters.get_table(m).items()], key=lambda t: t['name']) for m in MASTER_NAMES}
destination = ROOT / 'src/main/resources/slayertaskodds/wiki-tasks.json'
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(tables, indent=2) + '\n')


def flatten(tasks):
    for task in tasks:
        yield task
        yield from flatten(task.get('subtasks', []))


all_tasks = list(flatten(t for table in tables.values() for t in table))
quests = sorted({q for t in all_tasks for q in t['quests']})
unlocks = sorted({t[key] for t in all_tasks for key in ['unlock', 'bonusUnlock'] if key in t})
others = sorted({t['other'] for t in all_tasks if 'other' in t})
stats = {key: 99 for t in all_tasks for key in t['stats']}
stats['Combat'] = 126
base = dict(stats=stats, quests=quests, unlocks=[u for u in unlocks if u != 'Stop the Wyvern'], other=others, blocks=[])
profiles = [('all-eligible', base)]
empty = copy.deepcopy(base)
empty.update(quests=[], unlocks=[], other=[])
empty['stats'] = {k: 1 for k in stats}
empty['stats']['Combat'] = 3
profiles.append(('new-account', empty))
user = copy.deepcopy(base)
user['stats'].update(Slayer=89, Combat=109, Sailing=73)
user['unlocks'] = ['Like a boss', 'Unlock vampyres', 'Wings Spread', 'Basilocked']
user['blocks'] = ['Greater demons']
profiles.append(('reported-steve-setup', user))
for category in ['quests', 'unlocks', 'other']:
    for option in (quests if category == 'quests' else unlocks if category == 'unlocks' else others):
        state = copy.deepcopy(base)
        if option in state[category]:
            state[category].remove(option)
        else:
            state[category].append(option)
        profiles.append((category + ':' + option, state))
for stat, thresholds in sorted({k: sorted({t['stats'][k] for t in all_tasks if k in t['stats']}) for k in stats}.items()):
    for threshold in thresholds:
        for level in [threshold - 1, threshold]:
            state = copy.deepcopy(base)
            state['stats'][stat] = level
            profiles.append((stat + ':' + str(level), state))
for blocked in [['Greater demons', 'Greater demons'], ['Abyssal demons'], ['Boss'], ['Fossil Island Wyverns']]:
    state = copy.deepcopy(base)
    state['blocks'] = blocked
    profiles.append(('blocks:' + ','.join(blocked), state))

fixtures = []
for label, state in profiles:
    status = library.create_status(lua.table_from(state['stats']), lua.table_from(state['quests']),
        lua.table_from(state['unlocks']), lua.table_from(state['other']), lua.table_from(state['blocks']))
    results = {}
    for master in MASTER_NAMES:
        original = masters.get_table(master)
        effective, unavailable = library.get_effective_table(original, status, True)
        main, sub = calculator.calculate_percents(effective, unavailable)
        chances = {}
        for task_id, task in original.items():
            chances[name(task_id, task)] = main[task['name']] or 0
            if task['subtable']:
                for sub_id, child in task['subtable'].items():
                    chances[name(sub_id, child)] = sub[child['name']] or 0
        results[master.upper()] = dict(totalWeight=sum(t['weight'] for t in effective.values()), chances=chances)
    fixtures.append(dict(label=label, state=state, results=results))
destination = ROOT / 'src/test/resources/slayertaskodds/wiki-fixtures.json'
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(fixtures, separators=(',', ':')) + '\n')
print('Generated', len(fixtures), 'account scenarios across', len(MASTER_NAMES), 'masters from the original Lua.')
