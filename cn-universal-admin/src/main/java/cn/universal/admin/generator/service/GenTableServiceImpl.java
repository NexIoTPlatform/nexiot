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

package cn.universal.admin.generator.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.generator.constant.GenConstants;
import cn.universal.admin.generator.domain.GenTable;
import cn.universal.admin.generator.domain.GenTableColumn;
import cn.universal.admin.generator.mapper.GenTableColumnMapper;
import cn.universal.admin.generator.mapper.GenTableMapper;
import cn.universal.admin.generator.util.GenUtils;
import cn.universal.admin.generator.util.VelocityInitializer;
import cn.universal.admin.generator.util.VelocityUtils;
import cn.universal.common.exception.IoTException;
import cn.universal.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/** 业务 服务层实现 @Author ruoyi */
@Service
public class GenTableServiceImpl implements IGenTableService {

  private static final Logger log = LoggerFactory.getLogger(GenTableServiceImpl.class);

  @Resource private GenTableMapper genTableMapper;

  @Resource private GenTableColumnMapper genTableColumnMapper;

  /**
   * 查询业务信息
   *
   * @param id 业务ID
   * @return 业务信息
   */
  @Override
  public GenTable selectGenTableById(Long id) {
    GenTable genTable = genTableMapper.selectGenTableById(id);
    setTableFromOptions(genTable);
    return genTable;
  }

  /**
   * 查询业务列表
   *
   * @param genTable 业务信息
   * @return 业务集合
   */
  @Override
  public List<GenTable> selectGenTableList(GenTable genTable) {
    return genTableMapper.selectGenTableList(genTable);
  }

  /**
   * 查询据库列表
   *
   * @param genTable 业务信息
   * @return 数据库表集合
   */
  @Override
  public List<GenTable> selectDbTableList(GenTable genTable) {
    return genTableMapper.selectDbTableList(genTable);
  }

  /**
   * 查询据库列表
   *
   * @param tableNames 表名称组
   * @return 数据库表集合
   */
  @Override
  public List<GenTable> selectDbTableListByNames(String[] tableNames) {
    return genTableMapper.selectDbTableListByNames(tableNames);
  }

  /**
   * 查询所有表信息
   *
   * @return 表信息集合
   */
  @Override
  public List<GenTable> selectGenTableAll() {
    return genTableMapper.selectGenTableAll();
  }

  /**
   * 修改业务
   *
   * @param genTable 业务信息
   * @return 结果
   */
  @Override
  @Transactional
  public void updateGenTable(GenTable genTable) {
    String options = JSONUtil.toJsonStr(genTable.getParams());
    genTable.setOptions(options);
    int row = genTableMapper.updateGenTable(genTable);
    if (row > 0) {
      for (GenTableColumn cenTableColumn : genTable.getColumns()) {
        genTableColumnMapper.updateGenTableColumn(cenTableColumn);
      }
    }
  }

  /**
   * 删除业务对象
   *
   * @param tableIds 需要删除的数据ID
   * @return 结果
   */
  @Override
  @Transactional
  public void deleteGenTableByIds(Long[] tableIds) {
    genTableMapper.deleteGenTableByIds(tableIds);
    genTableColumnMapper.deleteGenTableColumnByIds(tableIds);
  }

  /**
   * 导入表结构
   *
   * @param tableList 导入表列表
   */
  @Override
  @Transactional
  public void importGenTable(List<GenTable> tableList) {
    String operName = SecurityUtils.getUnionId();
    try {
      for (GenTable table : tableList) {
        String tableName = table.getTableName();
        GenUtils.initTable(table, operName);
        int row = genTableMapper.insertGenTable(table);
        if (row > 0) {
          // 保存列信息
          List<GenTableColumn> genTableColumns =
              genTableColumnMapper.selectDbTableColumnsByName(tableName);
          for (GenTableColumn column : genTableColumns) {
            GenUtils.initColumnField(column, table);
            genTableColumnMapper.insertGenTableColumn(column);
          }
        }
      }
    } catch (Exception e) {
      throw new IoTException("导入失败：" + e.getMessage());
    }
  }

