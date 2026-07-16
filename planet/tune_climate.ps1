$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$gradle = Join-Path $projectRoot "gradlew.bat"
$skipBuild = $false
$godotProjectRoot = $projectRoot
$godotOverride = $null
$tunerArguments = @()
for ($index = 0; $index -lt $args.Count; $index++) {
    if ($args[$index] -eq "--skip-build") {
        $skipBuild = $true
        continue
    }
    if ($args[$index] -eq "--runtime-project") {
        if ($index + 1 -ge $args.Count) {
            throw "Missing path after --runtime-project"
        }
        $godotProjectRoot = $args[++$index]
        continue
    }
    if ($args[$index] -eq "--godot-bin") {
        if ($index + 1 -ge $args.Count) {
            throw "Missing path after --godot-bin"
        }
        $godotOverride = $args[++$index]
        continue
    }
    $tunerArguments += $args[$index]
}

function Find-GodotExecutable {
    if ($env:GODOT_BIN -and (Test-Path -LiteralPath $env:GODOT_BIN -PathType Leaf)) {
        return (Resolve-Path -LiteralPath $env:GODOT_BIN).Path
    }

    foreach ($commandName in @("godot", "godot4", "godot-mono", "godot4-mono")) {
        $command = Get-Command $commandName -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    $runningGodot = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.ProcessName -like "*godot*" -and $_.Path } |
        Select-Object -First 1
    if ($runningGodot) {
        return $runningGodot.Path
    }

    $downloads = Join-Path $HOME "Downloads"
    if (Test-Path -LiteralPath $downloads) {
        $downloadedGodot = Get-ChildItem -LiteralPath $downloads -Filter "*godot*.exe" -File -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($downloadedGodot) {
            return $downloadedGodot.FullName
        }
    }

    throw "Godot Kotlin/JVM was not found. Set GODOT_BIN to the matching editor executable."
}

$godot = if ($godotOverride) { $godotOverride } else { Find-GodotExecutable }
$consoleGodot = Join-Path (Split-Path -Parent $godot) `
    (([System.IO.Path]::GetFileNameWithoutExtension($godot)) + ".console.exe")
if (Test-Path -LiteralPath $consoleGodot -PathType Leaf) {
    $godot = $consoleGodot
}
if (-not $skipBuild) {
    & $gradle -p $projectRoot classes copyJars
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$tunerScene = Join-Path $godotProjectRoot "tune_climate.tscn"
$godotArguments = @("--headless", "--path", $godotProjectRoot, $tunerScene, "--") + $tunerArguments
# Some sandboxed shells inject both Path and PATH. Windows PowerShell's
# Start-Process rejects that duplicate even though Windows normally treats the
# names case-insensitively. Normalize only this runner process's environment.
$processPath = $env:Path
Remove-Item Env:PATH -ErrorAction SilentlyContinue
$env:Path = $processPath
& $godot @godotArguments
exit $LASTEXITCODE
