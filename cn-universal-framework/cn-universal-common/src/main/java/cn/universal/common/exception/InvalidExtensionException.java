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

package cn.universal.common.exception;

import java.util.Arrays;
import org.apache.commons.fileupload.FileUploadException;

/** 文件上传 误异常类 @Author ruoyi */
public class InvalidExtensionException extends FileUploadException {

  private static final long serialVersionUID = 1L;

  private String[] allowedExtension;
  private String extension;
  private String filename;

  public InvalidExtensionException(String[] allowedExtension, String extension, String filename) {
    super(
        "filename : ["
            + filename
            + "], extension : ["
            + extension
            + "], allowed extension : ["
            + Arrays.toString(allowedExtension)
            + "]");
    this.allowedExtension = allowedExtension;
    this.extension = extension;
    this.filename = filename;
  }

  public String[] getAllowedExtension() {
    return allowedExtension;
  }

  public String getExtension() {
    return extension;
  }

  public String getFilename() {
    return filename;
  }

  public static class InvalidImageExtensionException extends InvalidExtensionException {

    private static final long serialVersionUID = 1L;

    public InvalidImageExtensionException(
        String[] allowedExtension, String extension, String filename) {
      super(allowedExtension, extension, filename);
    }
  }

  public static class InvalidFlashExtensionException extends InvalidExtensionException {

    private static final long serialVersionUID = 1L;

    public InvalidFlashExtensionException(
        String[] allowedExtension, String extension, String filename) {
      super(allowedExtension, extension, filename);
    }
  }

  public static class InvalidMediaExtensionException extends InvalidExtensionException {

    private static final long serialVersionUID = 1L;

    public InvalidMediaExtensionException(
        String[] allowedExtension, String extension, String filename) {
      super(allowedExtension, extension, filename);
    }
  }

  public static class InvalidVideoExtensionException extends InvalidExtensionException {

    private static final long serialVersionUID = 1L;

    public InvalidVideoExtensionException(
        String[] allowedExtension, String extension, String filename) {
      super(allowedExtension, extension, filename);
    }
  }
}
