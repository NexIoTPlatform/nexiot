# IoT 平台 Docker 部署

本文件夹包含 IoT 平台的所有 Docker 相关配置和脚本。

## 📁 文件结构

```
docker/
├── README.md                           # 本说明文档
├── docker-compose.yml                  # 完整服务编排 (MySQL + Redis + EMQX + 应用)
├── docker-compose.apps.yml             # 仅应用服务编排
├── build-docker.sh                     # Docker 镜像构建脚本
├── start-docker.sh                     # 启动完整服务脚本
├── start-apps.sh                       # 启动应用服务脚本
├── env.example                         # 环境变量模板
├── mysql/                              # MySQL 配置和初始化
│   ├── conf/                           # MySQL 配置文件
│   │   └── my.cnf                      # MySQL 5.8 配置
│   └── init/                           # 数据库初始化脚本
│       ├── 01-init.sql                 # 基础初始化
│       └── 02-iot.sql                  # IoT 平台数据表和数据
├── DOCKER_DEPLOYMENT.md                # Docker 部署详细说明
├── DOCKER_TROUBLESHOOTING.md           # 问题排查指南
├── DEPENDENCY_ANALYSIS.md              # 依赖兼容性分析
└── DEPLOYMENT_SUMMARY.md               # 部署总结和状态
```

## 🚀 快速开始

### 1. 构建镜像

```bash
# 进入 docker 目录
cd docker

# 构建默认版本 (推荐)
./build-docker.sh default local

# 构建其他版本
./build-docker.sh alpine              # 后端 Alpine + 前端默认
./build-docker.sh corretto ubuntu     # 后端 Corretto + 前端 Ubuntu
```

### 2. 启动服务

#### 方式一：仅启动应用服务 (当前推荐)

```bash
cd docker
./start-apps.sh
```

**访问地址：**

- 前端界面: http://localhost:80
- 后端 API: http://localhost:9092

#### 方式二：启动完整服务 (需要解决网络问题)

```bash
cd docker
./start-docker.sh
```

**包含服务：**

- MySQL 5.8 (端口 3306)
- Redis 7.x (端口 6379)
- EMQX 5.3 (端口 1883, 8083, 8084, 18083)
- 后端应用 (端口 9092)
- 前端应用 (端口 80)
- Adminer 数据库管理 (端口 8081)

## 🔧 构建选项

### 后端版本

- `default`: OpenJDK 25 (Oracle Linux) - 推荐
- `alpine`: OpenJDK 25 Alpine (镜像更小)
- `corretto`: Amazon Corretto 25 (企业级支持)

### 前端版本

- `default`: Alpine + nginx (网络问题)
- `ubuntu`: Ubuntu + nginx
- `centos`: CentOS + nginx
- `simple`: Python HTTP 服务器
- `local`: 基于本地镜像 + nginx - 推荐

## 📊 服务管理

### 查看服务状态

```bash
cd docker

# 应用服务
docker-compose -f docker-compose.apps.yml ps

# 完整服务
docker-compose ps
```

### 查看日志

```bash
cd docker

# 后端日志
docker-compose -f docker-compose.apps.yml logs backend

# 前端日志
docker-compose -f docker-compose.apps.yml logs frontend

# 实时日志
docker-compose -f docker-compose.apps.yml logs -f
```

### 停止服务

```bash
cd docker

# 应用服务
docker-compose -f docker-compose.apps.yml down

# 完整服务
docker-compose down
```

## 🌐 网络问题解决

如果遇到镜像拉取失败，请配置 Docker 镜像加速器：

```bash
# 创建 Docker 配置文件
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF

# 重启 Docker 服务
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## 📝 端口配置

- **前端**: 80 (HTTP)
- **后端**: 9092 (Spring Boot)
- **MySQL**: 3306
- **Redis**: 6379
- **EMQX**: 1883 (MQTT), 8083 (MQTT/SSL), 8084 (MQTT/WebSocket), 18083 (管理界面)
- **Adminer**: 8081 (数据库管理)

## 🗄️ 数据库初始化

MySQL 启动后会自动执行以下初始化脚本：

1. `01-init.sql`: 创建数据库、用户、设置字符集等
2. `02-iot.sql`: 创建 IoT 平台所需的所有表结构和初始数据

## ⚠️ 注意事项

1. **端口冲突**: 确保本地端口 80, 9092, 3306, 6379, 1883 等未被占用
2. **网络问题**: 如果无法拉取镜像，请使用 `./build-docker.sh default local` 构建
3. **数据持久化**: MySQL 和 Redis 数据会保存在 Docker volumes 中
4. **配置文件**: 可以通过修改 `mysql/conf/my.cnf` 调整 MySQL 配置

## 🔍 故障排查

如果遇到问题，请查看：

1. [DOCKER_TROUBLESHOOTING.md](./DOCKER_TROUBLESHOOTING.md) - 问题排查指南
2. [DEPLOYMENT_SUMMARY.md](./DEPLOYMENT_SUMMARY.md) - 部署状态和问题诊断
3. 容器日志: `docker-compose logs -f [service_name]`

## 📞 技术支持

如需帮助，请提供：

1. Docker 版本 (`docker --version`)
2. Docker Compose 版本 (`docker-compose --version`)
3. 操作系统信息
4. 错误日志
5. 网络环境信息
