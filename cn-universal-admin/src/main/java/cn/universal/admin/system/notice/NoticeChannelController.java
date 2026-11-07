package cn.universal.admin.system.notice;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.manager.notice.dto.NoticeChannelDTO;
import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/notice/channel")
@Tag(name = "通知渠道管理", description = "通知渠道管理接口")
public class NoticeChannelController extends NoticeScopedController {

  private final NoticeChannelService noticeChannelService;
  private final ObjectMapper objectMapper;

  @Autowired
  public NoticeChannelController(
      NoticeChannelService noticeChannelService, ObjectMapper objectMapper) {
    this.noticeChannelService = noticeChannelService;
    this.objectMapper = objectMapper;
  }

  /**
   * 查询通知渠道列表
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param name 渠道名称
   * @param channelType 渠道类型
   * @return 通知渠道列表
   */
  @Operation(summary = "查询通知渠道列表", description = "根据条件查询通知渠道列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通知渠道列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  @Parameter(name = "name", description = "渠道名称", required = false)
  @Parameter(name = "channelType", description = "渠道类型", required = false)
  @GetMapping("/list")
  public TableDataInfo<NoticeChannelDTO> list(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String channelType,
      @RequestParam(required = false) String status) {
    requirePermission("notice:channel:list", "notice:channel:detail", "notice:channel:add", "notice:channel:edit", "notice:template:add", "notice:template:edit", "notice:template:detail");

    List<NoticeChannel> list = noticeChannelService.search(name, channelType, status);

    List<NoticeChannelDTO> dtoList =
        list.stream()
            .map(
                channel -> {
                  NoticeChannelDTO dto = new NoticeChannelDTO();
                  dto.setId(channel.getId());
                  dto.setTenantId(channel.getTenantId());
                  dto.setDeptId(channel.getDeptId());
                  dto.setName(channel.getName());
                  dto.setChannelType(channel.getChannelType());
                  dto.setStatus(channel.getStatus());
                  dto.setRemark(channel.getRemark());
                  dto.setCreator(channel.getCreator());
                  dto.setCreateTime(channel.getCreateTime());
                  dto.setUpdateTime(channel.getUpdateTime());

                  // 将config字符串转换为对象并进行脱敏处理
                  if (channel.getConfig() != null && !channel.getConfig().isEmpty()) {
                    try {
                      JSONObject desensitizedConfig =
                          maskConfig(channel.getConfig());
                      dto.setConfig(desensitizedConfig);
                    } catch (Exception e) {
                      dto.setConfig(null);
                    }
                  }

                  return dto;
                })
            .toList();

    return getDataTable(dtoList, pageNum, pageSize);
  }

  /**
   * 获取通知渠道详情
   *
   * @param id 渠道ID
   * @return 通知渠道详情
   */
  @Operation(summary = "获取通知渠道详情", description = "根据ID获取通知渠道详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取通知渠道详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = NoticeChannelDTO.class)))
  @Parameter(name = "id", description = "渠道ID", required = true)
  @GetMapping("/get")
  public NoticeChannelDTO get(@RequestParam Long id) {
    requirePermission("notice:channel:detail", "notice:channel:edit");
    NoticeChannel channel = noticeChannelService.getById(id);
    if (channel == null) {
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "渠道不存在");
    }

    if (!canAccessNotice(currentTenantUser(), channel, channel.getCreator())) {
      return null;
    }

    NoticeChannelDTO dto = new NoticeChannelDTO();
    dto.setId(channel.getId());
    dto.setTenantId(channel.getTenantId());
    dto.setDeptId(channel.getDeptId());
    dto.setName(channel.getName());
    dto.setChannelType(channel.getChannelType());
    dto.setStatus(channel.getStatus());
    dto.setRemark(channel.getRemark());
    dto.setCreator(channel.getCreator());
    dto.setCreateTime(channel.getCreateTime());
    dto.setUpdateTime(channel.getUpdateTime());

    // 将config字符串转换为对象并进行脱敏处理
    if (channel.getConfig() != null && !channel.getConfig().isEmpty()) {
      JSONObject desensitizedConfig = maskConfig(channel.getConfig());
      dto.setConfig(desensitizedConfig);
    }

    return dto;
  }

  /**
   * 保存通知渠道
   *
   * @param channelDTO 渠道信息
   * @return 保存结果
   */
  @Operation(summary = "保存通知渠道", description = "新增或修改通知渠道信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功保存通知渠道",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "渠道信息",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = NoticeChannelDTO.class)))
  @PostMapping("/save")
  public Map<String, Object> save(@RequestBody NoticeChannelDTO channelDTO) {
    requirePermission(channelDTO.getId() == null ? "notice:channel:add" : "notice:channel:edit");
    try {
      String currentUser = SecurityUtils.getUnionId();
      IoTUser iotUser = currentTenantUser();

      NoticeChannel existingChannel = null;
      if (channelDTO.getId() != null) {
        existingChannel = noticeChannelService.getById(channelDTO.getId());
        if (existingChannel == null) {
          return Map.of("code", 1, "msg", "渠道不存在");
        }
        if (!canAccessNotice(iotUser, existingChannel, existingChannel.getCreator())) {
          return Map.of("code", 1, "msg", "无权限修改该渠道");
        }
      }

      NoticeChannel channel = new NoticeChannel();
      channel.setId(channelDTO.getId());
      channel.setRemark(channelDTO.getRemark());

      // 如果是新增，设置创建者；如果是修改，保持原有创建者
      if (channelDTO.getId() == null) {
        channel.setName(channelDTO.getName());
        channel.setChannelType(channelDTO.getChannelType());
        channel.setStatus(channelDTO.getStatus());
        channel.setCreator(currentUser);
      } else if (existingChannel != null) {
        channel.setName(
            channelDTO.getName() != null ? channelDTO.getName() : existingChannel.getName());
        channel.setChannelType(
            channelDTO.getChannelType() != null
                ? channelDTO.getChannelType()
                : existingChannel.getChannelType());
        channel.setStatus(
            channelDTO.getStatus() != null ? channelDTO.getStatus() : existingChannel.getStatus());
        channel.setCreator(existingChannel.getCreator());
        channel.setCreateTime(existingChannel.getCreateTime());
      }

      channel.setUpdateTime(channelDTO.getUpdateTime());
      // 将config对象转换为JSON字符串
      if (channelDTO.getConfig() != null) {
        channel.setConfig(buildConfigJson(channelDTO, existingChannel));
      } else if (existingChannel != null) {
        channel.setConfig(existingChannel.getConfig());
      }

      noticeChannelService.save(channel);
      return Map.of("code", 0, "msg", "操作成功");
    } catch (Exception e) {
      return Map.of("code", 1, "msg", "操作失败: " + noticeError(e));
    }
  }

  static JSONObject maskConfig(String config) {
    JSONObject result = JsonDesensitizationUtil.desensitize(config);
    if (result != null && result.containsKey("webhook") && result.getStr("webhook") != null && !result.getStr("webhook").isBlank())
      result.set("webhook", "***");
    return result;
  }

  static String buildConfigJson(NoticeChannelDTO channelDTO, NoticeChannel existingChannel) {
    if (channelDTO == null || channelDTO.getConfig() == null) {
      return null;
    }

    String newConfigJson = JSONUtil.toJsonStr(channelDTO.getConfig());
    if (existingChannel == null
        || existingChannel.getConfig() == null
        || (channelDTO.getChannelType() != null && !Objects.equals(existingChannel.getChannelType(), channelDTO.getChannelType()))) {
      return newConfigJson;
    }

    return JsonDesensitizationUtil.mergeAuthConfig(newConfigJson, existingChannel.getConfig());
  }

  /**
   * 删除通知渠道
   *
   * @param ids 渠道ID列表
   * @return 删除结果
   */
  @Operation(summary = "删除通知渠道", description = "根据ID列表批量删除通知渠道")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除通知渠道",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "渠道ID列表",
      required = true,
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
  @PostMapping("/delete")
  public Map<String, Object> delete(@RequestBody List<Long> ids) {
    requirePermission("notice:channel:del");
    try {
      if (ids != null && !ids.isEmpty()) {
        IoTUser iotUser = currentTenantUser();

        if (iotUser == null || !iotUser.isAdmin()) {
          for (Long id : ids) {
            NoticeChannel channel = noticeChannelService.getById(id);
            if (channel != null && !canAccessNotice(iotUser, channel, channel.getCreator())) {
              return Map.of("code", 1, "msg", "无权限删除该渠道");
            }
          }
        }

        noticeChannelService.deleteBatch(ids);
        return Map.of("code", 0, "msg", "删除成功");
      } else {
        return Map.of("code", 1, "msg", "未提供有效的ID");
      }
    } catch (Exception e) {
      return Map.of("code", 1, "msg", "删除失败: " + noticeError(e));
    }
  }
}
