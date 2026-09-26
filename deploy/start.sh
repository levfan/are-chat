#!/bin/sh
# 同时拉起 Spring Boot（仅监听 127.0.0.1）与 nginx（前台），任一退出即结束容器交给重启策略
set -u

mkdir -p /data/uploads

JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=75.0}"
java $JAVA_OPTS -jar /app/app.jar --server.address=127.0.0.1 &
APP_PID=$!

nginx -t
nginx -g 'daemon off;' &
NGINX_PID=$!

trap 'kill -TERM "$APP_PID" "$NGINX_PID" 2>/dev/null' TERM INT

while kill -0 "$APP_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
    sleep 3
done
exit 1
