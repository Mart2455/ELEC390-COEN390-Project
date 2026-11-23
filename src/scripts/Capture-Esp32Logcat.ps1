[CmdletBinding()]
param(
    [ValidateRange(1, 3600)]
    [int]$DurationSeconds = 300,
    [string]$AdbPath,

    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]*$')]
    [string]$TestName
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $AdbPath) {
    $sdkRoots = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    $localProperties = Join-Path $projectRoot 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkSetting = Select-String -LiteralPath $localProperties -Pattern '^\s*sdk\.dir\s*=\s*(.+)$' |
            Select-Object -First 1
        if ($sdkSetting) {
            $sdkRoots += $sdkSetting.Matches[0].Groups[1].Value.Trim().Replace('\:', ':').Replace('\\', '\')
        }
    }
    $sdkRoots += Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    $AdbPath = $sdkRoots | Where-Object { $_ } |
        ForEach-Object { Join-Path $_ 'platform-tools\adb.exe' } |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
    if (-not $AdbPath) {
        $AdbPath = (Get-Command adb.exe -CommandType Application -ErrorAction SilentlyContinue).Source
    }
}
if (-not $AdbPath -or -not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) {
    throw 'ADB was not found. Install Android SDK Platform-Tools in Android Studio or pass -AdbPath with the full path to adb.exe.'
}
$AdbPath = (Resolve-Path -LiteralPath $AdbPath).Path

$deviceState = & $AdbPath get-state
if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') {
    throw 'Connect one Android device, enable USB debugging, and accept the debugging prompt on the phone.'
}

$results = Join-Path $projectRoot 'test-results\esp32-logcat'
New-Item -ItemType Directory -Path $results -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$fileSuffix = $stamp
if ($TestName) {
    $fileSuffix = "$TestName-$stamp"
}
$log = Join-Path $results "esp32-logcat-$fileSuffix.txt"
$csv = Join-Path $results "esp32-readings-$fileSuffix.csv"

Write-Host "Capturing ESP32 Logcat readings for $DurationSeconds seconds..."
$captureOptions = @{
    FilePath = $AdbPath
    WindowStyle = 'Hidden'
    ArgumentList = 'logcat -T 1 -v threadtime ESP32_DATA:D *:S'
    RedirectStandardOutput = $log
    PassThru = $true
}
$capture = Start-Process @captureOptions
try {
    $endedEarly = $capture.WaitForExit($DurationSeconds * 1000)
} finally {
    if (-not $capture.HasExited) {
        Stop-Process -InputObject $capture -ErrorAction SilentlyContinue
        $capture.WaitForExit()
    }
    $capture.Dispose()
}

$sensorColumns = [ordered]@{
    'Temperature' = 'TemperatureC'
    'Humidity' = 'HumidityPct'
    'Pressure' = 'PressureHpa'
    'Gas Resistance' = 'GasResistanceKOhms'
    'eCO2' = 'eCO2Ppm'
    'TVOC' = 'TVOCPpb'
    'MQ-3' = 'MQ3Raw'
}
$samplesByThread = @{}
$readings = @(Get-Content -LiteralPath $log | ForEach-Object {
    if ($_ -match '^(?<time>\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)\s+(?<pid>\d+)\s+(?<tid>\d+)\s+D\s+ESP32_DATA\s*:\s*(?<sensor>[^:]+):\s*(?<value>\S+)') {
        $threadKey = "$($Matches.pid):$($Matches.tid)"
        $sensor = $Matches.sensor
        # The receiver logs Temperature first and MQ-3 last for each response.
        if ($sensor -eq 'Temperature') {
            $sample = [ordered]@{ Timestamp = $Matches.time }
            foreach ($column in $sensorColumns.Values) {
                $sample[$column] = $null
            }
            $samplesByThread[$threadKey] = [pscustomobject]$sample
            $samplesByThread[$threadKey]
        }
        if ($samplesByThread.ContainsKey($threadKey) -and $sensorColumns.Contains($sensor)) {
            $column = $sensorColumns[$sensor]
            $samplesByThread[$threadKey].$column = $Matches.value
            if ($sensor -eq 'MQ-3') {
                $samplesByThread.Remove($threadKey)
            }
        }
    }
})
if ($readings.Count) {
    $readings | Export-Csv -LiteralPath $csv -NoTypeInformation -Encoding UTF8
} else {
    (@('Timestamp') + @($sensorColumns.Values) -join ',') | Set-Content -LiteralPath $csv -Encoding UTF8
    Write-Warning 'No sensor readings were logged. Check that monitoring is running and the app can reach the ESP32.'
}
Write-Host "Raw Logcat: $log"
Write-Host "CSV ($($readings.Count) samples): $csv"
if ($endedEarly) {
    Write-Warning 'Logcat stopped before the requested capture duration. The saved files contain the available output.'
}
