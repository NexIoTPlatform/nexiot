import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import log;

/**
 * ============================================================================
 *  UEZ3/1PN 智能断路器 - MagicScript 解析脚本
 * ============================================================================
 *  适用产品：UEZ3-63 M系列/F系列 (1P/1PN)、UEZ3L-63 M系列/F系列 (1PN)
 *  协议来源：《4-UEZ3通讯规约(1PN)_Cloud.pdf》、《2-Modbus TCP 通信规约_Cloud.pdf》
 *
 *  【协议关键点】
 *  这是宏发网关定制的"改造版 Modbus TCP"，用于 MQTT 主动上报：
 *  MBAP 头 7 字节被重新定义为：
 *    CRC(2字节,低字节在前) + 起始寄存器地址(2字节) + 长度(2字节) + UnitID(1字节)
 *  CRC 计算范围：从第 3 字节（起始寄存器地址）到报文末尾，使用 Modbus CRC16。
 *
 *  寄存器分区（详细见 PDF 点表）：
 *    位标志区      0x0000-0x0008
 *    电参量区      0x0010-0x0029
 *    统计参数区    0x0030-0x003A
 *    故障记录区    0x0040-0x0099 (10 条)
 *    报警记录区    0x00F0-0x0149 (10 条)
 *    系统参数区    0x0150-0x0159
 *    保护参数区    0x0170-0x0190
 *    定时功能区    0x0191-0x0196
 *    工厂参数区    0x01D0-0x01D1
 *
 *  控制命令（写 0x01D0）：
 *    0x0001 合闸 | 0x0003 分闸(不可手动合闸) | 0x0004 分闸
 *    0x0005 恢复默认 | 0x0006 电量清零 | 0x0800 故障记录清除 | 0x8000 远程漏电自检
 * ============================================================================
 */

// ------------------------------ 寄存器地址常量 ------------------------------
var ADDR = {
    // 位标志区
    "protectStatus": 0x0000,
    "alarmFlag": 0x0001,
    "preTripFlag": 0x0002,
    "tripFlag": 0x0003,
    "breakerType": 0x0004,
    "manualAuto": 0x0005,
    "switchCount": 0x0006,
    "switchState": 0x0007,
    "ratedFrameCurrent": 0x0008,
    // 电参量区
    "current": 0x0010,
    "leakageCurrent": 0x0012,
    "voltage": 0x0013,
    "temperature": 0x0015,
    "activePower": 0x0016,
    "activeEnergy": 0x0018,
    "frequency": 0x001A,
    "faultType": 0x001B,
    "powerFactor": 0x001C,
    "switchStateDup": 0x001D,
    "breakerId": 0x001E,
    "lPoleCurrent": 0x0022,
    "lTerminalTemp": 0x0024,
    "nTerminalTemp": 0x0025,
    "tempCrc": 0x0026,
    "tempMonitorSwitch": 0x0027,
    "remainEnergy": 0x0028,
    // 统计参数区
    "totalTripCount": 0x0030,
    "overloadTripCount": 0x0031,
    "shortCircuitTripCount": 0x0032,
    "leakageTripCount": 0x0033,
    "underVoltTripCount": 0x0034,
    "overVoltTripCount": 0x0035,
    "tempTripCount": 0x0036,
    "otherTripCount": 0x0037,
    "remoteTestTripCount": 0x0038,
    "totalAlarmCount": 0x0039,
    "switchCountStat": 0x003A,
    // 系统参数区
    "breakerTime": 0x0150,
    "settingCurrent": 0x0153,
    "settingLeakage": 0x0154,
    "ratedVoltage": 0x0155,
    "commAddr": 0x0156,
    "series": 0x0157,
    "baudRate": 0x0158,
    "parityBit": 0x0159,
    "breakerTypeParam": 0x015B,
    // 保护参数区
    "overloadCoolTime": 0x0170,
    "overloadActionValue": 0x0171,
    "overloadAlarmValue": 0x0172,
    "overloadTimeFactor": 0x0173,
    "overloadMode": 0x0174,
    "underVoltActionValue": 0x0175,
    "underVoltAlarmValue": 0x0176,
    "underVoltTripTime": 0x0177,
    "underVoltMode": 0x0178,
    "overVoltActionValue": 0x0179,
    "overVoltAlarmValue": 0x017A,
    "overVoltTripTime": 0x017B,
    "overVoltMode": 0x017C,
    "leakageActionValue": 0x017D,
    "leakageAlarmValue": 0x017E,
    "leakageType": 0x017F,
    "leakageMode": 0x0180,
    "tempActionValue": 0x0181,
    "tempAlarmValue": 0x0182,
    "tempTripTime": 0x0183,
    "tempMode": 0x0184,
    "instantActionValue": 0x0185,
    "instantMode": 0x0186,
    "overPowerActionValue": 0x0187,
    "overPowerAlarmValue": 0x0188,
    "overPowerTripTime": 0x0189,
    "overPowerMode": 0x018A,
    "leakageRecloseTime": 0x018B,
    "terminalTempActionValue": 0x018D,
    "terminalTempAlarmValue": 0x018E,
    "terminalTempTripTime": 0x018F,
    "terminalTempMode": 0x0190,
    // 工厂参数区
    "controlCmd": 0x01D0,
    "softwareVersion": 0x01D1
};

// ------------------------------ 通用工具函数 ------------------------------

/**
 * 整数转 4 位大写 HEX（2 字节，用于寄存器地址/长度字段）
 */
var toHex4 = (v) => {
    return String.format("%04X", v);
};

/**
 * 整数转 2 位大写 HEX（1 字节，用于 UnitID/FC/ByteCount）
 */
var toHex2 = (v) => {
    return String.format("%02X", v);
};

/**
 * 16 进制字符串（4 位）转整数
 */
var hexToInt = (hex) => {
    return Integer.parseInt(hex, 16);
};

/**
 * 解析 IEEE754 单精度浮点（4 字节大端 HEX）
 * 例: "40C5D2F2" -> 6.182
 */
var parseFloat32 = (hex) => {
    try {
        var intBits = Integer.parseInt(hex, 16);
        return Float.intBitsToFloat(intBits);
    } catch (e) {
        log.warn("parseFloat32 异常：hex【{}】", hex);
        return 0.0;
    }
};

