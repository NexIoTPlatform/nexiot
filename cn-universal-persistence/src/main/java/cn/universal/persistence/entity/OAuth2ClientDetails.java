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

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "oauth_client_details")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
@Schema(description = "OAuth2客户端详情实体")
public class OAuth2ClientDetails implements Serializable {

  /** 客户端标识 */
  @Id
  @Column(name = "client_id")
  @Schema(description = "客户端标识")
  private String clientId;

  /** 资源标识 */
  @Column(name = "resource_ids")
  @Schema(description = "资源标识")
  private String resourceIds;

  /** 客户端秘钥 */
  @Column(name = "client_secret")
  @Schema(description = "客户端秘钥")
  private String clientSecret;

  @Schema(description = "授权范围")
  private String scope;

  /**
   * 授权类型 Authorization_code：授权码模式 refresh_token：刷新token password：密码模式 implicit：隐私授权模式
   * client_credentials：客户端模式
   */
  @Column(name = "Authorized_grant_types")
  @Schema(description = "授权类型")
  private String AuthorizedGrantTypes;

  /** 授权成功后回调URL */
  @Column(name = "web_server_redirect_uri")
  @Schema(description = "授权回调URL")
  private String webServerRedirectUri;

  @Schema(description = "权限")
  private String Authorities;

  /** 访问令牌的有效时间 */
  @Column(name = "access_token_validity")
  @Schema(description = "访问令牌有效期(秒)")
  private Integer accessTokenValidity;

  /** 刷新令牌的有效时间 */
  @Column(name = "refresh_token_validity")
  @Schema(description = "刷新令牌有效期(秒)")
  private Integer refreshTokenValidity;

  /** 额外的信息，备用 */
  @Column(name = "additional_information")
  @Schema(description = "附加信息")
  private String additionalInformation;

  /** 自动批准标识：0代表不显示授权页面 1显示授权页面 */
  @Schema(description = "自动批准标识")
  private String autoapprove;

  /** 用户名 */
  @Schema(description = "用户名")
  private String username;

  /** 密码 */
  @Schema(description = "密码")
  private String password;

  /** 唯一标识 */
  @Column(name = "iot_union_id")
  @Schema(description = "IoT联合ID")
  private String iotUnionId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  private static final long serialVersionUID = 1L;

  /**
   * 获取客户端标识
   *
   * @return client_id - 客户端标识
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * 设置客户端标识
   *
   * @param clientId 客户端标识
   */
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /**
   * 获取资源标识
   *
   * @return resource_ids - 资源标识
   */
  public String getResourceIds() {
    return resourceIds;
  }

  /**
   * 设置资源标识
   *
   * @param resourceIds 资源标识
   */
  public void setResourceIds(String resourceIds) {
    this.resourceIds = resourceIds;
  }

  /**
   * 获取客户端秘钥
   *
   * @return client_secret - 客户端秘钥
   */
  public String getClientSecret() {
    return clientSecret;
  }

  /**
   * 设置客户端秘钥
   *
   * @param clientSecret 客户端秘钥
   */
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  /**
   * @return scope
   */
  public String getScope() {
    return scope;
  }

  /**
   * @param scope
   */
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
   * 获取授权类型 Authorization_code：授权码模式 refresh_token：刷新token password：密码模式 implicit：隐私授权模式
   * client_credentials：客户端模式
   *
   * @return Authorized_grant_types - 授权类型 Authorization_code：授权码模式 refresh_token：刷新token
   *     password：密码模式 implicit：隐私授权模式 client_credentials：客户端模式
   */
  public String getAuthorizedGrantTypes() {
    return AuthorizedGrantTypes;
  }

  /**
   * 设置授权类型 Authorization_code：授权码模式 refresh_token：刷新token password：密码模式 implicit：隐私授权模式
   * client_credentials：客户端模式
   *
   * @param AuthorizedGrantTypes 授权类型 Authorization_code：授权码模式 refresh_token：刷新token password：密码模式
   *     implicit：隐私授权模式 client_credentials：客户端模式
   */
  public void setAuthorizedGrantTypes(String AuthorizedGrantTypes) {
    this.AuthorizedGrantTypes = AuthorizedGrantTypes;
  }

  /**
   * 获取授权成功后回调URL
   *
   * @return web_server_redirect_uri - 授权成功后回调URL
   */
  public String getWebServerRedirectUri() {
    return webServerRedirectUri;
  }

  /**
   * 设置授权成功后回调URL
   *
   * @param webServerRedirectUri 授权成功后回调URL
   */
  public void setWebServerRedirectUri(String webServerRedirectUri) {
    this.webServerRedirectUri = webServerRedirectUri;
  }

  /**
   * @return Authorities
   */
  public String getAuthorities() {
    return Authorities;
  }

  /**
   * @param Authorities
   */
  public void setAuthorities(String Authorities) {
    this.Authorities = Authorities;
  }

  /**
   * 获取访问令牌的有效时间
   *
   * @return access_token_validity - 访问令牌的有效时间
   */
  public Integer getAccessTokenValidity() {
    return accessTokenValidity;
  }

  /**
   * 设置访问令牌的有效时间
   *
   * @param accessTokenValidity 访问令牌的有效时间
   */
  public void setAccessTokenValidity(Integer accessTokenValidity) {
    this.accessTokenValidity = accessTokenValidity;
  }

  /**
   * 获取刷新令牌的有效时间
   *
   * @return refresh_token_validity - 刷新令牌的有效时间
   */
  public Integer getRefreshTokenValidity() {
    return refreshTokenValidity;
  }

  /**
   * 设置刷新令牌的有效时间
   *
   * @param refreshTokenValidity 刷新令牌的有效时间
   */
  public void setRefreshTokenValidity(Integer refreshTokenValidity) {
    this.refreshTokenValidity = refreshTokenValidity;
  }

  /**
   * 获取额外的信息，备用
   *
   * @return additional_information - 额外的信息，备用
   */
  public String getAdditionalInformation() {
    return additionalInformation;
  }

  /**
   * 设置额外的信息，备用
   *
   * @param additionalInformation 额外的信息，备用
   */
  public void setAdditionalInformation(String additionalInformation) {
    this.additionalInformation = additionalInformation;
  }

  /**
   * 获取自动批准标识：0代表不显示授权页面 1显示授权页面
   *
   * @return autoapprove - 自动批准标识：0代表不显示授权页面 1显示授权页面
   */
  public String getAutoapprove() {
    return autoapprove;
  }

  /**
   * 设置自动批准标识：0代表不显示授权页面 1显示授权页面
   *
   * @param autoapprove 自动批准标识：0代表不显示授权页面 1显示授权页面
   */
  public void setAutoapprove(String autoapprove) {
    this.autoapprove = autoapprove;
  }

  /**
   * 获取用户名
   *
   * @return username - 用户名
   */
  public String getUsername() {
    return username;
  }

  /**
   * 设置用户名
   *
   * @param username 用户名
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * 获取密码
   *
   * @return password - 密码
   */
  public String getPassword() {
    return password;
  }

  /**
   * 设置密码
   *
   * @param password 密码
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * 获取唯一标识
   *
   * @return iot_union_id - 唯一标识
   */
  public String getIotUnionId() {
    return iotUnionId;
  }

  /**
   * 设置唯一标识
   *
   * @param iotUnionId 唯一标识
   */
  public void setIotUnionId(String iotUnionId) {
    this.iotUnionId = iotUnionId;
  }
}
