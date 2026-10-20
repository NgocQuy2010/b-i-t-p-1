# Run once before importing the project into Android Studio.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
if (Test-Path $target) {
    Write-Host 'Gradle wrapper JAR already available.'
    exit 0
}
$url = 'https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar'
Write-Host 'Downloading official Gradle wrapper JAR from existing course repository...'
Invoke-WebRequest -Uri $url -OutFile $target -UseBasicParsing
if ((Get-Item $target).Length -lt 30000) { throw 'Invalid Gradle wrapper JAR downloaded.' }
Write-Host 'Wrapper ready. Open this project in Android Studio.'
