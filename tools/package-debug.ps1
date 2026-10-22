param([string]$DeviceReport)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $changes = @(& git status --porcelain --untracked-files=normal)
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect Git state.' }
    if ($changes.Count) { throw 'Commit shared source changes before packaging so the source archive matches the APK.' }
    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve source commit.' }
    $packageDir = Join-Path $projectRoot ('build/handoff-' + $commit.Substring(0, 7) + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    New-Item -ItemType Directory -Path $packageDir -ErrorAction Stop | Out-Null

    & (Join-Path $PSScriptRoot 'build-local.ps1') -Tasks assembleDebug,lintDebug,testDebugUnitTest 2>&1 |
        Tee-Object -FilePath (Join-Path $packageDir 'build.log')

    $testCount = 0
    foreach ($result in Get-ChildItem -LiteralPath 'app/build/test-results/testDebugUnitTest' -Filter 'TEST-*.xml') {
        [xml]$test = Get-Content -LiteralPath $result.FullName -Raw
        if ([int]$test.testsuite.failures -ne 0 -or [int]$test.testsuite.errors -ne 0 -or [int]$test.testsuite.skipped -ne 0) {
            throw 'Tests must pass without skipped cases before packaging.'
        }
        $testCount += [int]$test.testsuite.tests
    }
    if ($testCount -eq 0) { throw 'No unit test results were found.' }
    $apk = Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk'
    $apkHash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
    Copy-Item -LiteralPath $apk -Destination (Join-Path $packageDir 'WikiRead-debug.apk')
    Copy-Item -LiteralPath 'README.md' -Destination $packageDir
    Copy-Item -LiteralPath 'app/build/reports/tests/testDebugUnitTest' -Destination (Join-Path $packageDir 'unit-tests') -Recurse
    Copy-Item -LiteralPath 'app/build/reports/lint-results-debug.html' -Destination $packageDir
    $sourceArchive = Join-Path $packageDir 'source.zip'
    & git archive --format=zip --output=$sourceArchive $commit
    if ($LASTEXITCODE -ne 0) { throw 'Cannot archive committed source.' }

    $deviceChecks = 0
    if ($DeviceReport) {
        $evidence = Get-Content -LiteralPath $DeviceReport -Raw | ConvertFrom-Json
        if ($evidence.result -ne 'PASS' -or $evidence.apkSha256 -ne $apkHash -or
            -not $evidence.historyAndRotationRestored -or -not $evidence.sqliteAndPreferencesRestored -or @($evidence.checks).Count -eq 0 -or
            @($evidence.checks | Where-Object { $_.result -ne 'PASS' }).Count -gt 0) {
            throw 'Device evidence must pass, restore app data and match the packaged APK SHA-256.'
        }
        $deviceChecks = @($evidence.checks).Count
        $deviceDir = Join-Path $packageDir 'device-test'
        New-Item -ItemType Directory -Path $deviceDir | Out-Null
        Copy-Item -LiteralPath $DeviceReport -Destination (Join-Path $deviceDir 'report.json')
        Get-ChildItem -LiteralPath (Split-Path -Parent $DeviceReport) -Filter '*.png' |
            Copy-Item -Destination $deviceDir
    }

    [ordered]@{
        createdAt = (Get-Date).ToString('o')
        commit = $commit
        apkSha256 = $apkHash
        sourceArchive = 'source.zip'
        unitTestsPassed = $testCount
        deviceChecksPassed = $deviceChecks
        checks = @('assembleDebug', 'lintDebug', 'testDebugUnitTest')
        scope = 'Integrated prototype: real search, full HTML/WebView reader, canonical bookmarks/history and persistent reader font settings.'
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $packageDir 'manifest.json') -Encoding utf8

    $hashes = foreach ($file in Get-ChildItem -LiteralPath $packageDir -File -Recurse | Sort-Object FullName) {
        $relative = $file.FullName.Substring($packageDir.Length + 1).Replace('\', '/')
        (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash + '  ' + $relative
    }
    $hashes | Set-Content -LiteralPath (Join-Path $packageDir 'SHA256SUMS.txt') -Encoding utf8
    $zip = $packageDir + '.zip'
    Compress-Archive -Path (Join-Path $packageDir '*') -DestinationPath $zip
    Write-Output "Debug handoff: $zip"
} finally { Pop-Location }
