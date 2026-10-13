param([string[]]$Tasks=@('assembleDebug','lintDebug'))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$portableJdk=Join-Path $projectRoot '.tools\jdk17'
if(Test-Path -LiteralPath $portableJdk){
    $jdk=Get-ChildItem -LiteralPath $portableJdk -Directory | Select-Object -First 1
    if($jdk){$env:JAVA_HOME=$jdk.FullName}
}
if(-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))){
    throw 'Configure JDK 17 in JAVA_HOME or use Android Studio Gradle JDK.'
}
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.tools\gradle-home'
$env:Path=(Join-Path $env:JAVA_HOME 'bin')+';'+$env:Path
$sdk=Join-Path $projectRoot '.tools\android-sdk'
if(Test-Path -LiteralPath (Join-Path $sdk 'platforms\android-36')){
    $env:ANDROID_HOME=$sdk
    $localProperties='sdk.dir='+$sdk.Replace('\','/').Replace(':','\:')+[Environment]::NewLine
    [System.IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'),$localProperties)
}
Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat --no-daemon @Tasks
    if($LASTEXITCODE -ne 0){throw "Gradle exited with code $LASTEXITCODE"}
} finally { Pop-Location }
