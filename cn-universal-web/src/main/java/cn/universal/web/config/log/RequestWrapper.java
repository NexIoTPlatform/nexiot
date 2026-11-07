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

package cn.universal.web.config.log;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author 01
 *
 * @program wrapper-demo
 * @description 包装HttpServletRequest，目的是让其输入流可重复读
 * @create 2018-12-24 20:48
 * @since 1.0
 */
@Slf4j
public class RequestWrapper extends HttpServletRequestWrapper {

  /** 存储body数据的容器 */
  private final byte[] body;

  public RequestWrapper(HttpServletRequest request) throws IOException {
    super(request);

    // 将body数据存储起来
    String bodyStr = getBodyString(request);
    body = bodyStr.getBytes(Charset.defaultCharset());
  }

  /**
   * 获取请求Body
   *
   * @param request request
   * @return String
   */
  public String getBodyString(final ServletRequest request) {
    try {
      return inputStream2String(request.getInputStream());
    } catch (IOException e) {
      log.error("", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * 获取请求Body
   *
   * @return String
   */
  public String getBodyString() {
    final InputStream inputStream = new ByteArrayInputStream(body);

    return inputStream2String(inputStream);
  }

  /**
   * 将inputStream里的数据读取出来并转换成字符串
   *
   * @param inputStream inputStream
   * @return String
   */
  private String inputStream2String(InputStream inputStream) {
    StringBuilder sb = new StringBuilder();
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()));
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    } catch (IOException e) {
      log.error("", e);
      throw new RuntimeException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.error("", e);
        }
      }
    }

    return sb.toString();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {

    final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

    return new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return inputStream.read();
      }

      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener readListener) {}
    };
  }
}
