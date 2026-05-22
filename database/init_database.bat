@echo off
chcp 65001 >nul
echo ====================================
echo 人脸识别门禁系统 - 数据库初始化
echo ====================================
echo.

set MYSQL_PATH="C:\Program Files (x86)\mysql-8.4.5-winx64\bin\mysql.exe"
set SQL_FILE="%~dp0face_access_system.sql"

echo 请输入MySQL root密码（如果没有密码，直接按回车）:
set /p MYSQL_PASSWORD=

echo.
echo 正在连接MySQL并创建数据库...
echo.

if "%MYSQL_PASSWORD%"=="" (
    %MYSQL_PATH% -u root < %SQL_FILE%
) else (
    %MYSQL_PATH% -u root -p%MYSQL_PASSWORD% < %SQL_FILE%
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================
    echo 数据库初始化成功！
    echo ====================================
    echo.
    echo 数据库名称: face_access_system
    echo 默认管理员账号: admin
    echo 默认管理员密码: 123456
    echo.
    echo 请修改后端配置文件:
    echo demo\src\main\resources\application.yml
    echo 更新数据库连接信息
    echo.
) else (
    echo.
    echo ====================================
    echo 数据库初始化失败！
    echo ====================================
    echo.
    echo 请检查:
    echo 1. MySQL服务是否已启动
    echo 2. root密码是否正确
    echo 3. SQL文件路径是否正确
    echo.
)

pause
