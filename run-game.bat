@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat run
set "GAME_EXIT_CODE=%ERRORLEVEL%"
if not "%GAME_EXIT_CODE%"=="0" (
  echo.
  echo 游戏启动失败。请确认已安装 Java 17 或更高版本。
  pause
)
endlocal & exit /b %GAME_EXIT_CODE%
