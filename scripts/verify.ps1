param([string]$OfflineLibs, [switch]$SkipBuild, [string]$PortableExe)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (-not $SkipBuild) {
    $buildArgs = @('test', 'build', 'installDist', '--no-daemon')
    if ($OfflineLibs) { $buildArgs += @('--offline', "-PofflineLibs=$OfflineLibs") }
    & "$projectRoot\gradlew.bat" @buildArgs
    if ($LASTEXITCODE -ne 0) { throw '构建或测试失败' }
}
$outputDirectory = Join-Path $projectRoot ('build/verification-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $outputDirectory | Out-Null
$modes = @('title','world','inventory','map','pause','night','gameplay')
foreach ($mode in $modes) {
    $screenshot = Join-Path $outputDirectory "$mode.png"
    $report = Join-Path $outputDirectory "$mode.json"
    $args = @('--enable-native-access=ALL-UNNAMED', '-Dfile.encoding=UTF-8', '-Dblockhorizon.smoke=true',
        "-Dblockhorizon.smokeScreenshot=$screenshot", "-Dblockhorizon.smokeReport=$report",
        "-Dblockhorizon.dataDir=$outputDirectory/data")
    if ($mode -eq 'title') { $args += '-Dblockhorizon.smokeTitle=true' }
    elseif ($mode -in @('inventory','map','pause')) { $args += "-Dblockhorizon.smokeOverlay=$mode" }
    elseif ($mode -in @('night','gameplay')) { $args += "-Dblockhorizon.smokeScenario=$mode" }
    if ($mode -eq 'gameplay') { $args += '-Dblockhorizon.smokeSave=true' }
    $envBefore = $env:JAVA_TOOL_OPTIONS
    try {
        if ($PortableExe) {
            $env:JAVA_TOOL_OPTIONS = ($args[2..($args.Length-1)] | ForEach-Object { '"' + $_ + '"' }) -join ' '
            $process = Start-Process -FilePath $PortableExe -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru
        } else {
            $args += @('-cp', 'build/install/BlockHorizon/lib/*', 'com.blockhorizon.desktop.DesktopLauncher')
            $process = Start-Process -FilePath 'java' -ArgumentList ($args | ForEach-Object { '"' + $_ + '"' }) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru
        }
        if (-not $process.WaitForExit(45000)) { $process.Kill(); throw "$mode 验证超时" }
        if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $screenshot) -or -not (Test-Path -LiteralPath $report)) {
            throw "$mode 没有成功的窗口/截图/结果证据，退出码 $($process.ExitCode)"
        }
        $evidence = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($mode -eq 'title' -and -not $evidence.randomSeedButton) { throw '随机种子按钮验证失败' }
        if ($mode -ne 'title' -and $evidence.visibleChunks -ge $evidence.totalChunks) { throw '未观察到区块剔除' }
        if ($mode -eq 'gameplay') {
            $saved = Get-Content -LiteralPath (Join-Path $outputDirectory 'data/saves/world.json') -Raw | ConvertFrom-Json
            if ($saved.version -ne 3 -or -not $saved.companionUnlocked -or $saved.blocksPlaced -ne 1 -or $saved.health -le 0) {
                throw '游戏退出后存档验证失败'
            }
        }
        Write-Host "PASS $mode $($evidence.visibleChunks)/$($evidence.totalChunks) chunks"
    } finally { $env:JAVA_TOOL_OPTIONS = $envBefore }
}
Write-Host "Evidence: $outputDirectory"
