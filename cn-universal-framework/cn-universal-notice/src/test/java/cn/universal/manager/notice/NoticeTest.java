// package cn.universal.manager.notice;
//
// import cn.universal.manager.notice.model.NoticeSendChannel;
// import cn.universal.manager.notice.model.NoticeTemplate;
// import cn.universal.manager.notice.service.NoticeChannelService;
// import cn.universal.manager.notice.service.NoticeTemplateService;
// import cn.universal.manager.notice.util.NoticeTemplateUtil;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
//
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// @SpringBootTest
// public class NoticeTest {
//
//  @Autowired private NoticeChannelService noticeChannelService;
//
//  @Autowired private NoticeTemplateService noticeTemplateService;
//
//  @Test
//  public void testChannelCRUD() {
//    // 测试渠道CRUD操作
//    NoticeSendChannel channel = new NoticeSendChannel();
//    channel.setName("测试钉钉机器人");
//    channel.setChannelType("dingTalk");
//    channel.setConfig("{\"webhook\":\"https://test.com\",\"secret\":\"test\"}");
//    channel.setStatus("1");
//    channel.setRemark("测试渠道");
//    channel.setCreator("test");
//
//    // 保存
//    noticeChannelService.save(channel);
//    System.out.println("保存渠道成功，ID: " + channel.getId());
//
//    // 查询
//    NoticeSendChannel saved = noticeChannelService.getById(channel.getId());
//    System.out.println("查询渠道: " + saved.getName());
//
//    // 搜索
//    List<NoticeSendChannel> list = noticeChannelService.search("测试", "dingTalk", "1");
//    System.out.println("搜索到 " + list.size() + " 个渠道");
//
//    // 删除
//    noticeChannelService.delete(channel.getId());
//    System.out.println("删除渠道成功");
//  }
//
//  @Test
//  public void testTemplateCRUD() {
//    // 测试模板CRUD操作
//    NoticeTemplate template = new NoticeTemplate();
//    template.setName("测试告警模板");
//    template.setChannelType("dingTalk");
//    template.setChannelId(1L);
//    template.setContent("设备${deviceName}发生${alertLevel}级别告警：${alertMessage}");
//    template.setReceivers("[\"test_group\"]");
//    template.setStatus("1");
//    template.setRemark("测试模板");
//    template.setCreator("test");
//
//    // 保存
//    noticeTemplateService.save(template);
//    System.out.println("保存模板成功，ID: " + template.getId());
//
//    // 查询
//    NoticeTemplate saved = noticeTemplateService.getById(template.getId());
//    System.out.println("查询模板: " + saved.getName());
//
//    // 搜索
//    List<NoticeTemplate> list = noticeTemplateService.search("测试", "dingTalk", "1");
//    System.out.println("搜索到 " + list.size() + " 个模板");
//
//    // 删除
//    noticeTemplateService.delete(template.getId());
//    System.out.println("删除模板成功");
//  }
//
//  @Test
//  public void testTemplateUtil() {
//    // 测试模板工具类
//    String template = "设备${deviceName}发生${alertLevel}级别告警：${alertMessage}，时间：${alertTime}";
//
//    Map<String, Object> params = new HashMap<>();
//    params.put("deviceName", "温度传感器001");
//    params.put("alertLevel", "严重");
//    params.put("alertMessage", "温度过高报警");
//    params.put("alertTime", "2024-01-15 14:30:25");
//
//    String result = NoticeTemplateUtil.replaceParams(template, params);
//    System.out.println("模板替换结果: " + result);
//  }
//
//  @Test
//  public void testTemplateTest() {
//    // 测试模板测试功能
//    Map<String, Object> params = new HashMap<>();
//    params.put("deviceName", "温度传感器001");
//    params.put("alertLevel", "严重");
//    params.put("alertMessage", "温度过高报警");
//    params.put("alertTime", "2024-01-15 14:30:25");
//
//    noticeTemplateService.testTemplate(1L, "test_group", params);
//  }
// }
