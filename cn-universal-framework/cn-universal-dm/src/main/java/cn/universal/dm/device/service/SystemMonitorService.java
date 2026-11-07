package cn.universal.dm.device.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Linux系统监控服务 获取CPU、内存、磁盘、网络流量数据
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Service
public class SystemMonitorService {

  // 系统监控数据缓存
  private final AtomicReference<Map<String, Object>> systemMetrics =
      new AtomicReference<>(new HashMap<>());

  // 上次CPU时间
  private long lastCpuTime = 0;
  private long lastCpuIdleTime = 0;

  /** 定时更新系统监控数据（每5秒执行一次） */
  //  @Scheduled(fixedRate = 5000)
  public void updateSystemMetrics() {
    try {
      Map<String, Object> metrics = new HashMap<>();

      // 获取CPU使用率
      double cpuUsage = getCpuUsage();
      metrics.put("cpuUsage", cpuUsage);

      // 获取内存使用率
      double memoryUsage = getMemoryUsage();
      metrics.put("memoryUsage", memoryUsage);

      // 获取磁盘使用率
      double diskUsage = getDiskUsage();
      metrics.put("diskUsage", diskUsage);

      // 获取网络流量
      Map<String, Object> networkTraffic = getNetworkTraffic();
      metrics.put("networkTraffic", networkTraffic);

      // 获取系统负载
      Map<String, Object> systemLoad = getSystemLoad();
      metrics.put("systemLoad", systemLoad);

      // 更新时间戳
      metrics.put("timestamp", System.currentTimeMillis());

      // 更新缓存
      systemMetrics.set(metrics);

      log.debug("[系统监控] 系统指标已更新: CPU={}%, Memory={}%, Disk={}%", cpuUsage, memoryUsage, diskUsage);

    } catch (Exception e) {
      log.error("[系统监控] 更新系统指标失败", e);
    }
  }

  /** 获取CPU使用率 */
  private double getCpuUsage() {
    try {
      // 读取/proc/stat文件获取CPU信息
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  new ProcessBuilder("cat", "/proc/stat").start().getInputStream()));

