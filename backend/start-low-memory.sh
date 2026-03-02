#!/bin/bash

# 宿舍用电管理系统 - 低内存启动脚本
# 适用于1核1G服务器

APP_NAME="dorm-power-backend"
JAR_FILE="target/dorm-power-backend-1.0.0.jar"

# JVM参数优化 - 1核1G配置
# -Xms256m: 初始堆内存256MB
# -Xmx512m: 最大堆内存512MB (留一半给系统)
# -XX:MetaspaceSize=64m: 元空间初始大小
# -XX:MaxMetaspaceSize=128m: 元空间最大大小
# -XX:+UseSerialGC: 使用串行GC (单核最优)
# -XX:+TieredCompilation: 分层编译
# -XX:TieredStopAtLevel=1: 快速启动
# -XX:+UseStringDeduplication: 字符串去重
# -XX:MaxDirectMemorySize=64m: 直接内存限制
# -Xss256k: 线程栈大小
# -XX:+HeapDumpOnOutOfMemoryError: OOM时dump
# -XX:HeapDumpPath=./logs/heapdump.hprof: dump路径

JVM_OPTS="-server \
  -Xms256m \
  -Xmx512m \
  -XX:MetaspaceSize=64m \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseSerialGC \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  -XX:+UseStringDeduplication \
  -XX:MaxDirectMemorySize=64m \
  -Xss256k \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./logs/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true"

# Spring Boot参数
SPRING_OPTS="--spring.profiles.active=low-memory \
  --server.tomcat.max-threads=50 \
  --server.tomcat.min-spare-threads=5 \
  --server.tomcat.max-connections=200 \
  --server.tomcat.accept-count=20"

# 创建日志目录
mkdir -p logs

# 启动应用
echo "Starting $APP_NAME with low-memory configuration..."
echo "JVM Options: $JVM_OPTS"
echo "Memory: 256MB initial, 512MB max"

java $JVM_OPTS -jar $JAR_FILE $SPRING_OPTS
