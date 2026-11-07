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

package cn.universal.ossm.service;

import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.entity.bo.SysOssBo;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 *
 * <p>提供IoT平台中文件存储和管理功能，包括： - 文件上传（支持MultipartFile和InputStream） - 文件信息查询和分页 - 文件删除和批量删除 - 视频文件特殊处理
 *
 * <p>支持多种文件类型，包括普通文件、视频文件等 @Author Lion Li
 */
public interface ISysOssService {

  /**
   * 根据OSS ID查询文件信息
   *
   * <p>通过文件在OSS系统中的唯一标识获取文件的详细信息
   *
   * @param ossId OSS文件ID
   * @return 文件信息，如果不存在返回null
   */
  SysOss getById(Long ossId);

  /**
   * 分页查询文件列表
   *
   * <p>根据查询条件分页获取文件列表，支持多种查询条件
   *
   * @param sysOss 查询条件对象，包含分页参数和查询条件
   * @return 文件列表
   */
  List<SysOss> queryPageList(SysOssBo sysOss);

  /**
   * 上传文件（MultipartFile方式）
   *
   * <p>处理Web表单上传的文件，自动关联到指定用户 适用于Web界面的文件上传功能
   *
   * @param file 上传的文件对象
   * @param unionId 用户唯一标识，用于关联文件所有者
   * @return 上传后的文件信息
   */
  SysOss upload(MultipartFile file, String unionId);

  /** 上传Web表单文件，并按租户隔离保存。实现侧必须使用流式读取，避免大文件整包进入堆内存。 */
  SysOss upload(MultipartFile file, String unionId, String tenantId);

  /**
   * 上传文件（InputStream方式）
   *
   * <p>处理流式文件上传，适用于程序内部的文件上传 支持任意类型的文件上传
   *
   * @param inputStream 文件输入流
   * @param fileName 文件名
   * @return 上传后的文件信息
   */
  SysOss uploadStream(InputStream inputStream, String fileName);

  /**
   * 上传视频文件（InputStream方式）
   *
   * <p>专门处理视频文件的上传，可能包含视频特有的处理逻辑 如视频格式验证、缩略图生成等
   *
   * @param inputStream 视频文件输入流
   * @param fileName 视频文件名
   * @return 上传后的视频文件信息
   */
  SysOss uploadVideoStream(InputStream inputStream, String fileName);

  /**
   * 批量删除文件（带验证）
   *
   * <p>根据文件ID列表批量删除文件，支持删除前的业务验证 验证失败的文件不会被删除
   *
   * @param ids 要删除的文件ID集合
   * @param isValid 是否进行业务验证
   * @return 删除操作是否成功
   */
  Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

  /**
   * 根据URL批量删除文件（带验证）
   *
   * <p>根据文件URL列表批量删除文件，支持删除前的业务验证 适用于通过文件访问地址进行删除的场景
   *
   * @param urls 要删除的文件URL数组
   * @param isValid 是否进行业务验证
   * @return 删除操作是否成功
   */
  Boolean deleteWithValidByUrls(String[] urls, Boolean isValid);
}
