/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import cn.universal.common.poi.ExcelTemplate;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.base.ModbusPollingExecutor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.IoTDeviceDeletePreview;
import cn.universal.persistence.dto.IoTDeviceExportDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDept;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceOwnerBO;
import cn.universal.persistence.entity.bo.IoTGwDeviceBO;
import cn.universal.persistence.entity.bo.IoTSubDeviceGatewayBindBO;
import cn.universal.persistence.entity.vo.GwIoTDeviceVo;
import cn.universal.persistence.entity.vo.IoTDeviceCompanyVO;
import cn.universal.persistence.entity.vo.IoTDeviceCountVO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTDeviceVO;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceProtocolMapper;
import cn.universal.persistence.mapper.IoTDeviceShadowMapper;
import cn.universal.persistence.mapper.IoTDeviceTagsMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.support.DeptDataPermissionSupport.DeptDataPermissionScope;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 设备Service业务层处理
 *
 * @since 2025-12-24
 */
@Service
@Slf4j
public class IoTDeviceServiceImpl extends BaseServiceImpl implements IIoTDeviceService {

  private static final String IMPORT_ROW_NO_KEY = "importRowNo";
  private static final String EXPORT_ROW_NO_FIELD_KEY = "__rowNo";
  private static final String FAIL_REASON_FIELD_KEY = "__errorMessage";
  private static final String PRODUCT_CUSTOM_FIELD_KEY = "customField";
  private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.:-]{1,64}$");

  @Value("${iot.register.auto.unionId:auto_register}")
  private String autoRegisterUnionId;

  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private JdbcTemplate jdbcTemplate;

  @Resource private IoTDeviceProtocolMapper ioTDeviceProtocolMapper;

  @Resource private IoTDeviceShadowMapper ioTDeviceShadowMapper;

  @Resource private IoTDeviceShadowService ioTDeviceShadowService;

  @Autowired(required = false)
  private ModbusPollingExecutor modbusPollingExecutor;

  @Resource private IoTDeviceTagsMapper ioTDeviceTagsMapper;

  @Resource private IoTDeviceFenceRelMapper ioTDeviceFenceRelMapper;

  @Resource private cn.universal.dm.device.service.impl.IoTDeviceService deviceDomainService;

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  @Resource private IoTUserService ioTUserService;

  @Resource private ISysDeptService sysDeptService;

  @Override
  @Cacheable(cacheNames = "iot_dev_tag_group_name", key = "#iotId")
  public List<String> selectDevInstanceGroupName(String iotId) {
    return ioTDeviceTagsMapper.selectDevGroupName(iotId);
  }

  /**
   * 查询设备
   *
   * @param id 设备主键
   * @return 设备
   */
  @Override
  public IoTDevice selectDevInstanceById(String id) {
    return selectDevInstanceById(id, null);
  }

  @Override
  public IoTDevice selectDevInstanceById(String id, IoTUser scopeUser) {
    IoTDevice ioTDevice = ioTDeviceMapper.selectDevInstanceById(id);
    if (Objects.isNull(ioTDevice)) {
      throw new IoTException("该设备不存在", 400);
    }
    IoTUser operator = scopeUser != null ? scopeUser : resolveFallbackScopeUser();
    if (isPendingOwnerDevice(ioTDevice)) {
      assertPendingOwnerProductAccess(ioTDevice, operator);
    } else {
      assertDeviceAccess(ioTDevice, operator, "你无权操作");
    }

    if (ioTDevice.getCoordinate() != null) {
      ioTDevice.getParams().put("locationStr", ioTDevice.getCoordinate());
    }
    markDeviceOwnerStatus(ioTDevice);
    return ioTDevice;
  }

  /** 设备归属判断 */
  public String ascription(String creatorId) {
    return ascription(creatorId, null);
  }

  /** 设备归属判断，支持后台任务回退到指定操作人 */
  public String ascription(String creatorId, String fallbackUnionId) {
    String operatorUnionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(operatorUnionId)) {
      operatorUnionId = fallbackUnionId;
    }
    if (StrUtil.isBlank(operatorUnionId)) {
      throw new IoTException("无法识别当前操作用户");
    }
    IoTUser iotUser = queryIotUser(operatorUnionId);
    if (iotUser == null) {
      throw new IoTException("操作用户不存在或已失效");
    }
    if (!creatorId.equals(iotUser.getUnionId()) && !iotUser.isAdmin() && !isTenantAdmin(iotUser)) {
      throw new IoTException("您没有权限操作此设备！");
    }
    return iotUser.getUnionId();
  }

  private String resolveDeviceAppUnionId(IoTDevice ioTDevice, String fallbackUnionId) {
    String operatorUnionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(operatorUnionId)) {
      operatorUnionId = fallbackUnionId;
    }
    if (StrUtil.isBlank(operatorUnionId)) {
      throw new IoTException("无法识别当前操作用户");
    }
    IoTUser operator = ioTUserService.selectUserByUnionId(operatorUnionId);
    if (operator == null) {
      throw new IoTException("操作用户不存在或已失效");
    }
    assertDeviceAccess(ioTDevice, operator, "您没有权限操作此设备！");
    IoTUser tenantOwner = queryIotUser(operator.getUnionId());
    if (tenantOwner == null) {
      throw new IoTException("操作用户不存在或已失效");
    }
    return tenantOwner.getUnionId();
  }

  /**
   * 查询每个应用的设备数量
   *
   * @param appUniqueId 应用唯一标识
   */
  @Override
  public List<IoTDeviceCountVO> countDevByApplication(String appUniqueId) {
    return ioTDeviceMapper.countDevByApplication(appUniqueId);
  }

  /**
   * 查询设备列表
   *
   * @param ioTDevice 设备
   * @return 设备
   */
  @Override
  //  @DataScope(alias = "u", created = "d.creator_id")
  public List<IoTDevice> selectDevInstanceList(IoTDevice ioTDevice, IoTUser iotUser) {
    applyDeviceDataScope(ioTDevice, iotUser);
    allowPendingOwnerVisible(ioTDevice);
    List<IoTDevice> list = ioTDeviceMapper.selectDevInstanceList(ioTDevice, null);
    markDeviceOwnerStatus(list);
    return list;
  }

  @Override
  public List<IoTDevice> selectDevInstanceListByPlatform(
      IoTDevice ioTDevice, IoTUser iotUser, String thirdPlatform) {
    applyDeviceDataScope(ioTDevice, iotUser);
    allowPendingOwnerVisible(ioTDevice);
    List<IoTDevice> list =
        ioTDeviceMapper.selectDevInstanceListByPlatform(ioTDevice, thirdPlatform, null);
    markDeviceOwnerStatus(list);
    return list;
  }

  /**
   * 查询设备列表
   *
   * @param ioTDevice 设备
   * @return 设备
   */
  @Override
  //  @DataScope(alias = "u", created = "d.creator_id")
  public List<IoTDevice> selectDevInstanceUnList(IoTDevice ioTDevice, IoTUser iotUser) {
    applyDeviceDataScope(ioTDevice, iotUser);
    return ioTDeviceMapper.selectDevInstanceUnList(ioTDevice, null);
  }

  @Override
  public List<IoTDevice> selectDevInstanceForExport(IoTDevice ioTDevice, IoTUser iotUser) {
    applyDeviceDataScope(ioTDevice, iotUser);
    return ioTDeviceMapper.selectDevInstanceForExport(ioTDevice, null);
  }

  @Override
  public List<IoTDeviceExportDTO> selectDeviceForExport(IoTDevice ioTDevice, IoTUser iotUser) {
    log.debug(
        "[设备导出] 查询设备数据: ioTDevice={}, unionId={}",
        ioTDevice,
        iotUser.isAdmin() ? "管理员" : iotUser.getUnionId());

    applyDeviceDataScope(ioTDevice, iotUser);
    List<IoTDeviceExportDTO> exportList = ioTDeviceMapper.selectDeviceForExport(ioTDevice, null);

    log.info("[设备导出] 查询完成,共{}条数据", exportList != null ? exportList.size() : 0);
    return exportList;
  }

  @Override
  public ExcelTemplate importTemplateExcel(String productKey) {
    return importTemplateExcel(productKey, false);
  }

  @Override
  public ExcelTemplate importTemplateExcel(
      String productKey, boolean includeIndustrialDifferences) {
    // 根据produceKey获取产品
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(productKey);
    if (Objects.isNull(ioTProduct)) {
      throw new BaseException("没有找到产品");
    }
    JSONObject config = parseProductConfiguration(ioTProduct);

    Map<String, String> headers = new LinkedMap<>();
    List<String> demoRow = new ArrayList<>();
    List<String> guideHeaders = new ArrayList<>();
    List<List<String>> guideRows = new ArrayList<>();

    guideHeaders.add("字段名称");
    guideHeaders.add("字段标识");
    guideHeaders.add("是否必填");
    guideHeaders.add("字段类型");
    guideHeaders.add("可选值");
    guideHeaders.add("示例");
    guideHeaders.add("说明");

    List<ImportTemplateField> templateFields = buildImportTemplateFields(ioTProduct, config);
    if (includeIndustrialDifferences) {
      templateFields.addAll(buildIndustrialImportTemplateFields());
    }
    for (ImportTemplateField field : templateFields) {
      headers.put(field.getFieldKey(), field.getHeaderName());
      demoRow.add(field.getExample());
      guideRows.add(
          buildGuideRow(
              field.getFieldName(),
              field.getFieldKey(),
              field.getRequired(),
              field.getFieldType(),
              field.getOptions(),
              field.getExample(),
              field.getDescription()));
    }

    if (DeviceNode.GATEWAY_SUB_DEVICE.name().equals(ioTProduct.getDeviceNode())) {
      guideRows.add(
          buildGuideRow(
              "所属网关设备", "界面选择", "是", "gateway", "-", "-", "网关子设备导入时，在页面上选择所属网关即可，无需在Excel中填写"));
    }
    return new ExcelTemplate(headers, demoRow, "设备导入", guideHeaders, guideRows);
  }

  private List<ImportTemplateField> buildIndustrialImportTemplateFields() {
    return List.of(
        new ImportTemplateField(
            "otherParams.industrialEndpointMode",
            "端点模式",
            "端点模式",
            "否",
            "enum",
            "INDEPENDENT,SHARED",
            "",
            "留空继承所选工业模板；独立PLC选择INDEPENDENT，多设备复用网关或OPC UA Server选择SHARED"),
        new ImportTemplateField(
            "otherParams.industrialEndpointKey",
            "独立端点Key",
            "独立端点Key",
            "否",
            "string",
            "-",
            "",
            "独立端点留空时按产品、设备和模板端点引用自动生成"),
        new ImportTemplateField(
            "otherParams.industrialSharedEndpointKey",
            "共享端点Key",
            "共享端点Key",
            "条件",
            "string",
            "-",
            "",
            "留空继承模板源共享端点；填写时复用指定端点，不存在则由首台设备创建"),
        new ImportTemplateField(
            "otherParams.industrialEndpointKeys",
            "多端点Key映射(JSON)",
            "多端点Key映射",
            "条件",
            "json",
            "-",
            "",
            "多端点模板使用；按模板端点引用映射目标端点Key，例如 {\"source-endpoint\":\"target-endpoint\"}"),
        new ImportTemplateField(
            "otherParams.industrialEndpointName",
            "端点名称",
            "端点名称",
            "否",
            "string",
            "-",
            "",
            "覆盖模板端点名称"),
        new ImportTemplateField(
            "otherParams.industrialHost",
            "采集主机/IP",
            "采集主机/IP",
            "否",
            "string",
            "-",
            "",
            "覆盖模板连接配置中的host"),
        new ImportTemplateField(
            "otherParams.industrialPort",
            "采集端口",
            "采集端口",
            "否",
            "integer",
            "1-65535",
            "",
            "覆盖模板连接配置中的port"),
        new ImportTemplateField(
            "otherParams.industrialEndpointUrl",
            "OPC UA Endpoint URL",
            "OPC UA Endpoint URL",
            "否",
            "string",
            "-",
            "",
            "覆盖模板连接配置中的endpointUrl"),
        new ImportTemplateField(
            "otherParams.industrialSlaveAddress",
            "从站地址",
            "从站地址",
            "否",
            "integer",
            "0-255",
            "",
            "覆盖模板连接配置中的slaveAddress"),
        new ImportTemplateField(
            "otherParams.industrialPointCodePrefix",
            "点位编码前缀",
            "点位编码前缀",
            "否",
            "string",
            "-",
            "",
            "共享端点留空时自动使用设备编号前缀"),
        new ImportTemplateField(
            "otherParams.industrialGroupCodePrefix",
            "采集组编码前缀",
            "采集组编码前缀",
            "否",
            "string",
            "-",
            "",
            "共享端点留空时自动使用设备编号前缀"),
        new ImportTemplateField(
            "otherParams.industrialEndpointOverrides",
            "端点配置覆盖(JSON)",
            "端点配置覆盖",
            "否",
            "json",
            "-",
            "",
            "高级入口；JSON对象会在host/port等差异列之后合并"));
  }

  @Override
  public List<IoTDevice> parseImportExcel(IoTProduct ioTProduct, InputStream inputStream)
      throws Exception {
    try (Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        throw new BaseException("文件sheet不存在");
      }

      Map<Integer, String> fieldKeyMap = resolveImportFieldKeyMap(sheet.getRow(0));
      if (fieldKeyMap.isEmpty()) {
        throw new BaseException("导入模板格式不正确，请重新下载模板后填写");
      }

      DataFormatter formatter = new DataFormatter();
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      List<IoTDevice> devicesImport = new ArrayList<>();
      int dataStartRow = resolveImportDataStartRow(sheet, fieldKeyMap, formatter, evaluator);
      for (int rowIndex = dataStartRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (isImportRowEmpty(row, fieldKeyMap, formatter, evaluator)) {
          continue;
        }
        IoTDevice device = buildImportDevice(row, rowIndex + 1, fieldKeyMap, formatter, evaluator);
        if (isTemplateDemoRow(device)) {
          continue;
        }
        devicesImport.add(device);
      }
      validImportDevice(devicesImport);
      return devicesImport;
    }
  }

  @Override
  public void exportFailedImportTemplate(
      String productKey,
      String taskName,
      List<IoTTaskItem> failedItems,
      HttpServletResponse response)
      throws Exception {
    if (CollectionUtil.isEmpty(failedItems)) {
      throw new BaseException("当前任务没有失败记录");
    }

    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(productKey);
    if (Objects.isNull(ioTProduct)) {
      throw new BaseException("没有找到产品");
    }

    JSONObject config = parseProductConfiguration(ioTProduct);
    List<ImportTemplateField> templateFields = buildImportTemplateFields(ioTProduct, config);
    SXSSFWorkbook workbook = new SXSSFWorkbook(100);
    try {
      Sheet sheet = workbook.createSheet(buildFailedSheetName(taskName));
      CellStyle hiddenHeaderStyle = createHiddenHeaderStyle(workbook);
      CellStyle rowNoHeaderStyle =
          createHeaderStyle(workbook, IndexedColors.BLUE_GREY, IndexedColors.WHITE);
      CellStyle headerStyle =
          createHeaderStyle(workbook, IndexedColors.GREY_50_PERCENT, IndexedColors.WHITE);
      CellStyle errorHeaderStyle =
          createHeaderStyle(workbook, IndexedColors.RED, IndexedColors.WHITE);
      CellStyle dataStyle = createDataStyle(workbook);
      CellStyle errorCellStyle = createErrorCellStyle(workbook);

      Row keyRow = sheet.createRow(0);
      keyRow.setHeight((short) 0);
      Row headerRow = sheet.createRow(1);

      int columnIndex = 0;
      writeCell(keyRow, columnIndex, EXPORT_ROW_NO_FIELD_KEY, hiddenHeaderStyle);
      writeCell(headerRow, columnIndex, "原Excel行号", rowNoHeaderStyle);
      sheet.setColumnWidth(columnIndex++, 14 * 256);

      for (ImportTemplateField field : templateFields) {
        writeCell(keyRow, columnIndex, field.getFieldKey(), hiddenHeaderStyle);
        writeCell(headerRow, columnIndex, field.getHeaderName(), headerStyle);
        sheet.setColumnWidth(columnIndex, resolveColumnWidth(field.getHeaderName(), false));
        columnIndex++;
      }

      writeCell(keyRow, columnIndex, FAIL_REASON_FIELD_KEY, hiddenHeaderStyle);
      writeCell(headerRow, columnIndex, "失败原因", errorHeaderStyle);
      sheet.setColumnWidth(columnIndex, resolveColumnWidth("失败原因", true));

      int rowIndex = 2;
      for (IoTTaskItem failedItem : failedItems) {
        Row dataRow = sheet.createRow(rowIndex++);
        int dataColumn = 0;
        writeCell(
            dataRow,
            dataColumn++,
            failedItem.getRowNo() == null ? "" : String.valueOf(failedItem.getRowNo()),
            dataStyle);

        IoTDevice rawDevice = parseImportDeviceRawData(failedItem.getRawData());
        for (ImportTemplateField field : templateFields) {
          writeCell(
              dataRow,
              dataColumn++,
              resolveExportFieldValue(rawDevice, field.getFieldKey()),
              dataStyle);
        }
        writeCell(
            dataRow,
            dataColumn,
            StrUtil.blankToDefault(failedItem.getMessage(), "导入失败"),
            errorCellStyle);
      }

      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setCharacterEncoding("utf-8");
      OutputStream outputStream = response.getOutputStream();
      workbook.write(outputStream);
      outputStream.flush();
    } finally {
      workbook.dispose();
      workbook.close();
    }
  }

  private List<String> buildGuideRow(
      String fieldName,
      String fieldKey,
      String required,
      String fieldType,
      String options,
      String example,
      String description) {
    List<String> row = new ArrayList<>();
    row.add(fieldName);
    row.add(fieldKey);
    row.add(required);
    row.add(fieldType);
    row.add(options);
    row.add(example);
    row.add(description);
    return row;
  }

  private IoTDevice parseImportDeviceRawData(String rawData) {
    JSONObject rawJson = JSONUtil.parseObj(StrUtil.blankToDefault(rawData, "{}"));
    IoTDevice device = JSONUtil.toBean(rawJson, IoTDevice.class);
    device.setLongitude(rawJson.getStr("longitude"));
    device.setLatitude(rawJson.getStr("latitude"));
    return device;
  }

  private JSONObject parseProductConfiguration(IoTProduct ioTProduct) {
    return StrUtil.isBlank(ioTProduct.getThirdConfiguration())
        ? new JSONObject()
        : JSONUtil.parseObj(ioTProduct.getThirdConfiguration());
  }

  private Map<Integer, String> resolveImportFieldKeyMap(Row keyRow) {
    Map<Integer, String> fieldKeyMap = new LinkedHashMap<>();
    if (keyRow == null || keyRow.getLastCellNum() < 0) {
      return fieldKeyMap;
    }
    for (int columnIndex = 0; columnIndex < keyRow.getLastCellNum(); columnIndex++) {
      Cell cell = keyRow.getCell(columnIndex);
      if (cell == null) {
        continue;
      }
      String fieldKey = StrUtil.trim(cell.toString());
      if (StrUtil.isNotBlank(fieldKey)) {
        fieldKeyMap.put(columnIndex, fieldKey);
      }
    }
    return fieldKeyMap;
  }

  private int resolveImportDataStartRow(
      Sheet sheet,
      Map<Integer, String> fieldKeyMap,
      DataFormatter formatter,
      FormulaEvaluator evaluator) {
    Row demoRow = sheet.getRow(2);
    if (isImportRowEmpty(demoRow, fieldKeyMap, formatter, evaluator)) {
      return 2;
    }
    IoTDevice exampleDevice = buildImportDevice(demoRow, 3, fieldKeyMap, formatter, evaluator);
    return isTemplateDemoRow(exampleDevice) ? 3 : 2;
  }

  private boolean isImportRowEmpty(
      Row row,
      Map<Integer, String> fieldKeyMap,
      DataFormatter formatter,
      FormulaEvaluator evaluator) {
    if (row == null || fieldKeyMap.isEmpty()) {
      return true;
    }
    for (Map.Entry<Integer, String> entry : fieldKeyMap.entrySet()) {
      String fieldKey = entry.getValue();
      if (StrUtil.isBlank(fieldKey) || isTemplateMetaFieldKey(fieldKey)) {
        continue;
      }
      if (StrUtil.isNotBlank(readCellValue(row.getCell(entry.getKey()), formatter, evaluator))) {
        return false;
      }
    }
    return true;
  }

  private IoTDevice buildImportDevice(
      Row row,
      int excelRowNo,
      Map<Integer, String> fieldKeyMap,
      DataFormatter formatter,
      FormulaEvaluator evaluator) {
    IoTDevice device = new IoTDevice();
    Map<String, Object> otherParams = new LinkedHashMap<>();
    for (Map.Entry<Integer, String> entry : fieldKeyMap.entrySet()) {
      String fieldKey = entry.getValue();
      if (StrUtil.isBlank(fieldKey) || isTemplateMetaFieldKey(fieldKey)) {
        continue;
      }
      String cellValue = readCellValue(row.getCell(entry.getKey()), formatter, evaluator);
      if (StrUtil.startWith(fieldKey, "otherParams.")) {
        String customFieldKey = StrUtil.subAfter(fieldKey, ".", true);
        if (StrUtil.isNotBlank(customFieldKey) && StrUtil.isNotBlank(cellValue)) {
          otherParams.put(customFieldKey, cellValue);
        }
        continue;
      }
      applyImportFieldValue(device, fieldKey, cellValue);
    }
    device.setOtherParams(otherParams);
    device.getParams().put(IMPORT_ROW_NO_KEY, excelRowNo);
    return device;
  }

  private boolean isTemplateMetaFieldKey(String fieldKey) {
    return StrUtil.startWith(fieldKey, "__");
  }

  private void applyImportFieldValue(IoTDevice device, String fieldKey, String cellValue) {
    String normalizedValue = StrUtil.trim(cellValue);
    switch (fieldKey) {
      case "deviceId":
        device.setDeviceId(normalizedValue);
        break;
      case "deviceName":
        device.setDeviceName(normalizedValue);
        break;
      case "longitude":
        device.setLongitude(normalizedValue);
        break;
      case "latitude":
        device.setLatitude(normalizedValue);
        break;
      case "detail":
        device.setDetail(normalizedValue);
        break;
      default:
        break;
    }
  }

  private String readCellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
    if (cell == null) {
      return null;
    }
    String value = formatter.formatCellValue(cell, evaluator);
    return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
  }

  private boolean isTemplateDemoRow(IoTDevice device) {
    return StrUtil.equals(StrUtil.trim(device.getDetail()), "请把此行删除");
  }

  private String buildFailedSheetName(String taskName) {
    String baseName = StrUtil.blankToDefault(StrUtil.trim(taskName), "导入失败明细");
    baseName = baseName.replaceAll("[\\\\/:*?\\[\\]]", "_");
    return baseName.length() > 31 ? baseName.substring(0, 31) : baseName;
  }

  private CellStyle createHiddenHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    return style;
  }

  private CellStyle createHeaderStyle(
      Workbook workbook, IndexedColors backgroundColor, IndexedColors fontColor) {
    CellStyle style = workbook.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    style.setFillForegroundColor(backgroundColor.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 10);
    font.setBold(true);
    font.setColor(fontColor.getIndex());
    style.setFont(font);
    return style;
  }

  private CellStyle createDataStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setWrapText(true);
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    return style;
  }

  private CellStyle createErrorCellStyle(Workbook workbook) {
    CellStyle style = createDataStyle(workbook);
    style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private void writeCell(Row row, int columnIndex, String value, CellStyle cellStyle) {
    Cell cell = row.createCell(columnIndex);
    cell.setCellValue(StrUtil.blankToDefault(value, ""));
    if (cellStyle != null) {
      cell.setCellStyle(cellStyle);
    }
  }

  private int resolveColumnWidth(String headerName, boolean wide) {
    if (wide) {
      return 48 * 256;
    }
    int baseWidth = Math.max(StrUtil.length(StrUtil.blankToDefault(headerName, "")), 12);
    return Math.min(baseWidth * 2 * 256, 28 * 256);
  }

  private String resolveExportFieldValue(IoTDevice device, String fieldKey) {
    if (device == null || StrUtil.isBlank(fieldKey)) {
      return "";
    }
    if (StrUtil.startWith(fieldKey, "otherParams.")) {
      String customFieldKey = StrUtil.subAfter(fieldKey, ".", true);
      Object value =
          device.getOtherParams() == null ? null : device.getOtherParams().get(customFieldKey);
      return value == null ? "" : String.valueOf(value);
    }
    switch (fieldKey) {
      case "deviceId":
        return StrUtil.blankToDefault(device.getDeviceId(), "");
      case "deviceName":
        return StrUtil.blankToDefault(device.getDeviceName(), "");
      case "longitude":
        return StrUtil.blankToDefault(device.getLongitude(), "");
      case "latitude":
        return StrUtil.blankToDefault(device.getLatitude(), "");
      case "detail":
        return StrUtil.blankToDefault(device.getDetail(), "");
      default:
        return "";
    }
  }

  private List<ImportTemplateField> buildImportTemplateFields(
      IoTProduct ioTProduct, JSONObject config) {
    List<ImportTemplateField> fields = new ArrayList<>();
    JSONArray customFieldArray = config.getJSONArray("customField");
    JSONObject autoGenerateField = getAutoGenerateField(customFieldArray);
    boolean gatewaySubDevice =
        DeviceNode.GATEWAY_SUB_DEVICE.name().equals(ioTProduct.getDeviceNode());
    boolean deviceIdCanAutoGenerate = gatewaySubDevice && autoGenerateField != null;

    String autoGenerateFieldName =
        autoGenerateField == null
            ? "自定义字段"
            : autoGenerateField.getStr("name", autoGenerateField.getStr("id", "自定义字段"));
    String deviceIdDescription = "设备唯一标识，导入时必须填写";
    if (deviceIdCanAutoGenerate) {
      deviceIdDescription = "如已在导入页面选择所属网关设备，可留空，系统会根据网关设备ID和" + autoGenerateFieldName + "自动生成";
    }

    fields.add(
        new ImportTemplateField(
            "deviceId",
            buildHeaderName("设备序列号", deviceIdCanAutoGenerate ? "条件" : "是"),
            "设备序列号",
            deviceIdCanAutoGenerate ? "条件" : "是",
            "string",
            "-",
            deviceIdCanAutoGenerate ? "" : "DL299NEXIOT",
            deviceIdDescription));
    fields.add(
        new ImportTemplateField(
            "deviceName",
            buildHeaderName("设备名称", "是"),
            "设备名称",
            "是",
            "string",
            "-",
            "导入设备",
            "导入后展示的设备名称"));
    fields.add(
        new ImportTemplateField(
            "longitude", "经度", "经度", "否", "number", "-", "81.221842", "为空时系统会补默认值"));
    fields.add(
        new ImportTemplateField(
            "latitude", "纬度", "纬度", "否", "number", "-", "33.358114", "为空时系统会补默认值"));
    fields.add(
        new ImportTemplateField(
            "detail", "备注", "备注", "否", "string", "-", "请把此行删除", "模板示例行建议删除后再导入"));

    if (CollectionUtil.isNotEmpty(customFieldArray)) {
      for (int i = 0; i < customFieldArray.size(); i++) {
        JSONObject field = customFieldArray.getJSONObject(i);
        if (field == null) {
          continue;
        }
        String fieldId = field.getStr("id");
        if (StrUtil.isBlank(fieldId)) {
          continue;
        }
        String required = resolveCustomFieldRequired(field);
        String fieldName = field.getStr("name", fieldId);
        fields.add(
            new ImportTemplateField(
                "otherParams." + fieldId,
                buildHeaderName(fieldName, required),
                fieldName,
                required,
                field.getStr("type", "string"),
                resolveCustomFieldOptions(field),
                resolveCustomFieldExample(field),
                resolveCustomFieldDescription(field)));
      }
    }
    return fields;
  }

  private String buildHeaderName(String fieldName, String required) {
    if ("是".equals(required)) {
      return fieldName + "（必填）";
    }
    if ("条件".equals(required)) {
      return fieldName + "（条件必填）";
    }
    return fieldName;
  }

  private JSONObject getAutoGenerateField(JSONArray customFieldArray) {
    if (CollectionUtil.isEmpty(customFieldArray)) {
      return null;
    }
    for (int i = 0; i < customFieldArray.size(); i++) {
      JSONObject field = customFieldArray.getJSONObject(i);
      if (field != null && field.getBool("autoGenerate", false)) {
        return field;
      }
    }
    return null;
  }

  private String resolveCustomFieldRequired(JSONObject field) {
    if (field == null || !field.getBool("required", false)) {
      return "否";
    }
    return field.containsKey("visibleWhen") ? "条件" : "是";
  }

  private String resolveCustomFieldOptions(JSONObject field) {
    JSONArray optionArray = field.getJSONArray("options");
    if (CollectionUtil.isEmpty(optionArray)) {
      return "-";
    }
    List<String> optionValues = new ArrayList<>();
    for (int i = 0; i < optionArray.size(); i++) {
      JSONObject option = optionArray.getJSONObject(i);
      if (option == null) {
        continue;
      }
      String value = StrUtil.trim(option.getStr("value", ""));
      String label = StrUtil.trim(option.getStr("label", ""));
      if (StrUtil.isNotBlank(value) && StrUtil.isNotBlank(label) && !StrUtil.equals(value, label)) {
        optionValues.add(value + "(" + label + ")");
      } else if (StrUtil.isNotBlank(value)) {
        optionValues.add(value);
      } else if (StrUtil.isNotBlank(label)) {
        optionValues.add(label);
      }
    }
    return CollectionUtil.isEmpty(optionValues) ? "-" : String.join(" / ", optionValues);
  }

  private String resolveCustomFieldExample(JSONObject field) {
    String explicitExample = stringifyTemplateValue(field.get("example"));
    if (StrUtil.isNotBlank(explicitExample)) {
      return explicitExample;
    }

    String defaultValue = stringifyTemplateValue(field.get("defaultValue"));
    if (StrUtil.isNotBlank(defaultValue)) {
      return defaultValue;
    }

    JSONArray optionArray = field.getJSONArray("options");
    if (CollectionUtil.isNotEmpty(optionArray)) {
      JSONObject firstOption = optionArray.getJSONObject(0);
      if (firstOption != null) {
        String optionValue = StrUtil.trim(firstOption.getStr("value", ""));
        if (StrUtil.isNotBlank(optionValue)) {
          return optionValue;
        }
        String optionLabel = StrUtil.trim(firstOption.getStr("label", ""));
        if (StrUtil.isNotBlank(optionLabel)) {
          return optionLabel;
        }
      }
    }

    if ("number".equalsIgnoreCase(field.getStr("type", "string"))) {
      Object min = field.get("min");
      if (ObjectUtil.isNotEmpty(min)) {
        return String.valueOf(min);
      }
      return "1";
    }

    String placeholder = StrUtil.trim(field.getStr("placeholder", ""));
    if (isTemplateExampleText(placeholder)) {
      return placeholder;
    }

    if ("password".equalsIgnoreCase(field.getStr("type", "string"))) {
      return "123456";
    }

    return field.getStr("name", field.getStr("id", "示例值")) + "示例";
  }

  private boolean isTemplateExampleText(String value) {
    if (StrUtil.isBlank(value)) {
      return false;
    }
    String trimmed = StrUtil.trim(value);
    return !StrUtil.startWithAny(trimmed, "请输入", "请填写", "请选择");
  }

  private String stringifyTemplateValue(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private String resolveCustomFieldDescription(JSONObject field) {
    List<String> descriptions = new ArrayList<>();
    String helpText = StrUtil.trim(field.getStr("helpText", ""));
    String description = StrUtil.trim(field.getStr("description", ""));
    if (StrUtil.isNotBlank(helpText)) {
      descriptions.add(helpText);
    } else if (StrUtil.isNotBlank(description)) {
      descriptions.add(description);
    }

    String constraintDescription = resolveCustomFieldConstraintDescription(field);
    if (StrUtil.isNotBlank(constraintDescription)) {
      descriptions.add(constraintDescription);
    }

    if (field.getBool("autoGenerate", false)
        && descriptions.stream().noneMatch(item -> StrUtil.contains(item, "自动生成"))) {
      descriptions.add("填写后用于自动生成设备序列号");
    }

    return CollectionUtil.isEmpty(descriptions) ? "按产品配置填写" : String.join("；", descriptions);
  }

  private String resolveCustomFieldConstraintDescription(JSONObject field) {
    String type = field.getStr("type", "string");
    List<String> constraints = new ArrayList<>();
    if ("number".equalsIgnoreCase(type)) {
      Object min = field.get("min");
      Object max = field.get("max");
      if (ObjectUtil.isNotEmpty(min) && ObjectUtil.isNotEmpty(max)) {
        constraints.add("取值范围：" + min + "-" + max);
      } else if (ObjectUtil.isNotEmpty(min)) {
        constraints.add("最小值：" + min);
      } else if (ObjectUtil.isNotEmpty(max)) {
        constraints.add("最大值：" + max);
      }
    } else {
      Object minLength = field.get("minLength");
      Object maxLength = field.get("maxLength");
      if (ObjectUtil.isNotEmpty(minLength) && ObjectUtil.isNotEmpty(maxLength)) {
        constraints.add("长度范围：" + minLength + "-" + maxLength + "个字符");
      } else if (ObjectUtil.isNotEmpty(minLength)) {
        constraints.add("最少" + minLength + "个字符");
      } else if (ObjectUtil.isNotEmpty(maxLength)) {
        constraints.add("最多" + maxLength + "个字符");
      }
    }

    String pattern = StrUtil.trim(field.getStr("pattern", ""));
    if (StrUtil.isNotBlank(pattern)) {
      constraints.add("格式校验：" + pattern);
    }
    return CollectionUtil.isEmpty(constraints) ? "" : String.join("；", constraints);
  }

  @Override
  public List<GwIoTDeviceVo> selectGwIotDevices(IoTGwDeviceBO ioTGwDeviceBO) {
    IoTProduct subDeviceProduct =
        ioTProductMapper.getProductByProductKey(ioTGwDeviceBO.getProductKey());
    if (ObjectUtil.isNull(subDeviceProduct)
        || !DeviceNode.GATEWAY_SUB_DEVICE.name().equals(subDeviceProduct.getDeviceNode())) {
      throw new IoTException("当前产品不是网关子设备产品");
    }
    assertTenantResourceAccess(
        subDeviceProduct.getTenantId(), subDeviceProduct.getCreatorId(), "无产品权限");
    ioTGwDeviceBO.setGwProductKey(StrUtil.blankToDefault(subDeviceProduct.getGwProductKey(), null));
    return ioTDeviceMapper.getGatewayDeviceList(ioTGwDeviceBO).stream()
        .filter(gateway -> isGatewayCandidateCompatible(subDeviceProduct, gateway))
        .toList();
  }

  private boolean isGatewayCandidateCompatible(IoTProduct subProduct, GwIoTDeviceVo gateway) {
    if (gateway == null) {
      return false;
    }
    IoTProduct gatewayProduct =
        IoTProduct.builder()
            .thirdPlatform(gateway.getThirdPlatform())
            .transportProtocol(gateway.getTransportProtocol())
            .build();
    return StrUtil.isBlank(validateSubDeviceGatewayCompatibility(subProduct, gatewayProduct));
  }

  @Override
  public List<GwIoTDeviceVo> selectSubDeviceGatewayCandidates(String id) {
    if (StrUtil.isBlank(id)) {
      throw new IoTException("设备ID不能为空");
    }
    IoTDevice subDevice = ioTDeviceMapper.selectDevInstanceById(id);
    if (subDevice == null) {
      throw new IoTException("设备不存在");
    }
    IoTUser operator = resolveFallbackScopeUser();
    assertDeviceAccess(subDevice, operator, "您没有权限操作此设备！");
    IoTProduct subProduct = ioTProductMapper.getProductByProductKey(subDevice.getProductKey());
    if (subProduct == null
        || !DeviceNode.GATEWAY_SUB_DEVICE.name().equals(subProduct.getDeviceNode())) {
      throw new IoTException("当前设备不是网关子设备");
    }

    IoTGwDeviceBO bo = new IoTGwDeviceBO();
    bo.setTenantId(subDevice.getTenantId());
    return ioTDeviceMapper.getGatewayDeviceList(bo).stream()
        .filter(gateway -> canAccessGatewayCandidate(gateway, operator))
        .filter(gateway -> isGatewayCandidateCompatible(subProduct, gateway))
        .toList();
  }

  private boolean canAccessGatewayCandidate(GwIoTDeviceVo gateway, IoTUser operator) {
    if (gateway == null) {
      return false;
    }
    return canAccessDeptResource(
        gateway.getTenantId(), gateway.getDeptId(), gateway.getCreatorId(), operator);
  }

  @Override
  public IoTDevice selectIoTDevice(String productKey, String deviceId) {
    IoTDevice device = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
    if (device != null) {
      assertDeviceAccess(device, resolveFallbackScopeUser(), "无设备权限");
    }
    return device;
  }

  @Override
  public List<IoTDevice> selectDevInstanceListWithTags(IoTDeviceBO ioTDeviceBO, IoTUser iotUser) {
    applyDeviceDataScope(ioTDeviceBO, iotUser);
    return ioTDeviceMapper.selectDevInstanceListWithTags(ioTDeviceBO, null);
  }

  // 导入设备
  @Override
  public int importDevice(List<IoTDevice> devicesImport, IoTProduct ioTProduct, String deviceId) {
    validImportDevice(devicesImport);
    int successImportNum = 0;
    List<String> errorMsgs = new ArrayList<>();

    for (int i = 0; i < devicesImport.size(); i++) {
      try {
        IoTDevice instance = devicesImport.get(i);
        validImportDevice(instance, ioTProduct, deviceId);
        IoTDeviceBO ioTDeviceBO = new IoTDeviceBO();
        BeanUtils.copyProperties(instance, ioTDeviceBO);
        ioTDeviceBO.setProductKey(ioTProduct.getProductKey());
        if (ioTDeviceBO.getOtherParams().containsKey("deviceKey")) {
          ioTDeviceBO.setSecretKey(ioTDeviceBO.getOtherParams().get("deviceKey").toString());
        }

        if (cn.universal.common.utils.StringUtils.isEmpty(ioTDeviceBO.getLatitude())) {
          ioTDeviceBO.setLatitude("44.61081");
        }
        if (cn.universal.common.utils.StringUtils.isEmpty(ioTDeviceBO.getLongitude())) {
          ioTDeviceBO.setLongitude("80.990372");
        }
        if (StrUtil.isNotBlank(deviceId)) {
          ioTDeviceBO.setGwProductKey(deviceId);
        }
        ioTDeviceBO.setCoordinate(ioTDeviceBO.getLongitude() + "," + ioTDeviceBO.getLatitude());
        R r = ((IIoTDeviceService) AopContext.currentProxy()).insertDevInstance(ioTDeviceBO);
        if (r.getMsg().equals("success") && r.getCode().equals(0)) {
          successImportNum++;
        } else {
          throw new BaseException(r.getMsg());
        }
      } catch (Exception e) {
        errorMsgs.add(String.format("第%s行导入报错,原因【%s】", i + 1, e.getMessage()));
      }
    }

    if (CollectionUtil.isNotEmpty(errorMsgs)) {
      throw new BaseException(StrUtil.join(",\n", errorMsgs));
    }
    return successImportNum;
  }

  private void validImportDevice(List<IoTDevice> devicesImport) {
    if (CollectionUtils.isEmpty(devicesImport)) {
      throw new BaseException("表格数据为空");
    }
    if (devicesImport.size() > 500) {
      throw new BaseException("超过一次最大导入数量500");
    }
  }

  private void validImportDevice(IoTDevice device, IoTProduct ioTProduct, String gwDeviceId) {
    JSONObject jsonObject =
        StrUtil.isBlank(ioTProduct.getThirdConfiguration())
            ? new JSONObject()
            : JSONUtil.parseObj(ioTProduct.getThirdConfiguration());

    if (StringUtils.isEmpty(device.getDeviceName())) {
      throw new BaseException("设备名称不能为空");
    }

    boolean autoGenerate = false;
    if (jsonObject.containsKey("customField")) {
      JSONArray customFieldArray = jsonObject.getJSONArray("customField");
      if (CollectionUtil.isNotEmpty(customFieldArray)) {
        for (int j = 0; j < customFieldArray.size(); j++) {
          JSONObject field = customFieldArray.getJSONObject(j);
          if (field == null) {
            continue;
          }
          String customFieldId = field.getStr("id");
          if (StrUtil.isBlank(customFieldId)) {
            continue;
          }
          String customFieldName = field.getStr("name", customFieldId);
          boolean required = field.getBool("required", false);
          if (field.getBool("autoGenerate", false)) {
            autoGenerate = true;
            required = true;
          }
          if (!isCustomFieldVisible(field, device.getOtherParams(), customFieldArray)) {
            continue;
          }
          Object fieldValue =
              device.getOtherParams() == null ? null : device.getOtherParams().get(customFieldId);
          if (required && (fieldValue == null || StrUtil.isBlank(String.valueOf(fieldValue)))) {
            throw new BaseException(customFieldName + "不能为空");
          }
        }
      }
    }

    if (!autoGenerate && StringUtils.isEmpty(device.getDeviceId())) {
      throw new BaseException("设备序列号不能为空");
    }
    if (autoGenerate && StrUtil.isBlank(gwDeviceId) && StringUtils.isEmpty(device.getDeviceId())) {
      throw new BaseException("自动生成设备ID需要先选择所属网关设备");
    }
  }

  private static final class ImportTemplateField {
    private final String fieldKey;
    private final String headerName;
    private final String fieldName;
    private final String required;
    private final String fieldType;
    private final String options;
    private final String example;
    private final String description;

    private ImportTemplateField(
        String fieldKey,
        String headerName,
        String fieldName,
        String required,
        String fieldType,
        String options,
        String example,
        String description) {
      this.fieldKey = fieldKey;
      this.headerName = headerName;
      this.fieldName = fieldName;
      this.required = required;
      this.fieldType = fieldType;
      this.options = options;
      this.example = example;
      this.description = description;
    }

    private String getFieldKey() {
      return fieldKey;
    }

    private String getHeaderName() {
      return headerName;
    }

    private String getFieldName() {
      return fieldName;
    }

    private String getRequired() {
      return required;
    }

    private String getFieldType() {
      return fieldType;
    }

    private String getOptions() {
      return options;
    }

    private String getExample() {
      return example;
    }

    private String getDescription() {
      return description;
    }
  }

  @Override
  public int bindApp(String ids, String appUniqueId) {
    String[] a = ids.split(",");
    int updated = ioTDeviceMapper.bindApp(a, appUniqueId);
    if (updated > 0) {
      for (IoTDevice device : ioTDeviceMapper.listByIds(a)) {
        IoTDeviceDTO deviceDTO = new IoTDeviceDTO();
        BeanUtils.copyProperties(device, deviceDTO);
        cacheInvalidationPublisher.publishDeviceChange(
            IoTDevicePostProcessor.Operation.UPDATE, deviceDTO);
      }
    }
    return updated;
  }

  /**
   * 新增设备
   *
   * @param devInstancebo 设备
   * @return 结果
   */
  @Override
  public R insertDevInstance(IoTDeviceBO devInstancebo) {
    String deviceIdError = validateNewDeviceId(devInstancebo);
    if (StrUtil.isNotBlank(deviceIdError)) {
      return R.error(deviceIdError);
    }
    JSONObject downRequest = new JSONObject();
    IoTUser currentActor = resolveCurrentActorUser();
    IoTUser tenantUser = resolveTenantScopedUser(currentActor, devInstancebo);
    if (tenantUser != null) {
      devInstancebo.setTenantId(resolveTenantId(tenantUser));
    }
    if (devInstancebo.getDeptId() == null) {
      if (currentActor != null && currentActor.getDeptId() != null) {
        devInstancebo.setDeptId(currentActor.getDeptId());
      } else if (tenantUser != null) {
        devInstancebo.setDeptId(tenantUser.getDeptId());
      }
    }
    validateDeviceDept(
        devInstancebo.getDeptId(),
        devInstancebo.getTenantId(),
        currentActor != null ? currentActor : tenantUser);

    // 异步导入任务没有登录上下文，优先使用显式传入的创建人ID。
    downRequest.set("appUnionId", resolveAppUnionId(devInstancebo));
    downRequest.set("applicationId", devInstancebo.getApplication());
    downRequest.set("application", devInstancebo.getApplication());
    downRequest.set("tenantId", devInstancebo.getTenantId());
    downRequest.set("deptId", devInstancebo.getDeptId());
    // 产品编号
    downRequest.set("productKey", devInstancebo.getProductKey());
    // 设备序列号
    downRequest.set("deviceId", devInstancebo.getDeviceId());
    // Lora密匙
    downRequest.set("deviceKey", devInstancebo.getSecretKey());
    // 添加
    downRequest.set("cmd", DownCmd.DEV_ADD.name());
    // 说明
    downRequest.set("detail", devInstancebo.getDetail());
    // 网关产品ProductKey
    downRequest.set("gwProductKey", devInstancebo.getGwProductKey());
    downRequest.set("gwDeviceId", devInstancebo.getGwDeviceId());
    downRequest.set("extDeviceId", devInstancebo.getExtDeviceId());
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(devInstancebo.getProductKey());
    JSONObject ob = new JSONObject();
    // 设备实例名称
    ob.set("deviceName", devInstancebo.getDeviceName());
    // 设备序列号
    ob.set("imei", devInstancebo.getDeviceId());
    // 维度
    ob.set("latitude", devInstancebo.getLatitude());
    // 经度
    ob.set("longitude", devInstancebo.getLongitude());
    // 子设备地址
    ob.set("slaveAddress", devInstancebo.getSlaveAddress());

    mergeDeviceConfigurationFromRequest(ob, devInstancebo, ioTProduct);
    // 添加额外参数
    if (devInstancebo.getOtherParams() != null) {
      for (Map.Entry<String, Object> entry : devInstancebo.getOtherParams().entrySet()) {
        ob.set(entry.getKey(), entry.getValue());
      }
    }
    downRequest.set("data", ob);
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  private String validateNewDeviceId(IoTDeviceBO devInstancebo) {
    if (devInstancebo == null) {
      return "设备参数不能为空";
    }
    String deviceId = StrUtil.trim(devInstancebo.getDeviceId());
    if (StrUtil.isBlank(deviceId)) {
      return "设备序列号不能为空";
    }
    if (!DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
      return "设备序列号格式不合法，仅支持1-64位英文字母、数字、下划线、点、冒号或中横线";
    }
    devInstancebo.setDeviceId(deviceId);
    return null;
  }

  private String resolveAppUnionId(IoTDeviceBO devInstancebo) {
    String unionId = devInstancebo == null ? null : StrUtil.trim(devInstancebo.getCreatorId());
    if (StrUtil.isBlank(unionId) && SecurityUtils.getAuthentication() != null) {
      unionId = SecurityUtils.getUnionId();
    }
    if (StrUtil.isBlank(unionId)) {
      throw new BaseException("无法获取操作用户信息");
    }
    IoTUser iotUser = queryIotUser(unionId);
    if (iotUser == null) {
      throw new BaseException("操作用户不存在");
    }
    return iotUser.getUnionId();
  }

  /**
   * 修改设备
   *
   * @param devInstancebo 设备
   * @return 结果
   */
  @Override
  public R updateDevInstance(IoTDeviceBO devInstancebo) {
    IoTDevice ioTDevice = ioTDeviceMapper.selectByPrimaryKey(devInstancebo.getId());
    if (ioTDevice == null) {
      return R.error("设备不存在");
    }
    IoTUser operator = resolveFallbackScopeUser();
    assertDeviceAccess(ioTDevice, operator, "您没有权限操作此设备！");
    String targetTenantId = resolveDeviceTenantId(ioTDevice, operator);
    Long targetDeptId =
        devInstancebo.getDeptId() != null ? devInstancebo.getDeptId() : ioTDevice.getDeptId();
    validateDeviceDept(targetDeptId, targetTenantId, operator);
    String appUnionId =
        resolveDeviceAppUnionId(ioTDevice, operator == null ? null : operator.getUnionId());
    JSONObject downRequest = new JSONObject();
    downRequest.set("appUnionId", appUnionId);
    downRequest.set("applicationId", devInstancebo.getApplication());
    downRequest.set("application", devInstancebo.getApplication());
    downRequest.set("productKey", ioTDevice.getProductKey());
    downRequest.set("deviceId", ioTDevice.getDeviceId());
    downRequest.set("tenantId", targetTenantId);
    downRequest.set("deptId", targetDeptId);
    downRequest.set("detail", devInstancebo.getDetail());
    downRequest.set("deviceAddress", devInstancebo.getDeviceAddress());
    downRequest.set("slaveAddress", devInstancebo.getSlaveAddress());
    downRequest.set("cmd", DownCmd.DEV_UPDATE.name());
    JSONObject ob = new JSONObject();
    ob.set("deviceName", devInstancebo.getDeviceName());
    ob.set("latitude", devInstancebo.getLatitude());
    ob.set("slaveAddress", devInstancebo.getSlaveAddress());
    ob.set("longitude", devInstancebo.getLongitude());
    ob.set("deviceAddress", devInstancebo.getDeviceAddress());
    mergeDeviceConfigurationFromRequest(ob, devInstancebo, ioTDevice);
    downRequest.set("data", ob);
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);
    R result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
    if (result != null && result.isSuccess()) {
      IoTDevice latestDevice = ioTDeviceMapper.selectByPrimaryKey(ioTDevice.getId());
      if (latestDevice == null) {
        latestDevice = ioTDevice;
      }
      IoTDevice updateEntity =
          IoTDevice.builder().id(ioTDevice.getId()).deptId(targetDeptId).build();
      if (StrUtil.isNotBlank(targetTenantId)
          && !StrUtil.equals(targetTenantId, latestDevice.getTenantId())) {
        updateEntity.setTenantId(targetTenantId);
      }
      // SIP 插件已合并配置并保留空值/脱敏密码，避免再次用界面返回值覆盖凭据。
      if (!"sip".equalsIgnoreCase(ioTProduct.getThirdPlatform())) {
        mergeDeviceConfigurationFromRequest(updateEntity, devInstancebo, latestDevice);
      }
      if (updateEntity.getDeptId() != null
          || updateEntity.getTenantId() != null
          || updateEntity.getConfiguration() != null) {
        ioTDeviceMapper.updateByPrimaryKeySelective(updateEntity);
      }
    }
    return result;
  }

  @Override
  public R updateDevInstanceOwner(String id, IoTDeviceOwnerBO ownerBO) {
    if (StrUtil.isBlank(id)) {
      return R.error("设备ID不能为空");
    }
    if (ownerBO == null || StrUtil.isBlank(ownerBO.getUnionId())) {
      return R.error("请选择设备归属账号");
    }
    IoTDevice ioTDevice = ioTDeviceMapper.selectDevInstanceById(id);
    if (ioTDevice == null) {
      return R.error("设备不存在");
    }
    IoTUser operator = resolveFallbackScopeUser();
    if (isPendingOwnerDevice(ioTDevice)) {
      assertPendingOwnerProductAccess(ioTDevice, operator);
    } else {
      assertDeviceAccess(ioTDevice, operator, "您没有权限操作此设备！");
    }
    IoTUser targetUser = ioTUserService.selectUserByUnionId(StrUtil.trim(ownerBO.getUnionId()));
    if (targetUser == null || targetUser.getDeleted() != null && targetUser.getDeleted() == 1) {
      return R.error("目标账号不存在或已删除");
    }
    if (targetUser.getStatus() != null && targetUser.getStatus() == 1) {
      return R.error("目标账号已停用");
    }
    assertOwnerAssignable(operator, targetUser);

    String targetTenantId = resolveTenantId(targetUser);
    validateDeviceDept(targetUser.getDeptId(), targetTenantId, operator);
    IoTDevice updateEntity =
        IoTDevice.builder()
            .id(ioTDevice.getId())
            .creatorId(targetUser.getUnionId())
            .tenantId(targetTenantId)
            .deptId(targetUser.getDeptId())
            .build();
    if (ownerBO.getAppUniqueId() != null) {
      updateEntity.setApplication(
          StrUtil.isBlank(ownerBO.getAppUniqueId()) ? "" : StrUtil.trim(ownerBO.getAppUniqueId()));
    }
    int rows = ioTDeviceMapper.updateDevInstance(updateEntity);
    publishDeviceUpdate(ioTDevice, rows);
    return rows > 0 ? R.ok("修改归属成功") : R.error("修改归属失败");
  }

  @Override
  public R updateSubDeviceGateway(String id, IoTSubDeviceGatewayBindBO bindBO) {
    if (StrUtil.isBlank(id)) {
      return R.error("设备ID不能为空");
    }
    if (bindBO == null
        || StrUtil.isBlank(bindBO.getGwProductKey())
        || StrUtil.isBlank(bindBO.getGwDeviceId())) {
      return R.error("请选择目标网关");
    }
    IoTDevice subDevice = ioTDeviceMapper.selectDevInstanceById(id);
    if (subDevice == null) {
      return R.error("设备不存在");
    }
    IoTUser operator = resolveFallbackScopeUser();
    assertDeviceAccess(subDevice, operator, "您没有权限操作此设备！");
    IoTProduct subProduct = ioTProductMapper.getProductByProductKey(subDevice.getProductKey());
    if (subProduct == null
        || !DeviceNode.GATEWAY_SUB_DEVICE.name().equals(subProduct.getDeviceNode())) {
      return R.error("只有网关子设备才能更换所属网关");
    }

    IoTDevice gateway =
        ioTDeviceMapper.selectIoTDevice(
            StrUtil.trim(bindBO.getGwProductKey()), StrUtil.trim(bindBO.getGwDeviceId()));
    if (gateway == null) {
      return R.error("目标网关设备不存在");
    }
    assertDeviceAccess(gateway, operator, "您没有权限操作目标网关");
    IoTProduct gatewayProduct = ioTProductMapper.getProductByProductKey(gateway.getProductKey());
    if (gatewayProduct == null
        || !DeviceNode.GATEWAY.name().equals(gatewayProduct.getDeviceNode())) {
      return R.error("目标设备不是网关设备");
    }
    String compatibilityError = validateSubDeviceGatewayCompatibility(subProduct, gatewayProduct);
    if (StrUtil.isNotBlank(compatibilityError)) {
      return R.error(compatibilityError);
    }
    if (!Objects.equals(subDevice.getTenantId(), gateway.getTenantId())) {
      return R.error("子设备和目标网关不属于同一租户");
    }
    String oldGwProductKey = subDevice.getGwProductKey();
    String oldGwDeviceId = subDevice.getExtDeviceId();
    if (StrUtil.equals(oldGwProductKey, gateway.getProductKey())
        && StrUtil.equals(oldGwDeviceId, gateway.getDeviceId())) {
      return R.ok("所属网关未变化");
    }
    if (hasIndustrialPoints(subDevice)) {
      return R.error("该子设备已有工业点位，请先迁移或删除点位后再更换所属网关");
    }
    String conflict = findSubDeviceProtocolAddressConflict(subDevice, gateway);
    if (StrUtil.isNotBlank(conflict)) {
      return R.error(conflict);
    }

    IoTDevice updateEntity =
        IoTDevice.builder()
            .id(subDevice.getId())
            .gwProductKey(gateway.getProductKey())
            .extDeviceId(gateway.getDeviceId())
            .build();
    int rows = ioTDeviceMapper.updateDevInstance(updateEntity);
    if (rows <= 0) {
      return R.error("更换所属网关失败");
    }
    refreshGatewaySubdeviceMapping(oldGwProductKey, oldGwDeviceId);
    refreshGatewaySubdeviceMapping(gateway.getProductKey(), gateway.getDeviceId());
    publishDeviceUpdate(subDevice, rows);
    return R.ok("已更换所属网关");
  }

  private void publishDeviceUpdate(IoTDevice device, int updated) {
    if (updated <= 0 || device == null) {
      return;
    }
    IoTDeviceDTO deviceDTO = new IoTDeviceDTO();
    BeanUtils.copyProperties(device, deviceDTO);
    cacheInvalidationPublisher.publishDeviceChange(
        IoTDevicePostProcessor.Operation.UPDATE, deviceDTO);
  }

  private String validateSubDeviceGatewayCompatibility(
      IoTProduct subProduct, IoTProduct gatewayProduct) {
    String subProtocol = effectiveGatewayProtocol(subProduct);
    String gatewayProtocol = effectiveGatewayProtocol(gatewayProduct);
    if (StrUtil.isNotBlank(subProtocol)
        && StrUtil.isNotBlank(gatewayProtocol)
        && !StrUtil.equalsIgnoreCase(subProtocol, gatewayProtocol)) {
      return "子设备产品协议与目标网关协议不一致";
    }
    if (StrUtil.isBlank(subProtocol) && StrUtil.isBlank(gatewayProtocol)) {
      String subPlatform = normalizeProductPlatform(subProduct);
      String gatewayPlatform = normalizeProductPlatform(gatewayProduct);
      if (StrUtil.isNotBlank(subPlatform)
          && StrUtil.isNotBlank(gatewayPlatform)
          && !StrUtil.equals(subPlatform, gatewayPlatform)) {
        return "子设备产品接入方式与目标网关不一致";
      }
    }
    return null;
  }

  private String effectiveGatewayProtocol(IoTProduct product) {
    if (product == null) {
      return null;
    }
    JSONObject config = parseJsonObjectQuietly(product.getConfiguration());
    JSONObject industrial = parseNestedJsonObject(config.get("industrial"));
    return normalizeIndustrialProtocol(
        firstText(
            product.getTransportProtocol(),
            config.getStr("industrialProtocol"),
            config.getStr("protocol"),
            industrial.getStr("protocol"),
            product.getThirdPlatform(),
            product.getMessageProtocol()));
  }

  private String normalizeProductPlatform(IoTProduct product) {
    return product == null || StrUtil.isBlank(product.getThirdPlatform())
        ? null
        : product.getThirdPlatform().trim().toLowerCase();
  }

  private boolean hasIndustrialPoints(IoTDevice subDevice) {
    if (subDevice == null
        || StrUtil.isBlank(subDevice.getProductKey())
        || StrUtil.isBlank(subDevice.getDeviceId())) {
      return false;
    }
    try {
      Number count =
          jdbcTemplate.queryForObject(
              "select count(1) from iot_industrial_point where product_key = ? and device_id = ?",
              Number.class,
              subDevice.getProductKey(),
              subDevice.getDeviceId());
      return count != null && count.longValue() > 0;
    } catch (Exception e) {
      log.debug("工业点位表不存在或查询失败，跳过子设备换绑点位保护: {}", e.getMessage());
      return false;
    }
  }

  private void refreshGatewaySubdeviceMapping(String gwProductKey, String gwDeviceId) {
    if (StrUtil.isBlank(gwProductKey) || StrUtil.isBlank(gwDeviceId)) {
      return;
    }
    try {
      IoTDevice gateway = ioTDeviceMapper.selectIoTDevice(gwProductKey, gwDeviceId);
      if (gateway == null) {
        return;
      }
      Map<String, Object> subdeviceMapping = new LinkedHashMap<>();
      List<IoTDevice> subDevices = ioTDeviceMapper.selectSubDeviceByGw(gwProductKey, gwDeviceId);
      for (IoTDevice subDevice : subDevices) {
        String address = protocolAddress(subDevice);
        if (StrUtil.isBlank(address)) {
          continue;
        }
        Map<String, Object> deviceInfo = new LinkedHashMap<>();
        deviceInfo.put("productKey", subDevice.getProductKey());
        deviceInfo.put("productName", subDevice.getProductName());
        deviceInfo.put("deviceId", subDevice.getDeviceId());
        deviceInfo.put("deviceName", subDevice.getDeviceName());
        subdeviceMapping.put(address, deviceInfo);
      }
      JSONObject config = parseDeviceConfiguration(gateway.getConfiguration());
      config.set("subdeviceMapping", subdeviceMapping);
      IoTDevice updateEntity =
          IoTDevice.builder().id(gateway.getId()).configuration(config.toString()).build();
      ioTDeviceMapper.updateDevInstance(updateEntity);
    } catch (Exception e) {
      log.warn(
          "刷新网关子设备映射失败, gwProductKey={}, gwDeviceId={}, err={}",
          gwProductKey,
          gwDeviceId,
          e.getMessage(),
          e);
    }
  }

  private String findSubDeviceProtocolAddressConflict(IoTDevice subDevice, IoTDevice gateway) {
    String protocolAddress = protocolAddress(subDevice);
    if (StrUtil.isBlank(protocolAddress)) {
      return null;
    }
    List<IoTDevice> siblings =
        ioTDeviceMapper.selectSubDeviceByGw(gateway.getProductKey(), gateway.getDeviceId());
    for (IoTDevice sibling : siblings) {
      if (sibling == null || Objects.equals(sibling.getId(), subDevice.getId())) {
        continue;
      }
      if (StrUtil.equals(protocolAddress, protocolAddress(sibling))) {
        return "目标网关下已存在相同协议地址的子设备";
      }
    }
    return null;
  }

  private String protocolAddress(IoTDevice device) {
    if (device == null) {
      return null;
    }
    JSONObject config = parseDeviceConfiguration(device.getConfiguration());
    String value =
        firstText(
            config.getStr("slaveAddress"),
            config.getStr("unitId"),
            config.getStr("unitIdentifier"),
            config.getStr("defaultUnitIdentifier"),
            config.getStr("deviceInstance"),
            config.getStr("remoteDeviceInstance"),
            device.getExt1());
    if (StrUtil.isNotBlank(value)) {
      return value.trim();
    }
    String deviceId = device.getDeviceId();
    if (StrUtil.isNotBlank(deviceId) && StrUtil.contains(deviceId, "-")) {
      return StrUtil.subAfter(deviceId, "-", true);
    }
    return null;
  }

  private String firstText(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private void mergeDeviceConfigurationFromRequest(
      JSONObject target, IoTDeviceBO devInstancebo, IoTDevice existingDevice) {
    JSONObject customConfig = extractRequestCustomFieldConfiguration(devInstancebo, existingDevice);
    if (!customConfig.isEmpty()) {
      target.putAll(customConfig);
    }
  }

  private void mergeDeviceConfigurationFromRequest(
      JSONObject target, IoTDeviceBO devInstancebo, IoTProduct product) {
    JSONObject customConfig = extractRequestCustomFieldConfiguration(devInstancebo, product);
    if (!customConfig.isEmpty()) {
      target.putAll(customConfig);
    }
  }

  private void mergeDeviceConfigurationFromRequest(
      IoTDevice target, IoTDeviceBO devInstancebo, IoTDevice existingDevice) {
    JSONObject merged = mergeDeviceConfiguration(devInstancebo, existingDevice);
    if (!merged.isEmpty()) {
      target.setConfiguration(merged.toString());
    }
  }

  private JSONObject mergeDeviceConfiguration(IoTDeviceBO devInstancebo, IoTDevice existingDevice) {
    JSONObject existingConfig = parseDeviceConfiguration(existingDevice.getConfiguration());
    JSONObject requestConfig = parseDeviceConfiguration(devInstancebo.getConfiguration());
    if (requestConfig.isEmpty()) {
      return new JSONObject();
    }
    Set<String> customFieldIds = resolveProductCustomFieldIds(existingDevice.getProductKey());
    if (customFieldIds.isEmpty()) {
      return new JSONObject();
    }

    JSONObject customConfig = new JSONObject();
    for (String fieldId : customFieldIds) {
      if (requestConfig.containsKey(fieldId)) {
        customConfig.set(fieldId, requestConfig.get(fieldId));
      }
    }
    if (customConfig.isEmpty()) {
      return new JSONObject();
    }

    for (String fieldId : customFieldIds) {
      existingConfig.remove(fieldId);
    }
    existingConfig.putAll(customConfig);
    return existingConfig;
  }

  private JSONObject extractRequestCustomFieldConfiguration(
      IoTDeviceBO devInstancebo, IoTDevice existingDevice) {
    if (existingDevice == null) {
      return new JSONObject();
    }
    return extractRequestCustomFieldConfiguration(
        devInstancebo, ioTProductMapper.getProductByProductKey(existingDevice.getProductKey()));
  }

  private JSONObject extractRequestCustomFieldConfiguration(
      IoTDeviceBO devInstancebo, IoTProduct product) {
    JSONObject requestConfig = parseDeviceConfiguration(devInstancebo.getConfiguration());
    if (requestConfig.isEmpty()) {
      return new JSONObject();
    }
    Set<String> customFieldIds = resolveProductCustomFieldIds(product);
    if (product != null && "sip".equalsIgnoreCase(product.getThirdPlatform())) {
      customFieldIds.addAll(Set.of("sipUsername", "sipPassword", "sipAutoAnswer", "sipEnabled"));
    }
    if (customFieldIds.isEmpty()) {
      return new JSONObject();
    }

    JSONObject customConfig = new JSONObject();
    for (String fieldId : customFieldIds) {
      if (requestConfig.containsKey(fieldId)) {
        customConfig.set(fieldId, requestConfig.get(fieldId));
      }
    }
    return customConfig;
  }

  private JSONObject parseDeviceConfiguration(String configuration) {
    if (StrUtil.isBlank(configuration)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(configuration);
    } catch (Exception e) {
      log.warn("设备configuration解析失败，将保留为空对象: {}", configuration, e);
      return new JSONObject();
    }
  }

  private JSONObject parseJsonObjectQuietly(String value) {
    if (StrUtil.isBlank(value) || !JSONUtil.isTypeJSON(value)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(value);
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private JSONObject parseNestedJsonObject(Object value) {
    if (value instanceof JSONObject object) {
      return object;
    }
    if (value instanceof Map<?, ?> map) {
      JSONObject object = new JSONObject();
      map.forEach(
          (key, item) -> {
            if (key != null) {
              object.set(String.valueOf(key), item);
            }
          });
      return object;
    }
    if (value instanceof String text && JSONUtil.isTypeJSON(text)) {
      try {
        return JSONUtil.parseObj(text);
      } catch (Exception e) {
        return new JSONObject();
      }
    }
    return new JSONObject();
  }

  private Set<String> resolveProductCustomFieldIds(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return new LinkedHashSet<>();
    }
    IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
    return resolveProductCustomFieldIds(product);
  }

  private Set<String> resolveProductCustomFieldIds(IoTProduct product) {
    Set<String> fieldIds = new LinkedHashSet<>();
    if (product == null) {
      return fieldIds;
    }
    if (StrUtil.isBlank(product.getThirdConfiguration())) {
      return fieldIds;
    }
    try {
      JSONObject thirdConfiguration = JSONUtil.parseObj(product.getThirdConfiguration());
      JSONArray customFields = thirdConfiguration.getJSONArray(PRODUCT_CUSTOM_FIELD_KEY);
      if (customFields == null) {
        return fieldIds;
      }
      for (Object customField : customFields) {
        JSONObject field = JSONUtil.parseObj(customField);
        String fieldId = field.getStr("id");
        if (StrUtil.isNotBlank(fieldId)) {
          fieldIds.add(fieldId);
        }
      }
    } catch (Exception e) {
      log.warn("产品自定义字段配置解析失败: productKey={}", product.getProductKey(), e);
    }
    return fieldIds;
  }

  private String normalizeIndustrialProtocol(String value) {
    if (StrUtil.isBlank(value)) {
      return null;
    }
    String normalized =
        value
            .trim()
            .toLowerCase()
            .replace("-", "")
            .replace("_", "")
            .replace("/", "")
            .replace(" ", "");
    return switch (normalized) {
      case "s7", "siemenss7" -> "S7";
      case "opcua", "opc" -> "OPCUA";
      case "modbus",
              "modbustcp",
              "modbusrtu",
              "rtu",
              "modbusserial",
              "modbusrtuserial",
              "modbusrtuoverserial",
              "rtuserial",
              "rtuoverserial",
              "serialrtu",
              "modbusascii",
              "ascii",
              "modbusasciiserial",
              "modbusasciioverserial",
              "asciiserial",
              "asciioverserial",
              "serialascii",
              "modbusrtutcp",
              "modbusrtuovertcp",
              "rtutcp",
              "rtuovertcp",
              "modbustcpclient" ->
          "MODBUS";
      case "eip", "ethernetip" -> "EIP";
      case "bacnet", "bacnetip" -> "BACNET";
      case "ads", "twincat" -> "ADS";
      case "mc", "mitsubishimc", "mitsubishi" -> "MC";
      case "fins", "omronfins", "omron" -> "FINS";
      case "iec104", "iec60870", "iec608705104", "iec60870104" -> "IEC104";
      case "generictcp", "tcpclient" -> "GENERIC_TCP";
      case "genericudp", "udpclient" -> "GENERIC_UDP";
      default -> null;
    };
  }

  private boolean isCustomFieldVisible(
      JSONObject field, Map<String, Object> values, JSONArray fields) {
    if (field == null) {
      return false;
    }
    Object visibleWhen = field.get("visibleWhen");
    if (visibleWhen == null) {
      return true;
    }
    if (visibleWhen instanceof JSONArray conditions) {
      for (Object condition : conditions) {
        if (!evaluateVisibleWhenCondition(asJsonObject(condition), values, fields)) {
          return false;
        }
      }
      return true;
    }
    return evaluateVisibleWhenCondition(asJsonObject(visibleWhen), values, fields);
  }

  private boolean evaluateVisibleWhenCondition(
      JSONObject condition, Map<String, Object> values, JSONArray fields) {
    if (condition == null || condition.isEmpty()) {
      return true;
    }
    String fieldId =
        firstText(
            condition.getStr("field"), condition.getStr("key"), condition.getStr("dependsOn"));
    if (StrUtil.isBlank(fieldId)) {
      return true;
    }
    Object actual = resolveCustomFieldValue(fieldId, values, fields);
    if (condition.containsKey("exists")) {
      boolean exists = condition.getBool("exists", false);
      return exists ? !isBlankValue(actual) : isBlankValue(actual);
    }
    Object acceptedValues =
        firstObject(condition.get("in"), condition.get("values"), condition.get("oneOf"));
    if (acceptedValues != null) {
      return containsComparable(acceptedValues, actual);
    }
    if (condition.containsKey("notIn")) {
      return !containsComparable(condition.get("notIn"), actual);
    }
    Object expected =
        firstObject(condition.get("eq"), condition.get("value"), condition.get("equals"));
    if (expected != null) {
      return containsComparable(expected, actual);
    }
    Object rejected = firstObject(condition.get("ne"), condition.get("not"));
    if (rejected != null) {
      return !containsComparable(rejected, actual);
    }
    return true;
  }

  private Object resolveCustomFieldValue(
      String fieldId, Map<String, Object> values, JSONArray fields) {
    if (values != null && values.containsKey(fieldId) && !isBlankValue(values.get(fieldId))) {
      return values.get(fieldId);
    }
    JSONObject field = findCustomField(fields, fieldId);
    if (field != null && field.containsKey("defaultValue")) {
      return field.get("defaultValue");
    }
    if ("transportType".equals(fieldId)) {
      return "tcp-client";
    }
    return values == null ? null : values.get(fieldId);
  }

  private JSONObject findCustomField(JSONArray fields, String fieldId) {
    if (CollectionUtil.isEmpty(fields) || StrUtil.isBlank(fieldId)) {
      return null;
    }
    for (int i = 0; i < fields.size(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (field != null && fieldId.equals(field.getStr("id"))) {
        return field;
      }
    }
    return null;
  }

  private boolean containsComparable(Object expected, Object actual) {
    String actualText = normalizeComparable(actual);
    for (Object value : toComparableValues(expected)) {
      if (StrUtil.equals(normalizeComparable(value), actualText)) {
        return true;
      }
    }
    return false;
  }

  private List<Object> toComparableValues(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof JSONArray array) {
      List<Object> values = new ArrayList<>();
      for (Object item : array) {
        values.add(item);
      }
      return values;
    }
    if (value instanceof Collection<?> collection) {
      return new ArrayList<>(collection);
    }
    return List.of(value);
  }

  private JSONObject asJsonObject(Object value) {
    if (value instanceof JSONObject jsonObject) {
      return jsonObject;
    }
    if (value instanceof Map<?, ?> map) {
      return JSONUtil.parseObj(map);
    }
    return null;
  }

  private Object firstObject(Object... values) {
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String normalizeComparable(Object value) {
    return value == null ? "" : String.valueOf(value).trim().toLowerCase();
  }

  private boolean isBlankValue(Object value) {
    return value == null || StrUtil.isBlank(String.valueOf(value));
  }

  /**
   * 批量删除设备
   *
   * @param ids 需要删除的设备主键
   * @return 结果
   */
  @Override
  public R deleteDevInstanceByIds(String[] ids) {
    R result = new R();
    List<IoTDevice> devices = deleteCandidates(ids);
    devices.forEach(this::assertGatewayCanBeDeleted);
    for (IoTDevice ioTDevice : devices) {
      String appUnionId = resolveDeviceAppUnionId(ioTDevice, null);
      JSONObject downRequest = new JSONObject();
      downRequest.set("appUnionId", appUnionId);
      downRequest.set("productKey", ioTDevice.getProductKey());
      downRequest.set("deviceId", ioTDevice.getDeviceId());
      downRequest.set("tenantId", ioTDevice.getTenantId());
      downRequest.set("gwProductKey", ioTDevice.getGwProductKey());
      downRequest.set("cmd", DownCmd.DEV_DEL.name());
      JSONObject ob = new JSONObject();
      downRequest.set("data", ob);
      IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
      // 转换为UnifiedDownlinkCommand（保持所有参数）
      UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);
      result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
      if (!result.getCode().equals(0)) {
        return R.error("设备名称: " + ioTDevice.getDeviceName() + " 删除失败，原因: " + result.getMsg());
      }
      ioTDeviceFenceRelMapper.deleteFenceInstance(ioTDevice.getIotId());
    }
    return R.ok("删除成功");
  }

  @Override
  public List<IoTDeviceDeletePreview> previewDeleteDevInstances(String[] ids) {
    return deleteCandidates(ids).stream().map(this::deletePreview).toList();
  }

  private List<IoTDevice> deleteCandidates(String[] ids) {
    if (ids == null || ids.length == 0) {
      throw new IoTException("请选择要删除的设备");
    }
    List<IoTDevice> devices = new ArrayList<>(ids.length);
    for (String id : new LinkedHashSet<>(List.of(ids))) {
      IoTDevice device = ioTDeviceMapper.selectDevInstanceById(id);
      if (device == null) {
        throw new IoTException("设备不存在或已被删除: " + id, 400);
      }
      resolveDeviceAppUnionId(device, null);
      devices.add(device);
    }
    return devices;
  }

  private IoTDeviceDeletePreview deletePreview(IoTDevice device) {
    int childDeviceCount = gatewayChildCount(device);
    List<String> blockers =
        childDeviceCount > 0
            ? List.of("网关仍绑定 " + childDeviceCount + " 台子设备，请先解绑或删除子设备")
            : List.of();
    return IoTDeviceDeletePreview.builder()
        .id(device.getId())
        .iotId(device.getIotId())
        .tenantId(device.getTenantId())
        .productKey(device.getProductKey())
        .deviceId(device.getDeviceId())
        .deviceName(device.getDeviceName())
        .childDeviceCount(childDeviceCount)
        .blocked(!blockers.isEmpty())
        .blockers(blockers)
        .moduleImpacts(deviceDomainService.previewDelete(device))
        .build();
  }

  private void assertGatewayCanBeDeleted(IoTDevice device) {
    int childDeviceCount = gatewayChildCount(device);
    if (childDeviceCount > 0) {
      throw new IoTException(
          "网关设备 " + device.getDeviceName() + " 仍绑定 " + childDeviceCount + " 台子设备，请先处理子设备", 409);
    }
  }

  private int gatewayChildCount(IoTDevice gateway) {
    IoTDevice relation =
        IoTDevice.builder()
            .gwProductKey(gateway.getProductKey())
            .extDeviceId(gateway.getDeviceId())
            .tenantId(gateway.getTenantId())
            .build();
    return ioTDeviceMapper.selectCount(relation);
  }

  /**
   * 删除设备信息
   *
   * @param id 设备主键
   * @return 结果
   */
  @Override
  public R deleteDevInstanceById(String id) {
    IoTDevice ioTDevice = deleteCandidates(new String[] {id}).get(0);
    assertGatewayCanBeDeleted(ioTDevice);
    String appUnionId = resolveDeviceAppUnionId(ioTDevice, null);
    JSONObject downRequest = new JSONObject();
    downRequest.set("appUnionId", appUnionId);
    downRequest.set("productKey", ioTDevice.getProductKey());
    downRequest.set("deviceId", ioTDevice.getDeviceId());
    downRequest.set("tenantId", ioTDevice.getTenantId());
    downRequest.set("cmd", DownCmd.DEV_DEL.name());
    JSONObject ob = new JSONObject();
    downRequest.set("data", ob);
    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(downRequest);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  /**
   * 根据应用id查找设备
   *
   * @param ids 设备主键
   * @return 结果
   */
  @Override
  public int selectDevByAppUniqueId(String[] ids) {
    return ioTDeviceMapper.selectDevByAppUniqueId(ids);
  }

  /**
   * 查询设备型号
   *
   * @param supportChild 等于true 查询支持子设备的型号
   * @return 设备型号
   */
  @Override
  public List<IoTDeviceCompanyVO> queryAllBySupportChild(Boolean supportChild, String apps) {
    return ioTDeviceMapper.selectAllBySupportChild(supportChild, apps);
  }

  /**
   * 查询所以设备型号
   *
   * @return 设备影子
   */
  @Override
  public IoTDeviceShadow getDeviceShadow(String iotId) {
    return ioTDeviceShadowService.getLatestDeviceShadow(iotId);
  }

  @Override
  public List<IoTDeviceShadow> getDeviceShadows(List<String> iotIds) {
    if (CollectionUtil.isEmpty(iotIds)) {
      return new ArrayList<>();
    }

    Set<String> uniqueIds = new LinkedHashSet<>();
    for (String iotId : iotIds) {
      if (StrUtil.isNotBlank(iotId)) {
        uniqueIds.add(iotId);
      }
    }

    if (CollectionUtil.isEmpty(uniqueIds)) {
      return new ArrayList<>();
    }

    List<IoTDeviceShadow> shadows =
        ioTDeviceShadowService.getLatestDeviceShadows(new ArrayList<>(uniqueIds));
    Map<String, IoTDeviceShadow> shadowMap = new LinkedHashMap<>();
    for (IoTDeviceShadow shadow : shadows) {
      if (shadow != null && StrUtil.isNotBlank(shadow.getIotId())) {
        shadowMap.put(shadow.getIotId(), shadow);
      }
    }

    List<IoTDeviceShadow> orderedShadows = new ArrayList<>();
    for (String iotId : uniqueIds) {
      IoTDeviceShadow shadow = shadowMap.get(iotId);
      if (shadow != null) {
        orderedShadows.add(shadow);
      }
    }
    return orderedShadows;
  }

  @Override
  public R probeModbusEndpoint(String id) {
    IoTDevice device = selectDevInstanceById(id);
    if (device == null) {
      return R.error("设备不存在");
    }
    if (modbusPollingExecutor == null) {
      return R.error("Modbus采集执行器未启用");
    }
    return modbusPollingExecutor.probe(device);
  }

  /** 设备下发 */
  @Override
  public R iotFunctionsDown(String productKey, String downRequest) {
    JSONObject jsonObject = JSONUtil.parseObj(downRequest);
    String deviceId = jsonObject.getStr("deviceId");
    String appUnionId = jsonObject.getStr("appUnionId");
    IoTDevice ioTDeviceBo = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
    if (ioTDeviceBo == null) {
      return R.error("设备不存在");
    }
    assertDeviceAccess(
        ioTDeviceBo,
        StrUtil.isBlank(appUnionId)
            ? resolveFallbackScopeUser()
            : ioTUserService.selectUserByUnionId(appUnionId),
        "您没有权限操作此设备！");

    IoTProduct ioTProduct = ioTProductMapper.getProductByProductKey(productKey);
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.fromJson(JSONUtil.parseObj(downRequest));
    // 使用已鉴权设备的身份，不能信任请求体中的租户或另一产品标识。
    command.setTenantId(ioTDeviceBo.getTenantId());
    command.setProductKey(ioTDeviceBo.getProductKey());
    command.setDeviceId(ioTDeviceBo.getDeviceId());
    command.setIotId(ioTDeviceBo.getIotId());
    R result = IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
    return result;
  }

  /** 根据产品key查型号配置 */
  @Override
  public IoTDeviceModelVO getModelByProductKey(String productKey) {
    return ioTDeviceMapper.getModelByProductKey(productKey);
  }

  @Override
  public List<IoTDevice> selectNotBindDevInstanceList(
      String groupId, IoTDevice ioTDevice, IoTUser iotUser) {
    applyDeviceDataScope(ioTDevice, iotUser);
    return ioTDeviceMapper.selectNotBindDevInstanceList(groupId, ioTDevice, null);
  }

  @Override
  public Page<IoTDeviceVO> selectFenceDevice(IoTDeviceBO ioTDeviceBO, int page, int size) {
    Page<IoTDeviceVO> p = PageHelper.startPage(page, size);
    List<IoTDeviceVO> ioTDeviceVOList = ioTDeviceMapper.selectFenceDevice(ioTDeviceBO);
    return p;
  }

  @Override
  public List<GwIoTDeviceVo> selectSubDeviceRelationByGw(String gwProductKey, String gwDeviceId) {
    if (StrUtil.isBlank(gwProductKey) || StrUtil.isBlank(gwDeviceId)) {
      return List.of();
    }
    IoTDevice gateway =
        ioTDeviceMapper.selectIoTDevice(StrUtil.trim(gwProductKey), StrUtil.trim(gwDeviceId));
    if (gateway == null) {
      return List.of();
    }
    assertDeviceAccess(gateway, resolveFallbackScopeUser(), "您没有权限操作目标网关");
    IoTProduct gatewayProduct = ioTProductMapper.getProductByProductKey(gateway.getProductKey());
    if (gatewayProduct == null
        || !DeviceNode.GATEWAY.name().equals(gatewayProduct.getDeviceNode())) {
      return List.of();
    }
    return ioTDeviceMapper.selectSubDeviceRelationByGw(gwProductKey, gwDeviceId);
  }

  /** 查询视频设备列表（强制过滤 deviceNode=VIDEO_DEVICE） */
  @Override
  public List<IoTDevice> selectVideoDeviceList(String keyword, IoTUser iotUser) {
    IoTDevice ioTDevice = new IoTDevice();
    // 强制设置 deviceNode 为 VIDEO_DEVICE
    ioTDevice.setDeviceNode("VIDEO_DEVICE");
    // keyword 用于模糊搜索设备名称和设备序列号
    if (StrUtil.isNotBlank(keyword)) {
      ioTDevice.setDeviceName(keyword);
      ioTDevice.setDeviceId(keyword);
    }
    applyDeviceDataScope(ioTDevice, iotUser);
    return ioTDeviceMapper.selectVideoDeviceList(ioTDevice, null);
  }

  private void applyDeviceDataScope(IoTDevice query, IoTUser fallbackUser) {
    applyDataPermissionScope(query, fallbackUser);
  }

  private void allowPendingOwnerVisible(IoTDevice query) {
    if (query != null && query.getParams() != null) {
      query.getParams().put("includePendingOwner", true);
      query.getParams().put("pendingOwnerUnionId", autoRegisterUnionId);
    }
  }

  private void applyDeviceDataScope(IoTDeviceBO query, IoTUser fallbackUser) {
    applyDataPermissionScope(query, fallbackUser);
  }

  private IoTUser resolveCurrentActorUser() {
    String unionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return ioTUserService.selectUserByUnionId(unionId);
  }

  private IoTUser resolveFallbackScopeUser() {
    IoTUser currentUser = resolveCurrentActorUser();
    if (currentUser != null) {
      return currentUser;
    }
    String unionId = SecurityUtils.getUnionIdQuietly();
    if (StrUtil.isBlank(unionId)) {
      return null;
    }
    return queryIotUser(unionId);
  }

  private IoTUser resolveTenantScopedUser(IoTUser currentActor, IoTDeviceBO devInstancebo) {
    if (currentActor != null) {
      return currentActor;
    }
    if (devInstancebo == null || StrUtil.isBlank(devInstancebo.getCreatorId())) {
      return null;
    }
    return ioTUserService.selectUserByUnionId(devInstancebo.getCreatorId());
  }

  private void validateDeviceDept(Long deptId, String tenantId) {
    validateDeviceDept(deptId, tenantId, resolveFallbackScopeUser());
  }

  private void validateDeviceDept(Long deptId, String tenantId, IoTUser operator) {
    if (deptId == null) {
      return;
    }
    SysDept dept = sysDeptService.selectDeptById(deptId);
    if (dept == null
        || StrUtil.isBlank(tenantId)
        || !StrUtil.equals(dept.getTenantId(), tenantId)) {
      throw new IoTException("设备所属部门不合法");
    }
    if (!StrUtil.equals(dept.getStatus(), "0")) {
      throw new IoTException("设备所属部门已停用");
    }
    if (!canManageDept(dept, operator)) {
      throw new IoTException("你无权分配当前部门");
    }
  }

  private boolean canManageDept(SysDept dept, IoTUser operator) {
    if (dept == null) {
      return false;
    }
    if (operator == null || operator.isAdmin()) {
      return true;
    }
    if (!StrUtil.equals(resolveTenantId(operator), dept.getTenantId())) {
      return false;
    }
    if (isTenantAdmin(operator)) {
      return true;
    }
    DeptDataPermissionScope scope = resolveDataPermissionScope(operator);
    if (scope == null) {
      return Objects.equals(operator.getDeptId(), dept.getDeptId());
    }
    if (scope.isAll()) {
      return true;
    }
    if (dept.getDeptId() != null && scope.getScopeDeptIds().contains(dept.getDeptId())) {
      return true;
    }
    return operator.getDeptId() != null
        && Objects.equals(operator.getDeptId(), dept.getDeptId())
        && scope.getScopeUnionIds().contains(operator.getUnionId());
  }

  private String resolveDeviceTenantId(IoTDevice ioTDevice, IoTUser operator) {
    if (ioTDevice != null && StrUtil.isNotBlank(ioTDevice.getTenantId())) {
      return ioTDevice.getTenantId();
    }
    if (operator != null) {
      String tenantId = resolveTenantId(operator);
      if (StrUtil.isNotBlank(tenantId)) {
        return tenantId;
      }
    }
    return ioTDevice == null ? null : resolveTenantId(ioTDevice.getCreatorId());
  }

  private void assertDeviceAccess(IoTDevice ioTDevice, IoTUser fallbackUser, String message) {
    if (ioTDevice == null) {
      throw new IoTException("设备不存在");
    }
    if (!canAccessDevice(ioTDevice, fallbackUser)) {
      throw new IoTException(message);
    }
  }

  private boolean isPendingOwnerDevice(IoTDevice ioTDevice) {
    return ioTDevice != null && StrUtil.equals(ioTDevice.getCreatorId(), autoRegisterUnionId);
  }

  private void markDeviceOwnerStatus(List<IoTDevice> devices) {
    if (devices == null) {
      return;
    }
    for (IoTDevice device : devices) {
      markDeviceOwnerStatus(device);
    }
  }

  private void markDeviceOwnerStatus(IoTDevice device) {
    if (device != null) {
      device.setPendingOwner(isPendingOwnerDevice(device));
    }
  }

  private void assertPendingOwnerProductAccess(IoTDevice ioTDevice, IoTUser operator) {
    if (ioTDevice == null) {
      throw new IoTException("设备不存在");
    }
    IoTProduct product = ioTProductMapper.getProductByProductKey(ioTDevice.getProductKey());
    if (product == null
        || !canAccessDeptResource(product.getTenantId(), null, product.getCreatorId(), operator)) {
      throw new IoTException("你无权操作");
    }
  }

  private void assertOwnerAssignable(IoTUser operator, IoTUser targetUser) {
    if (targetUser == null) {
      throw new IoTException("目标账号不存在");
    }
    if (operator == null || operator.isAdmin()) {
      return;
    }
    if (!StrUtil.equals(resolveTenantId(operator), resolveTenantId(targetUser))) {
      throw new IoTException("你无权分配到其他租户账号");
    }
    if (isTenantAdmin(operator)) {
      return;
    }
    if (!Objects.equals(operator.getDeptId(), targetUser.getDeptId())
        && !StrUtil.equals(operator.getUnionId(), targetUser.getUnionId())) {
      throw new IoTException("你无权分配到当前账号");
    }
  }

  private boolean canAccessDevice(IoTDevice ioTDevice, IoTUser fallbackUser) {
    if (ioTDevice == null) {
      return false;
    }
    return canAccessDeptResource(
        ioTDevice.getTenantId(), ioTDevice.getDeptId(), ioTDevice.getCreatorId(), fallbackUser);
  }

  private boolean isTenantAdmin(IoTUser user) {
    return user != null
        && !user.isAdmin()
        && StrUtil.equalsIgnoreCase(user.getTenantRole(), "ADMIN");
  }
}
