param([int]$TimeoutSeconds = 30)
$ErrorActionPreference = 'Stop'
$sources = [ordered]@{
    'wiki-calculator.lua' = 'Module:Slayer_weight_calculator'
    'wiki-library.lua' = 'Module:Slayer_task_library'
    'wiki-consts.lua' = 'Module:SlayerConsts'
    'wiki-masters.lua' = 'Module:SlayerConsts/MasterTables'
}
$changed = @()
foreach ($entry in $sources.GetEnumerator()) {
    $uri = 'https://oldschool.runescape.wiki/w/' + $entry.Value + '?action=raw'
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec $TimeoutSeconds -Headers @{
        'User-Agent' = 'SlayerTaskOddsDataCheck/1.0 (https://github.com/RYANOATES/Slayer-Task-Odds)'
    }
    $file = Join-Path $PSScriptRoot ('wiki-reference/' + $entry.Key)
    $local = [IO.File]::ReadAllText($file).Replace("`r`n", "`n")
    $remote = $response.Content.Replace("`r`n", "`n")
    if ($local -cne $remote) { $changed += $entry.Key; Write-Host "CHANGED: $($entry.Key)" }
    else { Write-Host "Unchanged: $($entry.Key)" }
}
if ($changed.Count -gt 0) { throw 'Wiki sources changed. Review them and regenerate the data and fixtures together.' }
Write-Host 'All Wiki sources match the bundled snapshots.'
