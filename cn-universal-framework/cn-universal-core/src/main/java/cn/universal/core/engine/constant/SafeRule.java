package cn.universal.core.engine.constant;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author gitee.com/NexIoT
 *
 * @version 1.0
 * @since 2023/5/19
 */

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/5/19
 */
public class SafeRule {

  public static Set<String> packages =
      Stream.of(
              "cn.hutool.json.JSONUtil",
              "cn.hutool.service.util.HexUtil",
              "cn.hutool.service.util.RandomUtil",
              "cn.hutool.service.util.StrUtil")
          .collect(Collectors.toSet());

  public static boolean filter(String packageName) {
    return packageName != null
        && !"".equalsIgnoreCase(packageName)
        && !packages.contains(packageName);
  }
}