/**
 * 解析有符号 16 位整数（2 字节大端 HEX，温度等可能为负）
 */
var parseInt16Signed = (hex) => {
    var v = Integer.parseInt(hex, 16);
    return v > 32767 ? v - 65536 : v;
};

/**
 * 数字保留指定小数位（避免浮点精度问题）
 */
var round = (value, scale) => {
    return NumberUtil.round(value, scale).doubleValue();
};

/**
 * 报文预处理（提取设备标识）
 * 协议：MBAP 头第 7 字节为 UnitID（从机地址），用作 deviceId 定位设备
 * @param {String} payload - 完整 16 进制报文
 * @returns {Object} 设备基础信息
 */
var preDecode = (payload) => {
    var payloadClean = StrUtil.removeAll(payload, ' ').toUpperCase();
    var unitId = 1;
    try {
        if (payloadClean.length() >= 14) {
            unitId = Integer.parseInt(payloadClean.substring(12, 14), 16);
        }
    } catch (e) {
        log.warn("preDecode 解析 UnitID 异常：{}", e.getMessage());
    }
    return {
        "deviceId": String.format("%d", unitId),
        "productKey": "UEZ3_1PN"
    };
};

// ------------------------------ encode 子函数：报文构建 ------------------------------

/**
 * 构建完整报文（MBAP头 + PDU）
 * 协议：MBAP = CRC(2,低字节在前) + 起始寄存器地址(2) + 长度(2) + UnitID(1)
 *       CRC 计算范围：从起始寄存器地址字段到 PDU 末尾
 * @param {int} slaveAddr - 从机地址
 * @param {int} startReg - 起始寄存器地址（MBAP头字段）
 * @param {String} pduHex - PDU 部分十六进制（FC + 后续数据）
 * @returns {String} 完整报文 16 进制
 */
var buildFrame = (slaveAddr, startReg, pduHex) => {
    var unitIdHex = toHex2(slaveAddr);
    var startRegHex = toHex4(startReg);
    // 长度 = UnitID(1) + PDU字节数
    var pduBytes = pduHex.length() / 2;
    var length = 1 + pduBytes;
    var lengthHex = toHex4(length);
    // 待 CRC 计算部分 = 起始寄存器地址 + 长度 + UnitID + PDU
    var noCrcHex = startRegHex + lengthHex + unitIdHex + pduHex;
    // modbus_crc16 返回低字节在前的 4 位 HEX，直接作为前 2 字节
    var crcHex = modbus_crc16(noCrcHex);
    return crcHex + noCrcHex;
};

/**
 * 构建读寄存器请求（FC=03/04）
 * @param {int} slaveAddr - 从机地址
 * @param {int} fc - 功能码 0x03 或 0x04
 * @param {int} startReg - 起始寄存器地址
 * @param {int} regCount - 寄存器数量
 */
var buildReadFrame = (slaveAddr, fc, startReg, regCount) => {
    var pduHex = toHex2(fc) + toHex4(startReg) + toHex4(regCount);
    return buildFrame(slaveAddr, startReg, pduHex);
};

/**
 * 构建写单寄存器请求（FC=06）
 * @param {int} slaveAddr - 从机地址
 * @param {int} regAddr - 寄存器地址
 * @param {int} value - 写入值（0-65535）
 */
var buildWriteSingleFrame = (slaveAddr, regAddr, value) => {
    var pduHex = "06" + toHex4(regAddr) + toHex4(value);
    return buildFrame(slaveAddr, regAddr, pduHex);
};

/**
 * 构建写多寄存器请求（FC=10）
 * @param {int} slaveAddr - 从机地址
 * @param {int} startReg - 起始寄存器地址
 * @param {Array<int>} values - 寄存器值列表
 */
var buildWriteMultiFrame = (slaveAddr, startReg, values) => {
    var regCount = 0;
    var dataHex = "";
    for (value in values) {
        dataHex = dataHex + toHex4(value);
        regCount = regCount + 1;
    }
    var byteCount = regCount * 2;
    var pduHex = "10" + toHex4(startReg) + toHex4(regCount) + toHex2(byteCount) + dataHex;
    return buildFrame(slaveAddr, startReg, pduHex);
};

/**
 * 时间字符串(yyyy-MM-dd HH:mm:ss) 转 BCD 6 字节 HEX
 * 协议：地址 0x0150(分秒) + 0x0151(日时) + 0x0152(年月)，BCD 码格式
 * 例: 2007-06-05 04:03:02 -> 02 03 04 05 06 07
 */
var timeToBcdHex = (datetime) => {
    // 期望格式 yyyy-MM-dd HH:mm:ss
    var year = datetime.substring(2, 4);   // 取年后两位
    var month = datetime.substring(5, 7);
    var day = datetime.substring(8, 10);
    var hour = datetime.substring(11, 13);
    var minute = datetime.substring(14, 16);
    var second = datetime.substring(17, 19);
    // 0x0150: 分(低)秒(高) -> minute second
    // 0x0151: 日(低)时(高) -> day hour
    // 0x0152: 年(低)月(高) -> year month
    // PDF 示例：02 03 04 05 06 07 表示 2007-6-5 04:03:02
    // 即字节顺序为 分秒日时年月
    return minute + second + day + hour + year + month;
};

// ------------------------------ 核心函数：encode（物模型 → 16 进制报文） ------------------------------

/**
 * UEZ3/1PN encode（物模型 → 改造版 Modbus TCP 报文）
 * @param {String} functionReqJson - 物模型 JSON 字符串（含 function、data）
 * @param {Object} shadow - 设备影子（暂未使用，预留）
 * @returns {String|null} 完整 16 进制报文
 */
