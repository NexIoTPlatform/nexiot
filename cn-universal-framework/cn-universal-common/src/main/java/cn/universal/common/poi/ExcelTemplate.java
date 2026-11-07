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

package cn.universal.common.poi;

import cn.universal.common.exception.BaseException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Excel相关处理工具类 */
public class ExcelTemplate {

  private static final Logger log = LoggerFactory.getLogger(ExcelTemplate.class);

  /** 工作薄对象 */
  private Workbook wb;

  /** 工作表对象 */
  private Sheet sheet;

  private CellStyle headerStyle;
  private CellStyle hiddenHeaderStyle;

  private Map<String, String> headers;
  private String excelName;

  private List<String> demoRow;
  private List<String> guideHeaders;
  private List<List<String>> guideRows;

  public ExcelTemplate(Map<String, String> headers, List<String> demoRow, String excelName) {
    this(headers, demoRow, excelName, null, null);
  }

  public ExcelTemplate(
      Map<String, String> headers,
      List<String> demoRow,
      String excelName,
      List<String> guideHeaders,
      List<List<String>> guideRows) {
    this.createWorkbook();
    this.headers = headers;
    this.excelName = excelName;
    this.demoRow = demoRow;
    this.guideHeaders = guideHeaders;
    this.guideRows = guideRows;
  }

  /** 创建一个工作簿 */
  public void createWorkbook() {
    this.wb = new SXSSFWorkbook(500);
  }

  public void init() {

    try {
      createSheet();

      // 产生一行
      Row row = sheet.createRow(0);

      int column = 0;
      row.setHeight((short) 0);
      // 写入各个字段的列头名称
      for (String header : headers.keySet()) {
        this.createNormalCell(header, row, column++);
      }

      column = 0;
      row = sheet.createRow(1);
      // 写入各个字段的列头名称
      for (String header : headers.values()) {
        this.createCell(header, row, column++);
      }

      column = 0;
      row = sheet.createRow(2);
      // 写入各个字段的列头名称
      for (String rowValue : demoRow) {
        this.createNormalCell(rowValue, row, column++);
      }

      if (guideHeaders != null && !guideHeaders.isEmpty()) {
        createGuideSheet();
      }

    } catch (Exception e) {
      log.error("导出Excel异常", e);
      throw new BaseException("导出Excel失败，请联系网站管理员！");
    }
  }

  public void setXSSFPrompt(
      Sheet sheet,
      String promptTitle,
      String promptContent,
      int firstRow,
      int endRow,
      int firstCol,
      int endCol) {
    DataValidationHelper helper = sheet.getDataValidationHelper();
    DataValidationConstraint constraint = helper.createCustomConstraint("DD1");
    CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
    DataValidation dataValidation = helper.createValidation(constraint, regions);
    dataValidation.createPromptBox(promptTitle, promptContent);
    dataValidation.setShowPromptBox(true);
    sheet.addValidationData(dataValidation);
  }

  /** 创建单元格 */
  public Cell createNormalCell(String header, Row row, int column) {
    // 创建列
    Cell cell = row.createCell(column);
    // 写入列信息
    cell.setCellValue(header);
    cell.setCellStyle(hiddenHeaderStyle);
    return cell;
  }

  /** 创建单元格 */
  public Cell createCell(String header, Row row, int column) {
    // 创建列
    Cell cell = row.createCell(column);
    // 写入列信息
    cell.setCellValue(header);
    cell.setCellStyle(headerStyle);
    return cell;
  }

  /** 创建工作表 */
  public void createSheet() {
    this.sheet = wb.createSheet();
    createHeaderStyles(wb);
    wb.setSheetName(0, excelName);
  }

  private void createGuideSheet() {
    Sheet guideSheet = wb.createSheet("字段说明");
    Row headerRow = guideSheet.createRow(0);
    for (int i = 0; i < guideHeaders.size(); i++) {
      createCell(guideHeaders.get(i), headerRow, i);
    }
    for (int rowIndex = 0; rowIndex < guideRows.size(); rowIndex++) {
      Row dataRow = guideSheet.createRow(rowIndex + 1);
      List<String> row = guideRows.get(rowIndex);
      for (int colIndex = 0; colIndex < row.size(); colIndex++) {
        Cell cell = dataRow.createCell(colIndex);
        cell.setCellValue(row.get(colIndex));
      }
    }
  }

  private void createHeaderStyles(Workbook wb) {
    headerStyle = wb.createCellStyle();
    headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    headerStyle.setBorderRight(BorderStyle.THIN);
    headerStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    Font headerFont = wb.createFont();
    headerFont.setFontName("Arial");
    headerFont.setFontHeightInPoints((short) 10);
    headerFont.setBold(true);
    headerFont.setColor(IndexedColors.WHITE.getIndex());
    headerStyle.setFont(headerFont);

    hiddenHeaderStyle = wb.createCellStyle();
    hiddenHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    hiddenHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
    headerFont = wb.createFont();
    headerFont.setFontName("Arial");
    headerFont.setFontHeightInPoints((short) 10);
    headerFont.setColor(IndexedColors.BLACK.getIndex());
    hiddenHeaderStyle.setFont(headerFont);
  }

  public void exportExcel(HttpServletResponse response) {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    this.init();
    try {
      wb.write(response.getOutputStream());
    } catch (Exception e) {
      log.error("导出Excel异常", e);
    } finally {
      IOUtils.closeQuietly(wb);
    }
  }
}
