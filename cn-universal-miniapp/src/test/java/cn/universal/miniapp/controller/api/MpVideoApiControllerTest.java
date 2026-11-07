/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 小程序视频API单元测试
 *
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.miniapp.controller.api;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** MpVideoApiController 单元测试 */
@DisplayName("小程序视频API控制器测试")
@ExtendWith(MockitoExtension.class)
class MpVideoApiControllerTest {

  @InjectMocks private MpVideoApiController controller;

  @BeforeEach
  void setUp() {
    // 设置代理配置
    ReflectionTestUtils.setField(controller, "videoProxyEnabled", true);
    ReflectionTestUtils.setField(controller, "videoProxyDomain", "wxapi.nexiot.cc");
    ReflectionTestUtils.setField(controller, "rtmpProxyPort", 1935);
    ReflectionTestUtils.setField(controller, "rtspProxyPort", 554);
    ReflectionTestUtils.setField(controller, "wsProxyPort", 31080);
    ReflectionTestUtils.setField(controller, "wssProxyPort", 30443);
  }

  @Test
  @DisplayName("HTTPS FLV 地址转换")
  void all() throws Exception {
    String originalUrl =
"""
{
   "flv": "http://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push",
   "fmp4": "http://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push",
   "hls": "http://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push",
   "https_flv": "https://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push",
   "https_fmp4": "https://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push",
   "https_hls": "https://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push",
   "https_ts": "https://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002.live.ts?originTypeStr=rtp_push",
   "rtc": "http://114.66.63.124:31080/index/api/webrtc?app=rtp&stream=41010500002000000001_41020000001310000002&type=play&originTypeStr=rtp_push",
   "rtcs": "https://114.66.63.124:30443/index/api/webrtc?app=rtp&stream=41010500002000000001_41020000001310000002&type=play&originTypeStr=rtp_push",
   "rtmp": "rtmp://114.66.63.124:1935/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push",
   "rtsp": "rtsp://114.66.63.124:554/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push",
   "ts": "http://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.ts?originTypeStr=rtp_push",
   "ws_flv": "ws://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push",
   "ws_fmp4": "ws://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push",
   "ws_hls": "ws://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push",
   "ws_ts": "ws://114.66.63.124:31080/rtp/41010500002000000001_41020000001310000002.live.ts?originTypeStr=rtp_push",
   "wss_flv": "wss://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push",
   "wss_fmp4": "wss://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push",
   "wss_hls": "wss://114.66.63.124:30443/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push"
}
""";
    JSONObject json = JSONUtil.parseObj(originalUrl);
    Map<String, String> map = ((JSONObject) json).toBean(HashMap.class);
    map.keySet()
        .forEach(
            k -> {
              try {
                String result = invokeConvertSingleUrl(map.get(k));
                System.out.println(result);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  // ==================== HTTPS 协议测试 ====================

  @Test
  @DisplayName("HTTPS FLV 地址转换")
  void testConvertHttpsFlvUrl() throws Exception {
    String originalUrl =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push";
    String expected =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("HTTPS HLS 地址转换")
  void testConvertHttpsHlsUrl() throws Exception {
    String originalUrl =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push";
    String expected =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("HTTPS FMP4 地址转换")
  void testConvertHttpsFmp4Url() throws Exception {
    String originalUrl =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push";
    String expected =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
  }

  // ==================== RTMP 协议测试 ====================

  @Test
  @DisplayName("RTMP 地址端口不带")
  void testConvertRtmpUrlWithPort() throws Exception {
    String originalUrl =
        "rtmp://wxapi.nexiot.cc:1935/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push";
    String expected =
        "rtmp://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
    // 不带端口号
    assertFalse(result.contains(":1935"));
  }

  @Test
  @DisplayName("RTMP 地址去除原始端口，由8端改换")
  void testConvertRtmpUrlRemoveOriginalPort() throws Exception {
    String originalUrl = "rtmp://original.server.com:5000/rtp/stream123";
    String result = invokeConvertSingleUrl(originalUrl);

    // 不带端口号，由nginx代理
    assertTrue(result.startsWith("rtmp://wxapi.nexiot.cc"));
    assertFalse(result.contains(":5000"));
    assertFalse(result.contains(":1935"));
  }

  // ==================== RTSP 协议测试 ====================

  @Test
  @DisplayName("RTSP 地址端口不带")
  void testConvertRtspUrlWithPort() throws Exception {
    String originalUrl =
        "rtsp://wxapi.nexiot.cc:554/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push";
    String expected =
        "rtsp://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
    assertFalse(result.contains(":554"));
  }

  @Test
  @DisplayName("RTSP 地址去除原始端口")
  void testConvertRtspUrlRemoveOriginalPort() throws Exception {
    String originalUrl = "rtsp://original.server.com:8554/rtp/stream456";
    String result = invokeConvertSingleUrl(originalUrl);

    // 不带端口号，由nginx代理
    assertTrue(result.startsWith("rtsp://wxapi.nexiot.cc"));
    assertFalse(result.contains(":8554"));
    assertFalse(result.contains(":554"));
  }

  // ==================== WebSocket 协议测试 ====================

  @Test
  @DisplayName("WS 地址转换为 WSS（安全协议）")
  void testConvertWsUrlToWss() throws Exception {
    String originalUrl =
        "ws://wxapi.nexiot.cc:31080/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push";
    String result = invokeConvertSingleUrl(originalUrl);

    // WS 应该转换为 WSS
    assertTrue(result.startsWith("wss://"));
    // 不带端口号，由nginx代理
    assertFalse(result.contains(":30443"));
    assertFalse(result.contains(":31080"));
    // 保留路径和查询参数
    assertTrue(result.contains("/rtp/"));
    assertTrue(result.contains("originTypeStr=rtp_push"));
  }

  @Test
  @DisplayName("WS HLS 地址转换")
  void testConvertWsHlsUrl() throws Exception {
    String originalUrl =
        "ws://wxapi.nexiot.cc:31080/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push";
    String result = invokeConvertSingleUrl(originalUrl);

    assertTrue(result.startsWith("wss://wxapi.nexiot.cc"));
    assertFalse(result.contains(":30443"));
    assertTrue(result.contains("/hls.m3u8"));
    assertTrue(result.contains("originTypeStr=rtp_push"));
  }

  // ==================== WSS 协议测试 ====================

  @Test
  @DisplayName("WSS FLV 地址端口不带")
  void testConvertWssFLVUrlKeepPort() throws Exception {
    String originalUrl =
        "wss://wxapi.nexiot.cc:30443/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    // 不带端口号，由nginx代理
    String expected =
        "wss://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.flv?originTypeStr=rtp_push";
    assertEquals(expected, result);
    assertFalse(result.contains(":30443"));
  }

  @Test
  @DisplayName("WSS FMP4 地址转换")
  void testConvertWssFmp4Url() throws Exception {
    String originalUrl =
        "wss://wxapi.nexiot.cc:30443/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push";
    String expected =
        "wss://wxapi.nexiot.cc:30443/rtp/41010500002000000001_41020000001310000002.live.mp4?originTypeStr=rtp_push";

    String result = invokeConvertSingleUrl(originalUrl);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("WSS HLS 地址转换")
  void testConvertWssHlsUrl() throws Exception {
    String originalUrl =
        "wss://wxapi.nexiot.cc:30443/rtp/41010500002000000001_41020000001310000002/hls.m3u8?originTypeStr=rtp_push";
    String result = invokeConvertSingleUrl(originalUrl);

    assertTrue(result.startsWith("wss://wxapi.nexiot.cc:30443"));
    assertTrue(result.contains("/hls.m3u8"));
  }

  @Test
  @DisplayName("WSS 地址去除非标准端口")
  void testConvertWssUrlRemoveNonStandardPort() throws Exception {
    String originalUrl = "wss://original.server.com:9443/rtp/stream789";
    String result = invokeConvertSingleUrl(originalUrl);

    // 不带端口号，由nginx代理
    assertTrue(result.startsWith("wss://wxapi.nexiot.cc"));
    assertFalse(result.contains(":9443"));
    assertFalse(result.contains(":30443"));
  }

  // ==================== 边界情况测试 ====================

  @Test
  @DisplayName("URL 不包含查询参数")
  void testConvertUrlWithoutQueryParams() throws Exception {
    String originalUrl =
        "https://wxapi.nexiot.cc/rtp/41010500002000000001_41020000001310000002.live.flv";
    String result = invokeConvertSingleUrl(originalUrl);

    assertTrue(result.startsWith("https://wxapi.nexiot.cc"));
    assertTrue(result.contains(".live.flv"));
  }

  @Test
  @DisplayName("RTMP URL 包含多个查询参数")
  void testConvertRtmpUrlWithMultipleQueryParams() throws Exception {
    String originalUrl = "rtmp://wxapi.nexiot.cc:1935/rtp/stream123?param1=value1&param2=value2";
    String result = invokeConvertSingleUrl(originalUrl);

    assertTrue(result.contains("param1=value1"));
    assertTrue(result.contains("param2=value2"));
  }

  @Test
  @DisplayName("未知协议保持不变")
  void testConvertUnknownProtocol() throws Exception {
    String originalUrl = "custom://example.com/path";
    String result = invokeConvertSingleUrl(originalUrl);

    assertEquals(originalUrl, result);
  }

  @Test
  @DisplayName("空 URL 处理")
  void testConvertEmptyUrl() throws Exception {
    String originalUrl = "";
    String result = invokeConvertSingleUrl(originalUrl);

    assertEquals(originalUrl, result);
  }

  // ==================== 辅助方法 ====================

  /** 通过反射调用私有方法 convertSingleUrl */
  private String invokeConvertSingleUrl(String url) throws Exception {
    return (String) ReflectionTestUtils.invokeMethod(controller, "convertSingleUrl", url);
  }
}
