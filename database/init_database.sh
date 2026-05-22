#!/bin/bash

echo "===================================="
echo "人脸识别门禁系统 - 数据库初始化"
echo "===================================="
echo ""

MYSQL_PATH="mysql"
SQL_FILE="$(dirname "$0")/face_access_system.sql"
DEFAULT_MYSQL_USER="root"

read -p "请输入MySQL用户名（默认: ${DEFAULT_MYSQL_USER}）: " MYSQL_USER
MYSQL_USER=${MYSQL_USER:-$DEFAULT_MYSQL_USER}

echo "请输入MySQL密码（输入时不显示）:"
read -s MYSQL_PASSWORD

echo ""
echo "正在连接MySQL并创建数据库..."
echo ""

if [ -z "$MYSQL_PASSWORD" ]; then
    $MYSQL_PATH -u "$MYSQL_USER" < "$SQL_FILE"
else
    $MYSQL_PATH -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" < "$SQL_FILE"
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "===================================="
    echo "数据库初始化成功！"
    echo "===================================="
    echo ""
    echo "数据库名称: face_access_system"
    echo "默认管理员账号: admin"
    echo "默认管理员密码: 123456"
    echo ""
    echo "请确认后端配置文件:"
    echo "demo/src/main/resources/application.yml"
    echo "中的数据库连接与本机一致"
    echo ""
else
    echo ""
    echo "===================================="
    echo "数据库初始化失败！"
    echo "===================================="
    echo ""
    echo "请检查:"
    echo "1. MySQL服务是否已启动"
    echo "2. 用户名或密码是否正确"
    echo "3. 用户是否有创建数据库权限"
    echo "4. SQL文件路径是否正确"
    echo ""
fi