var encode = (functionReqJson, shadow) => {
    var functionReq = toJson(functionReqJson);
    var funcId = functionReq.getStr("function");
    var funcData = functionReq.getObj("data", {});
    var slaveAddr = funcData.getInt("slaveAddr") || 1;

    if (slaveAddr < 1 || slaveAddr > 247) {
        var addrErr = `从机地址非法【${slaveAddr}】，应为 1-247`;
        log.error("encode 错误：{}", addrErr);
        throw new Error(addrErr);
    }

    log.info("encode 开始：功能【{}】，从机地址【{}】", funcId, slaveAddr);

    // ============ 读操作（FC=04 读输入寄存器 / FC=03 读保持寄存器）============
    if (funcId === "meterRead") {
        // 读所有电参量：0x0010 起 12 寄存器
        return buildReadFrame(slaveAddr, 0x04, 0x0010, 0x000C);
    }
    if (funcId === "readAllStats") {
        // 读所有统计参数：0x0030 起 8 寄存器
        return buildReadFrame(slaveAddr, 0x04, 0x0030, 0x0008);
    }
    if (funcId === "readCurrent") {
        return buildReadFrame(slaveAddr, 0x04, 0x0010, 0x0002);
    }
    if (funcId === "readLeakage") {
        return buildReadFrame(slaveAddr, 0x04, 0x0012, 0x0001);
    }
    if (funcId === "readVoltage") {
        return buildReadFrame(slaveAddr, 0x04, 0x0013, 0x0002);
    }
    if (funcId === "readTemperature") {
        return buildReadFrame(slaveAddr, 0x04, 0x0015, 0x0001);
    }
    if (funcId === "readPower") {
        return buildReadFrame(slaveAddr, 0x04, 0x0016, 0x0002);
    }
    if (funcId === "readEnergy") {
        return buildReadFrame(slaveAddr, 0x04, 0x0018, 0x0002);
    }
    if (funcId === "readFrequency") {
        return buildReadFrame(slaveAddr, 0x04, 0x001A, 0x0001);
    }
    if (funcId === "readFaultType") {
        return buildReadFrame(slaveAddr, 0x04, 0x001B, 0x0001);
    }
    if (funcId === "readTripCount") {
        // 总跳闸次数：0x0030
        return buildReadFrame(slaveAddr, 0x04, 0x0030, 0x0001);
    }
    if (funcId === "readProtectStatus") {
        // 读保护状态/报警/预跳闸/跳闸：0x0000 起 4 寄存器
        return buildReadFrame(slaveAddr, 0x04, 0x0000, 0x0004);
    }
    if (funcId === "readOverloadParams") {
        // 过载保护参数：0x0170 起 5 寄存器
        return buildReadFrame(slaveAddr, 0x03, 0x0170, 0x0005);
    }
    if (funcId === "readUnderVoltageParams") {
        return buildReadFrame(slaveAddr, 0x03, 0x0175, 0x0004);
    }
    if (funcId === "readOverVoltageParams") {
        return buildReadFrame(slaveAddr, 0x03, 0x0179, 0x0004);
    }
    if (funcId === "readLeakageParams") {
        return buildReadFrame(slaveAddr, 0x03, 0x017D, 0x0004);
    }
    if (funcId === "readTemperatureParams") {
        return buildReadFrame(slaveAddr, 0x03, 0x0181, 0x0004);
    }
    if (funcId === "readInstantParams") {
        return buildReadFrame(slaveAddr, 0x03, 0x0185, 0x0002);
    }
    if (funcId === "readRatedParams") {
        // 整定电流、整定漏电、额定电压：0x0153 起 3 寄存器
        return buildReadFrame(slaveAddr, 0x03, 0x0153, 0x0003);
    }
    if (funcId === "readDeviceTime") {
        // 装置时间：0x0150 起 3 寄存器
        return buildReadFrame(slaveAddr, 0x03, 0x0150, 0x0003);
    }

    // ============ 写操作：控制命令（FC=06 写 0x01D0）============
    if (funcId === "switchOnOff") {
        // 远程合分闸：0x01D0 控制命令
        var valveOn = funcData.getStr("valveOn");
        var cmdValue = 0;
        if (valveOn === "1") {
            cmdValue = 0x0001;  // 合闸
        } else if (valveOn === "3") {
            cmdValue = 0x0003;  // 分闸（不可手动合闸）
        } else if (valveOn === "4") {
            cmdValue = 0x0004;  // 分闸
        } else {
            throw new Error(`switchOnOff 的 valveOn 非法【${valveOn}】，应为 1/3/4`);
        }
        return buildWriteSingleFrame(slaveAddr, 0x01D0, cmdValue);
    }
    if (funcId === "clearFaults") {
        // 清除故障记录：0x01D0 = 0x0800
        return buildWriteSingleFrame(slaveAddr, 0x01D0, 0x0800);
    }
    if (funcId === "clearEnergy") {
        // 电量清零：0x01D0 = 0x0006
        return buildWriteSingleFrame(slaveAddr, 0x01D0, 0x0006);
    }
    if (funcId === "restoreDefault") {
        // 恢复默认：0x01D0 = 0x0005
        return buildWriteSingleFrame(slaveAddr, 0x01D0, 0x0005);
    }
    if (funcId === "remoteLeakageTest") {
        // 远程漏电自检：0x01D0 = 0x8000
        return buildWriteSingleFrame(slaveAddr, 0x01D0, 0x8000);
    }

    // ============ 写操作：保护处理方式（FC=06 写各处理方式寄存器）============
    if (funcId === "setOverloadMode") {
        return buildWriteSingleFrame(slaveAddr, 0x0174, funcData.getInt("mode"));
    }
    if (funcId === "setUnderVoltageMode") {
        return buildWriteSingleFrame(slaveAddr, 0x0178, funcData.getInt("mode"));
    }
    if (funcId === "setOverVoltageMode") {
        return buildWriteSingleFrame(slaveAddr, 0x017C, funcData.getInt("mode"));
    }
    if (funcId === "setLeakageMode") {
        return buildWriteSingleFrame(slaveAddr, 0x0180, funcData.getInt("mode"));
    }
    if (funcId === "setTemperatureMode") {
        return buildWriteSingleFrame(slaveAddr, 0x0184, funcData.getInt("mode"));
    }
    if (funcId === "setInstantMode") {
        return buildWriteSingleFrame(slaveAddr, 0x0186, funcData.getInt("mode"));
    }

    // ============ 写操作：单参数设置（FC=06）============
    if (funcId === "setSettingCurrent") {
        var ir = funcData.getInt("current");
        if (ir < 6 || ir > 100) {
            throw new Error(`整定电流非法【${ir}】，应为 6-100A`);
        }
        return buildWriteSingleFrame(slaveAddr, 0x0153, ir);
    }
    if (funcId === "setRatedLeakage") {
        // 寄存器值 = mA * 100
        var leakageMa = funcData.getInt("leakage");
        return buildWriteSingleFrame(slaveAddr, 0x0154, leakageMa * 100);
    }
    if (funcId === "setRatedVoltage") {
        return buildWriteSingleFrame(slaveAddr, 0x0155, funcData.getInt("voltage"));
    }
    if (funcId === "setCommAddr") {
        var newAddr = funcData.getInt("newAddr");
        if (newAddr < 1 || newAddr > 247) {
            throw new Error(`新通信地址非法【${newAddr}】，应为 1-247`);
        }
        return buildWriteSingleFrame(slaveAddr, 0x0156, newAddr);
    }

    // ============ 写操作：装置校时（FC=06 写 0x0150-0x0152 BCD 时间）============
    if (funcId === "syncTime") {
        var datetime = funcData.getStr("datetime");
        if (!datetime || datetime.length() < 19) {
            throw new Error("syncTime 需传入 datetime，格式 yyyy-MM-dd HH:mm:ss");
        }
        var bcdHex = timeToBcdHex(datetime);
        // 写 3 个寄存器：分秒、日时、年月
        return buildWriteMultiFrame(slaveAddr, 0x0150, [
            Integer.parseInt(bcdHex.substring(0, 4), 16),
            Integer.parseInt(bcdHex.substring(4, 8), 16),
            Integer.parseInt(bcdHex.substring(8, 12), 16)
        ]);
    }

    // ============ 写操作：过载保护参数批量设置（FC=10 写 0x0170-0x0174）============
    if (funcId === "setOverloadParams") {
        return buildWriteMultiFrame(slaveAddr, 0x0170, [
            funcData.getInt("coolTime"),
            funcData.getInt("actionValue"),
            funcData.getInt("alarmValue"),
            funcData.getInt("timeFactor"),
            funcData.getInt("mode")
        ]);
    }

    // ============ 指令透传 ============
    if (funcId === "cmd") {
        var hex = funcData.getStr("hex");
        if (!hex) {
            throw new Error("cmd 透传需传入 hex");
        }
        return StrUtil.removeAll(hex, ' ').toUpperCase();
    }

    // ============ 未知功能 ============
    log.error("encode 错误：不支持的功能 ID【{}】", funcId);
    return null;
};

