/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 */

package cn.universal.ossm.extend;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import cn.universal.core.engine.extend.ImageOssFunctions;
import cn.universal.ossm.util.VideoSnapshotOssHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OSS-backed image upload function exposed to user Magic scripts.
 *
 * <p>The actual image detection and naming rules live in {@link VideoSnapshotOssHelper}, so the
 * Magic engine remains independent from any object-storage provider.
 */
@Component
@RequiredArgsConstructor
public class OssImageUploadFunction implements ImageOssFunctions {

  private final VideoSnapshotOssHelper snapshotOssHelper;

  /** Magic function name in camelCase form. */
  @Override
  @Function
  @Comment("上传图片到 OSS，返回 OSS URL；有租户上下文时按租户配置上传，失败返回空字符串")
  public String uploadImage(
      @Comment(name = "image", value = "图片 URL、Base64/Data URL、byte[]、字节集合或图片 Map")
          Object image) {
    if (image == null) {
      return StrUtil.EMPTY;
    }

    Map<String, Object> source = toSourceMap(image);
    Map<String, Object> params = new LinkedHashMap<>();
    // Keep the original value because this function only returns the URL.
    params.put("keepRawSnapshotData", true);
    try {
      Map<String, Object> uploaded =
          snapshotOssHelper.uploadSnapshotIfPresent(null, params, source, "magic");
      Object url = uploaded.get("ossUrl");
      return url == null ? StrUtil.EMPTY : String.valueOf(url);
    } catch (Exception ignored) {
      // A failed optional upload must not break the user's Magic decoding result.
      return StrUtil.EMPTY;
    }
  }

  /** Snake-case alias for scripts that use the naming style of the existing extensions. */
  @Function
  @Comment("上传图片到 OSS，返回 OSS URL；有租户上下文时按租户配置上传，失败返回空字符串")
  public String upload_image(
      @Comment(name = "image", value = "图片 URL、Base64/Data URL、byte[]、字节集合或图片 Map")
          Object image) {
    return uploadImage(image);
  }

  private Map<String, Object> toSourceMap(Object image) {
    if (image instanceof Map<?, ?> map) {
      Map<String, Object> source = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (entry.getKey() != null) {
          source.put(String.valueOf(entry.getKey()), entry.getValue());
        }
      }
      return source;
    }
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("data", image);
    return source;
  }
}
