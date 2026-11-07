@echo off
setlocal EnableExtensions
rem ======================================================================
rem Windows startup script
rem
rem ======================================================================

rem Open in a browser
rem start "" "http://localhost:8080/example/hello?name=123"

rem 项目名称
set "APPLICATION=cn-universal-web"

rem 主类名
set "MAIN_CLASS=cn.universal.CnUniversalIoTApplication"

rem 获取脚本所在目录
set "BIN_PATH=%~dp0"
rem 获取项目根目录
for %%I in ("%BIN_PATH%..") do set "BASE_PATH=%%~fI"

rem 配置文件目录
set "CONFIG_DIR=%BASE_PATH%\config"

rem 日志目录
set "LOG_DIR=%BASE_PATH%\logs"
set "STARTUP_LOG=%LOG_DIR%\startup.log"

rem 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

rem 检查Java是否可用
where java >nul 2>&1
if errorlevel 1 (
    echo Error: java command not found. Please install Java 21 and add it to PATH.
    exit /b 1
)

rem 检查jar包是否存在
if not exist "%BASE_PATH%\%APPLICATION%.jar" (
    echo Error: JAR file %BASE_PATH%\%APPLICATION%.jar not found!
    exit /b 1
)

rem 检查lib目录是否存在
if not exist "%BASE_PATH%\lib" (
    echo Error: lib directory %BASE_PATH%\lib not found!
    exit /b 1
)

rem 检查配置文件目录是否存在
if not exist "%CONFIG_DIR%" (
    echo Warning: Config directory %CONFIG_DIR% not found!
)

rem 构建classpath (Windows使用分号分隔)
set "CLASSPATH=%BASE_PATH%\%APPLICATION%.jar;%BASE_PATH%\lib\*"

rem JVM Configuration
set "JAVA_OPT=-Duser.timezone=Asia/Shanghai -server -Xms4096m -Xmx4096m -Xmn2048m -XX:MetaspaceSize=2048m -XX:MaxMetaspaceSize=2048m -XX:-OmitStackTraceInFastThrow"

echo Starting %APPLICATION%...
echo ================================================================>> "%STARTUP_LOG%"
echo Starting %APPLICATION% at %DATE% %TIME%>> "%STARTUP_LOG%"
echo BASE_PATH: %BASE_PATH%>> "%STARTUP_LOG%"
echo CLASSPATH: %CLASSPATH%>> "%STARTUP_LOG%"

rem 启动应用。使用后台cmd承载java进程，保持与Linux nohup脚本一致。
pushd "%BASE_PATH%" >nul
start "%APPLICATION%" /MIN cmd /c ""java" %JAVA_OPT% -cp "%CLASSPATH%" %MAIN_CLASS% --spring.config.location="%CONFIG_DIR%/" --logging.config="%CONFIG_DIR%\logback-spring.xml" >> "%STARTUP_LOG%" 2>&1"
set "START_RESULT=%ERRORLEVEL%"
popd >nul

if not "%START_RESULT%"=="0" (
    echo Error: Failed to start %APPLICATION%
    exit /b 1
)

rem 等待进程稳定运行，避免刚启动就退出时误判成功。
timeout /t 5 /nobreak >nul
call :find_pid
if not defined PID (
    echo Error: %APPLICATION% process was not found after startup.
    echo Startup log: %STARTUP_LOG%
    exit /b 1
)

echo PID: %PID%
echo PID: %PID%>> "%STARTUP_LOG%"
echo %APPLICATION% started successfully!
exit /b 0

:find_pid
set "PID="
set "PID_FILE=%TEMP%\%APPLICATION%-pid.tmp"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$main=$env:MAIN_CLASS; $proc=Get-CimInstance Win32_Process -Filter 'Name = ''java.exe''' | Where-Object { $_.CommandLine -like ('*' + $main + '*') } | Select-Object -First 1; if ($proc) { $proc.ProcessId }" > "%PID_FILE%" 2>nul
if exist "%PID_FILE%" set /p PID=<"%PID_FILE%"
del "%PID_FILE%" >nul 2>&1
exit /b 0
