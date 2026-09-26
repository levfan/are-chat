#!/bin/sh
# 同时拉起 Spring Boot（仅监听 127.0.0.1）与 nginx（前台），任一退出即结束容器交给重启策略
set -u

mkdir -p /data/uploads

# sun-misc-unsafe-memory-access=allow：压制 fastjson2 在 JDK 25 上的 sun.misc.Unsafe 弃用告警（JEP 498，未来 JDK 移除该方法前由 fastjson2 上游跟进）
JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=75.0 --sun-misc-unsafe-memory-access=allow}"
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