// ------------------------------ decode 子函数：数据解析 ------------------------------

/**
 * 解析报警标志位（0x0001，PDF 备注2）为事件列表
 * 位定义：bit0 过载 / bit2 漏电 / bit3 欠压 / bit4 过压 / bit5 温度 / bit6 外部 / bit7 过功率
 *         bit8 L端子温度 / bit9 N端子温度 / bit10 剩余电量不足 / bit11 失压
 * 注意：采用逐位 if 判断，避免 map 数字 key / list literal length 等兼容性问题
 */
var decodeAlarmFlags = (flagValue) => {
    var events = [];
    if ((flagValue & 0x0001) !== 0) { events.push({ "id": "overloadAlarm", "name": "overloadAlarm", "level": "warn" }); }
    if ((flagValue & 0x0004) !== 0) { events.push({ "id": "leakageAlarm", "name": "leakageAlarm", "level": "warn" }); }
    if ((flagValue & 0x0008) !== 0) { events.push({ "id": "underVoltageAlarm", "name": "underVoltageAlarm", "level": "warn" }); }
    if ((flagValue & 0x0010) !== 0) { events.push({ "id": "overVoltageAlarm", "name": "overVoltageAlarm", "level": "warn" }); }
    if ((flagValue & 0x0020) !== 0) { events.push({ "id": "temperatureAlarm", "name": "temperatureAlarm", "level": "warn" }); }
    if ((flagValue & 0x0040) !== 0) { events.push({ "id": "externalAlarm", "name": "externalAlarm", "level": "warn" }); }
    if ((flagValue & 0x0080) !== 0) { events.push({ "id": "overPowerAlarm", "name": "overPowerAlarm", "level": "warn" }); }
    if ((flagValue & 0x0100) !== 0) { events.push({ "id": "lTerminalTempAlarm", "name": "lTerminalTempAlarm", "level": "warn" }); }
    if ((flagValue & 0x0200) !== 0) { events.push({ "id": "nTerminalTempAlarm", "name": "nTerminalTempAlarm", "level": "warn" }); }
    if ((flagValue & 0x0400) !== 0) { events.push({ "id": "lowBalanceAlarm", "name": "lowBalanceAlarm", "level": "warn" }); }
    if ((flagValue & 0x0800) !== 0) { events.push({ "id": "lossVoltageAlarm", "name": "lossVoltageAlarm", "level": "warn" }); }
    return events;
};

/**
 * 解析跳闸标志位（0x0003，PDF 备注4）为事件列表
 * 位定义：bit0 过载 / bit1 瞬时 / bit2 漏电 / bit3 欠压 / bit4 过压 / bit5 高过压
 *         bit6 温度 / bit7 外部 / bit8 自检标志(3=远程自检) / bit10 过功率
 *         bit11 L端子温度 / bit12 N端子温度 / bit13 剩余电量不足
 */
