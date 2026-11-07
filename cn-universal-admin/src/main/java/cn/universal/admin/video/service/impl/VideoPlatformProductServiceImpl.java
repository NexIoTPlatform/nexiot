/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台产品自动创建服务实现
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.video.service.VideoPlatformProductService;
import cn.universal.admin.video.service.VideoSdkThingModelTemplate;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import java.util.Objects;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

/** 视频平台产品创建策略 WVP：自动创建GB/级联两个产品（autoCreateProducts=1） HIK/ICC：懒创建，首次勾选使用设备时创建平台产品 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoPlatformProductServiceImpl implements VideoPlatformProductService {
  private String metadata =
"""
{
  "tags": [],
  "events": [
    {
      "id": "online",
      "name": "设备上线",
      "valueType": {
        "type": "string"
      }
    },
    {
      "id": "offline",
      "name": "设备下线",
      "valueType": {
        "type": "string"
      }
    },
    {
      "id": "cameraAlarm",
      "name": "摄像机告警",
      "valueType": {
        "type": "object",
        "properties": [
          {
            "id": "instanceKey",
            "name": "平台实例",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "platformType",
            "name": "平台类型",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "vendor",
            "name": "厂商",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "deviceId",
            "name": "设备ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "channelId",
            "name": "通道ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "alarmType",
            "name": "告警类型",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "cameraEventType",
            "name": "相机事件类型",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "alarmName",
            "name": "告警名称",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "cameraEventName",
            "name": "相机事件名称",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "alarmState",
            "name": "告警状态",
            "valueType": {
              "type": "enum",
              "elements": [
                {
                  "text": "触发",
                  "value": "start"
                },
                {
                  "text": "恢复",
                  "value": "stop"
                },
                {
                  "text": "未知",
                  "value": "unknown"
                }
              ]
            }
          },
          {
            "id": "alarmLevel",
            "name": "告警等级",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "alarmTime",
            "name": "告警时间",
            "valueType": {
              "type": "long"
            }
          },
          {
            "id": "eventId",
            "name": "原始事件ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "vendorEventCode",
            "name": "厂商事件码",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "vendorEventCodeHex",
            "name": "厂商事件码HEX",
            "valueType": {
              "type": "string"
            }
          }
        ]
      }
    }
  ],
  "functions": [
    {
      "id": "cameraLiveStream",
      "name": "实时预览",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "streamType",
          "name": "码流类型",
          "valueType": {
            "type": "enum",
            "elements": [
              {
                "text": "主码流",
                "value": "main"
              },
              {
                "text": "子码流",
                "value": "sub"
              }
            ]
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "rtsp",
            "name": "RTSP地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "rtmp",
            "name": "RTMP地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "flv",
            "name": "FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "hls",
            "name": "HLS地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "webrtc",
            "name": "WebRTC地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "ws_flv",
            "name": "WS-FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "wss_flv",
            "name": "WSS-FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "httpflv",
            "name": "HTTP-FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "httpsflv",
            "name": "HTTPS-FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "httpmp4",
            "name": "HTTP-MP4地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "httpsmp4",
            "name": "HTTPS-MP4地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "hik",
            "name": "海康私有协议地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "preferredUrl",
            "name": "首选播放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "streamProtocol",
            "name": "首选协议",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "streamType",
            "name": "码流类型",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "streamSource",
            "name": "取流来源",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "playHint",
            "name": "播放提示",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "urls",
            "name": "协议地址集合",
            "valueType": {
              "type": "object",
              "properties": [
                {
                  "id": "rtsp",
                  "name": "RTSP地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "rtmp",
                  "name": "RTMP地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "flv",
                  "name": "FLV地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "hls",
                  "name": "HLS地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "webrtc",
                  "name": "WebRTC地址",
                  "valueType": {
                    "type": "string"
                  }
                }
              ]
            }
          }
        ]
      }
    },
    {
      "id": "stopPreview",
      "name": "停止预览",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        }
      ],
      "output": {}
    },
    {
      "id": "cameraTurn",
      "name": "云台控制",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "command",
          "name": "控制命令",
          "valueType": {
            "type": "enum",
            "elements": [
              {
                "text": "上",
                "value": "up"
              },
              {
                "text": "下",
                "value": "down"
              },
              {
                "text": "左",
                "value": "left"
              },
              {
                "text": "右",
                "value": "right"
              },
              {
                "text": "左上",
                "value": "leftUp"
              },
              {
                "text": "左下",
                "value": "leftDown"
              },
              {
                "text": "右上",
                "value": "rightUp"
              },
              {
                "text": "右下",
                "value": "rightDown"
              },
              {
                "text": "放大",
                "value": "zoomIn"
              },
              {
                "text": "缩小",
                "value": "zoomOut"
              },
              {
                "text": "停止",
                "value": "stop"
              }
            ]
          }
        },
        {
          "id": "speed",
          "name": "速度",
          "valueType": {
            "type": "int"
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "deviceId",
            "name": "设备ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "channelId",
            "name": "通道ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "command",
            "name": "控制命令",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "speed",
            "name": "速度",
            "valueType": {
              "type": "int"
            }
          },
          {
            "id": "executed",
            "name": "是否已执行",
            "valueType": {
              "type": "boolean"
            }
          },
          {
            "id": "ts",
            "name": "执行时间",
            "valueType": {
              "type": "long"
            }
          }
        ]
      }
    },
    {
      "id": "cameraPlayback",
      "name": "录像回放",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "startTime",
          "name": "开始时间",
          "valueType": {
            "type": "long"
          }
        },
        {
          "id": "endTime",
          "name": "结束时间",
          "valueType": {
            "type": "long"
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "rtsp",
            "name": "RTSP回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "rtmp",
            "name": "RTMP回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "flv",
            "name": "FLV回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "hls",
            "name": "HLS回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "ws_flv",
            "name": "WS-FLV回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "wss_flv",
            "name": "WSS-FLV回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "https_hls",
            "name": "HTTPS-HLS回放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "downloadUrl",
            "name": "录像下载地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "app",
            "name": "流应用名",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "stream",
            "name": "流ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "mediaServerId",
            "name": "媒体服务ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "key",
            "name": "播放会话Key",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "channelId",
            "name": "通道ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "startTime",
            "name": "开始时间",
            "valueType": {
              "type": "long"
            }
          },
          {
            "id": "endTime",
            "name": "结束时间",
            "valueType": {
              "type": "long"
            }
          },
          {
            "id": "beginTime",
            "name": "厂商开始时间",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "protocol",
            "name": "协议类型",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "prefer",
            "name": "首选协议",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "urls",
            "name": "协议地址集合",
            "valueType": {
              "type": "object",
              "properties": [
                {
                  "id": "rtsp",
                  "name": "RTSP地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "rtmp",
                  "name": "RTMP地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "flv",
                  "name": "FLV地址",
                  "valueType": {
                    "type": "string"
                  }
                },
                {
                  "id": "hls",
                  "name": "HLS地址",
                  "valueType": {
                    "type": "string"
                  }
                }
              ]
            }
          },
          {
            "id": "records",
            "name": "关联录像片段",
            "valueType": {
              "type": "array",
              "elementType": {
                "type": "object",
                "properties": [
                  {
                    "id": "id",
                    "name": "记录ID",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "name",
                    "name": "记录名称",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "startTime",
                    "name": "开始时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "endTime",
                    "name": "结束时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "playbackUrl",
                    "name": "回放地址",
                    "valueType": {
                      "type": "string"
                    }
                  }
                ]
              }
            }
          },
          {
            "id": "responseData",
            "name": "厂商原始响应",
            "valueType": {
              "type": "object"
            }
          }
        ]
      }
    },
    {
      "id": "stopPlayback",
      "name": "停止回放",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        }
      ],
      "output": {}
    },
    {
      "id": "cameraSnapshot",
      "name": "抓拍",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "snapshotUrl",
            "name": "抓拍图片地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "snapshotData",
            "name": "抓拍图片数据",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "picInfo",
            "name": "图片相对路径",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "picLen",
            "name": "图片大小",
            "valueType": {
              "type": "int"
            }
          },
          {
            "id": "cameraIndexCode",
            "name": "海康监控点编码",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "deviceCode",
            "name": "大华设备编码",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "responseData",
            "name": "厂商原始响应",
            "valueType": {
              "type": "object"
            }
          }
        ]
      }
    },
    {
      "id": "queryRecords",
      "name": "查询录像",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "startTime",
          "name": "开始时间",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "endTime",
          "name": "结束时间",
          "valueType": {
            "type": "string"
          }
        }
      ],
      "output": {
        "type": "array",
        "elementType": {
          "type": "object",
          "properties": [
            {
              "id": "id",
              "name": "记录ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "name",
              "name": "记录名称",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "channelId",
              "name": "通道ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "cameraIndexCode",
              "name": "海康监控点编码",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "startTime",
              "name": "开始时间",
              "valueType": {
                "type": "long"
              }
            },
            {
              "id": "endTime",
              "name": "结束时间",
              "valueType": {
                "type": "long"
              }
            },
            {
              "id": "beginTime",
              "name": "厂商开始时间",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "recordType",
              "name": "录像类型",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "recordLocation",
              "name": "录像位置",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "recordSource",
              "name": "录像来源",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "streamType",
              "name": "码流类型",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "fileSize",
              "name": "文件大小",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "fileSizeBytes",
              "name": "文件大小字节数",
              "valueType": {
                "type": "long"
              }
            },
            {
              "id": "fileLength",
              "name": "文件时长",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "filePath",
              "name": "文件路径",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "playbackUrl",
              "name": "回放地址",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "address",
              "name": "录像地址",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "secrecy",
              "name": "保密属性",
              "valueType": {
                "type": "int"
              }
            },
            {
              "id": "type",
              "name": "记录类型",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "recordName",
              "name": "录像名称",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "videoRecordType",
              "name": "视频录像类型",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "planId",
              "name": "计划ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "ssId",
              "name": "存储服务ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "diskId",
              "name": "磁盘ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "streamId",
              "name": "流ID",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "forgotten",
              "name": "遗忘标记",
              "valueType": {
                "type": "string"
              }
            },
            {
              "id": "raw",
              "name": "厂商原始记录",
              "valueType": {
                "type": "object"
              }
            }
          ]
        }
      }
    },
    {
      "id": "queryCloudRecords",
      "name": "查询云端录像",
      "config": false,
      "inputs": [
        {
          "id": "app",
          "name": "应用名",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "stream",
          "name": "流ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "page",
          "name": "页码",
          "valueType": {
            "type": "int"
          }
        },
        {
          "id": "count",
          "name": "每页数量",
          "valueType": {
            "type": "int"
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "app",
            "name": "应用名",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "stream",
            "name": "流ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "page",
            "name": "页码",
            "valueType": {
              "type": "int"
            }
          },
          {
            "id": "count",
            "name": "每页数量",
            "valueType": {
              "type": "int"
            }
          },
          {
            "id": "total",
            "name": "总数量",
            "valueType": {
              "type": "long"
            }
          },
          {
            "id": "list",
            "name": "录像列表",
            "valueType": {
              "type": "array",
              "elementType": {
                "type": "object",
                "properties": [
                  {
                    "id": "id",
                    "name": "录像ID",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "cloudRecordId",
                    "name": "云端录像ID",
                    "valueType": {
                      "type": "int"
                    }
                  },
                  {
                    "id": "app",
                    "name": "应用名",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "stream",
                    "name": "流ID",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "startTime",
                    "name": "开始时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "endTime",
                    "name": "结束时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "duration",
                    "name": "时长",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "fileSize",
                    "name": "文件大小",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "url",
                    "name": "录像地址",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "raw",
                    "name": "原始记录",
                    "valueType": {
                      "type": "object"
                    }
                  }
                ]
              }
            }
          },
          {
            "id": "items",
            "name": "录像条目",
            "valueType": {
              "type": "array",
              "elementType": {
                "type": "object",
                "properties": [
                  {
                    "id": "id",
                    "name": "录像ID",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "cloudRecordId",
                    "name": "云端录像ID",
                    "valueType": {
                      "type": "int"
                    }
                  },
                  {
                    "id": "startTime",
                    "name": "开始时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "endTime",
                    "name": "结束时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "url",
                    "name": "录像地址",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "raw",
                    "name": "原始记录",
                    "valueType": {
                      "type": "object"
                    }
                  }
                ]
              }
            }
          },
          {
            "id": "records",
            "name": "录像记录",
            "valueType": {
              "type": "array",
              "elementType": {
                "type": "object",
                "properties": [
                  {
                    "id": "id",
                    "name": "录像ID",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "cloudRecordId",
                    "name": "云端录像ID",
                    "valueType": {
                      "type": "int"
                    }
                  },
                  {
                    "id": "startTime",
                    "name": "开始时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "endTime",
                    "name": "结束时间",
                    "valueType": {
                      "type": "long"
                    }
                  },
                  {
                    "id": "url",
                    "name": "录像地址",
                    "valueType": {
                      "type": "string"
                    }
                  },
                  {
                    "id": "raw",
                    "name": "原始记录",
                    "valueType": {
                      "type": "object"
                    }
                  }
                ]
              }
            }
          },
          {
            "id": "responseData",
            "name": "平台原始响应",
            "valueType": {
              "type": "object"
            }
          }
        ]
      }
    },
    {
      "id": "loadCloudRecord",
      "name": "加载云端录像",
      "config": false,
      "inputs": [
        {
          "id": "app",
          "name": "应用名",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "stream",
          "name": "流ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "cloudRecordId",
          "name": "录像ID",
          "valueType": {
            "type": "int"
          }
        }
      ],
      "output": {
        "type": "object",
        "properties": [
          {
            "id": "app",
            "name": "应用名",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "stream",
            "name": "流ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "cloudRecordId",
            "name": "云端录像ID",
            "valueType": {
              "type": "int"
            }
          },
          {
            "id": "mediaServerId",
            "name": "媒体服务ID",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "rtsp",
            "name": "RTSP地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "rtmp",
            "name": "RTMP地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "flv",
            "name": "FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "hls",
            "name": "HLS地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "ws_flv",
            "name": "WS-FLV地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "downloadUrl",
            "name": "下载地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "playUrl",
            "name": "播放地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "url",
            "name": "资源地址",
            "valueType": {
              "type": "string"
            }
          },
          {
            "id": "responseData",
            "name": "平台原始响应",
            "valueType": {
              "type": "object"
            }
          }
        ]
      }
    },
    {
      "id": "queryPresets",
      "name": "查询预置位",
      "config": false,
      "inputs": [
        {
          "id": "deviceId",
          "name": "设备ID",
          "valueType": {
            "type": "string"
          }
        },
        {
          "id": "channelId",
          "name": "通道ID",
          "valueType": {
            "type": "string"
          }
        }
      ],
      "output": {}
    }
  ],
  "properties": [
    {
      "id": "deviceStatus",
      "mode": "r",
      "name": "设备状态",
      "config": false,
      "source": "device",
      "valueType": {
        "type": "enum",
        "elements": [
          {
            "text": "在线",
            "value": "online"
          },
          {
            "text": "离线",
            "value": "offline"
          }
        ]
      }
    },
    {
      "id": "manufacturer",
      "mode": "r",
      "name": "厂商",
      "config": false,
      "source": "device",
      "valueType": {
        "type": "string"
      }
    },
    {
      "id": "model",
      "mode": "r",
      "name": "型号",
      "config": false,
      "source": "device",
      "valueType": {
        "type": "string"
      }
    }
  ]
}
""";
  private final IoTProductMapper productMapper;
  private final VideoPlatformInstanceMapper instanceMapper;
  private final ObjectMapper objectMapper;
  private final VideoSdkThingModelTemplate sdkThingModelTemplate;
  private final IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @Override
  @Transactional
  public void autoCreateWvpProducts(VideoPlatformInstance instance) {
    if (!"wvp".equals(instance.getPlatformType())) {
      log.warn("仅WVP平台支持自动创建产品，当前平台类型：{}", instance.getPlatformType());
      return;
    }
    if (instance.getAutoCreateProducts() != 1) {
      log.info("WVP平台实例未开启自动创建产品，跳过");
      return;
    }

    // 检查是否已创建GB产品
    String gbProductKey = instance.getInstanceKey() + "_GB";
    IoTProduct gbProduct = productMapper.getProductByProductKey(gbProductKey);
    if (gbProduct == null) {
      createPlatformProduct(instance, "VIDEO_DEVICE", gbProductKey, instance.getName() + "-GB设备");
      log.info("WVP平台自动创建GB产品：{}", gbProductKey);
    }

    // 检查是否已创建级联产品
    //    String cascadeProductKey = instance.getInstanceKey() + "_CASCADE";
    //    IoTProduct cascadeProduct = productMapper.getProductByProductKey(cascadeProductKey);
    //    if (cascadeProduct == null) {
    //      createPlatformProduct(
    //          instance, "VIDEO_GATEWAY", cascadeProductKey, instance.getName() + "-级联平台");
    //      log.info("WVP平台自动创建级联产品：{}", cascadeProductKey);
    //    }
  }

  @Override
  @Transactional
  public IoTProduct ensurePlatformProduct(VideoPlatformInstance instance, String deviceNode) {
    String productKey = buildPlatformProductKey(instance, deviceNode);
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null) {
      product = createPlatformProduct(instance, deviceNode, productKey, instance.getName());
      log.info("懒创建平台产品：{} [{}]", productKey, deviceNode);
    }
    if ("hik_isup".equals(instance.getPlatformType()) && (!Objects.equals(instance.getTenantId(), product.getTenantId())
        || !Objects.equals(instance.getPlatformType(), product.getThirdPlatform())))
      throw new BaseException("视频产品归属与 ISUP 实例不匹配");
    return mergeSdkEvents(instance, product);
  }

  @Override
  @Transactional
  public IoTProduct prepareAlarmProduct(VideoPlatformInstance instance) {
    IoTProduct product = productMapper.getProductByProductKey(instance.getInstanceKey());
    if (product == null || Integer.valueOf(1).equals(product.getIsDeleted())) {
      throw new BaseException("视频产品不存在：" + instance.getInstanceKey());
    }
    if (!Objects.equals(instance.getTenantId(), product.getTenantId())
        || !Objects.equals(instance.getPlatformType(), product.getThirdPlatform())) {
      throw new BaseException("视频产品归属与平台实例不匹配");
    }
    return mergeSdkEvents(instance, product);
  }

  private IoTProduct mergeSdkEvents(VideoPlatformInstance instance, IoTProduct product) {
    if (isDirectSdk(instance) || "wvp".equals(instance.getPlatformType())) {
      String merged = sdkThingModelTemplate.merge(product.getMetadata(), instance.getPlatformType(), false);
      if (!merged.equals(product.getMetadata())) {
        // 比较旧物模型后再更新：多节点同时补齐或管理员正在编辑时，不能覆盖较新的内容。
        Example expected = new Example(IoTProduct.class);
        Example.Criteria criteria = expected.createCriteria().andEqualTo("id", product.getId());
        if (product.getMetadata() == null) criteria.andIsNull("metadata");
        else criteria.andEqualTo("metadata", product.getMetadata());
        IoTProduct patch = new IoTProduct();
        patch.setMetadata(merged);
        patch.setUpdateTime(new Date());
        if (productMapper.updateByExampleSelective(patch, expected) != 1) {
          throw new BaseException("产品物模型已变更，请重试");
        }
        product.setMetadata(merged);
        product.setUpdateTime(patch.getUpdateTime());
        cacheInvalidationPublisher.publishProductChange(product.getProductKey(), "video-sdk-metadata-update");
      }
    }
    return product;
  }

  @Override
  public IoTProduct getOrCreatePlatformProduct(String instanceKey, String deviceNode) {
    Example example = new Example(VideoPlatformInstance.class);
    example.createCriteria().andEqualTo("instanceKey", instanceKey);
    VideoPlatformInstance instance =
        instanceMapper.selectByExample(example).stream().findFirst().orElse(null);
    if (instance == null) {
      throw new BaseException("平台实例不存在：" + instanceKey);
    }
    return ensurePlatformProduct(instance, deviceNode);
  }

  /** 创建产品 */
  private IoTProduct createPlatformProduct(
      VideoPlatformInstance instance, String deviceNode, String productKey, String productName) {
    try {
      IoTProduct product = new IoTProduct();
      product.setProductKey(productKey);
      product.setProductId(productKey); // 与productKey一致
      product.setName(productName);
      product.setThirdPlatform(instance.getPlatformType());
      product.setDeviceNode(deviceNode);
      product.setState((IoTConstant.NORMAL.byteValue())); // 启用
      product.setCreatorId(StrUtil.blankToDefault(SecurityUtils.getUnionIdQuietly(), instance.getCreatorId()));
      product.setTenantId(instance.getTenantId());
      product.setCreateTime(new Date());
      product.setUpdateTime(new Date());
      product.setStorePolicy("mysql");
      product.setTransportProtocol("以太网");
      product.setIsDeleted(0);
      // 将平台实例配置写入thirdConfiguration
      Map<String, Object> thirdConfig = new HashMap<>();
      thirdConfig.put("instanceKey", instance.getInstanceKey());
      thirdConfig.put("endpoint", instance.getEndpoint());
      if (!"hik_isup".equals(instance.getPlatformType())) thirdConfig.put("auth", instance.getAuth());
      thirdConfig.put("version", instance.getVersion());
      if (!"hik_isup".equals(instance.getPlatformType())) thirdConfig.put("options", instance.getOptions());
      thirdConfig.put("customField", new ArrayList<String>());
      product.setThirdConfiguration(objectMapper.writeValueAsString(thirdConfig));

      // WVP 使用独立国标物模型；各类视频事件由各自字典填充，保留预览、回放、云台等函数。
      product.setMetadata(isDirectSdk(instance) || "wvp".equals(instance.getPlatformType())
          ? sdkThingModelTemplate.merge(metadata, instance.getPlatformType(), true) : metadata);

      productMapper.insertSelective(product);
      return product;
    } catch (Exception e) {
      log.error("创建平台产品失败：{}", e.getMessage(), e);
      throw new BaseException("创建平台产品失败：" + e.getMessage());
    }
  }

  private boolean isDirectSdk(VideoPlatformInstance instance) {
    return cn.universal.dm.video.VideoSdkPlatforms.managed(instance.getPlatformType());
  }

  /** 根据平台类型与设备节点构建产品Key */
  private String buildPlatformProductKey(VideoPlatformInstance instance, String deviceNode) {
    //    // WVP有特殊规则（已在autoCreateWvpProducts中处理），这里是通用规则
    //    if ("wvp".equals(instance.getPlatformType())) {
    //      if ("VIDEO_DEVICE".equals(deviceNode)) {
    //        return instance.getInstanceKey() + "_GB";
    //      } else if ("VIDEO_GATEWAY".equals(deviceNode)) {
    //        return instance.getInstanceKey() + "_CASCADE";
    //      }
    //    }
    // HIK/ICC/其他平台：instanceKey_deviceNode
    return instance.getInstanceKey();
  }
}
