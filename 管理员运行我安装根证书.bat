@echo off
title 安装根证书到“受信任的根证书颁发机构”

cd /d "%~dp0"

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo [错误] 当前没有管理员权限！
    echo 请右键此脚本，选择：
    echo “以管理员身份运行”
    echo.
    pause
    exit /b 1
)

echo.
echo 已检测到管理员权限。
echo 正在导入根证书到：
echo 本地计算机 ^> 受信任的根证书颁发机构
echo.

if not exist "%~dp0ca.crt" (
    echo [错误] 未找到 ca.crt 文件！请把 ca.crt 放在本脚本同一目录。
    pause
    exit /b 1
)

certutil -addstore -f Root "%~dp0ca.crt"

if %errorlevel% neq 0 (
    echo.
    echo [失败] 证书导入失败。
) else (
    echo.
    echo [成功] 根证书已成功导入！
)

echo.
pause
