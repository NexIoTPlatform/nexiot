/*
 * Modbus RTU helpers for Magic-API
 */
package cn.universal.core.engine.extend;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class Lock_Functions implements IdeMagicFunction {

  @Function
  @Comment("4G蓝牙锁签名")
  public String sign_lock123(
      @Comment(name = "params", value = "签名对象") Object params,
      @Comment(name = "secretKey", value = "密钥") String secretKey) {
    if (params == null || secretKey == null) {
      return StrUtil.EMPTY;
    }
    try {
      String str = JSONUtil.toJsonStr(params);
      char charArray[] = str.toCharArray();
      Arrays.sort(charArray);
      String signStr = new String(charArray);
      return MD5.create()
          .digestHex(MD5.create().digestHex(signStr + secretKey).toUpperCase())
          .toUpperCase();
    } catch (Exception e) {

    }
    return StrUtil.EMPTY;
  }
}
