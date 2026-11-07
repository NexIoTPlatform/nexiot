/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.persistence.entity.bo;

import cn.universal.common.annotation.Excel;
import java.io.Serializable;
import lombok.Data;

/** 用户批量导入模板。只开放允许由管理员维护的字段。 */
@Data
public class IoTUserImportBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Excel(name = "用户名", sort = 1, prompt = "必填，2-30个字符")
  private String username;

  @Excel(name = "用户昵称", sort = 2, prompt = "必填，2-30个字符")
  private String alias;

  @Excel(name = "密码", sort = 3, prompt = "新增必填，需包含字母、数字和特殊字符，长度8-30位")
  private String password;

  @Excel(name = "手机号", sort = 4, prompt = "必填")
  private String mobile;

  @Excel(name = "邮箱", sort = 5)
  private String email;

  @Excel(name = "部门ID", sort = 6, prompt = "租户成员可选，留空时使用默认部门")
  private Long deptId;

  @Excel(name = "状态", sort = 7, readConverterExp = "0=正常,1=停用", defaultValue = "0")
  private Integer status;

  @Excel(name = "备注", sort = 8)
  private String remark;
}
