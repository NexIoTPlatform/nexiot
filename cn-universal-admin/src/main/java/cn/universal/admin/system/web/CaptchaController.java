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

package cn.universal.admin.system.web;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.universal.common.domain.R;
import cn.universal.common.utils.RSAUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 验证码操作处理 */
@RestController
@Tag(name = "验证码管理", description = "验证码相关接口")
@Slf4j
public class CaptchaController {

  @Resource private StringRedisTemplate stringRedisTemplate;

  // 可配置参数
  private static final int WIDTH = 160;
  private static final int HEIGHT = 60;
  private static final int LENGTH = 4;
  private static final String FONT_NAME = "Arial";
  private static final int FONT_SIZE = 48;
  private static final Color BACKGROUND = Color.WHITE;
  private static final String CAPTCHA_TYPE = "circle"; // 可选: line/shear/circle

  /**
   * 获取验证码图片和公钥
   *
   * @param type 可选参数，验证码类型(line/shear/circle)
   * @return 验证码信息
   */
  @Operation(summary = "获取验证码", description = "获取验证码图片和公钥")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取验证码",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "type", description = "验证码类型(line/shear/circle)", required = false)
  @GetMapping("/getCaptchaCode")
  public R getCode(@RequestParam(value = "type", required = false) String type) throws IOException {
    Map<String, Object> map = new HashMap<>();
    String uuid = IdUtil.simpleUUID();
    String verifyKey = "captcha_codes:" + uuid;

    // 生成验证码
    AbstractCaptcha captcha = createCaptcha(type);
    captcha.createCode();
    stringRedisTemplate.opsForValue().set(verifyKey, captcha.getCode(), 2, TimeUnit.MINUTES);
    map.put("uuid", uuid);
    map.put("img", captcha.getImageBase64());

    // 获取/生成RSA公钥
    try {
      String publicKey = stringRedisTemplate.opsForValue().get("RSAPublicKey");
      if (publicKey == null) {
        RSAUtils.genKeyPair();
        String rsaPublicKey = RSAUtils.keyMap.get(0);
        String rsaPrivateKey = RSAUtils.keyMap.get(1);
        publicKey = rsaPublicKey;
        stringRedisTemplate.opsForValue().set("RSAPublicKey", rsaPublicKey, 24, TimeUnit.HOURS);
        stringRedisTemplate.opsForValue().set("RSAPrivateKey", rsaPrivateKey, 24, TimeUnit.HOURS);
      }
      map.put("key", publicKey);
    } catch (Exception e) {
      log.error("rsa密钥生成失败,错误信息:{}", e.toString());
    }
    return R.ok(map);
  }

  /** 创建验证码对象，支持多种类型 */
  private AbstractCaptcha createCaptcha(String type) {
    String useType = (type == null || type.isEmpty()) ? CAPTCHA_TYPE : type;
    Font font = new Font(FONT_NAME, Font.BOLD, FONT_SIZE);
    AbstractCaptcha captcha;
    switch (useType) {
      case "circle":
        captcha = CaptchaUtil.createCircleCaptcha(WIDTH, HEIGHT, LENGTH, 20);
        break;
      case "shear":
        captcha = CaptchaUtil.createShearCaptcha(WIDTH, HEIGHT, LENGTH, 4);
        break;
      default:
        captcha = CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, LENGTH, 150);
        break;
    }
    captcha.setFont(font);
    captcha.setBackground(BACKGROUND);
    captcha.setGenerator(new RandomGenerator(LENGTH));
    return captcha;
  }
}
