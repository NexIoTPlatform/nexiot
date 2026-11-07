/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 设备同步处理器接口
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import java.util.ArrayList;
import cn.universal.persistence.entity.VideoPlatformInstance;
import java.util.List;
import java.util.Map;

/**
 * 设备同步处理器接口
 * 不同平台实现各自的同步逻辑
 */
public interface DeviceSyncHandler {

  /**
   * 获取支持的平台类型
   */
  String getPlatformType();

  /**
   * 同步设备列表到数据库
   * 
   * @param instance 平台实例
   * @param filters 过滤条件
   * @return 同步结果统计
   */
  DeviceSyncResult syncDevices(VideoPlatformInstance instance, Map<String, Object> filters);

  /**
   * 设备同步结果
   */
  class DeviceSyncResult {
    private int totalDevices; // 总设备数
    private int insertedDevices; // 新增设备数
    private int updatedDevices; // 更新设备数
    private int totalChannels; // 总通道数
    private int insertedChannels; // 新增通道数
    private List<String> errors; // 错误信息列表

    public DeviceSyncResult() {}

    public DeviceSyncResult(
        int totalDevices,
        int insertedDevices,
        int updatedDevices,
        int totalChannels,
        int insertedChannels) {
      this.totalDevices = totalDevices;
      this.insertedDevices = insertedDevices;
      this.updatedDevices = updatedDevices;
      this.totalChannels = totalChannels;
      this.insertedChannels = insertedChannels;
    }

    // Getters and Setters
    public int getTotalDevices() {
      return totalDevices;
    }

    public void setTotalDevices(int totalDevices) {
      this.totalDevices = totalDevices;
    }

    public int getInsertedDevices() {
      return insertedDevices;
    }

    public void setInsertedDevices(int insertedDevices) {
      this.insertedDevices = insertedDevices;
    }

    public int getUpdatedDevices() {
      return updatedDevices;
    }

    public void setUpdatedDevices(int updatedDevices) {
      this.updatedDevices = updatedDevices;
    }

    public int getTotalChannels() {
      return totalChannels;
    }

    public void setTotalChannels(int totalChannels) {
      this.totalChannels = totalChannels;
    }

    public int getInsertedChannels() {
      return insertedChannels;
    }

    public void setInsertedChannels(int insertedChannels) {
      this.insertedChannels = insertedChannels;
    }

    public List<String> getErrors() {
      return errors;
    }

    public void setErrors(List<String> errors) {
      this.errors = errors;
    }

    public void addInsertedDevice() {
      this.insertedDevices++;
    }

    public void addUpdatedDevice() {
      this.updatedDevices++;
    }

    public void addChannel(int count) {
      this.totalChannels += count;
      this.insertedChannels += count;
    }

    public void addError(String error) {
      if (error == null || error.isBlank()) {
        return;
      }
      if (this.errors == null) {
        this.errors = new ArrayList<>();
      }
      this.errors.add(error);
    }

    public void mergeFrom(DeviceSyncResult other) {
      if (other == null) {
        return;
      }
      this.totalDevices += other.totalDevices;
      this.insertedDevices += other.insertedDevices;
      this.updatedDevices += other.updatedDevices;
      this.totalChannels += other.totalChannels;
      this.insertedChannels += other.insertedChannels;
      if (other.errors != null && !other.errors.isEmpty()) {
        if (this.errors == null) {
          this.errors = new ArrayList<>();
        }
        this.errors.addAll(other.errors);
      }
    }
  }
}
