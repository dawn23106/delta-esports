@echo off
echo ============================================
echo   沧月电竞 - 隧道自动重连
echo   端口: 4173 (preview)
echo ============================================
:loop
echo.
echo [%date% %time%] 正在启动隧道...
npx -y localtunnel --port 4173 2>&1
echo [%date% %time%] 隧道断开，3秒后自动重连...
timeout /t 3 /nobreak >nul
goto loop
