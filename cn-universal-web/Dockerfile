# 使用本地可用的 OpenJDK 25 基础镜像，避免外网拉取失败
FROM eclipse-temurin:25-jre

# 设置工作目录
WORKDIR /app

# 安装必要的工具
# 安装必要工具（基于 Debian/Ubuntu 的 openjdk 官方镜像）
RUN set -eux; \
    if command -v apt-get >/dev/null 2>&1; then \
      apt-get update -y && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*; \
    elif command -v microdnf >/dev/null 2>&1; then \
      microdnf update -y && microdnf install -y curl && microdnf clean all; \
    else \
      echo "No supported package manager found" && exit 1; \
    fi

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 复制应用文件到带项目名的目录，保持 bin 的相对层级结构
COPY target/cn-universal-web/ /app/cn-universal-web/

# 设置权限
RUN chown -R appuser:appuser /app
USER appuser

# 暴露端口
EXPOSE 9092

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9092/actuator/health || exit 1

# 预创建日志目录
RUN mkdir -p /app/logs /app/logs/global /app/logs/api /app/logs/tcp /app/logs/mqtt

# 启动命令：创建日志目录与文件，执行启动脚本，前台跟随 info.log 保持容器存活
ENTRYPOINT ["sh", "-c", "mkdir -p /app/logs && touch /app/logs/info.log && /app/cn-universal-web/bin/start.sh && tail -n +1 -F /app/logs/all.log"]
