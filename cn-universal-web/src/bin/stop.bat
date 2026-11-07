@echo off
setlocal EnableExtensions
rem ======================================================================
rem Windows stop script
rem
rem ======================================================================

rem 项目名称
set "APPLICATION=cn-universal-web"

rem 主类名
set "MAIN_CLASS=cn.universal.CnUniversalIoTApplication"

rem 获取脚本所在目录
set "BIN_PATH=%~dp0"
rem 获取项目根目录
for %%I in ("%BIN_PATH%..") do set "BASE_PATH=%%~fI"

echo Stopping %APPLICATION%...

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $main=$env:MAIN_CLASS; $procs=Get-CimInstance Win32_Process -Filter 'Name = ''java.exe''' | Where-Object { $_.CommandLine -like ('*' + $main + '*') }; if (-not $procs) { exit 3 }; foreach ($proc in $procs) { Write-Host ('Found process: ' + $proc.ProcessId); Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop }"
set "STOP_RESULT=%ERRORLEVEL%"

if "%STOP_RESULT%"=="3" (
    echo %APPLICATION% is not running
    exit /b 0
)
if not "%STOP_RESULT%"=="0" (
    echo Warning: Failed to stop %APPLICATION%.
    exit /b 1
)

timeout /t 1 /nobreak >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "$main=$env:MAIN_CLASS; $proc=Get-CimInstance Win32_Process -Filter 'Name = ''java.exe''' | Where-Object { $_.CommandLine -like ('*' + $main + '*') } | Select-Object -First 1; if ($proc) { exit 1 }"
if errorlevel 1 (
    echo Warning: Some %APPLICATION% process may still be running.
    exit /b 1
)

echo %APPLICATION% stopped successfully
exit /b 0