var decodeTripFlags = (flagValue) => {
    var events = [];
    if ((flagValue & 0x0001) !== 0) { events.push({ "id": "overloadTrip", "name": "overloadTrip", "level": "urgent" }); }
    if ((flagValue & 0x0002) !== 0) { events.push({ "id": "instantTrip", "name": "instantTrip", "level": "urgent" }); }
    if ((flagValue & 0x0004) !== 0) { events.push({ "id": "leakageTrip", "name": "leakageTrip", "level": "urgent" }); }
    if ((flagValue & 0x0008) !== 0) { events.push({ "id": "underVoltageTrip", "name": "underVoltageTrip", "level": "urgent" }); }
    if ((flagValue & 0x0010) !== 0) { events.push({ "id": "overVoltageTrip", "name": "overVoltageTrip", "level": "urgent" }); }
    if ((flagValue & 0x0020) !== 0) { events.push({ "id": "highOverVoltageTrip", "name": "highOverVoltageTrip", "level": "urgent" }); }
    if ((flagValue & 0x0040) !== 0) { events.push({ "id": "temperatureTrip", "name": "temperatureTrip", "level": "urgent" }); }
    if ((flagValue & 0x0080) !== 0) { events.push({ "id": "externalTrip", "name": "externalTrip", "level": "urgent" }); }
    if ((flagValue & 0x0400) !== 0) { events.push({ "id": "overPowerTrip", "name": "overPowerTrip", "level": "urgent" }); }
    if ((flagValue & 0x0800) !== 0) { events.push({ "id": "lTerminalTempTrip", "name": "lTerminalTempTrip", "level": "urgent" }); }
    if ((flagValue & 0x1000) !== 0) { events.push({ "id": "nTerminalTempTrip", "name": "nTerminalTempTrip", "level": "urgent" }); }
    if ((flagValue & 0x2000) !== 0) { events.push({ "id": "lowBalanceTrip", "name": "lowBalanceTrip", "level": "urgent" }); }
    // bit8-9 自检标志：3=远程自检
    if (((flagValue >> 8) & 0x03) === 3) {
        events.push({ "id": "remoteSelfTestTrip", "name": "远程自检跳闸", "level": "info" });
    }
    return events;
};

/**
 * 解析电参量区数据（0x0010 起 12 寄存器 = 24 字节）
 * 数据布局（来自 PDF 示例）：电流(浮点) + 漏电(整型) + 电压(浮点) + 温度(整型)
 *   + 有功功率(浮点) + 有功能量(浮点) + 频率(整型) + 故障类型(整型)
 */
var decodeElectricParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 48) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `电参量数据长度不足，需 24 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    // 电流 0x0010-0x0011 (浮点)
    props.current = round(parseFloat32(dataHex.substring(0, 8)), 3);
    // 漏电电流 0x0012 (整型 /100 mA)
    props.leakageCurrent = round(hexToInt(dataHex.substring(8, 12)) / 100.0, 2);
    // 电压 0x0013-0x0014 (浮点)
    props.voltage = round(parseFloat32(dataHex.substring(12, 20)), 2);
    // 温度 0x0015 (有符号整型 ℃)
    props.temperature = parseInt16Signed(dataHex.substring(20, 24));
    // 有功功率 0x0016-0x0017 (浮点 W)
    props.activePower = round(parseFloat32(dataHex.substring(24, 32)), 2);
    // 有功能量 0x0018-0x0019 (浮点 kWh)
    props.activeEnergy = round(parseFloat32(dataHex.substring(32, 40)), 4);
    // 频率 0x001A (整型 /100 Hz)
    props.frequency = round(hexToInt(dataHex.substring(40, 44)) / 100.0, 2);
    // 故障类型 0x001B (整型 位域)
    props.faultType = hexToInt(dataHex.substring(44, 48));
};

/**
 * 解析统计参数区数据（0x0030 起 8 寄存器 = 16 字节）
 * 数据布局：总跳闸/过载/短路/漏电/欠压/过压/温度/其它 跳闸次数
 */
var decodeStatsParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 32) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `统计数据长度不足，需 16 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    props.totalTripCount = hexToInt(dataHex.substring(0, 4));
    props.overloadTripCount = hexToInt(dataHex.substring(4, 8));
    props.shortCircuitTripCount = hexToInt(dataHex.substring(8, 12));
    props.leakageTripCount = hexToInt(dataHex.substring(12, 16));
    props.underVoltTripCount = hexToInt(dataHex.substring(16, 20));
    props.overVoltTripCount = hexToInt(dataHex.substring(20, 24));
    props.tempTripCount = hexToInt(dataHex.substring(24, 28));
    props.otherTripCount = hexToInt(dataHex.substring(28, 32));
};

/**
 * 解析保护状态位（0x0000 起 4 寄存器 = 8 字节）
 * 数据布局：保护状态位 + 报警标志位 + 预跳闸标志位 + 跳闸标志位
 */
var decodeProtectStatus = (dataHex, props, dataInfo, events) => {
    if (dataHex.length() < 16) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `保护状态数据长度不足，需 8 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    props.protectStatus = hexToInt(dataHex.substring(0, 4));
    var alarmFlag = hexToInt(dataHex.substring(4, 8));
    props.alarmFlag = alarmFlag;
    props.preTripFlag = hexToInt(dataHex.substring(8, 12));
    var tripFlag = hexToInt(dataHex.substring(12, 16));
    props.tripFlag = tripFlag;
    // 触发报警事件
    var alarmEvents = decodeAlarmFlags(alarmFlag);
    for (event in alarmEvents) {
        events.push(event);
    }
    // 触发跳闸事件
    var tripEvents = decodeTripFlags(tripFlag);
    for (event in tripEvents) {
        events.push(event);
    }
};

/**
 * 解析系统参数区（0x0153 起 3 寄存器 = 6 字节）
 * 数据布局：整定电流 Ir + 整定漏电 IΔn(/100) + 额定电压
 */
var decodeRatedParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 12) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `额定参数数据长度不足，需 6 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    props.settingCurrent = hexToInt(dataHex.substring(0, 4));
    props.settingLeakage = hexToInt(dataHex.substring(4, 8)) / 100;
    props.ratedVoltage = hexToInt(dataHex.substring(8, 12));
};

/**
 * 解析过载保护参数（0x0170 起 5 寄存器 = 10 字节）
 * 数据布局：冷却时间(Y*0.3) + 动作值 + 报警值 + 时间系数(Y*0.3) + 处理方式
 */
var decodeOverloadParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 20) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `过载参数数据长度不足，需 10 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    props.overloadCoolTime = round(hexToInt(dataHex.substring(0, 4)) * 0.3, 1);
    props.overloadActionValue = hexToInt(dataHex.substring(4, 8));
    props.overloadAlarmValue = hexToInt(dataHex.substring(8, 12));
    props.overloadTimeFactor = round(hexToInt(dataHex.substring(12, 16)) * 0.3, 1);
    props.overloadMode = hexToInt(dataHex.substring(16, 20));
};

/**
 * 解析欠压保护参数（0x0175 起 4 寄存器 = 8 字节）
 */
var decodeUnderVoltageParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 16) {
        dataInfo.parseStatus = "fail";
        return;
    }
    props.underVoltActionValue = hexToInt(dataHex.substring(0, 4));
    props.underVoltAlarmValue = hexToInt(dataHex.substring(4, 8));
    props.underVoltTripTime = round(hexToInt(dataHex.substring(8, 12)) * 0.3, 1);
    props.underVoltMode = hexToInt(dataHex.substring(12, 16));
};

/**
 * 解析过压保护参数（0x0179 起 4 寄存器 = 8 字节）
 */
var decodeOverVoltageParams = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 16) {
        dataInfo.parseStatus = "fail";
        return;
    }
    props.overVoltActionValue = hexToInt(dataHex.substring(0, 4));
    props.overVoltAlarmValue = hexToInt(dataHex.substring(4, 8));
    props.overVoltTripTime = round(hexToInt(dataHex.substring(8, 12)) * 0.3, 1);
    props.overVoltMode = hexToInt(dataHex.substring(12, 16));
};

/**
 * 解析电参量区扩展数据（0x0016 起 22 寄存器 = 44 字节，宏发网关主动上报常用范围）
 * 数据布局：有功功率(浮点) + 有功能量(浮点) + 频率(整型) + 故障类型(整型)
 *           + 功率因素(整型) + 开关状态(整型) + 断路器ID(8字节) + L极电流(浮点)
 *           + L端子温度(整型) + N端子温度(整型) + 温度CRC(整型) + 端子温度监测开关(整型)
 *           + 剩余电能(浮点) + 保留(2寄存器)
 */
var decodeElectricExtended = (dataHex, props, dataInfo, events) => {
    if (dataHex.length() < 88) {
        dataInfo.parseStatus = "fail";
        dataInfo.error = `扩展电参量数据长度不足，需 44 字节，实际 ${dataHex.length() / 2} 字节`;
        return;
    }
    var offset = 0;
    // 0x0016-0x0017 有功功率 (浮点 W)
    props.activePower = round(parseFloat32(dataHex.substring(offset, offset + 8)), 2); offset += 8;
    // 0x0018-0x0019 有功能量 (浮点 kWh)
    props.activeEnergy = round(parseFloat32(dataHex.substring(offset, offset + 8)), 4); offset += 8;
    // 0x001A 频率 (整型 /100 Hz)
    props.frequency = round(hexToInt(dataHex.substring(offset, offset + 4)) / 100.0, 2); offset += 4;
    // 0x001B 故障类型 (整型 位域)
    var faultType = hexToInt(dataHex.substring(offset, offset + 4)); offset += 4;
    props.faultType = faultType;
    // 故障类型位域触发跳闸事件
    var tripEvents = decodeTripFlags(faultType);
    for (event in tripEvents) {
        events.push(event);
    }
    // 0x001C 功率因素 (整型 /1000)
    props.powerFactor = round(hexToInt(dataHex.substring(offset, offset + 4)) / 1000.0, 3); offset += 4;
    // 0x001D 开关状态 (整型 0/1/2)
    props.switchState = hexToInt(dataHex.substring(offset, offset + 4)); offset += 4;
    // 0x001E-0x0021 断路器 ID 号 (8 字节)
    props.breakerId = dataHex.substring(offset, offset + 16); offset += 16;
    // 0x0022-0x0023 L 极电流 (浮点 A)
    props.lPoleCurrent = round(parseFloat32(dataHex.substring(offset, offset + 8)), 3); offset += 8;
    // 0x0024 L 端子温度 (有符号整型 ℃)
    props.lTerminalTemp = parseInt16Signed(dataHex.substring(offset, offset + 4)); offset += 4;
    // 0x0025 N 端子温度 (有符号整型 ℃)
    props.nTerminalTemp = parseInt16Signed(dataHex.substring(offset, offset + 4)); offset += 4;
    // 0x0026 温度 CRC 校验
    props.tempCrc = hexToInt(dataHex.substring(offset, offset + 4)); offset += 4;
    // 0x0027 端子温度监测开关 (1=开启, 非1=关闭)
    props.tempMonitorSwitch = hexToInt(dataHex.substring(offset, offset + 4)); offset += 4;
    // 0x0028-0x0029 剩余电能 (浮点 kWh)
    props.remainEnergy = round(parseFloat32(dataHex.substring(offset, offset + 8)), 2); offset += 8;
    // 0x002A-0x002B 保留
};

/**
 * 解析装置时间（0x0150 起 3 寄存器 = 6 字节，BCD 码格式）
 * 字节顺序：分秒 日时 年月
 * 示例：02 03 04 05 06 07 -> 2007-06-05 04:03:02
 */
var decodeDeviceTime = (dataHex, props, dataInfo) => {
    if (dataHex.length() < 12) {
        dataInfo.parseStatus = "fail";
        return;
    }
    var minute = dataHex.substring(0, 2);
    var second = dataHex.substring(2, 4);
    var day = dataHex.substring(4, 6);
    var hour = dataHex.substring(6, 8);
    var year = dataHex.substring(8, 10);
    var month = dataHex.substring(10, 12);
    // 补全年份（假设 20xx 年）
    props.breakerTime = `20${year}-${month}-${day} ${hour}:${minute}:${second}`;
};

// ------------------------------ 核心函数：decode（16 进制报文 → 物模型） ------------------------------

/**
 * UEZ3/1PN decode（改造版 Modbus TCP 报文 → 物模型）
 * @param {String} payload - 完整 16 进制报文（设备上报原始报文）
 * @param {Object} context - 上下文（预留）
 * @returns {Array} 解析结果（含 PROPERTIES 对象，data 字段存指令标识，properties 存设备属性）
 */
var decode = (payload, context) => {
    var result = [];
    var payloadClean = StrUtil.removeAll(payload, ' ').toUpperCase();

    var properties = {
        "messageType": "PROPERTIES",
        "properties": {
            "sourcePayload": payload
        },
        "data": {
            "functionId": "",
            "functionName": "",
            "replyFunction": "",
            "parseStatus": "success"
        }
    };

    // 协议要求最小长度：MBAP(7) + FC(1) + ByteCount(1) = 9 字节 = 18 字符
    if (payloadClean.length() < 18) {
        properties.data.parseStatus = "fail";
        properties.data.functionName = "报文长度不足";
        properties.data.error = `报文最小需 9 字节(18位HEX)，当前 ${payloadClean.length() / 2} 字节`;
        result.push(properties);
        return result;
    }

    // 提取 MBAP 头字段
    var crcInPacket = payloadClean.substring(0, 4);              // CRC 低字节在前
    var startReg = hexToInt(payloadClean.substring(4, 8));       // 起始寄存器地址
    var length = hexToInt(payloadClean.substring(8, 12));        // 长度
    var unitId = hexToInt(payloadClean.substring(12, 14));       // UnitID

    properties.data.unitId = unitId;
    properties.data.startReg = startReg;
    properties.properties.unitId = unitId;

    // CRC 校验：计算从第 3 字节（起始寄存器地址）到末尾
    var noCrcHex = payloadClean.substring(4);
    var calcCrc = modbus_crc16(noCrcHex);
    if (calcCrc.toUpperCase() !== crcInPacket.toUpperCase()) {
        properties.data.parseStatus = "fail";
        properties.data.functionName = "CRC 校验失败";
        properties.data.error = `期望 CRC【${crcInPacket}】，计算 CRC【${calcCrc}】`;
        result.push(properties);
        return result;
    }

    // 提取 PDU
    var fc = hexToInt(payloadClean.substring(14, 16));
    properties.data.funcCode = fc;

    // ================= FC=03/04 读寄存器响应 =================
    if (fc === 0x03 || fc === 0x04) {
        if (payloadClean.length() < 20) {
            properties.data.parseStatus = "fail";
            properties.data.functionName = "读响应长度不足";
            result.push(properties);
            return result;
        }
        var byteCount = hexToInt(payloadClean.substring(16, 18));
        var dataHex = payloadClean.substring(18, 18 + byteCount * 2);

        // 按起始寄存器地址分支解析
        if (startReg === 0x0000) {
            // 保护状态位（4 寄存器）
            properties.data.replyFunction = "readProtectStatus";
            properties.data.functionName = "断路器保护状态响应";
            var events = [];
            properties.properties.events = events;
            decodeProtectStatus(dataHex, properties.properties, properties.data, events);
        }
        else if (startReg === 0x0010) {
            // 电参量区（12 寄存器）
            properties.data.replyFunction = "meterRead";
            properties.data.functionName = "读所有电参量响应";
            decodeElectricParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0016) {
            // 电参量区扩展（22 寄存器，宏发网关主动上报常用范围：0x0016-0x002B）
            properties.data.replyFunction = "reportElectricExtended";
            properties.data.functionName = "电参量扩展上报";
            var extEvents = [];
            properties.properties.events = extEvents;
            decodeElectricExtended(dataHex, properties.properties, properties.data, extEvents);
        }
        else if (startReg === 0x0012) {
            // 漏电电流（1 寄存器）
            properties.data.replyFunction = "readLeakage";
            properties.data.functionName = "读漏电电流响应";
            properties.properties.leakageCurrent = round(hexToInt(dataHex.substring(0, 4)) / 100.0, 2);
        }
        else if (startReg === 0x0015) {
            // 温度（1 寄存器）
            properties.data.replyFunction = "readTemperature";
            properties.data.functionName = "读温度响应";
            properties.properties.temperature = parseInt16Signed(dataHex.substring(0, 4));
        }
        else if (startReg === 0x0016) {
            // 有功功率（2 寄存器）
            properties.data.replyFunction = "readPower";
            properties.data.functionName = "读有功功率响应";
            properties.properties.activePower = round(parseFloat32(dataHex.substring(0, 8)), 2);
        }
        else if (startReg === 0x0018) {
            // 有功能量（2 寄存器）
            properties.data.replyFunction = "readEnergy";
            properties.data.functionName = "读有功能量响应";
            properties.properties.activeEnergy = round(parseFloat32(dataHex.substring(0, 8)), 4);
        }
        else if (startReg === 0x001A) {
            // 频率（1 寄存器）
            properties.data.replyFunction = "readFrequency";
            properties.data.functionName = "读频率响应";
            properties.properties.frequency = round(hexToInt(dataHex.substring(0, 4)) / 100.0, 2);
        }
        else if (startReg === 0x001B) {
            // 故障类型（1 寄存器）
            properties.data.replyFunction = "readFaultType";
            properties.data.functionName = "读故障类型响应";
            properties.properties.faultType = hexToInt(dataHex.substring(0, 4));
        }
        else if (startReg === 0x0030) {
            // 统计参数区（8 寄存器）
            properties.data.replyFunction = "readAllStats";
            properties.data.functionName = "读所有统计参数响应";
            decodeStatsParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0150) {
            // 装置时间（3 寄存器）
            properties.data.replyFunction = "readDeviceTime";
            properties.data.functionName = "读装置时间响应";
            decodeDeviceTime(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0153) {
            // 额定参数（3 寄存器）
            properties.data.replyFunction = "readRatedParams";
            properties.data.functionName = "读额定参数响应";
            decodeRatedParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0170) {
            // 过载保护参数（5 寄存器）
            properties.data.replyFunction = "readOverloadParams";
            properties.data.functionName = "读过载保护参数响应";
            decodeOverloadParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0175) {
            // 欠压保护参数（4 寄存器）
            properties.data.replyFunction = "readUnderVoltageParams";
            properties.data.functionName = "读欠压保护参数响应";
            decodeUnderVoltageParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x0179) {
            // 过压保护参数（4 寄存器）
            properties.data.replyFunction = "readOverVoltageParams";
            properties.data.functionName = "读过压保护参数响应";
            decodeOverVoltageParams(dataHex, properties.properties, properties.data);
        }
        else if (startReg === 0x01D1) {
            // 软件版本（1 寄存器）
            properties.data.replyFunction = "readSoftwareVersion";
            properties.data.functionName = "读软件版本响应";
            var verRaw = hexToInt(dataHex.substring(0, 4));
            properties.properties.softwareVersion = `${verRaw / 10}`;
        }
        else {
            // 通用读响应：尝试根据数据长度解析常见字段
            properties.data.replyFunction = "readGeneric";
            properties.data.functionName = `通用读响应(起始:0x${toHex4(startReg)})`;
            properties.data.regCount = byteCount / 2;
            properties.data.regData = dataHex;
            log.info("decode 通用读响应：起始地址【0x{}】，寄存器数【{}】", toHex4(startReg), byteCount / 2);
        }
    }

    // ================= FC=06 写单寄存器响应 =================
    else if (fc === 0x06) {
        if (payloadClean.length() < 24) {
            properties.data.parseStatus = "fail";
            properties.data.functionName = "写响应长度不足";
            result.push(properties);
            return result;
        }
        var regAddr = hexToInt(payloadClean.substring(16, 20));
        var regValue = hexToInt(payloadClean.substring(20, 24));
        properties.data.replyFunction = "writeSingleReg";
        properties.data.functionName = "写单寄存器响应";
        properties.data.regAddr = regAddr;
        properties.data.regValue = regValue;
        properties.properties.operationResult = "成功";

        // 按寄存器地址细化指令标识
        if (regAddr === 0x01D0) {
            // 控制命令响应
            if (regValue === 0x0001) {
                properties.data.replyFunction = "switchOnOff";
                properties.data.functionName = "远程合闸响应";
                properties.properties.switchState = 1;
            } else if (regValue === 0x0003 || regValue === 0x0004) {
                properties.data.replyFunction = "switchOnOff";
                properties.data.functionName = "远程分闸响应";
                properties.properties.switchState = 0;
            } else if (regValue === 0x0005) {
                properties.data.replyFunction = "restoreDefault";
                properties.data.functionName = "恢复默认响应";
            } else if (regValue === 0x0006) {
                properties.data.replyFunction = "clearEnergy";
                properties.data.functionName = "电量清零响应";
            } else if (regValue === 0x0800) {
                properties.data.replyFunction = "clearFaults";
                properties.data.functionName = "清除故障记录响应";
            } else if (regValue === 0x8000) {
                properties.data.replyFunction = "remoteLeakageTest";
                properties.data.functionName = "远程漏电自检响应";
            }
        } else if (regAddr === 0x0174) {
            properties.data.replyFunction = "setOverloadMode";
            properties.data.functionName = "设置过载保护处理方式响应";
            properties.properties.overloadMode = regValue;
        } else if (regAddr === 0x0178) {
            properties.data.replyFunction = "setUnderVoltageMode";
            properties.data.functionName = "设置欠压保护处理方式响应";
            properties.properties.underVoltMode = regValue;
        } else if (regAddr === 0x017C) {
            properties.data.replyFunction = "setOverVoltageMode";
            properties.data.functionName = "设置过压保护处理方式响应";
            properties.properties.overVoltMode = regValue;
        } else if (regAddr === 0x0180) {
            properties.data.replyFunction = "setLeakageMode";
            properties.data.functionName = "设置漏电保护处理方式响应";
            properties.properties.leakageMode = regValue;
        } else if (regAddr === 0x0184) {
            properties.data.replyFunction = "setTemperatureMode";
            properties.data.functionName = "设置温度保护处理方式响应";
            properties.properties.tempMode = regValue;
        } else if (regAddr === 0x0186) {
            properties.data.replyFunction = "setInstantMode";
            properties.data.functionName = "设置瞬时保护处理方式响应";
            properties.properties.instantMode = regValue;
        } else if (regAddr === 0x0153) {
            properties.data.replyFunction = "setSettingCurrent";
            properties.data.functionName = "设置整定电流响应";
            properties.properties.settingCurrent = regValue;
        } else if (regAddr === 0x0156) {
            properties.data.replyFunction = "setCommAddr";
            properties.data.functionName = "修改通信地址响应";
            properties.properties.commAddr = regValue;
        }
    }

    // ================= FC=10 写多寄存器响应 =================
    else if (fc === 0x10) {
        if (payloadClean.length() < 24) {
            properties.data.parseStatus = "fail";
            properties.data.functionName = "写多寄存器响应长度不足";
            result.push(properties);
            return result;
        }
        var startAddr10 = hexToInt(payloadClean.substring(16, 20));
        var regCount10 = hexToInt(payloadClean.substring(20, 24));
        properties.data.replyFunction = "writeMultiReg";
        properties.data.functionName = "写多寄存器响应";
        properties.data.regAddr = startAddr10;
        properties.data.regCount = regCount10;
        properties.properties.operationResult = "成功";

        if (startAddr10 === 0x0150) {
            properties.data.replyFunction = "syncTime";
            properties.data.functionName = "装置校时响应";
        } else if (startAddr10 === 0x0170) {
            properties.data.replyFunction = "setOverloadParams";
            properties.data.functionName = "设置过载保护参数响应";
        }
    }

    // ================= 异常响应（功能码最高位置 1）=================
    else if ((fc & 0x80) !== 0) {
        var originalFc = fc & 0x7F;
        var exceptionCode = hexToInt(payloadClean.substring(16, 18));
        properties.data.parseStatus = "fail";
        properties.data.replyFunction = "exceptionResponse";
        properties.data.functionName = `异常响应(原FC=0x${toHex2(originalFc)})`;
        properties.data.exceptionCode = exceptionCode;
        var exceptionMsg = "未知异常";
        if (exceptionCode === 1) {
            exceptionMsg = "非法功能码";
        } else if (exceptionCode === 2) {
            exceptionMsg = "非法数据地址";
        } else if (exceptionCode === 3) {
            exceptionMsg = "非法数据值";
        }
        properties.data.exceptionMsg = exceptionMsg;
        properties.properties.operationResult = `失败：${exceptionMsg}`;
    }

    // ================= 未知功能码 =================
    else {
        properties.data.parseStatus = "fail";
        properties.data.replyFunction = "unknownFuncCode";
        properties.data.functionName = `未知功能码 0x${toHex2(fc)}`;
    }

    result.push(properties);
    log.info("decode 完成：状态【{}】，功能【{}】", properties.data.parseStatus, properties.data.replyFunction);
    return result;
};
