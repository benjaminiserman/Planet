$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$gradle = Join-Path $projectRoot "gradlew.bat"
$tunerArguments = @($args)

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

$godot = Find-GodotExecutable
Push-Location $projectRoot
try {
    & $gradle classes copyJars
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $godotArguments = @("--headless", "--path", $projectRoot, "tune_climate.tscn", "--") + $tunerArguments
    $godotProcess = Start-Process -FilePath $godot -ArgumentList $godotArguments -Wait -PassThru -NoNewWindow
    exit $godotProcess.ExitCode
}
finally {
    Pop-Location
}