  /**
   * 预览代码
   *
   * @param tableId 表编号
   * @return 预览数据列表
   */
  @Override
  public Map<String, String> previewCode(Long tableId) {
    Map<String, String> dataMap = new LinkedHashMap<>();
    // 查询表信息
    GenTable table = genTableMapper.selectGenTableById(tableId);
    // 设置主子表信息
    setSubTable(table);
    // 设置主键列信息
    setPkColumn(table);
    VelocityInitializer.initVelocity();

    VelocityContext context = VelocityUtils.prepareContext(table);

    // 获取模板列表
    List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
    for (String template : templates) {
      // 渲染模板
      StringWriter sw = new StringWriter();
      Template tpl = Velocity.getTemplate(template, "utf-8");
      tpl.merge(context, sw);
      dataMap.put(template, sw.toString());
    }
    return dataMap;
  }

  /**
   * 生成代码（下载方式）
   *
   * @param tableName 表名称
   * @return 数据
   */
  @Override
  public byte[] downloadCode(String tableName) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ZipOutputStream zip = new ZipOutputStream(outputStream);
    generatorCode(tableName, zip);
    IOUtils.closeQuietly(zip);
    return outputStream.toByteArray();
  }

  /**
   * 生成代码（自定义路径）
   *
   * @param tableName 表名称
   */
  @Override
  public void generatorCode(String tableName) {
    // 查询表信息
    GenTable table = genTableMapper.selectGenTableByName(tableName);
    // 设置主子表信息
    setSubTable(table);
    // 设置主键列信息
    setPkColumn(table);

    VelocityInitializer.initVelocity();

    VelocityContext context = VelocityUtils.prepareContext(table);

    // 获取模板列表
    List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
    for (String template : templates) {
      if (!StringUtils.containsAny(
          template, "sql.vm", "api.js.vm", "index.vue.vm", "index-tree.vue.vm")) {
        // 渲染模板
        StringWriter sw = new StringWriter();
        Template tpl = Velocity.getTemplate(template, StandardCharsets.UTF_8.toString());
        tpl.merge(context, sw);
        try {
          String path = getGenPath(table, template);
          FileUtils.writeStringToFile(new File(path), sw.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new IoTException("渲染模板失败，表名：" + table.getTableName());
        }
      }
    }
  }

  /**
   * 同步数据库
   *
   * @param tableName 表名称
   */
  @Override
  @Transactional
  public void synchDb(String tableName) {
    GenTable table = genTableMapper.selectGenTableByName(tableName);
    List<GenTableColumn> tableColumns = table.getColumns();
    List<String> tableColumnNames =
        tableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

    List<GenTableColumn> dbTableColumns =
        genTableColumnMapper.selectDbTableColumnsByName(tableName);
    if (CollectionUtils.isEmpty(dbTableColumns)) {
      throw new IoTException("同步数据失败，原表结构不存在");
    }
    List<String> dbTableColumnNames =
        dbTableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

    dbTableColumns.forEach(
        column -> {
          if (!tableColumnNames.contains(column.getColumnName())) {
            GenUtils.initColumnField(column, table);
            genTableColumnMapper.insertGenTableColumn(column);
          }
        });

    List<GenTableColumn> delColumns =
        tableColumns.stream()
            .filter(column -> !dbTableColumnNames.contains(column.getColumnName()))
            .collect(Collectors.toList());
    if (!CollectionUtils.isEmpty(delColumns)) {
      genTableColumnMapper.deleteGenTableColumns(delColumns);
    }
  }

  /**
   * 批量生成代码（下载方式）
   *
   * @param tableNames 表数组
   * @return 数据
   */
  @Override
  public byte[] downloadCode(String[] tableNames) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ZipOutputStream zip = new ZipOutputStream(outputStream);
    for (String tableName : tableNames) {
      generatorCode(tableName, zip);
    }
    IOUtils.closeQuietly(zip);
    return outputStream.toByteArray();
  }

  /** 查询表信息并生成代码 */
  private void generatorCode(String tableName, ZipOutputStream zip) {
    // 查询表信息
    GenTable table = genTableMapper.selectGenTableByName(tableName);
    // 设置主子表信息
    setSubTable(table);
    // 设置主键列信息
    setPkColumn(table);

    VelocityInitializer.initVelocity();

    VelocityContext context = VelocityUtils.prepareContext(table);

    // 获取模板列表
    List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory());
    for (String template : templates) {
      // 渲染模板
      StringWriter sw = new StringWriter();
      Template tpl = Velocity.getTemplate(template, StandardCharsets.UTF_8.toString());
      tpl.merge(context, sw);
      try {
        // 添加到zip
        zip.putNextEntry(new ZipEntry(VelocityUtils.getFileName(template, table)));
        IOUtils.write(sw.toString(), zip, StandardCharsets.UTF_8.toString());
        IOUtils.closeQuietly(sw);
        zip.flush();
        zip.closeEntry();
      } catch (IOException e) {
        log.error("渲染模板失败，表名：" + table.getTableName(), e);
      }
    }
  }

  /**
   * 修改保存参数校验
   *
   * @param genTable 业务信息
   */
  @Override
  public void validateEdit(GenTable genTable) {
    if (GenConstants.TPL_TREE.equals(genTable.getTplCategory())) {
      String options = JSONUtil.toJsonStr(genTable.getParams());
      JSONObject paramsObj = JSONUtil.parseObj(options);
      if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_CODE))) {
        throw new IoTException("树编码字段不能为空");
      } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_PARENT_CODE))) {
        throw new IoTException("树父编码字段不能为空");
      } else if (StringUtils.isEmpty(paramsObj.getStr(GenConstants.TREE_NAME))) {
        throw new IoTException("树名称字段不能为空");
      } else if (GenConstants.TPL_SUB.equals(genTable.getTplCategory())) {
        if (StringUtils.isEmpty(genTable.getSubTableName())) {
          throw new IoTException("关联子表的表名不能为空");
        } else if (StringUtils.isEmpty(genTable.getSubTableFkName())) {
          throw new IoTException("子表关联的外键名不能为空");
        }
      }
    }
  }

  /**
   * 设置主键列信息
   *
   * @param table 业务表信息
   */
  public void setPkColumn(GenTable table) {
    for (GenTableColumn column : table.getColumns()) {
      if (column.isPk()) {
        table.setPkColumn(column);
        break;
      }
    }
    if (Objects.isNull(table.getPkColumn())) {
      table.setPkColumn(table.getColumns().get(0));
    }
    if (GenConstants.TPL_SUB.equals(table.getTplCategory())) {
      for (GenTableColumn column : table.getSubTable().getColumns()) {
        if (column.isPk()) {
          table.getSubTable().setPkColumn(column);
          break;
        }
      }
      if (Objects.isNull(table.getSubTable().getPkColumn())) {
        table.getSubTable().setPkColumn(table.getSubTable().getColumns().get(0));
      }
    }
  }

  /**
   * 设置主子表信息
   *
   * @param table 业务表信息
   */
  public void setSubTable(GenTable table) {
    String subTableName = table.getSubTableName();
    if (StringUtils.isNotEmpty(subTableName)) {
      table.setSubTable(genTableMapper.selectGenTableByName(subTableName));
    }
  }

  /**
   * 设置代码生成其他选项值
   *
   * @param genTable 设置后的生成对象
   */
  public void setTableFromOptions(GenTable genTable) {
    JSONObject paramsObj = JSONUtil.parseObj(genTable.getOptions());
    String treeCode = paramsObj.getStr(GenConstants.TREE_CODE);
    String treeParentCode = paramsObj.getStr(GenConstants.TREE_PARENT_CODE);
    String treeName = paramsObj.getStr(GenConstants.TREE_NAME);
    String parentMenuId = paramsObj.getStr(GenConstants.PARENT_MENU_ID);
    String parentMenuName = paramsObj.getStr(GenConstants.PARENT_MENU_NAME);

    genTable.setTreeCode(treeCode);
    genTable.setTreeParentCode(treeParentCode);
    genTable.setTreeName(treeName);
    genTable.setParentMenuId(parentMenuId);
    genTable.setParentMenuName(parentMenuName);
  }

  /**
   * 获取代码生成地址
   *
   * @param table 业务表信息
   * @param template 模板文件路径
   * @return 生成地址
   */
  public static String getGenPath(GenTable table, String template) {
    String genPath = table.getGenPath();
    if (StringUtils.equals(genPath, "/")) {
      return System.getProperty("user.dir")
          + File.separator
          + "src"
          + File.separator
          + VelocityUtils.getFileName(template, table);
    }
    return genPath + File.separator + VelocityUtils.getFileName(template, table);
  }
}
