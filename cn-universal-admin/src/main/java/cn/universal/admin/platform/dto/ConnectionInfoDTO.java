package cn.universal.admin.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 连接信息数据传输对象 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "连接信息数据传输对象")
public class ConnectionInfoDTO {

  /** 内置连接信息 */
  @Schema(description = "内置连接信息")
  private BuiltinConnection builtin;

  /** 网络组件连接信息 */
  @Schema(description = "网络组件连接信息")
  private NetworkConnection network;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "内置连接信息")
  public static class BuiltinConnection {
    /** 连接方式 */
    @Schema(description = "连接方式")
    private String connectionType;

    /** 连接地址 */
    @Schema(description = "连接地址")
    private String host;

    /** 端口 */
    @Schema(description = "端口")
    private Integer port;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

    /** 密码（默认不显示，需要点击查看） */
    @Schema(description = "密码")
    private String password;

    /** 配置说明 */
    @Schema(description = "配置说明")
    private String description;

    /** MQTT主题信息（仅MQTT类型有） */
    @Schema(description = "MQTT主题信息")
    private TopicInfo topics;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "网络组件连接信息")
  public static class NetworkConnection {
    @Schema(description = "设备使用的服务器地址，与监听网卡地址区分")
    private String publicHost;

    /** 是否启用网络组件 */
    @Schema(description = "是否启用网络组件")
    private Boolean enabled;

    /** 组件名称 */
    @Schema(description = "组件名称")
    private String name;

    /** 组件类型 */
    @Schema(description = "组件类型")
    private String type;

    /** 连接地址 */
    @Schema(description = "连接地址")
    private String host;

    /** 端口 */
    @Schema(description = "端口")
    private Integer port;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

    /** 密码（默认不显示，需要点击查看） */
    @Schema(description = "密码")
    private String password;

    /** MQTT客户端ID */
    @Schema(description = "MQTT客户端ID")
    private String clientId;

    /** 是否集群部署 */
    @Schema(description = "是否集群部署")
    private Boolean clusterEnabled;

    /** 运行状态 */
    @Schema(description = "运行状态")
    private Boolean state;

    /** 订阅主题信息 */
    @Schema(description = "订阅主题信息")
    private TopicInfo subscribeTopics;

    /** 错误信息（当enabled为false时） */
    @Schema(description = "错误信息")
    private String message;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "主题信息")
  public static class TopicInfo {
    /** 物模型主题 */
    @Schema(description = "物模型主题")
    private ThingTopics thingTopics;

    /** 透传主题 */
    @Schema(description = "透传主题")
    private PassthroughTopics passthroughTopics;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "物模型主题")
  public static class ThingTopics {
    @Schema(description = "属性上报主题")
    private String propertyUp;

    @Schema(description = "事件上报主题")
    private String eventUp;

    @Schema(description = "命令下发主题")
    private String commandDown;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "透传主题")
  public static class PassthroughTopics {
    @Schema(description = "数据上报主题")
    private String dataUp;

    @Schema(description = "命令下发主题")
    private String commandDown;
  }
}
