@echo off
chcp 65001 >nul
echo.
echo   ===== 停止 Delta Esports 所有服务 =====
echo.

:: Kill backend
taskkill /fi "WINDOWTITLE eq △ Backend*" /f >nul 2>&1
echo   [✓] 后端服务已停止

:: Kill frontend windows
taskkill /fi "WINDOWTITLE eq ▲ Mobile*" /f >nul 2>&1
echo   [✓] Mobile 前端已停止

taskkill /fi "WINDOWTITLE eq ▲ Admin*" /f >nul 2>&1
echo   [✓] Admin 前端已停止

echo.
echo   所有服务已停止
timeout /t 2 >nul
exit /b 0
