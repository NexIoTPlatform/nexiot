package cn.universal.admin.system.notice;

import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.persistence.entity.admin.SysDictData;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通知渠道元数据，页面字段来自字典，避免在前端维护 JSON 配置。 */
@RestController
@RequestMapping("/admin/v1/notice/meta")
public class NoticeChannelMetaController {
  private final ISysDictTypeService dictService;
  public NoticeChannelMetaController(ISysDictTypeService dictService) { this.dictService = dictService; }
  @GetMapping("/fields")
  public List<Map<String, String>> fields(@RequestParam(defaultValue = "notice_wechat_field") String dictType) {
    if (!List.of("notice_wechat_field", "notice_wechat_template_field", "notice_channel_type").contains(dictType)) throw new IllegalArgumentException("不支持的通知字典");
    List<SysDictData> entries = dictService.selectDictDataByType(dictType);
    return (entries == null ? List.<SysDictData>of() : entries).stream().filter(d -> "0".equals(d.getStatus())).map(this::toMap).collect(Collectors.toList());
  }
  private Map<String, String> toMap(SysDictData d) { return Map.of("key", d.getDictValue(), "label", d.getDictLabel(), "meta", d.getCssClass() == null ? "" : d.getCssClass()); }
}
