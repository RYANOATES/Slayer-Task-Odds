# Development-only importer. Runtime images are bundled; the plugin makes no requests.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $root 'src/main/resources/slayertaskodds/monsters'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$manifest = Join-Path $root 'tools/monster-image-sources.json'
$previous = @{}
if (Test-Path -LiteralPath $manifest) {
    foreach ($record in (Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json)) { $previous[$record.key] = $record }
}
$data = Get-Content (Join-Path $root 'src/main/resources/slayertaskodds/wiki-tasks.json') -Raw | ConvertFrom-Json
$names = @($data.PSObject.Properties.Value | ForEach-Object { foreach ($task in $_) { $task.name; foreach ($sub in $task.subtasks) { $sub.name } } }) | Sort-Object -Unique
# A representative creature is used for assignments containing multiple species.
$overrides = @{
    'Boss'=''; 'Birds'='Chicken'; 'Bears'='Grizzly bear'; 'Bats'='Bat';
    'Custodian stalker'='Elder custodian stalker'; 'Sea snakes'='Sea Snake Young';
    'Barrows'='Dharok the Wretched'; 'Crabs'='Rock crab'; 'Dagannoth Kings'='Dagannoth Rex';
    'Fossil Island Wyverns'='Ancient Wyvern'; 'Grotesque Guardians'='Dusk';
    'Kalphites'='Kalphite Worker'; 'Kreearra'="Kree'arra"; 'Kril Tsutsaroth'="K'ril Tsutsaroth";
    'Metal dragons'='Iron dragon'; 'Minions of Scabaras'='Locust rider';
    'Revenants'='Revenant dragon'; 'Spiritual creatures'='Spiritual mage';
    'Vetion'="Vet'ion"; 'Warped Creatures'='Warped terrorbird'; 'TzHaar'='TzHaar-Xil';
    'Shades'='Loar Shade'; 'Elves'='Elf warrior'; 'Dwarves'='Dwarf';
    'Wolves'='Wolf'; 'Werewolves'='Werewolf'; 'Jellies'='Jelly';
    'Duke Sucellus'='Duke Sucellus'; 'Sarachnis'='Sarachnis'; 'Vardorvis'='Vardorvis'; 'Venenatis'='Venenatis';
    'Lesser naguas'='Sulphur Nagua'; 'Lizardmen'='Lizardman'; 'Mutated Zygomites'='Zygomite'
}
$candidates = foreach ($name in $names) {
    $base = if ($overrides.ContainsKey($name)) { $overrides[$name] } elseif ($name -match 's$' -and $name -notmatch 'Tsutsaroth|Cerberus') { $name.Substring(0, $name.Length - 1) } else { $name }
    [pscustomobject]@{ Task=$name; File=if($base){'File:'+$base+'.png'}else{''}; Key=($name.ToLowerInvariant() -replace '[^a-z0-9]','') }
}
$records = @()
$headers = @{'User-Agent'='SlayerTaskOddsAssetImporter/1.0 (https://github.com/RYANOATES/Slayer-Task-Odds)'}
for ($offset=0; $offset -lt $candidates.Count; $offset+=40) {
    $batch=@($candidates | Select-Object -Skip $offset -First 40)
    $titles=($batch.File | Where-Object {$_}) -join '|'
    $uri='https://oldschool.runescape.wiki/api.php?action=query&format=json&redirects=1&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=80&iiurlheight=80&titles='+[uri]::EscapeDataString($titles)
    $response=Invoke-RestMethod -Uri $uri -Headers $headers
    foreach($item in $batch) {
        $title=$item.File
        foreach($normal in $response.query.normalized){if($normal.from -ceq $title){$title=$normal.to}}
        foreach($redirect in $response.query.redirects){if($redirect.from -ceq $title){$title=$redirect.to}}
        $page=$response.query.pages.PSObject.Properties.Value | Where-Object {$_.title -ceq $title} | Select-Object -First 1
        if(!$page.imageinfo -and $item.File) {
            # Infobox portraits resolve files with phase, level, or variant suffixes.
            $article=$item.File.Substring(5,$item.File.Length-9)
            $lookup='https://oldschool.runescape.wiki/api.php?action=query&format=json&redirects=1&prop=pageimages&piprop=name&titles='+[uri]::EscapeDataString($article)
            $articleData=Invoke-RestMethod -Uri $lookup -Headers $headers
            $articlePage=$articleData.query.pages.PSObject.Properties.Value | Select-Object -First 1
            if($articlePage.pageimage) {
                $imageTitle='File:'+$articlePage.pageimage
                $lookup='https://oldschool.runescape.wiki/api.php?action=query&format=json&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=80&iiurlheight=80&titles='+[uri]::EscapeDataString($imageTitle)
                $imageData=Invoke-RestMethod -Uri $lookup -Headers $headers
                $page=$imageData.query.pages.PSObject.Properties.Value | Select-Object -First 1
            }
        }
        if(!$page.imageinfo){ Write-Output "Missing: $($item.Task) [$($item.File)]"; continue }
        $info=$page.imageinfo[0]
        $url=if($info.thumburl){$info.thumburl}else{$info.url}
        $path=Join-Path $destination ($item.Key+'.png')
        if(!(Test-Path -LiteralPath $path) -or $previous[$item.Key].thumbnail -ne $url) { Invoke-WebRequest -Uri $url -Headers $headers -OutFile $path }
        $records += [pscustomobject]@{task=$item.Task; key=$item.Key; resource=$item.Key+'.png'; source=$info.descriptionurl; original=$info.url; thumbnail=$url; sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant(); metadata=$info.extmetadata}
    }
    Write-Output "Processed $([Math]::Min($offset+40,$candidates.Count)) / $($candidates.Count) tasks"
}
$records | ConvertTo-Json -Depth 12 | Set-Content $manifest -Encoding utf8
($records | ForEach-Object { $_.key+'='+$_.resource }) | Set-Content (Join-Path $destination 'catalog.properties') -Encoding ascii
Write-Output "Bundled $($records.Count) portraits for $($names.Count) task choices."
