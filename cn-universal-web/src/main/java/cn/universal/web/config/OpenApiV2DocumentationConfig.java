package cn.universal.web.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 独立 V2 OpenAPI 分组：GET /v3/api-docs/nexiot-openapi-v2 可直接导入 Apifox/Swagger/Postman。
 *
 * <p>只增加分组，不改变现有全量文档、V1 路由或 Swagger 开关。文档自身的访问仍遵守部署环境配置。
 */
@Configuration
@ConditionalOnProperty(name = "swagger.enable", havingValue = "true")
public class OpenApiV2DocumentationConfig {
  @Bean
  public GroupedOpenApi nexiotOpenApiV2() {
    return GroupedOpenApi.builder()
        .group("nexiot-openapi-v2")
        .displayName("NexIoT 开放接口 V2")
        .pathsToMatch("/api/openapi/v2/**")
        .addOpenApiCustomizer(
            api -> {
              api.setInfo(
                  new Info()
                      .title("NexIoT 子系统集成开放接口")
                      .version("2.0")
                      .description(
                          "使用 POST /oauth2/token 的 client_credentials 获取应用令牌，然后携带 Authorization:"
                              + " Bearer <token>。所有接口限定当前应用、设备绑定与租户。统一响应 code=0 表示成功，data"
                              + " 为业务结果，requestId 用于排障。普通分页返回 total/page，游标分页返回"
                              + " hasMore/nextCursor；日志查询时间统一使用 Unix 秒。命令 accepted"
                              + " 仅表示平台/协议受理，设备执行结果通过日志、最新属性或应用推送确认。V1 保持兼容。详见仓库"
                              + " docs/openapi/README-v2.md。"));
              if (api.getPaths() == null) return;
              var error =
                  new ObjectSchema()
                      .addProperty(
                          "code", new IntegerSchema().description("非0业务错误码；常见值400/403/409/500"))
                      .addProperty("msg", new StringSchema().description("安全可读的错误信息"))
                      .addProperty("data", new ObjectSchema().nullable(true))
                      .addProperty(
                          "requestId",
                          new StringSchema().description("请求跟踪编号；安全过滤器拒绝时响应格式以认证服务器为准"));
              Map<String, String> errors =
                  Map.of(
                      "400",
                      "参数格式、范围或游标无效",
                      "401",
                      "令牌缺失、无效或已过期；由 OAuth2 资源服务器返回",
                      "403",
                      "应用无效、资源未绑定或租户不匹配",
                      "409",
                      "资源冲突、设备状态或能力不支持",
                      "429",
                      "历史查询限流，请稍后重试",
                      "500",
                      "服务内部错误，请使用 requestId 排障",
                      "502",
                      "上游协议或视频服务未返回有效结果",
                      "503",
                      "历史服务暂不可用");
              api.getPaths()
                  .values()
                  .forEach(
                      path ->
                          path.readOperations()
                              .forEach(
                                  operation ->
                                      errors.forEach(
                                          (status, description) -> {
                                            ApiResponse response =
                                                new ApiResponse().description(description);
                                            if (!"401".equals(status))
                                              response.content(
                                                  new Content()
                                                      .addMediaType(
                                                          "application/json",
                                                          new io.swagger.v3.oas.models.media
                                                                  .MediaType()
                                                              .schema(error)));
                                            operation
                                                .getResponses()
                                                .addApiResponse(status, response);
                                          })));
            })
        .build();
  }
}
