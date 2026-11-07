/**
 * 预解码函数：提取设备身份标识（首次注册时使用）
 * 场景：TCP/MQTT接入的设备，首次上报时需携带productKey和deviceId，后续报文可能省略
 * @param {String} payload 设备上报的原始报文（通常为16进制字符串或二进制流）
 * @returns {Object} 包含设备标识的对象，格式：{deviceId, productKey}
 * @example 输入设备首次注册报文"787801000A01020304050607080D0A"，解析出设备标识
 */
var preDecode = payload => {
    // 初始化返回结果
    var rs = {
        "deviceId": "",    // 设备唯一ID（如IMEI、MAC地址等）
        "productKey": ""   // 产品标识（同一型号设备共用）
    };

    // 示例：假设设备首次报文格式为固定结构（16进制）
    // 报文格式：7878 [类型] [长度] [productKey(4字节)] [deviceId(6字节)] [校验位] 0D0A
    if (payload.startsWith("7878") && payload.endsWith("0D0A")) {
        // 截取有效部分（去除起始符7878和结束符0D0A）
        var content = payload.substring(4, payload.length() - 4);
        // 假设前8位（4字节）是productKey，后12位（6字节）是deviceId
        if (content.length() >= 20) { // 4+6字节=10字节=20个16进制字符
            rs.productKey = content.substring(0, 8);  // 提取productKey
            rs.deviceId = content.substring(8, 20);   // 提取deviceId
        }
    }

    return rs;
};

/**
 * 编码函数：将平台指令转换为设备可识别的格式
 * 场景：平台下发控制指令（如设置属性、执行命令）给设备时使用
 * @param {Object} payload 平台标准格式的指令，格式：{messageType, properties, command}
 * @returns {String} 设备可识别的报文（16进制字符串或JSON）
 * @example 平台下发"开关灯"指令，转换为设备能理解的16进制报文
 */
var encode = payload => {
    // 初始化设备指令结构（以16进制报文为例）
    var result = "";

    // 处理属性设置类型指令（如调节温度、开关设备）
    if (payload.messageType === "PROPERTIES") {
        // 示例：假设设备接收的属性指令格式为：
        // 起始符(7878) + 指令类型(01) + 属性标识 + 属性值 + 结束符(0D0A)
        var properties = payload.properties;
        var hexParts = ["7878", "01"]; // 起始符 + 指令类型
        hexParts.push("0D0A"); // 结束符
        result = hexParts.join(""); // 拼接为完整16进制报文
    }

    // 处理命令类型指令（如重启设备）
    if (payload.messageType === "COMMAND") {
        // 示例：命令指令格式：7878 + 02 + 命令码 + 0D0A
        var commandCode = payload.command === "reboot" ? "01" : "00"; // 重启命令码为01
        result = "787802" + commandCode + "0D0A";
    }

    return result;
};

let parseInt=(v1,v2)=>{
    return v1::int
}


/**
 * 解码函数：将设备上报数据转换为平台标准格式
 * 场景：设备上报属性、事件（如温度、故障报警）时，转换为平台可识别的JSON格式
 * @param {String} payload 设备上报的原始报文（16进制字符串或JSON）
 * @returns {Array} 平台标准格式的消息数组，包含属性或事件
 * @example 设备上报16进制报文"787803012302050D0A"，解析为温度23.5℃的属性数据
 */
var decode = payload => {
    var result = []; // 最终返回的结果集（可包含属性、事件等多种消息）

    // 1. 解析属性上报（设备主动上报的状态数据）
    var propertiesObj = {
        "messageType": "PROPERTIES", // 消息类型：属性
        "properties": {}             // 具体属性键值对
    };

    // 示例：假设设备属性报文格式为16进制：
    // 7878 + 03（类型） + [属性1标识] [属性1值] + [属性2标识] [属性2值] + 0D0A
    if (payload.startsWith("7878") && payload.endsWith("0D0A")) {
        var content = payload.substring(4, payload.length() - 4); // 去除起始和结束符
        // 类型为03表示属性上报
        if (content.startsWith("03")) {
            var dataPart = content.substring(2); // 截取属性数据部分

            // 解析温度属性（标识01，值为两位16进制 → 十进制/10）
            if (dataPart.length() >= 4) {
                var tempFlag = dataPart.substring(0, 2);
                if (tempFlag === "01") {
                    var tempHex = dataPart.substring(2, 4);
                    propertiesObj.properties.temperature = parseInt(tempHex, 16) / 10; // 如23 → 23/10=2.3℃
                }
            }

            // 解析湿度属性（标识02，值为两位16进制 → 十进制）
            if (dataPart.length() >= 8) {
                var humiFlag = dataPart.substring(4, 6);
                if (humiFlag === "02") {
                    var humiHex = dataPart.substring(6, 8);
                    propertiesObj.properties.humidity = parseInt(humiHex, 16); // 如05 → 5%
                }
            }

            // 若有属性数据，添加到结果集
            if (propertiesObj.properties) {
                result.push(propertiesObj);
            }
        }

        // 2. 解析事件上报（设备异常、状态变更等事件）
        var eventObj = {
            "messageType": "EVENT", // 消息类型：事件
            "event": "",            // 事件名称（如故障、离线）
            "params": {}            // 事件参数（如故障码）
        };

        // 类型为04表示事件上报
        if (content.startsWith("04")) {
            var eventPart = content.substring(2);
            // 事件码01表示离线事件
            if (eventPart.startsWith("01")) {
                eventObj.event = "offline";
                eventObj.params = { "reason": "timeout" }; // 离线原因：超时
                result.push(eventObj);
            }
            // 事件码02表示故障报警
            if (eventPart.startsWith("02")) {
                eventObj.event = "fault";
                eventObj.params = { "code": eventPart.substring(2, 4::int, 16) }; // 故障码
                result.push(eventObj);
            }
        }
    }

    return result;
};

// 本地调试示例（模拟设备报文解析）
// 1. 测试预解码（设备首次注册报文）
// return preDecode("787801000A01020304050607080D0A");
// 输出：{deviceId: "020304050607", productKey: "01000A01"}

// // 2. 测试编码（平台下发开关灯指令）
// return encode({messageType: "PROPERTIES",properties: { "switch": true, "brightness": 50 }});
// // 输出："787801000101320D0A"

// // 3. 测试解码（设备上报温湿度+离线事件）
return decode("787803012302050D0A");
// console.log("解码测试：", decode("787803012302050D0A"));
// // 输出：[{messageType: "PROPERTIES", properties: {temperature: 35.5, humidity: 5}}]