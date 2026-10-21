param([string[]]$Tasks=@('assembleDebug','lintDebug'))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$portableJdk=Join-Path $projectRoot '.tools\jdk17'
if((-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) -and (Test-Path -LiteralPath $portableJdk)){
    $jdk=Get-ChildItem -LiteralPath $portableJdk -Directory | Select-Object -First 1
    if($jdk){$env:JAVA_HOME=$jdk.FullName}
}
if(-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))){
    throw 'Configure JDK 17 in JAVA_HOME or use Android Studio Gradle JDK.'
}
if(-not $env:GRADLE_USER_HOME){$env:GRADLE_USER_HOME=Join-Path $projectRoot '.tools\gradle-home'}
$env:Path=(Join-Path $env:JAVA_HOME 'bin')+';'+$env:Path
$sdk=Join-Path $projectRoot '.tools\android-sdk'
$localPropertiesPath=Join-Path $projectRoot 'local.properties'
$hasLocalSdk=(Test-Path -LiteralPath $localPropertiesPath) -and
    [bool](Select-String -LiteralPath $localPropertiesPath -Pattern '^\s*sdk\.dir\s*=' -Quiet)
if(-not $hasLocalSdk -and -not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT -and
    (Test-Path -LiteralPath (Join-Path $sdk 'platforms\android-36'))){
    $env:ANDROID_HOME=$sdk
}
Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat --no-daemon @Tasks
    if($LASTEXITCODE -ne 0){throw "Gradle exited with code $LASTEXITCODE"}
} finally { Pop-Location }
