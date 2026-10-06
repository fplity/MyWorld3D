param([string]$OfflineLibs, [string]$RuntimeJdk = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (-not $RuntimeJdk -or -not (Test-Path -LiteralPath "$RuntimeJdk/bin/jlink.exe")) {
    throw '请使用 -RuntimeJdk 指定可再分发的 OpenJDK 25 JDK 目录'
}
$buildArgs = @('test', 'build', 'installDist', '--no-daemon')
if ($OfflineLibs) { $buildArgs += @('--offline', "-PofflineLibs=$OfflineLibs") }
& "$projectRoot/gradlew.bat" @buildArgs
if ($LASTEXITCODE -ne 0) { throw '构建失败，停止打包' }
$packageDirectory = Join-Path $projectRoot ('build/portable-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $packageDirectory | Out-Null
$runtime = Join-Path $packageDirectory 'runtime'
& "$RuntimeJdk/bin/jlink.exe" --module-path "$RuntimeJdk/jmods" --add-modules java.base,java.desktop,java.logging,jdk.unsupported --strip-debug --no-header-files --no-man-pages --output $runtime
if ($LASTEXITCODE -ne 0) { throw '运行时裁剪失败' }
& "$RuntimeJdk/bin/jpackage.exe" --type app-image --name BlockHorizon --app-version 1.1.0 --vendor BlockHorizon --input build/install/BlockHorizon/lib --main-jar BlockHorizon-1.1.0.jar --main-class com.blockhorizon.desktop.DesktopLauncher --runtime-image $runtime --dest $packageDirectory --java-options '--enable-native-access=ALL-UNNAMED' --java-options '-Dfile.encoding=UTF-8'
if ($LASTEXITCODE -ne 0) { throw '便携包创建失败' }
$image = Join-Path $packageDirectory 'BlockHorizon'
Copy-Item -LiteralPath "$projectRoot/README.md", "$projectRoot/THIRD_PARTY_NOTICES.md" -Destination $image
Copy-Item -LiteralPath "$projectRoot/CHANGELOG.md" -Destination $image
Copy-Item -LiteralPath "$projectRoot/docs" -Destination $image -Recurse
$archive = Join-Path $projectRoot 'build/distributions/BlockHorizon-1.1.0-windows-x64.zip'
Compress-Archive -LiteralPath $image -DestinationPath $archive -Force
Write-Host "Executable: $image/BlockHorizon.exe"
Write-Host "Archive: $archive"
Get-FileHash -LiteralPath $archive -Algorithm SHA256
