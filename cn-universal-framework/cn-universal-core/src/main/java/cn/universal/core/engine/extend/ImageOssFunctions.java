/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 */

package cn.universal.core.engine.extend;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;

/**
 * Magic 脚本图片上传契约。
 *
 * <p>核心模块只定义契约，不依赖具体的对象存储实现。该契约继承 {@link IdeMagicFunction}， 实现类中使用 {@code @Function} 标记的方法会被 Magic
 * 注册器发现并直接提供给脚本调用。
 */
@FunctionalInterface
public interface ImageOssFunctions extends IdeMagicFunction {

  /**
   * 上传 Magic 解析得到的图片。
   *
   * <p>图片格式、字段识别、文件名和存储路径由实现方决定。上传失败必须返回空字符串，避免影响主流程。
   *
   * @param image 图片值，例如 URL、Base64/Data URL、byte[]、字节集合或包含图片字段的 Map
   * @return OSS URL，上传失败或输入不支持时返回空字符串
   */
  @Function
  @Comment("上传图片到 OSS，返回 OSS URL；有租户上下文时按租户配置上传，失败返回空字符串")
  String uploadImage(
      @Comment(name = "image", value = "图片 URL、Base64/Data URL、byte[]、字节集合或图片 Map") Object image);
}
