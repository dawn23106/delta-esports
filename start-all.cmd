@echo off
chcp 65001 >nul
title ========== Delta Esports — 一键启动 ==========

echo.
echo   ╔══════════════════════════════════════════╗
echo   ║      沧月电竞 Delta Esports              ║
echo   ║      一键启动全部服务                      ║
echo   ╚══════════════════════════════════════════╝
echo.

:: ===================== 环境检测 =====================
echo [1/3] 检测环境...

:: 自动设置 JAVA_HOME（如果未设置）
if "%JAVA_HOME%"=="" (
    if exist "%USERPROFILE%\.jdks\ms-17.0.19" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-17.0.19"
        echo   已自动检测 JDK: %JAVA_HOME%
    )
)

call :check java  "Java"   || exit /b 1
call :check node  "Node.js" || exit /b 1
call :check npm   "npm"     || exit /b 1

echo   环境检测通过!
echo.

:: ===================== 后端 =====================
echo [2/3] 启动后端服务 (Spring Boot :8080)...
start "△ Backend :8080" cmd /c ^
"cd /d "%~dp0backend" ^
 && echo ===== Delta Esports Backend ===== ^
 && echo Port: 8080 ^
 && echo Swagger: http://localhost:8080/swagger-ui.html ^
 && echo. ^
 && mvnw spring-boot:run"

:: ===================== 前端 - 微信小程序与客服后台 =====================
echo [3/3] 启动前端...

start "▲ MiniProgram Compiler" cmd /c ^
"cd /d "%~dp0frontend-uniapp" ^
 && echo ===== Delta Esports WeChat MiniProgram ===== ^
 && echo Output: dist/dev/mp-weixin ^
 && echo Open the repository root in WeChat DevTools after compilation. ^
 && echo. ^
 && npm run dev:mp-weixin"

start "▲ Admin :5174" cmd /c ^
"cd /d "%~dp0frontend-admin" ^
 && echo ===== Delta Esports Admin ===== ^
 && echo Port: 5174 ^
 && echo URL: http://localhost:5174 ^
 && echo. ^
 && npm run dev"

echo.
echo   ╔══════════════════════════════════════════╗
echo   ║  全部服务已启动!                            ║
echo   ║                                            ║
echo   ║  Backend :  http://localhost:8080           ║
echo   ║  Swagger :  http://localhost:8080/swagger   ║
echo   ║  MiniApp :  frontend-uniapp/dist/dev/mp-weixin ║
echo   ║  Admin   :  http://localhost:5174           ║
echo   ║                                            ║
echo   ║  使用 stop-all.cmd 停止所有服务                 ║
echo   ╚══════════════════════════════════════════╝
echo.
pause
exit /b 0

:: ===================== 检测函数 =====================
:check
where %~1 >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 未找到 %~2，请先安装 %~2
    echo   后端需要 JDK 17+, 下载: https://adoptium.net
    echo   前端需要 Node.js 18+, 下载: https://nodejs.org
    exit /b 1
)
echo   [✓] %~2 已就绪
exit /b 0
