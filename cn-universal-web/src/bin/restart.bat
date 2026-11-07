@echo off
setlocal EnableExtensions
rem ======================================================================
rem Windows restart script
rem
rem ======================================================================

rem 项目名称
set "APPLICATION=cn-universal-web"

rem 获取脚本所在目录
set "BIN_PATH=%~dp0"

echo Restarting %APPLICATION%...

rem 停止服务
echo Stopping %APPLICATION%...
call "%BIN_PATH%stop.bat"
if errorlevel 1 (
    echo Warning: Failed to stop %APPLICATION% properly
)

rem 等待一下确保进程完全停止
timeout /t 3 /nobreak >nul

rem 启动服务
echo Starting %APPLICATION%...
call "%BIN_PATH%start.bat"
if errorlevel 1 (
    echo Error: Failed to start %APPLICATION%
    exit /b 1
)

echo %APPLICATION% restarted successfully!
exit /b 0
