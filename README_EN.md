# 🤖 NexIoT AI IoT Platform

<div align="center">

![License](https://img.shields.io/badge/license-AGPL3.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.5-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)
![Redis](https://img.shields.io/badge/Redis-Latest-red.svg)
![IoTDB](https://img.shields.io/badge/IoTDB-Supported-green.svg)
![ClickHouse](https://img.shields.io/badge/ClickHouse-Supported-yellow.svg)
![AI](https://img.shields.io/badge/AI-Enabled-purple.svg)

## 🎯 Innovative "True Zero Code Intrusion" IoT Platform

> **💡 Breaking traditional IoT platform design concepts · Device drivers completely externalized · One-click export and use · Zero code intrusion**

[📖 Documentation](https://nexiotplatform.github.io/universal-iot-docs/) | [🌐 Online Demo](http://iot.192886.xyz:81/) | [🔧 AI Debugging IDE](http://iot.192886.xyz:81/magic/debug/index.html)

**English | [中文](README-zh.md)**

</div>

## ✨ Platform Overview

**NexIoT AI IoT Platform** is an enterprise-level IoT platform with innovative architecture design featuring **True Zero Code Intrusion**.

### 🎯 Core Highlights

> **🚀 What can this project do for you?**

- 🏢 **Suitable for Medium and Large Enterprises**: **IoT Infrastructure Center, Unified Device Data Access**, for those developing their own products or B|G projects
- 🔓 **No More Vendor Lock-in**: No longer held hostage by device suppliers or software providers charging exorbitant prices
- 🎓 **Easy to Learn**: Non-Java developers can complete device integration; university students can easily use the debugger, saving significant R&D, testing, and O&M costs
- 🤝 **Ecosystem Co-construction**: Products, thing models, and driver content can be exported with one click, enabling ecosystem co-construction and sharing
- ⚡ **Real-time Hot Deployment**: **Real-time hot deployment** takes effect; from 0 to 100 device integrations, no service restarts needed for years
- 🚀 **High Availability Cluster**: Open-source version supports clustering, millions of devices, no problem

## 🌟 Platform Features

- ✅ **Zero Code Intrusion**: Externalized device drivers, no platform code modification required, no recompilation or redeployment needed, zero coupling with platform core code, truly zero intrusion
- ✅ **Full Protocol Support**: Industrial protocols and IoT protocols such as TCP, Modbus RTU/TCP, MQTT, HTTP
- ✅ **Cloud Platform Integration**: Integration with Tianyi IoT, Mobile OneNet, WVP GB28281 National Standard Video, and other platforms
- ✅ **Multi-database Support**: Supports **MySQL 8.0+**, **IoTDB**, **ClickHouse**, **InfluxDB**, and other relational and time-series databases

## 🏗️ Technical Architecture

### 🛠️ Technology Stack

#### 🚀 Core Framework (Minimal Lightweight)

- **Backend Framework**: `Java 21` `SpringBoot 3.5` `Tk.Mybatis 5.0.1` 
- **Frontend Technology**: Built on `RuoYi-Antdv`, thanks to the open-source community!
- **Log Storage**: **IoTDB** / **ClickHouse** / **InfluxDB** / MySQL / None (product-level seamless dynamic switching)

## 🧭 Deployment and Startup (One-click Startup)

### Images are December 5, 2025 enterprise version images (preview), containing closed-source access protocols!

- **One-click startup**: `docker-compose up -d`
- **Access addresses**:
  - Backend `http://localhost:80` (default `nexiot/nexiot@123321`)
  - IDE Debugger `http://localhost:9092/magic/debug/index.html` (same password as backend)
  - EMQX Management `http://localhost:18083` (default `admin/public`)

> **🔧 For real device demos, please add WeChat, thank you!!**

## 📈 Current Progress

### Recent Roadmap

- **🚀 WVP Video Platform Integration (Planned Jan 2026)**: Integration with WVP video platform series `✅ (Completed in December 2025)`
- **🚀 Dahua ICC Series Products**: Integration with Dahua ICC product series `(No environment, delayed)`
- **🚀 Hikvision Integrated Security Management Platform**: Integration using Hikvision platform product series `(No environment, delayed)`
- **📱 Mobile Application (Planned Dec 2025)**: Paid Tuniao Ding mini-program with instruction control, attributes, alarm viewing, lightweight, multi-administrator `✅ (Beta completed in December 2025)`
- **📱 SCADA Integration (Planned Mar 2026)**: Integration of SCADA with NexIoT

## 🚀 Quick Start

### 🎯 Demo Address (Latest Version)

> **✨ All real devices, open source driver code, all transparently visible!**

> **💎 Exclusively sponsored by [Aeolian Cloud](https://www.aeoliancloud.com/cart/goodsList.htm) for NexIoT online demo server**

- **🌐 Demo Address**: <http://demo.nexiot.cc/>
- **🔧 Debug IDE**: <http://demo.nexiot.cc/magic/debug/index.html>
- **👤 Demo Account**: `test`
- **🔑 Demo Password**: `nexiot@123321`
- **📖 Documentation**: <https://docs.nexiot.cc/>

## 📊 Feature Details

> **For differences between open-source and enterprise versions, click [Version Comparison](https://docs.nexiot.cc/versions/comparison.html/ "Version Comparison")** to view

- [x] Based on JDK21, virtual threads, supports RBAC, completed security fixes, passed third-level security protection; **open-source supports clustering**
- [x] Supports WEB-IDE product driver writing, truly "zero" code intrusion protocol-oriented
- [x] Hot updates and zero intrusion: Product drivers/protocols externalized, one-click import/export for immediate use, hot deployment
- [x] Multi-protocol access: **No code changes** support for arbitrary TCP protocols, arbitrary MQTT Topics and message formats for device access, supports configurable and extensible packet sticking/unpacking, read/write timeouts, parser types via page configuration
- [x] Third-party MQTT compatibility: Supports binding/unbinding external MQTT Brokers, custom downward topics, extending access limits
- [x] Data strategy: Automatic registration, independent retention by attributes/events, configurable hex/string send/receive, log storage None/MySQL/ClickHouse/IoTDB/**InfluxDB** seamless switching
- [x] Product Management
    - [x] Supports thing model definition (properties, events, functions), import, export
    - [x] Supports setting offline thresholds for different product devices
    - [x] Supports setting product documentation addresses, product image uploads
    - [x] **Supports configuring data receive/send types (hexadecimal, string)**
- [x] **Supports enabling device active registration**
- [x] **Supports setting TCP packet sticking, unpacking, read/write timeout, parser type strategies**
- [x] Supports data retention strategies (independent storage by attributes or events for BI data analysis)
- [x] **Supports defining additional device registration parameters (such as security codes, keys)**
- [x] **Supports third-party MQTT downward topics**, HTTP service addresses
- [x] **Supports using third-party MQTT service components, binding/unbinding**
- [x] **Product drivers support IDE (Magic), JAR (local packaging and remote URI), JavaScript, SpringBean methods for driver writing**
- [x] **Zero code intrusion** device access
    - [x] Supports access to arbitrary TCP protocols (demo site has over 50 devices including RenTashi DTU gateways, JT808, DLT645-2007 meters, etc.)
    - [x] Supports access to arbitrary MQTT protocols and topics regardless of complex signature interactions or simple data reporting
    - [x] Supports complete device lifecycle management
    - [x] Supports one gateway accessing **various different sub-devices**
    - [x] Supports simultaneous two or more northbound application data pushes (HTTP, MQTT)
    - [x] Supports independent data subscription, rule forwarding
    - [x] Supports gateway, gateway sub-device topology relationships
    - [x] Gateway sub-devices support **direct function instruction call initiation**, unified standard
    - [x] Supports real-time device status viewing, historical data, visualization charts, location map tracks and complete log recording
    - [x] Supports device shadows, supports property expected value writing (arbitrary tags and data storage)
    - [x] **Supports instruction calls (including API) and device response message matching**, execution success rather than platform call success
- [x] Application Management (similar to multi-tenant)
    - [x] Supports creating multiple applications
    - [x] Supports applications generating independent AK/SK, independent northbound data addresses
    - [x] Supports OAuth2 standard authorization, supports (Maven) Java standard SDK
    - [x] **Any ordinary user supports unlimited tenant mini-programs**; mini-programs support multi-management, add, delete, query, modify, function calls, various permission controls
    - [x] Mini-programs `support theme colors`, `names`, `LOGOs` and other remote configuration
    - [x] Supports `H5`, `Android`, `IOS`
- [x] Notification Management
    - [x] Supports notification channel management (including DingTalk, SMS, voice, Feishu, email, etc.)
    - [x] Supports device data template filling
    - [x] Supports arbitrary notification template format definition, whether WEBHOOK or email, high flexibility, template replacement rendering
- [x] Supports **China Telecom IoT (CTAIOT)** perfect integration; products and devices all managed uniformly in `nexiot`
- [x] Supports seamless dynamic switching of log storage methods, supports None(not storing) / Mysql / ClickHouse / IoTDB / InfluxDB, **most comprehensive in the industry**
- [x] Supports product export, one-click import (including thing models, device drivers, etc.) truly realizing driver ecosystem sharing, **you export drivers, I import and use**
- [x] Video Capabilities
    - [x] Supports national standard GB2828-2016/2022 video access, supports **NVR**
    - [x] Supports LeCheng cloud video access
    - [x] **Unified thing model calls for video APIs and ordinary devices**

### 📢 Advanced Features

#### December 2025
- No-response device adaptation: Time window design, supports API specifying message sequence numbers, callbacks inform you of execution status, eliminating needle-in-haystack searches
- Complex interaction ACK: One line of code in IDE protocol writing, quickly implements instruction response loop closure
- Mini-program multi-user management control permissions

## 📸 Practical Cases and Video Tutorials

### 📊 Connected Device Cases (Partial Display)

|                                                |                                                |                                        |
|------------------------------------------------|------------------------------------------------|----------------------------------------|
| ![Meter Device](/__MACOSX/shot/1018/dianbiap.png)      | ![Camera](/__MACOSX/shot/1018/lechen.png)         | ![Water Sensor](/__MACOSX/shot/1018/111.jpg)   |
| ![Gateway DTU](/__MACOSX/shot/1018/dtu.jpg)          | ![Water Sensor](/__MACOSX/shot/1026/device-sj.jpg)     | ![SOS](/__MACOSX/shot/1109/sos.jpg)    |
| ![4G Tracker](/__MACOSX/shot/1018/4gcz.png)         | ![Audio Visual Alarm](/__MACOSX/shot/1026/device-sgbjq.png) | ![Water Sensor](/__MACOSX/shot/1018/4gcz2.png) |

#### ⚡ Integration Results Showcase

![Integration Dynamic](__MACOSX/shot/dianbiao.gif)

---

## 📸 Platform Interface Showcase

|          🏗️ System Architecture                            |           🚀 Cloud-native Deployment Architecture                              |
|---------------------------------------|-----------------------------------------|
| ![System Architecture](/__MACOSX/shot/dataflow.png) |![Cloud-native Deployment](/__MACOSX/shot/deploy.jpg)  |

### 🖥️ Intelligent Function Showcase

#### 📺 Product Management

|                                                         |                                                             |
|---------------------------------------------------------|-------------------------------------------------------------|
| ![Product List](/__MACOSX/shot/260118/product/list.png)         | ![Product Detail](/__MACOSX/shot/260118/product/detail.png)           |
| ![Standard Thing Model](/__MACOSX/shot/260118/product/create.png) | ![Thing Model Fields](/__MACOSX/shot/260118/product/metadata-01.png)     |
| ![Thing Model Search](/__MACOSX/shot/260118/product/create-metadata.png)     | ![Function Creation](/__MACOSX/shot/260118/product/metadata-02.png)      |
| ![Connection Info](/__MACOSX/shot/260118/product/connect.png)          | ![Network Management](/__MACOSX/shot/260118/product/connect-mqtt.png)     |
| ![Alarm Management](/__MACOSX/shot/260118/product/stand-metadata-2.png) | ![Product Detail](/__MACOSX/shot/260118/product/stand-metadata-1.png) |

#### 📺 Device Management

|                                                     |                                                    |
|-----------------------------------------------------|----------------------------------------------------|
| ![Device List](/__MACOSX/shot/260118/device/list.png)      | ![Device Detail](/__MACOSX/shot/260118/device/detail.png)   |
| ![Functions](/__MACOSX/shot/260118/device/function.png)    | ![Logs](/__MACOSX/shot/260118/device/log.png)        |
| ![Device Status](/__MACOSX/shot/260118/device/shadow.png)    | ![Gateway Device](/__MACOSX/shot/260118/device/tupu.png)     |
| ![Sub-device List](/__MACOSX/shot/260118/device/tupulist.png) | ![Network Management](/__MACOSX/shot/260118/device/video.png)    |
| ![Alarm Management](/__MACOSX/shot/260118/device/lunxun.png)    | ![Product Detail](/__MACOSX/shot/260118/device/lunxun-2.png) |

#### 🚀 All Protocol Visual Management and Online Debugging

| Breakpoint                                         | Execution Result                                         |
|----------------------------------------------|----------------------------------------------|
| ![Breakpoint](/__MACOSX/shot/260118/protocol/02.png) | ![Execution](/__MACOSX/shot/260118/protocol/03.png) |

#### 🔄 Rule Orchestration and Engine

|                                                     |                                                          |
|-----------------------------------------------------|----------------------------------------------------------|
| ![Rule Orchestration](/__MACOSX/shot/260118/other/rule-01.png)    | ![Rule Engine](/__MACOSX/shot/260118/other/qiaojie-01.png)      |
| ![Data Bridge](/__MACOSX/shot/260118/other/qiaojie-02.png) | ![Rule Engine](/__MACOSX/shot/260118/other/cert.png)            |
| ![Linkage](/__MACOSX/shot/260118/other/liandong-01.png)  | ![Rule Engine](/__MACOSX/shot/260118/other/liandong-result.png) |
| ![Linkage](/__MACOSX/shot/260118/other/tz01.png)         | ![Rule Engine](/__MACOSX/shot/260118/other/tz02.png)            |

### 🛠️ Network Components

|                                  |                                              |                                           |
|-----------------------------------------------|----------------------------------------------------------|-------------------------------------------------|
| ![tcp](/__MACOSX/shot/260118/network/tcp.png) | ![mqtt detail](/__MACOSX/shot/260118/network/mqtt-detail.png) | ![tcp binding](/__MACOSX/shot/260118/network/tcp.png) |

### 🛠️ Northbound Application Multi-tenant Applications

|                                            |                                             |                                            |
|--------------------------------------------|---------------------------------------------|--------------------------------------------|
| ![AK/SK](/__MACOSX/shot/260118/app/01.png) | ![mqtt detail](/__MACOSX/shot/260118/app/03.png) | ![tcp binding](/__MACOSX/shot/260118/app/02.png) |

### 🎯 Data Analysis and Trends

|                                                      |                                                  |                                                    |
|------------------------------------------------------|--------------------------------------------------|----------------------------------------------------|
| ![Thing Model List Data](/__MACOSX/shot/260109/logmeta-tubiao.png) | ![Show Trend](/__MACOSX/shot/260109/logmeta-qushi.png) | ![Export Thing Model](/__MACOSX/shot/260109/logmeta-export.png) |
| ![Data Management 1](/__MACOSX/shot/260118/platform/dt.png) | ![Data Management 2](/__MACOSX/shot/260118/platform/d2.png)                       | ![Device Management](/__MACOSX/shot/260118/other/zhuapai.png) |

### 🎯 wvp-GB28281-wvp+Hikvision ISC+Dahua ICC

![Video Wall](/__MACOSX/shot/260109/videowall.png)

|                                                 |                                         ||
|-------------------------------------------------|-----------------------------------------|---|
|![Platform Instance](/__MACOSX/shot/260118/platform/hlht.png)  |  ![Recording List](/__MACOSX/shot/260118/platform/lxlb.png)  | ![Device Recording](/__MACOSX/shot/260118/platform/sblx.png)   
|  ![National Standard Real-time](/__MACOSX/shot/260118/platform/gbss.png)            | ![Recording](/__MACOSX/shot/260118/platform/lsbf.png)     |![Device Recording Playback](/__MACOSX/shot/260118/platform/lxhf.png) |
|        |

#### 🌐 China Telecom Product Access

![China Telecom Product Access](__MACOSX/shot/260118/99.gif)

### 🚀 SCADA Dashboard Integrated Integration (Beta)

![SCADA Dashboard 2](__MACOSX/shot/260118/zutai/01.png)

| Dashboard                                          | SCADA                                        | 
|---------------------------------------------|-------------------------------------------|
| ![Dashboard 2](__MACOSX/shot/260118/zutai/99.png)   | ![Dashboard 2](__MACOSX/shot/260118/zutai/04.png) |
| ![SCADA Dashboard 2](__MACOSX/shot/260118/zutai/02.png) | ![Dashboard 2](__MACOSX/shot/260118/zutai/03.png) |
| ![SCADA Dashboard 2](__MACOSX/shot/260118/zutai/05.png) | ![Dashboard 2](__MACOSX/shot/260118/zutai/06.png) |
| ![SCADA Dashboard 2](__MACOSX/shot/260118/zutai/07.png) | ![Dashboard 2](__MACOSX/shot/260118/zutai/08.png) |

### 🎯 Mobile/Mobile Applets

|                                               |                                              |                                              |
|-----------------------------------------------|----------------------------------------------|----------------------------------------------|
| ![Mini Program](/__MACOSX/shot/260118/mp/mp-index.png) | ![app](__MACOSX/shot/260118/mp/app-user.png) | ![app](__MACOSX/shot/260118/mp/app-edit.png) |

|                                    |                                   |                                   | 
|------------------------------------|-----------------------------------|-----------------------------------|
| ![Mini Program](/__MACOSX/shot/260118/mp/00.jpg) | ![app](__MACOSX/shot/260118/mp/01.jpg) | ![app](__MACOSX/shot/260118/mp/02.jpg) |
| ![Mini Program](/__MACOSX/shot/260118/mp/03.jpg) | ![app](__MACOSX/shot/260118/mp/04.jpg) | ![app](__MACOSX/shot/260118/mp/05.jpg) |

> Video [NexIoT Mini Program, This Time It's Strong!] https://www.bilibili.com/video/BV1WMqDB6EAc/?share_source=copy_web&vd_source=c9e1500efcc8aa0763f711fadaa68dff

## 📺 Video Tutorials

More videos, please follow Bilibili and Douyin

### 📚 Basic Introduction Tutorials

| No. | Tutorial Name | Video Link |
|:---:|:---|:---|
| 1 | [NexIoT Course] (1) IDEA and Docker One-click Startup | [📺 Bilibili View](https://www.bilibili.com/video/BV1WNUnBnEx5/?share_source=copy_web&vd_source=c9e1500efcc8aa0763f711fadaa68dff) |
| 2 | [NexIoT Course] (2) EMQX Configuration | [📺 Bilibili View](https://www.bilibili.com/video/BV1MdUJB4E7k/?share_source=copy_web&vd_source=c9e1500efcc8aa0763f711fadaa68dff) |

### 📨 MQTT Access Tutorials

| No. | Tutorial Name | Video Link |
|:---:|:---|:---|
| 1 | Arbitrary Topic and Full Process Integration Tutorial | [📺 Bilibili View](https://www.bilibili.com/video/BV1q1UZBmEHS/?share_source=copy_web&vd_source=c9e1500efcc8aa0763f711fadaa68dff) |

### 📢 Important Notice

- **Self-media Reposting**: Welcome to repost project information, please follow the AGPL3.0 open-source license
- **Enterprise Authorization**: Commercial use requires authorization, enterprise authorization information retained
- **Legal Liability**: If institutional misuse causes legal consequences, liability reserved

### 🌐 Community Contact Methods

|WeChat                            | Bilibili                              | Douyin                               | Mini Program                           |
|-------------------------------|---------------------------------|----------------------------------|-------------------------------|
|  ![WeChat](/__MACOSX/shot/wx.png) | ![Bilibili](/__MACOSX/shot/bzhan.jpg) | ![Douyin](/__MACOSX/shot/douyin.jpg) | ![wx](/__MACOSX/shot/wxq.png) |

## 🛠️ Technical Support and Services

We provide comprehensive technical support services, from basic Q&A to enterprise-level solutions, meeting different levels of needs. Charging is to provide better service and greater motivation for the authors!

### 📋 Service Type Comparison

| Service Type          | Service Content                                 | Price           | Applicable Scenarios      | Contact Method                                                         |
|---------------|--------------------------------------|--------------|-----------|--------------------------------------------------------------|
| **🆓 Community Technical Q&A** | • Basic problem Q&A<br>• Usage guidance<br>• Community exchange         | **Free**       | Learning, Usage     | QQ Group, WeChat Group                                                      |
| **📚 Technical Documentation**   | • Technical documentation<br>• Practice guides<br>• Video tutorials           | **Free**       | Self-learning, Reference     | [Documentation](https://docs.nexiot.cc/) |
| **☁️ Online Deployment Service** | • Customer provides hardware<br>• Complete system deployment<br>• Cloud service deployment      | **¥199**     | Rapid launch, cloud deployment  | Contact Technical                                                         |
| **🔧 Exclusive Technical Support** | • Various integration support<br>• Usage guidance<br>• Deployment solutions<br>• Solutions | **¥600/4 hours** | Remote support      | Contact Technical                                                         |
| **⚡ Device Access Service**  | • IDE source code analysis<br>• Product configuration export<br>• Customized access     | **¥1000/day**  | Complex device access, custom development | Contact Customer Service                                                         |
| **🏢 Enterprise Peace of Mind Service** | • Commercial version local deployment<br>• Proxy O&M service<br>• 5×8 technical support    | **¥9,999/year** | -         | Contact Customer Service                                                         |

---

## 📄 Open Source License and Authorization

Please follow the AGPL3.0 open-source license, commercial use requires authorization

### 📜 Detailed Authorization Terms

#### ✅ Authorized User Rights

1. **Internal Use**: Authorized users may indefinitely use internally
    - Enterprise authorization: Does not include subsidiaries, branch companies and third-party companies
    - Individual authorization: For personal use only, cannot be used for employed company or third party
2. **Development Permissions**: Authorized users may conduct secondary source code development through project form
    - Customized software must be encrypted and packaged before customer delivery
    - If source code delivery required, customers must purchase corresponding commercial authorization

#### ❌ Authorization Restrictions

1. **Prohibition of Transfer**: Cannot distribute or transfer authorized source code to third parties
    - Whether paid or free transfer
    - Cannot apply for software copyright containing this project
2. **Closed-source Content**

 **TCP**, **UDP**, **China Telecom IoT ctaiot** modules. Self-secondary development expansion possible, commercial use requires project sponsorship, authorization required.

### 🙏 Acknowledgments

Thanks to the following open-source projects and technology platforms:

- **Open-source Frameworks**: RuoYi, Antdv, jetlink, ssssssss-team
- **Cloud Platforms**: Alibaba Cloud, Huawei Cloud, Tencent Cloud, AEP, OneNet and other IoT platforms
- **Community Support**: Support and feedback from all contributors and users

### Client Situations

- **City/District Level Smart City IoT Platform**
- **Hong Kong Metro**
- **Listed Companies**
- **Enterprise Parks**
- **Intelligent Projects**
- **Other Industry Clients**