      String line = reader.readLine();
      if (line != null && line.startsWith("cpu ")) {
        String[] parts = line.split("\\s+");

        if (parts.length >= 8) {
          try {
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            long iowait = Long.parseLong(parts[5]);
            long irq = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long totalCpuTime = user + nice + system + idle + iowait + irq + softirq;
            long totalCpuIdleTime = idle + iowait;

            if (lastCpuTime > 0) {
              long totalDiff = totalCpuTime - lastCpuTime;
              long idleDiff = totalCpuIdleTime - lastCpuIdleTime;

              if (totalDiff > 0) {
                double cpuUsage = 100.0 * (1.0 - (double) idleDiff / totalDiff);
                lastCpuTime = totalCpuTime;
                lastCpuIdleTime = totalCpuIdleTime;
                return Math.min(100.0, Math.max(0.0, cpuUsage));
              }
            } else {
              lastCpuTime = totalCpuTime;
              lastCpuIdleTime = totalCpuIdleTime;
            }
          } catch (NumberFormatException e) {
            log.error("[系统监控] 解析CPU数据失败: {}", line, e);
          }
        }
      }
      reader.close();
    } catch (Exception e) {
      log.error("[系统监控] 获取CPU使用率失败", e);
    }
    return 0.0;
  }

  /** 获取内存使用率 */
  private double getMemoryUsage() {
    try {
      // 读取/proc/meminfo文件获取内存信息
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  new ProcessBuilder("cat", "/proc/meminfo").start().getInputStream()));

      long totalMemory = 0;
      long availableMemory = 0;

      String line;
      while ((line = reader.readLine()) != null) {
        try {
          if (line.startsWith("MemTotal:")) {
            // 解析格式: "MemTotal:        8131912 kB"
            String[] parts = line.split("\\s+");
            if (parts.length >= 3) {
              totalMemory = Long.parseLong(parts[1]);
              log.debug("[系统监控] 总内存: {} kB", totalMemory);
            }
          } else if (line.startsWith("MemAvailable:")) {
            // 解析格式: "MemAvailable:    4029336 kB"
            String[] parts = line.split("\\s+");
            if (parts.length >= 3) {
              availableMemory = Long.parseLong(parts[1]);
              log.debug("[系统监控] 可用内存: {} kB", availableMemory);
              break;
            }
          }
        } catch (NumberFormatException e) {
          log.debug("[系统监控] 跳过无法解析的内存信息行: {}", line);
        }
      }
      reader.close();

      if (totalMemory > 0) {
        double usage = 100.0 * (1.0 - (double) availableMemory / totalMemory);
        log.debug(
            "[系统监控] 内存使用率计算: 总内存={} kB, 可用内存={} kB, 使用率={}%", totalMemory, availableMemory, usage);
        return Math.min(100.0, Math.max(0.0, usage));
      }
    } catch (Exception e) {
      log.error("[系统监控] 获取内存使用率失败", e);
    }
    return 0.0;
  }

  /** 获取磁盘使用率 */
  private double getDiskUsage() {
    try {
      // 使用df命令获取磁盘使用情况
      Process process = new ProcessBuilder("df", "/").start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      // 跳过标题行
      reader.readLine();

      String line = reader.readLine();
      if (line != null) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 5) {
          try {
            String usageStr = parts[4].replace("%", "");
            return Double.parseDouble(usageStr);
          } catch (NumberFormatException e) {
            log.error("[系统监控] 解析磁盘使用率失败: {}", parts[4], e);
          }
        }
      }
      reader.close();
    } catch (Exception e) {
      log.error("[系统监控] 获取磁盘使用率失败", e);
    }
    return 0.0;
  }

  /** 获取网络流量 */
  private Map<String, Object> getNetworkTraffic() {
    Map<String, Object> networkData = new HashMap<>();

    try {
      // 读取/proc/net/dev文件获取网络统计
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  new ProcessBuilder("cat", "/proc/net/dev").start().getInputStream()));

      String line;
      long totalRxBytes = 0;
      long totalTxBytes = 0;

      while ((line = reader.readLine()) != null) {
        // 跳过标题行
        if (line.contains("Inter-|") || line.trim().isEmpty()) {
          continue;
        }

        // 解析网络接口行
        if (line.contains(":")) {
          String[] parts = line.split("\\s+");
          if (parts.length >= 10) {
            try {
              // 获取接口名称（去掉冒号）
              String interfaceName = parts[0].replace(":", "");

              // 跳过lo接口（本地回环）和虚拟接口
              if (!interfaceName.equals("lo")
                  && !interfaceName.startsWith("docker")
                  && !interfaceName.startsWith("veth")
                  && !interfaceName.startsWith("br-")) {

                // 解析接收字节数（第2个数字）
                long rxBytes = Long.parseLong(parts[1]);
                // 解析发送字节数（第10个数字）
                long txBytes = Long.parseLong(parts[9]);

                totalRxBytes += rxBytes;
                totalTxBytes += txBytes;

                log.debug(
                    "[系统监控] 网络接口: {}, 接收: {} bytes, 发送: {} bytes", interfaceName, rxBytes, txBytes);
              }
            } catch (NumberFormatException e) {
              // 跳过无法解析的行
              log.debug("[系统监控] 跳过无法解析的网络接口行: {}", line);
            }
          }
        }
      }
      reader.close();

      // 转换为MB/s（这里简化处理，实际应该计算速率）
      double rxMBps = totalRxBytes / (1024.0 * 1024.0);
      double txMBps = totalTxBytes / (1024.0 * 1024.0);

      networkData.put("rxMBps", rxMBps);
      networkData.put("txMBps", txMBps);
      networkData.put("totalRxBytes", totalRxBytes);
      networkData.put("totalTxBytes", totalTxBytes);

      log.debug("[系统监控] 网络流量统计: 接收 {} MB, 发送 {} MB", rxMBps, txMBps);

      // 如果没有检测到网络流量，记录调试信息
      if (totalRxBytes == 0 && totalTxBytes == 0) {
        log.debug("[系统监控] 未检测到网络流量，可能需要检查网络接口配置");
      }

    } catch (Exception e) {
      log.error("[系统监控] 获取网络流量失败", e);
      // 返回默认值
      networkData.put("rxMBps", 0.0);
      networkData.put("txMBps", 0.0);
      networkData.put("totalRxBytes", 0L);
      networkData.put("totalTxBytes", 0L);
    }

    return networkData;
  }

  /** 获取系统负载 */
  private Map<String, Object> getSystemLoad() {
    Map<String, Object> loadData = new HashMap<>();

    try {
      // 读取/proc/loadavg文件获取系统负载
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  new ProcessBuilder("cat", "/proc/loadavg").start().getInputStream()));

      String line = reader.readLine();
      if (line != null) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 3) {
          try {
            loadData.put("load1min", Double.parseDouble(parts[0]));
            loadData.put("load5min", Double.parseDouble(parts[1]));
            loadData.put("load15min", Double.parseDouble(parts[2]));
          } catch (NumberFormatException e) {
            log.error("[系统监控] 解析系统负载失败: {}", line, e);
          }
        }
      }
      reader.close();

    } catch (Exception e) {
      log.error("[系统监控] 获取系统负载失败", e);
    }

    return loadData;
  }

  /** 获取系统监控数据 */
  public Map<String, Object> getSystemMetrics() {
    return systemMetrics.get();
  }

  /** 获取CPU使用率 */
  public double getCpuUsageRate() {
    Map<String, Object> metrics = systemMetrics.get();
    return (Double) metrics.getOrDefault("cpuUsage", 0.0);
  }

  /** 获取内存使用率 */
  public double getMemoryUsageRate() {
    Map<String, Object> metrics = systemMetrics.get();
    return (Double) metrics.getOrDefault("memoryUsage", 0.0);
  }

  /** 获取磁盘使用率 */
  public double getDiskUsageRate() {
    Map<String, Object> metrics = systemMetrics.get();
    return (Double) metrics.getOrDefault("diskUsage", 0.0);
  }

  /** 获取网络流量（MB/s） */
  public double getNetworkTrafficMBps() {
    Map<String, Object> metrics = systemMetrics.get();
    Map<String, Object> networkData =
        (Map<String, Object>) metrics.getOrDefault("networkTraffic", new HashMap<>());
    return (Double) networkData.getOrDefault("rxMBps", 0.0);
  }

  /** 检查是否为Linux系统 */
  public boolean isLinuxSystem() {
    String os = System.getProperty("os.name").toLowerCase();
    return os.contains("linux");
  }

  /** 获取系统信息 */
  public Map<String, Object> getSystemInfo() {
    Map<String, Object> systemInfo = new HashMap<>();

    try {
      systemInfo.put("osName", System.getProperty("os.name"));
      systemInfo.put("osVersion", System.getProperty("os.version"));
      systemInfo.put("osArch", System.getProperty("os.arch"));
      systemInfo.put("javaVersion", System.getProperty("java.version"));
      systemInfo.put("javaVendor", System.getProperty("java.vendor"));
      systemInfo.put("userHome", System.getProperty("user.home"));
      systemInfo.put("userDir", System.getProperty("user.dir"));

      // 获取处理器信息
      int processors = Runtime.getRuntime().availableProcessors();
      systemInfo.put("processors", processors);

      // 获取JVM内存信息
      Runtime runtime = Runtime.getRuntime();
      long totalMemory = runtime.totalMemory();
      long freeMemory = runtime.freeMemory();
      long usedMemory = totalMemory - freeMemory;

      systemInfo.put("jvmTotalMemory", totalMemory);
      systemInfo.put("jvmFreeMemory", freeMemory);
      systemInfo.put("jvmUsedMemory", usedMemory);
      systemInfo.put(
          "jvmMemoryUsage", totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0.0);

      // 添加调试信息
      systemInfo.put("isLinuxSystem", isLinuxSystem());

    } catch (Exception e) {
      log.error("[系统监控] 获取系统信息失败", e);
    }

    return systemInfo;
  }

  /** 获取调试信息 */
  public Map<String, Object> getDebugInfo() {
    Map<String, Object> debugInfo = new HashMap<>();

    try {
      // 检查关键文件是否存在
      debugInfo.put("procStatExists", new File("/proc/stat").exists());
      debugInfo.put("procMeminfoExists", new File("/proc/meminfo").exists());
      debugInfo.put("procNetDevExists", new File("/proc/net/dev").exists());
      debugInfo.put("procLoadavgExists", new File("/proc/loadavg").exists());

      // 获取网络接口信息
      try {
        Process process = new ProcessBuilder("ip", "link", "show").start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> interfaces = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.contains(":") && !line.contains("lo:")) {
            interfaces.add(line.trim());
          }
        }
        reader.close();
        debugInfo.put("networkInterfaces", interfaces);
      } catch (Exception e) {
        debugInfo.put("networkInterfacesError", e.getMessage());
      }

    } catch (Exception e) {
      log.error("[系统监控] 获取调试信息失败", e);
    }

    return debugInfo;
  }
}
