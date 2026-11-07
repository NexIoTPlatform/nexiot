<div align="center">

# 🤖 NexIoT AI物联网平台

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.5-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3%20%2F%20Arco-42b883.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg)
![MQTT](https://img.shields.io/badge/MQTT-Supported-green.svg)
![HTTP](https://img.shields.io/badge/HTTP-Supported-blue.svg)
![AIoT](https://img.shields.io/badge/Enterprise-Video%20%26%20AIoT-purple.svg)

## 🎯 产品驱动外置的「真·零代码侵入」物联网平台

> **设备驱动外置 · 在线编写与调试 · 产品一键导入导出 · 连接设备、视频、工业现场与业务应用**

[📖 产品文档](https://docs.nexiot.cc/) | [🌐 企业版演示](http://demo.nexiot.cc/) | [🔧 驱动调试 IDE](http://demo.nexiot.cc/magic/debug/index.html)

</div>

---

<a id="introduction"></a>

## ✨ 平台简介

**NexIoT AI物联网平台** 面向设备集成、工业采集、视频 AIoT 和企业应用建设，以产品、物模型和外置驱动为中心，统一设备接入、数据处理、功能调用与应用集成。

平台将设备差异放到产品驱动和接入配置中处理：驱动可以独立编写、调试、导入导出和更新，业务系统通过统一的设备模型使用数据与能力。

### 🎯 这个项目能为你做什么？

-  **建设自己的 IoT 基础能力中心**：统一管理不同来源的产品、设备、数据和应用，服务企业内部系统及 B / G 类项目。
-  **降低重复设备集成成本**：复用物模型、产品驱动和接入配置，逐步积累自己的设备接入资产。
- ️ **让驱动开发与平台开发分工更清晰**：在 WEB IDE 中编写、运行、调试产品驱动，减少设备适配对平台核心代码的改动。
-  **沉淀可共享的接入成果**：产品、物模型与驱动内容一起导入导出，便于跨项目复用。
-  **连接设备与视频业务**：将工业采集、视频、AI 算法、规则和可视化纳入同一平台的产品与权限体系。
-  **从基础接入逐步扩展业务**：使用开源版建设 MQTT / HTTP 设备应用，企业项目可扩展工业、视频与 AIoT 等能力。

<a id="editions"></a>

## 🏗️ 技术架构

| 层次 | 当前技术与实现 |
| --- | --- |
| 后端 | **Java 25**、Spring Boot 3.5、MyBatis、虚拟线程 |
| 前端 | **Vue3 / TypeScript / Arco Design / Vite** |
| 开源版默认数据库 | **PostgreSQL**，本地验收版本为 PostgreSQL 18.4 |
| 基础组件 | Redis、EMQX / MQTT Broker、Docker Compose |
| 历史与日志存储 | PostgreSQL，以及保留的 MySQL、IoTDB、ClickHouse、InfluxDB、TDengine 等适配实现，按配置选择与验证 |
| 产品驱动 | WEB IDE、JAR 扩展及产品级编解码、在线调试、历史源码管理 |

| 🏗️ 驱动与数据流 | 🚀 部署架构 |
| --- | --- |
| ![驱动与数据流](__MACOSX/shot/dataflow.png) | ![部署架构](__MACOSX/shot/deploy.jpg) |

<a id="community-start"></a>

## 🧭 前端、后台与 Docker 运行

开源版默认使用 **PostgreSQL + Redis + MQTT Broker**。当前 Docker Compose 负责启动这三个基础服务，后台和 Vue3 前端按下述步骤运行。

### 1. Docker 启动基础服务

准备 Docker Compose，复制并编辑环境文件，设置数据库及 EMQX 管理密码：

```bash

cp .env.example .env

# 编辑 .env 中的 POSTGRES_PASSWORD 和 EMQX_DASHBOARD_PASSWORD

docker compose --env-file .env -f docker/docker-compose.yml up -d --build
```

首次安装会初始化系统表结构与开源版菜单。已有环境使用[增量迁移](cn-universal-db-migrator/README.md)，不能重放初始化 SQL。

### 2. 编译与启动后台

使用 **Java 25、Maven 3.9**，确保已安装 `curl`，在仓库根目录执行：

```bash

mvn clean package -Dmaven.test.skip=true

mkdir -p .runtime

tar -xzf cn-universal-web/target/cn-universal-web.tar.gz -C .runtime

cp runtime.env.example .runtime/cn-universal-web/config/runtime.env

chmod 600 .runtime/cn-universal-web/config/runtime.env
```

编辑 `.runtime/cn-universal-web/config/runtime.env`，将 `PG_DATASOURCE_PASSWORD` 改为 `.env` 中的数据库密码，并确认连接地址和端口：

```bash

.runtime/cn-universal-web/bin/restart.sh

```


### 3. 启动开源版前端

使用 **Node.js 22.12+**，在仓库根目录另开终端：

```bash

cd nexiot-ui-arco
npm ci --no-audit --no-fund
npm run dev -- --port 8000
```

| 入口 | 默认值 |
| --- | --- |
| 前端控制台 | <http://localhost:8000> |
| 后台服务 | <http://localhost:9092> |
| 全新初始化的平台管理员 | `admin` |
| 初始安装密码 | `NexIoT@2026-ChangeMe` |
| EMQX 管理台 | <http://localhost:18083>，密码使用 `.env` 中的配置 |

平台管理员用于创建租户和租户管理员，进入租户后进行产品、设备与应用操作。首次登录后修改安装密码。


## 🌐 演示环境和文档

- **全功能演示**：<http://nexiot.cc/>
- **驱动调试 IDE**：<http://test.iot.192886.xyz:81/magic/debug/index.html>
- **产品文档**：<https://docs.nexiot.cc/>
- **演示账号**：`test`，直接微信扫码，也可联系微信 `outlookFil`。
- **真实设备演示与商业合作**：联系维护者，说明设备类型、协议、业务场景与部署需求。

<a id="features"></a>

## 📊 功能与版本对比

**[📖 查看完整功能对照：开源版 vs 企业版 →](docs/FEATURES.md)**

## 📸 全功能展示


![企业版登录页](__MACOSX/shot/20261107/login.png)

### 🖥️ 工作台、产品与设备

#### 📦 基础能力

|                                                            |                                                            |
|------------------------------------------------------------|------------------------------------------------------------|
| ![首页](__MACOSX/shot/20261107/01-index.png)                 | ![首页](__MACOSX/shot/20261107/02-index.png)                 |
| ![产品](__MACOSX/shot/20261107/03-product.png)               | ![magic调试](__MACOSX/shot/20261107/04-magic.png)            |
| ![设备列表](__MACOSX/shot/20261107/05-d.png)                   | ![属性](__MACOSX/shot/20261107/06-ds.png)                    |

#### 📡 工业采集

支持Modbus TCP、S7、OPCUA、IEC104等工业协议

|                                              |                                                |
|----------------------------------------------|------------------------------------------------|
| ![点位模版](__MACOSX/shot/20261107/07-point.png) | ![首页](__MACOSX/shot/20261107/08-s7.png)        |
| ![产品](__MACOSX/shot/20261107/03-product.png) | ![magic调试](__MACOSX/shot/20261107/09-push.png) |
| ![设备列表](__MACOSX/shot/20261107/05-d.png)     | ![属性](__MACOSX/shot/20261107/06-ds.png)        |

#### 📡 视频AIoT

海康、大华、宇视频、天地伟业、相机，NetSDK直连进你自己的系统；支持海康ISUP、大华主动注册、乐橙云、萤石云、

|                                               |                                                  |
|-----------------------------------------------|--------------------------------------------------|
| ![首页](__MACOSX/shot/20261107/21-video.png)  | ![点位模版](__MACOSX/shot/20261107/24-videox.png)    |
| ![产品](__MACOSX/shot/20261107/21-unview.png)  | ![magic调试](__MACOSX/shot/20261107/23-videos.png) |

#### 📡 Mac/Win 双客户端

支持Mac/Win,支持硬解、低延迟

![点位模版](__MACOSX/shot/20261107/80-pc01.png)

#### 📡 告警中心

告警中心上线：秒级到人、有据可查、自动恢复

|                                               |                                                |
|-----------------------------------------------|------------------------------------------------|
| ![首页](__MACOSX/shot/20261107/40-alarmindex.png)  | ![点位模版](__MACOSX/shot/20261107/41-image.png)   |
| ![产品](__MACOSX/shot/20261107/42-center.png)  | ![magic调试](__MACOSX/shot/20261107/43-flow.png) |

#### 📡 组态大屏

告警中心上线：秒级到人、有据可查、自动恢复

|                                             |                                                |
|---------------------------------------------|------------------------------------------------|
| ![首页](__MACOSX/shot/20261107/50-2d.png)     | ![点位模版](__MACOSX/shot/20261107/50-3d.png)      |




[📺 视频：NexIoT 小程序](https://www.bilibili.com/video/BV1WMqDB6EAc/)

### 🧱 奈科斯应用工坊与移动端 DIY

基于 NexIoT 北向应用组织业务应用与移动端界面，连接产品、设备、数据和用户。配套工程及演示信息请联系维护者。

|  NexIoT小程序                                                | 奈科斯应用工坊                                    |          奈科斯应用工坊安卓                       |
|------------------------------------------------------|--------------------------------------------|----------------------------------------------|
| ![应用工坊小程序](__MACOSX/shot/qrcode/nexiot.jpg)  | ![应用工坊小程序](__MACOSX/shot/qrcode/saas.jpg) |![奈科斯应用工坊安卓](__MACOSX/shot/qrcode/saas-android.jpg) |


### 🔌 API 与业务集成

![API 与业务集成](__MACOSX/shot/apis.png)

## 🏙️ 行业场景与客户案例

面向智慧城市、工业现场、轨道交通、交通基建、校园与园区等场景，连接设备接入与业务系统建设。

<table>
  <tr>
    <td align="center" width="50%">
      <strong>智慧城市</strong><br/>
      市/区级智慧城市物联网平台
    </td>
    <td align="center" width="50%">
      <strong>科技企业</strong><br/>
      持续服务多类企业客户
    </td>
  </tr>

  <tr>
    <td align="center" width="50%">
      <strong>校园教育</strong><br/><br/>
      教育局智慧化、智慧校园等
    </td>
    <td align="center" width="50%">
      <strong>企业园区</strong><br/><br/>
      实验室、企业园区、智慧车间等等
    </td>
  </tr>
   <tr>
    <td align="center" width="50%">
      <strong>轨道交通</strong><br/><br/>
      <img src="__MACOSX/shot/kehu/szdt.png" alt="深圳地铁" height="28" /><br/>
      深圳地铁
    </td>
    <td align="center" width="50%">
      <strong>交通基建</strong><br/><br/>
      <img src="__MACOSX/shot/kehu/ynjt.png" alt="云南交投" height="28" /><br/>
      云南交投
    </td>
  </tr>
</table>

## 📺 视频教程与社区交流

教程与设备接入经验可关注 B 站、抖音及社区渠道。历史教程请结合当前版本的配置与界面使用。

### 📚 基础入门教程

| 序号 | 教程名称                            | 视频链接 |
|:---:|:--------------------------------|:---|
| 1 | 【NexIoT课程】（一）IDEA与Docker一键启动    | [📺 B站观看](https://www.bilibili.com/video/BV1WNUnBnEx5/) |
| 2 | 【NexIoT课程】（二）EMQX配置             | [📺 B站观看](https://www.bilibili.com/video/BV1MdUJB4E7k/) |
| 2 | 【NexIoT课程】（三）工业采集 Modbus 从零保姆教程 | [📺 B站观看](https://www.bilibili.com/video/BV13eNc6qEPw/?spm_id_from=333.1387.0.0) |


### 📨 MQTT接入教程

| 序号 | 教程名称 | 视频链接 |
|:---:|:---|:---|
| 1 | 任意主题Topic与全流程对接教程 | [📺 B站观看](https://www.bilibili.com/video/BV1q1UZBmEHS/) |


<a id="contact"></a>

### 🌐 社区交流与商业合作

- **微信**：`outlookFil`，请备注来意。


|微信                            | B站                              | 抖音                               | 
|-------------------------------|---------------------------------|----------------------------------|
|  ![微信](__MACOSX/shot/wx.png) | ![B站](__MACOSX/shot/bzhan.jpg) | ![抖音](__MACOSX/shot/douyin.jpg) |

## 📄 开源协议与重要声明

- **开源协议**：本项目遵循 Apache-2.0 开源协议，允许个人及企业在遵守协议与本声明的前提下进行商用用途、学习研究和二次开发
- **保留声明**：任何使用、修改、集成、抽取或部署本项目时，不得删除本项目LOGO、来源、版权信息、License、NOTICE、代码注释及相关声明
- **禁止行为**：严禁去标识化、冒名发布、二次开源、二次分发、转授权，或将本项目内容包装为自有项目/产品对外传播/申请软著
- **法律追责**：违反上述要求的，将依法追究相关法律责任


### 🙏 致谢

感谢以下开源项目和技术平台：

- **开源框架**：若依、Antdv、jetlink、ssssssss-team
- **云平台**：阿里云、华为云、腾讯云、AEP、OneNet 等物联网平台
- **社区支持**：所有贡献者和用户的支持与反馈