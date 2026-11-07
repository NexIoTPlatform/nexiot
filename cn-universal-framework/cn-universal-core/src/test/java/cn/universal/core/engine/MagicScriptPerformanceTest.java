package cn.universal.core.engine;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class MagicScriptPerformanceTest {

  @Test
  public void shouldPrintMixedIngressDecodeBenchmark() {
    BenchmarkMetrics metrics = runMixedIngressDecodeBenchmark();
    Assert.assertTrue(metrics.successful);
  }

  @Test
  public void shouldPrintDensePointRoutingBenchmark() {
    BenchmarkMetrics metrics = runDensePointRoutingBenchmark();
    Assert.assertTrue(metrics.successful);
  }

  @Test
  public void shouldPrintGatewayBatchIngressBenchmark() {
    BenchmarkMetrics metrics = runGatewayBatchIngressBenchmark();
    Assert.assertTrue(metrics.successful);
  }

  public static void main(String[] args) {
    MagicScriptPerformanceTest test = new MagicScriptPerformanceTest();
    BenchmarkMetrics[] metrics =
        new BenchmarkMetrics[] {
          test.runMixedIngressDecodeBenchmark(),
          test.runDensePointRoutingBenchmark(),
          test.runGatewayBatchIngressBenchmark()
        };
    test.printSuiteSummary(metrics);
  }

  private BenchmarkMetrics runMixedIngressDecodeBenchmark() {
    Workload workload = buildMixedIngressWorkload(360, 14);
    return benchmark("mixed-ingress-decode", workload, 24, 100, "mixed-ingress.ms");
  }

  private BenchmarkMetrics runDensePointRoutingBenchmark() {
    Workload workload = buildDensePointRoutingWorkload(72, 96);
    return benchmark("dense-point-routing", workload, 20, 80, "dense-point-routing.ms");
  }

  private BenchmarkMetrics runGatewayBatchIngressBenchmark() {
    Workload workload = buildGatewayBatchIngressWorkload(36, 18, 14);
    return benchmark("gateway-batch-ingress", workload, 16, 60, "gateway-batch-ingress.ms");
  }

  private BenchmarkMetrics benchmark(
      String name, Workload workload, int warmupIterations, int measureIterations, String scriptName) {
    long compileStart = System.nanoTime();
    MagicScript magicScript =
        MagicScript.create(workload.script + "\n// benchmark:" + name + "\n", null);
    magicScript.compile();
    long compileNanos = System.nanoTime() - compileStart;

    long firstExecuteStart = System.nanoTime();
    Map<String, Object> sample = execute(magicScript, workload.payload, scriptName);
    long firstExecuteNanos = System.nanoTime() - firstExecuteStart;
    assertSummary(workload.expectedSummary, sample);

    for (int i = 0; i < warmupIterations; i++) {
      Map<String, Object> warmupResult = execute(magicScript, workload.payload, scriptName);
      if (i == warmupIterations - 1) {
        assertSummary(workload.expectedSummary, warmupResult);
      }
    }

    long[] latencies = new long[measureIterations];
    long measuredChecksum = 0L;
    long measuredPrimaryUnits = 0L;
    long measuredSecondaryUnits = 0L;
    long benchStart = System.nanoTime();
    for (int i = 0; i < measureIterations; i++) {
      long start = System.nanoTime();
      Map<String, Object> current = execute(magicScript, workload.payload, scriptName);
      long cost = System.nanoTime() - start;
      latencies[i] = cost;
      assertSummary(workload.expectedSummary, current);
      measuredChecksum += asLong(current, "checksum");
      measuredPrimaryUnits += workload.primaryUnitsPerExecution;
      measuredSecondaryUnits += workload.secondaryUnitsPerExecution;
    }
    long totalNanos = System.nanoTime() - benchStart;
    Arrays.sort(latencies);

    BenchmarkMetrics metrics =
        new BenchmarkMetrics(
            name,
            workload.description,
            compileNanos,
            firstExecuteNanos,
            totalNanos,
            latencies,
            warmupIterations,
            measureIterations,
            workload.script.length(),
            countLines(workload.script),
            workload.payload.length(),
            workload.payload.getBytes(StandardCharsets.UTF_8).length,
            workload.primaryUnitsPerExecution,
            workload.primaryUnitLabel,
            measuredPrimaryUnits,
            workload.secondaryUnitsPerExecution,
            workload.secondaryUnitLabel,
            measuredSecondaryUnits,
            measuredChecksum,
            sample);
    printReport(metrics);
    return metrics;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> execute(MagicScript magicScript, String payload, String scriptName) {
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName(scriptName);
    context.set("payload", payload);
    return (Map<String, Object>) magicScript.execute(context);
  }

  private void assertSummary(Map<String, Object> expected, Map<String, Object> actual) {
    for (Map.Entry<String, Object> entry : expected.entrySet()) {
      Object expectedValue = entry.getValue();
      Object actualValue = actual.get(entry.getKey());
      Assert.assertNotNull("field=" + entry.getKey(), actualValue);
      if (expectedValue instanceof Number && actualValue instanceof Number) {
        Assert.assertEquals(
            "field=" + entry.getKey(),
            ((Number) expectedValue).longValue(),
            ((Number) actualValue).longValue());
      } else {
        Assert.assertEquals("field=" + entry.getKey(), expectedValue, actualValue);
      }
    }
  }

  private long asLong(Map<String, Object> result, String key) {
    Object value = result.get(key);
    return value == null ? 0L : ((Number) value).longValue();
  }

  private void printReport(BenchmarkMetrics metrics) {
    double compileMs = nanosToMillis(metrics.compileNanos);
    double firstExecuteMs = nanosToMillis(metrics.firstExecuteNanos);
    double totalMs = nanosToMillis(metrics.totalNanos);
    double avgExecuteMs = totalMs / metrics.measureIterations;
    double avgPerPrimaryUs = microsPerUnit(metrics.totalNanos, metrics.measuredPrimaryUnits);
    double avgPerSecondaryUs = microsPerUnit(metrics.totalNanos, metrics.measuredSecondaryUnits);
    double primaryThroughputPerSecond =
        throughputPerSecond(metrics.measuredPrimaryUnits, metrics.totalNanos);
    double secondaryThroughputPerSecond =
        throughputPerSecond(metrics.measuredSecondaryUnits, metrics.totalNanos);

    System.out.println();
    System.out.println("========== NexIoT驱动性能 性能基准 ==========");
    System.out.println("场景: " + metrics.name);
    System.out.println("说明: " + metrics.description);
    System.out.printf(
        Locale.ROOT, "脚本规模: %d chars / %d lines%n", metrics.scriptChars, metrics.scriptLines);
    System.out.printf(
        Locale.ROOT, "payload规模: %d chars / %d bytes%n", metrics.payloadChars, metrics.payloadBytes);
    System.out.printf(
        Locale.ROOT,
        "单次负载: %d %s",
        metrics.primaryUnitsPerExecution,
        metrics.primaryUnitLabel);
    if (metrics.secondaryUnitsPerExecution > 0 && metrics.secondaryUnitLabel != null) {
      System.out.printf(
          Locale.ROOT, ", %d %s%n", metrics.secondaryUnitsPerExecution, metrics.secondaryUnitLabel);
    } else {
      System.out.println();
    }
    System.out.println("预热轮次: " + metrics.warmupIterations);
    System.out.println("实测轮次: " + metrics.measureIterations);
    System.out.printf(
        Locale.ROOT,
        "冷启动耗时: %.3f ms (compile %.3f + first execute %.3f)%n",
        compileMs + firstExecuteMs,
        compileMs,
        firstExecuteMs);
    System.out.printf(Locale.ROOT, "测量阶段总耗时: %.3f ms%n", totalMs);
    System.out.printf(Locale.ROOT, "单轮平均耗时: %.3f ms%n", avgExecuteMs);
    System.out.printf(
        Locale.ROOT,
        "延迟分布: min=%.3f ms, p50=%.3f ms, p95=%.3f ms, p99=%.3f ms, max=%.3f ms%n",
        nanosToMillis(metrics.latencies[0]),
        nanosToMillis(percentile(metrics.latencies, 0.50)),
        nanosToMillis(percentile(metrics.latencies, 0.95)),
        nanosToMillis(percentile(metrics.latencies, 0.99)),
        nanosToMillis(metrics.latencies[metrics.latencies.length - 1]));
    System.out.printf(Locale.ROOT, "抖动: stddev=%.3f ms%n", nanosToMillis(stdDevNanos(metrics.latencies)));
    System.out.printf(
        Locale.ROOT, "平均每%s耗时: %.3f us%n", metrics.primaryUnitLabel, avgPerPrimaryUs);
    if (metrics.measuredSecondaryUnits > 0 && metrics.secondaryUnitLabel != null) {
      System.out.printf(
          Locale.ROOT,
          "平均每%s耗时: %.3f us%n",
          metrics.secondaryUnitLabel,
          avgPerSecondaryUs);
    }
    System.out.printf(
        Locale.ROOT, "%s吞吐: %.2f /s%n", metrics.primaryUnitLabel, primaryThroughputPerSecond);
    if (metrics.measuredSecondaryUnits > 0 && metrics.secondaryUnitLabel != null) {
      System.out.printf(
          Locale.ROOT, "%s吞吐: %.2f /s%n", metrics.secondaryUnitLabel, secondaryThroughputPerSecond);
    }
    System.out.println("累计" + metrics.primaryUnitLabel + "数: " + metrics.measuredPrimaryUnits);
    if (metrics.measuredSecondaryUnits > 0 && metrics.secondaryUnitLabel != null) {
      System.out.println("累计" + metrics.secondaryUnitLabel + "数: " + metrics.measuredSecondaryUnits);
    }
    System.out.println("累计checksum: " + metrics.measuredChecksum);
    System.out.println("样例摘要: " + metrics.sampleSummary);
    System.out.println("=========================================");
  }

  private double nanosToMillis(long nanos) {
    return nanos / 1_000_000D;
  }

  private double nanosToMillis(double nanos) {
    return nanos / 1_000_000D;
  }

  private double microsPerUnit(long totalNanos, long units) {
    return units == 0 ? 0D : (totalNanos / 1_000D) / units;
  }

  private double throughputPerSecond(long units, long totalNanos) {
    return totalNanos == 0 ? 0D : units * 1_000_000_000D / totalNanos;
  }

  private double stdDevNanos(long[] latencies) {
    if (latencies.length == 0) {
      return 0D;
    }
    double mean = 0D;
    for (long latency : latencies) {
      mean += latency;
    }
    mean /= latencies.length;
    double variance = 0D;
    for (long latency : latencies) {
      double delta = latency - mean;
      variance += delta * delta;
    }
    return Math.sqrt(variance / latencies.length);
  }

  private long percentile(long[] sorted, double ratio) {
    if (sorted.length == 0) {
      return 0L;
    }
    int index = (int) Math.ceil(sorted.length * ratio) - 1;
    if (index < 0) {
      index = 0;
    }
    if (index >= sorted.length) {
      index = sorted.length - 1;
    }
    return sorted[index];
  }

  private int countLines(String value) {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    int lines = 1;
    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) == '\n') {
        lines++;
      }
    }
    return lines;
  }

  private void printSuiteSummary(BenchmarkMetrics[] metricsList) {
    System.out.println();
    System.out.println("========== NexIoT驱动性能 基准总览 ==========");
    System.out.printf(
        Locale.ROOT,
        "%-26s %12s %12s %16s %16s%n",
        "场景",
        "avg(ms)",
        "p95(ms)",
        "帧吞吐(/s)",
        "点位吞吐(/s)");
    for (BenchmarkMetrics metrics : metricsList) {
      double avgMs = nanosToMillis(metrics.totalNanos) / metrics.measureIterations;
      double p95Ms = nanosToMillis(percentile(metrics.latencies, 0.95));
      double primaryThroughput = throughputPerSecond(metrics.measuredPrimaryUnits, metrics.totalNanos);
      double secondaryThroughput =
          throughputPerSecond(metrics.measuredSecondaryUnits, metrics.totalNanos);
      System.out.printf(
          Locale.ROOT,
          "%-26s %12.3f %12.3f %16.2f %16.2f%n",
          metrics.name,
          avgMs,
          p95Ms,
          primaryThroughput,
          secondaryThroughput);
    }
    System.out.println("=========================================");
  }

  private Workload buildMixedIngressWorkload(int messageCount, int pointCount) {
    String script =
        """
        var applyField = (properties, summary, field) => {
          if (!field || field.indexOf("=") < 0) {
            return;
          }
          var splitIndex = field.indexOf("=");
          var key = field.substring(0, splitIndex);
          var value = field.substring(splitIndex + 1);
          switch (key) {
            case "TMP":
              properties["temperature"] = Integer.parseInt(value);
              summary["checksum"] += properties["temperature"];
              break;
            case "HUM":
              properties["humidity"] = Integer.parseInt(value);
              summary["checksum"] += properties["humidity"];
              break;
            case "BAT":
              properties["battery"] = Integer.parseInt(value);
              summary["checksum"] += properties["battery"];
              break;
            case "SEQ":
              properties["sequence"] = Integer.parseInt(value);
              summary["checksum"] += properties["sequence"];
              break;
            case "STATE":
              switch (value) {
                case "RUN":
                case "ONLINE":
                  properties["online"] = 1;
                  summary["onlineFrames"] += 1;
                  summary["checksum"] += 1;
                  break;
                case "OFFLINE":
                case "STOP":
                  properties["online"] = 0;
                  break;
                default:
                  properties["online"] = -1;
                  summary["unknownStateFrames"] += 1;
                  summary["checksum"] -= 1;
                  break;
              }
              break;
            case "ALM":
              if (value !== "0") {
                properties["alarmCode"] = value;
                summary["alarmFrames"] += 1;
                summary["checksum"] += 100;
              }
              break;
            case "POINTS":
              var points = value.split(",");
              for (let i = 0; i < points.length; i++) {
                var point = points[i];
                if (!point || point.indexOf(":") < 0) {
                  summary["invalidPointTokens"] += 1;
                  continue;
                }
                var pointIndex = point.indexOf(":");
                var pointId = point.substring(0, pointIndex);
                var pointValue = Integer.parseInt(point.substring(pointIndex + 1));
                properties["p_" + pointId] = pointValue;
                summary["pointCount"] += 1;
                summary["checksum"] += pointValue;
              }
              break;
            default:
              properties["raw_" + key] = value;
              summary["checksum"] += value.length();
              break;
          }
        };

        var decodeLine = (line, summary) => {
          var parts = line.split("\\\\|");
          var message = {
            "messageType": "PROPERTIES",
            "deviceId": "",
            "properties": {
              "ingress": "tcp"
            },
            "data": {
              "functionId": "unknown"
            }
          };
          if (parts.length < 4) {
            message["properties"]["invalid"] = 1;
            return message;
          }
          var frameType = parts[0];
          message["deviceId"] = parts[1];
          message["properties"]["gatewayId"] = parts[2];
          switch (frameType) {
            case "RT":
            case "RPT":
              message["data"]["functionId"] = "report";
              summary["reportFrames"] += 1;
              var fields = parts[3].split(";");
              for (let i = 0; i < fields.length; i++) {
                applyField(message["properties"], summary, fields[i]);
              }
              break;
            case "EVT":
              message["data"]["functionId"] = "event";
              summary["eventFrames"] += 1;
              message["properties"]["eventCode"] = parts[3];
              summary["checksum"] += parts[3].length();
              break;
            case "CTRL":
              message["data"]["functionId"] = "control";
              summary["controlFrames"] += 1;
              message["properties"]["command"] = parts[3];
              summary["checksum"] += parts[3].length();
              break;
            default:
              message["data"]["functionId"] = "unknown";
              summary["unknownFrames"] += 1;
              message["properties"]["rawFrameType"] = frameType;
              summary["checksum"] += frameType.length();
              break;
          }
          return message;
        };

        var decodeBatch = payload => {
          var lines = payload.split("\\\\n");
          var results = [];
          var summary = {
            "count": 0,
            "checksum": 0,
            "reportFrames": 0,
            "eventFrames": 0,
            "controlFrames": 0,
            "unknownFrames": 0,
            "alarmFrames": 0,
            "onlineFrames": 0,
            "unknownStateFrames": 0,
            "pointCount": 0,
            "invalidPointTokens": 0,
            "sampleDeviceId": ""
          };
          for (let i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (!line) {
              continue;
            }
            var message = decodeLine(line, summary);
            results.push(message);
          }
          summary["count"] = results.length;
          if (results.length > 0) {
            summary["sampleDeviceId"] = results[results.length - 1]["deviceId"];
          }
          return summary;
        };

        return decodeBatch(payload);
        """;

    StringBuilder payload = new StringBuilder();
    Map<String, Object> expected = new LinkedHashMap<>();
    long checksum = 0L;
    long reportFrames = 0L;
    long eventFrames = 0L;
    long controlFrames = 0L;
    long unknownFrames = 0L;
    long alarmFrames = 0L;
    long onlineFrames = 0L;
    long unknownStateFrames = 0L;
    long parsedPointCount = 0L;
    long invalidPointTokens = 0L;
    String sampleDeviceId = "";
    for (int i = 0; i < messageCount; i++) {
      String frameType;
      switch (i % 6) {
        case 0:
        case 1:
        case 4:
          frameType = i % 2 == 0 ? "RT" : "RPT";
          reportFrames++;
          int tmp = 20 + (i % 15);
          int hum = 40 + (i % 20);
          int bat = 3000 + i;
          int seq = 1000 + i;
          String firmware = "v" + (1 + (i % 3)) + "." + (i % 10);
          String state;
          int onlineValue;
          switch (i % 3) {
            case 0:
              state = "RUN";
              onlineValue = 1;
              onlineFrames++;
              break;
            case 1:
              state = "OFFLINE";
              onlineValue = 0;
              break;
            default:
              state = "UNKNOWN";
              onlineValue = -1;
              unknownStateFrames++;
              break;
          }
          String alarm = i % 4 == 0 ? "2" : "0";
          if (!"0".equals(alarm)) {
            alarmFrames++;
            checksum += 100;
          }
          checksum += tmp + hum + bat + seq + onlineValue + firmware.length();
          StringBuilder points = new StringBuilder();
          for (int p = 0; p < pointCount; p++) {
            int pointValue = i + (p * 2);
            checksum += pointValue;
            parsedPointCount++;
            if (p > 0) {
              points.append(",");
            }
            points.append("P").append(p).append(":").append(pointValue);
          }
          if (i % 5 == 0) {
            points.append(",BROKEN");
            invalidPointTokens++;
          }
          payload
              .append(frameType)
              .append("|dev-")
              .append(i)
              .append("|gw-")
              .append(i % 8)
              .append("|TMP=")
              .append(tmp)
              .append(";HUM=")
              .append(hum)
              .append(";BAT=")
              .append(bat)
              .append(";SEQ=")
              .append(seq)
              .append(";STATE=")
              .append(state)
              .append(";ALM=")
              .append(alarm)
              .append(";FW=")
              .append(firmware)
              .append(";POINTS=")
              .append(points);
          break;
        case 2:
          frameType = "EVT";
          eventFrames++;
          String eventCode = "EVT_" + (100 + i);
          checksum += eventCode.length();
          payload
              .append(frameType)
              .append("|dev-")
              .append(i)
              .append("|gw-")
              .append(i % 8)
              .append("|")
              .append(eventCode);
          break;
        case 3:
          frameType = "CTRL";
          controlFrames++;
          String command = i % 2 == 0 ? "START_PUMP" : "STOP_PUMP";
          checksum += command.length();
          payload
              .append(frameType)
              .append("|dev-")
              .append(i)
              .append("|gw-")
              .append(i % 8)
              .append("|")
              .append(command);
          break;
        default:
          frameType = "X" + (i % 5);
          unknownFrames++;
          checksum += frameType.length();
          payload
              .append(frameType)
              .append("|dev-")
              .append(i)
              .append("|gw-")
              .append(i % 8)
              .append("|RAW_")
              .append(i);
          break;
      }
      sampleDeviceId = "dev-" + i;
      if (i < messageCount - 1) {
        payload.append("\n");
      }
    }
    expected.put("count", (long) messageCount);
    expected.put("checksum", checksum);
    expected.put("reportFrames", reportFrames);
    expected.put("eventFrames", eventFrames);
    expected.put("controlFrames", controlFrames);
    expected.put("unknownFrames", unknownFrames);
    expected.put("alarmFrames", alarmFrames);
    expected.put("onlineFrames", onlineFrames);
    expected.put("unknownStateFrames", unknownStateFrames);
    expected.put("pointCount", parsedPointCount);
    expected.put("invalidPointTokens", invalidPointTokens);
    expected.put("sampleDeviceId", sampleDeviceId);
    return new Workload(
        "混合 RT/RPT/EVT/CTRL/unknown 帧，报告帧带状态、序号、固件和脏点位 token，覆盖嵌套 switch 与 classic for。",
        script,
        payload.toString(),
        expected,
        messageCount,
        "帧",
        parsedPointCount,
        "点位");
  }

  private Workload buildDensePointRoutingWorkload(int frameCount, int pointsPerFrame) {
    String script =
        """
        var classifyQuality = (summary, quality) => {
          switch (quality) {
            case "GOOD":
              summary["goodQuality"] += 1;
              summary["checksum"] += 1;
              break;
            case "BAD":
              summary["badQuality"] += 1;
              summary["checksum"] -= 1;
              break;
            case "UNCERTAIN":
              summary["uncertainQuality"] += 1;
              break;
            default:
              summary["otherQuality"] += 1;
              summary["checksum"] += quality.length();
              break;
          }
        };

        var routePoint = (summary, pointName, rawValue, quality) => {
          var pointType = pointName.substring(0, 2);
          var value = Integer.parseInt(rawValue);
          switch (pointType) {
            case "AN":
            case "AI":
              summary["analogPoints"] += 1;
              summary["checksum"] += value;
              break;
            case "SW":
              summary["switchPoints"] += 1;
              if (value % 2 === 1) {
                summary["switchOn"] += 1;
                summary["checksum"] += 10;
              }
              break;
            case "AL":
              summary["alarmPoints"] += 1;
              summary["checksum"] += value * 2;
              break;
            default:
              summary["otherPoints"] += 1;
              summary["checksum"] += value;
              break;
          }
          classifyQuality(summary, quality);
        };

        var decodeDense = payload => {
          var frames = payload.split("\\\\n");
          var summary = {
            "count": 0,
            "checksum": 0,
            "analogPoints": 0,
            "switchPoints": 0,
            "alarmPoints": 0,
            "otherPoints": 0,
            "switchOn": 0,
            "goodQuality": 0,
            "badQuality": 0,
            "uncertainQuality": 0,
            "otherQuality": 0,
            "pointCount": 0,
            "invalidTokens": 0,
            "sampleDeviceId": ""
          };
          for (let frameIndex = 0; frameIndex < frames.length; frameIndex++) {
            var frame = frames[frameIndex];
            if (!frame) {
              continue;
            }
            var sections = frame.split("\\\\|");
            if (sections.length < 4) {
              continue;
            }
            summary["count"] += 1;
            summary["sampleDeviceId"] = sections[1];
            var points = sections[3].split(";");
            for (let pointIndex = 0; pointIndex < points.length; pointIndex++) {
              var token = points[pointIndex];
              if (!token || token.indexOf("=") < 0) {
                summary["invalidTokens"] += 1;
                continue;
              }
              var eq = token.indexOf("=");
              var pointName = token.substring(0, eq);
              var values = token.substring(eq + 1).split(",");
              if (values.length < 2) {
                summary["invalidTokens"] += 1;
                continue;
              }
              summary["pointCount"] += 1;
              routePoint(summary, pointName, values[0], values[1]);
            }
          }
          return summary;
        };

        return decodeDense(payload);
        """;

    StringBuilder payload = new StringBuilder();
    Map<String, Object> expected = new LinkedHashMap<>();
    long checksum = 0L;
    long analogPoints = 0L;
    long switchPoints = 0L;
    long alarmPoints = 0L;
    long otherPoints = 0L;
    long switchOn = 0L;
    long goodQuality = 0L;
    long badQuality = 0L;
    long uncertainQuality = 0L;
    long otherQuality = 0L;
    long pointCount = 0L;
    long invalidTokens = 0L;
    String sampleDeviceId = "";
    for (int frame = 0; frame < frameCount; frame++) {
      payload
          .append("ASDU|station-")
          .append(frame)
          .append("|gw-")
          .append(frame % 4)
          .append("|");
      for (int point = 0; point < pointsPerFrame; point++) {
        String pointName;
        int value = frame * 10 + point;
        switch (point % 4) {
          case 0:
            pointName = "AN" + point;
            analogPoints++;
            checksum += value;
            break;
          case 1:
            pointName = "SW" + point;
            switchPoints++;
            if (value % 2 == 1) {
              switchOn++;
              checksum += 10;
            }
            break;
          case 2:
            pointName = "AL" + point;
            alarmPoints++;
            checksum += value * 2L;
            break;
          default:
            pointName = "OT" + point;
            otherPoints++;
            checksum += value;
            break;
        }
        String quality;
        switch (point % 5) {
          case 0:
            quality = "GOOD";
            goodQuality++;
            checksum += 1;
            break;
          case 1:
            quality = "BAD";
            badQuality++;
            checksum -= 1;
            break;
          case 2:
            quality = "UNCERTAIN";
            uncertainQuality++;
            break;
          case 3:
            quality = "STALE";
            otherQuality++;
            checksum += quality.length();
            break;
          default:
            quality = "OTHER";
            otherQuality++;
            checksum += quality.length();
            break;
        }
        if (point > 0) {
          payload.append(";");
        }
        payload.append(pointName).append("=").append(value).append(",").append(quality);
        pointCount++;
        if ((point + 1) % 16 == 0) {
          payload.append(";BROKEN");
          invalidTokens++;
        }
      }
      sampleDeviceId = "station-" + frame;
      if (frame < frameCount - 1) {
        payload.append("\n");
      }
    }
    expected.put("count", (long) frameCount);
    expected.put("checksum", checksum);
    expected.put("analogPoints", analogPoints);
    expected.put("switchPoints", switchPoints);
    expected.put("alarmPoints", alarmPoints);
    expected.put("otherPoints", otherPoints);
    expected.put("switchOn", switchOn);
    expected.put("goodQuality", goodQuality);
    expected.put("badQuality", badQuality);
    expected.put("uncertainQuality", uncertainQuality);
    expected.put("otherQuality", otherQuality);
    expected.put("pointCount", pointCount);
    expected.put("invalidTokens", invalidTokens);
    expected.put("sampleDeviceId", sampleDeviceId);
    return new Workload(
        "高密度点位路由，单帧内混合模拟量、开关量、告警量和脏 token，覆盖双层 switch、quality 分类和 default 分支。",
        script,
        payload.toString(),
        expected,
        frameCount,
        "帧",
        pointCount,
        "点位");
  }

  private Workload buildGatewayBatchIngressWorkload(
      int packetCount, int framesPerPacket, int tagsPerReportFrame) {
    String script =
        """
        var applyQuality = (summary, quality) => {
          switch (quality) {
            case "GOOD":
              summary["goodQuality"] += 1;
              summary["checksum"] += 1;
              break;
            case "BAD":
              summary["badQuality"] += 1;
              summary["checksum"] -= 1;
              break;
            case "STALE":
            case "UNCERTAIN":
              summary["degradedQuality"] += 1;
              break;
            default:
              summary["otherQuality"] += 1;
              summary["checksum"] += quality.length();
              break;
          }
        };

        var applyRoute = (summary, route) => {
          switch (route) {
            case "N":
              summary["northRoute"] += 1;
              break;
            case "S":
              summary["southRoute"] += 1;
              break;
            case "E":
            case "W":
              summary["edgeRoute"] += 1;
              break;
            default:
              summary["fallbackRoute"] += 1;
              break;
          }
        };

        var applyTag = (summary, tagToken, deviceProps) => {
          if (!tagToken || tagToken.indexOf("=") < 0) {
            summary["invalidPoints"] += 1;
            return;
          }
          var eq = tagToken.indexOf("=");
          var pointName = tagToken.substring(0, eq);
          var valueText = tagToken.substring(eq + 1);
          var valueParts = valueText.split(",");
          if (valueParts.length < 2) {
            summary["invalidPoints"] += 1;
            return;
          }
          var value = Integer.parseInt(valueParts[0]);
          var quality = valueParts[1];
          var pointType = pointName.substring(0, 2);
          summary["pointCount"] += 1;
          switch (pointType) {
            case "AN":
            case "AI":
              summary["analogPoints"] += 1;
              summary["checksum"] += value;
              break;
            case "SW":
              summary["switchPoints"] += 1;
              if (value === 1) {
                summary["switchOn"] += 1;
                summary["checksum"] += 10;
              }
              break;
            case "AL":
              summary["alarmPoints"] += 1;
              if (value > 0) {
                summary["activeAlarms"] += 1;
                summary["checksum"] += value * 3;
              }
              break;
            default:
              summary["otherPoints"] += 1;
              summary["checksum"] += value;
              break;
          }
          deviceProps[pointName] = value;
          applyQuality(summary, quality);
        };

        var decodeFrame = (summary, gatewayId, frameToken) => {
          if (!frameToken) {
            return;
          }
          var pieces = frameToken.split("~");
          if (pieces.length < 4) {
            summary["invalidFrames"] += 1;
            return;
          }
          var deviceId = pieces[0];
          var frameType = pieces[1];
          var route = pieces[2];
          var body = pieces[3];
          summary["count"] += 1;
          summary["sampleGatewayId"] = gatewayId;
          summary["sampleDeviceId"] = deviceId;
          applyRoute(summary, route);
          var properties = {
            "gatewayId": gatewayId,
            "deviceId": deviceId
          };
          switch (frameType) {
            case "RT":
            case "RPT":
              summary["reportFrames"] += 1;
              var fields = body.split(";");
              for (let fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
                var field = fields[fieldIndex];
                if (!field) {
                  continue;
                }
                if (field.indexOf("TAGS=") === 0) {
                  var tags = field.substring(5).split("#");
                  for (let tagIndex = 0; tagIndex < tags.length; tagIndex++) {
                    applyTag(summary, tags[tagIndex], properties);
                  }
                  continue;
                }
                if (field.indexOf("=") < 0) {
                  continue;
                }
                var splitIndex = field.indexOf("=");
                var key = field.substring(0, splitIndex);
                var value = field.substring(splitIndex + 1);
                switch (key) {
                  case "SEQ":
                    summary["checksum"] += Integer.parseInt(value);
                    break;
                  case "STAT":
                    switch (value) {
                      case "RUN":
                      case "ONLINE":
                        summary["onlineFrames"] += 1;
                        summary["checksum"] += 2;
                        break;
                      case "OFFLINE":
                      case "STOP":
                        summary["offlineFrames"] += 1;
                        break;
                      default:
                        summary["unknownStateFrames"] += 1;
                        summary["checksum"] -= 2;
                        break;
                    }
                    break;
                  case "MODE":
                    switch (value) {
                      case "AUTO":
                        summary["autoModeFrames"] += 1;
                        break;
                      case "MAN":
                        summary["manualModeFrames"] += 1;
                        break;
                      default:
                        summary["otherModeFrames"] += 1;
                        break;
                    }
                    break;
                  default:
                    summary["checksum"] += value.length();
                    break;
                }
              }
              break;
            case "EVT":
              summary["eventFrames"] += 1;
              var eventParts = body.split(";");
              for (let eventIndex = 0; eventIndex < eventParts.length; eventIndex++) {
                var item = eventParts[eventIndex];
                if (!item || item.indexOf("=") < 0) {
                  continue;
                }
                var itemSplit = item.indexOf("=");
                var key = item.substring(0, itemSplit);
                var value = item.substring(itemSplit + 1);
                switch (key) {
                  case "CODE":
                    summary["checksum"] += value.length();
                    break;
                  case "SEV":
                    switch (value) {
                      case "CRITICAL":
                        summary["criticalEvents"] += 1;
                        summary["severityScore"] += 5;
                      case "MAJOR":
                        summary["severityScore"] += 2;
                        break;
                      case "MINOR":
                        summary["severityScore"] += 1;
                        break;
                      default:
                        break;
                    }
                    break;
                  default:
                    summary["checksum"] += value.length();
                    break;
                }
              }
              break;
            case "CTRL":
              summary["controlFrames"] += 1;
              if (body.indexOf("ACK") >= 0) {
                summary["ackFrames"] += 1;
                summary["checksum"] += 3;
              } else if (body.indexOf("SET") >= 0) {
                summary["setFrames"] += 1;
                summary["checksum"] += 2;
              } else {
                summary["otherControlFrames"] += 1;
              }
              break;
            case "RETRY":
              summary["retryFrames"] += 1;
              var limit = Integer.parseInt(body);
              for (let attempt = 0; attempt < limit; attempt++) {
                summary["retryAttempts"] += 1;
                if (attempt === 2 && limit > 4) {
                  summary["checksum"] += 1;
                  continue;
                }
                summary["checksum"] += attempt;
              }
              break;
            default:
              summary["unknownFrames"] += 1;
              summary["checksum"] += frameType.length();
              break;
          }
        };

        var decodeGatewayBatch = payload => {
          var packets = payload.split("\\\\n");
          var summary = {
            "packetCount": 0,
            "count": 0,
            "checksum": 0,
            "reportFrames": 0,
            "eventFrames": 0,
            "controlFrames": 0,
            "retryFrames": 0,
            "unknownFrames": 0,
            "invalidFrames": 0,
            "pointCount": 0,
            "invalidPoints": 0,
            "onlineFrames": 0,
            "offlineFrames": 0,
            "unknownStateFrames": 0,
            "autoModeFrames": 0,
            "manualModeFrames": 0,
            "otherModeFrames": 0,
            "northRoute": 0,
            "southRoute": 0,
            "edgeRoute": 0,
            "fallbackRoute": 0,
            "analogPoints": 0,
            "switchPoints": 0,
            "alarmPoints": 0,
            "otherPoints": 0,
            "switchOn": 0,
            "activeAlarms": 0,
            "goodQuality": 0,
            "badQuality": 0,
            "degradedQuality": 0,
            "otherQuality": 0,
            "criticalEvents": 0,
            "severityScore": 0,
            "ackFrames": 0,
            "setFrames": 0,
            "otherControlFrames": 0,
            "retryAttempts": 0,
            "sampleGatewayId": "",
            "sampleDeviceId": ""
          };
          for (let packetIndex = 0; packetIndex < packets.length; packetIndex++) {
            var packet = packets[packetIndex];
            if (!packet) {
              continue;
            }
            var sections = packet.split("\\\\|");
            if (sections.length < 3) {
              continue;
            }
            var gatewayId = sections[0];
            summary["packetCount"] += 1;
            var frames = sections[2].split("\\\\$");
            for (let frameIndex = 0; frameIndex < frames.length; frameIndex++) {
              decodeFrame(summary, gatewayId, frames[frameIndex]);
            }
          }
          return summary;
        };

        return decodeGatewayBatch(payload);
        """;

    StringBuilder payload = new StringBuilder();
    Map<String, Object> expected = new LinkedHashMap<>();
    long checksum = 0L;
    long frameCount = 0L;
    long reportFrames = 0L;
    long eventFrames = 0L;
    long controlFrames = 0L;
    long retryFrames = 0L;
    long unknownFrames = 0L;
    long invalidFrames = 0L;
    long pointCount = 0L;
    long invalidPoints = 0L;
    long onlineFrames = 0L;
    long offlineFrames = 0L;
    long unknownStateFrames = 0L;
    long autoModeFrames = 0L;
    long manualModeFrames = 0L;
    long otherModeFrames = 0L;
    long northRoute = 0L;
    long southRoute = 0L;
    long edgeRoute = 0L;
    long fallbackRoute = 0L;
    long analogPoints = 0L;
    long switchPoints = 0L;
    long alarmPoints = 0L;
    long otherPoints = 0L;
    long switchOn = 0L;
    long activeAlarms = 0L;
    long goodQuality = 0L;
    long badQuality = 0L;
    long degradedQuality = 0L;
    long otherQuality = 0L;
    long criticalEvents = 0L;
    long severityScore = 0L;
    long ackFrames = 0L;
    long setFrames = 0L;
    long otherControlFrames = 0L;
    long retryAttempts = 0L;
    String sampleGatewayId = "";
    String sampleDeviceId = "";
    for (int packet = 0; packet < packetCount; packet++) {
      String gatewayId = "gw-" + packet;
      sampleGatewayId = gatewayId;
      if (packet > 0) {
        payload.append("\n");
      }
      payload.append(gatewayId).append("|zone-").append(packet % 3).append("|");
      for (int frame = 0; frame < framesPerPacket; frame++) {
        int globalFrame = packet * framesPerPacket + frame;
        if (frame > 0) {
          payload.append("$");
        }
        frameCount++;
        String deviceId = "dev-" + packet + "-" + frame;
        sampleDeviceId = deviceId;

        String route;
        switch (globalFrame % 5) {
          case 0:
            route = "N";
            northRoute++;
            break;
          case 1:
            route = "S";
            southRoute++;
            break;
          case 2:
            route = "E";
            edgeRoute++;
            break;
          case 3:
            route = "W";
            edgeRoute++;
            break;
          default:
            route = "X";
            fallbackRoute++;
            break;
        }

        String frameType;
        switch (globalFrame % 7) {
          case 0:
          case 1:
          case 4:
            frameType = globalFrame % 2 == 0 ? "RT" : "RPT";
            reportFrames++;
            int seq = 10_000 + globalFrame;
            String state;
            switch (globalFrame % 4) {
              case 0:
                state = "RUN";
                onlineFrames++;
                checksum += 2;
                break;
              case 1:
                state = "OFFLINE";
                offlineFrames++;
                break;
              case 2:
                state = "UNKNOWN";
                unknownStateFrames++;
                checksum -= 2;
                break;
              default:
                state = "STOP";
                offlineFrames++;
                break;
            }
            String mode;
            switch (globalFrame % 3) {
              case 0:
                mode = "AUTO";
                autoModeFrames++;
                break;
              case 1:
                mode = "MAN";
                manualModeFrames++;
                break;
              default:
                mode = "ECO";
                otherModeFrames++;
                break;
            }
            String extra = "extra-" + (globalFrame % 11);
            checksum += seq + extra.length();
            StringBuilder tags = new StringBuilder();
            for (int tag = 0; tag < tagsPerReportFrame; tag++) {
              if (tag > 0) {
                tags.append("#");
              }
              String pointName;
              int value;
              switch (tag % 4) {
                case 0:
                  pointName = "AN" + tag;
                  value = 200 + (packet * 3) + frame + tag;
                  analogPoints++;
                  checksum += value;
                  break;
                case 1:
                  pointName = "SW" + tag;
                  value = (globalFrame + tag) % 2;
                  switchPoints++;
                  if (value == 1) {
                    switchOn++;
                    checksum += 10;
                  }
                  break;
                case 2:
                  pointName = "AL" + tag;
                  value = (globalFrame + tag) % 4;
                  alarmPoints++;
                  if (value > 0) {
                    activeAlarms++;
                    checksum += value * 3L;
                  }
                  break;
                default:
                  pointName = "CT" + tag;
                  value = 10 + packet + tag;
                  otherPoints++;
                  checksum += value;
                  break;
              }

              String quality;
              switch ((globalFrame + tag) % 5) {
                case 0:
                  quality = "GOOD";
                  goodQuality++;
                  checksum += 1;
                  break;
                case 1:
                  quality = "BAD";
                  badQuality++;
                  checksum -= 1;
                  break;
                case 2:
                  quality = "STALE";
                  degradedQuality++;
                  break;
                case 3:
                  quality = "UNCERTAIN";
                  degradedQuality++;
                  break;
                default:
                  quality = "OTHER";
                  otherQuality++;
                  checksum += quality.length();
                  break;
              }
              pointCount++;
              tags.append(pointName).append("=").append(value).append(",").append(quality);
              if (tag % 7 == 6) {
                tags.append("#BROKEN");
                invalidPoints++;
              }
            }
            payload
                .append(deviceId)
                .append("~")
                .append(frameType)
                .append("~")
                .append(route)
                .append("~SEQ=")
                .append(seq)
                .append(";STAT=")
                .append(state)
                .append(";MODE=")
                .append(mode)
                .append(";EXTRA=")
                .append(extra)
                .append(";TAGS=")
                .append(tags);
            break;
          case 2:
            frameType = "EVT";
            eventFrames++;
            String eventCode = "EV_" + globalFrame;
            String severity;
            switch (globalFrame % 4) {
              case 0:
                severity = "CRITICAL";
                criticalEvents++;
                severityScore += 7;
                break;
              case 1:
                severity = "MAJOR";
                severityScore += 2;
                break;
              case 2:
                severity = "MINOR";
                severityScore += 1;
                break;
              default:
                severity = "INFO";
                break;
            }
            String origin = "gw";
            checksum += eventCode.length() + origin.length();
            payload
                .append(deviceId)
                .append("~")
                .append(frameType)
                .append("~")
                .append(route)
                .append("~CODE=")
                .append(eventCode)
                .append(";SEV=")
                .append(severity)
                .append(";ORIGIN=")
                .append(origin);
            break;
          case 3:
            frameType = "CTRL";
            controlFrames++;
            String command;
            switch (globalFrame % 3) {
              case 0:
                command = "ACK_RESET";
                ackFrames++;
                checksum += 3;
                break;
              case 1:
                command = "SET_RATE";
                setFrames++;
                checksum += 2;
                break;
              default:
                command = "PING";
                otherControlFrames++;
                break;
            }
            payload
                .append(deviceId)
                .append("~")
                .append(frameType)
                .append("~")
                .append(route)
                .append("~")
                .append(command);
            break;
          case 5:
            frameType = "RETRY";
            retryFrames++;
            int limit = 3 + (globalFrame % 4);
            retryAttempts += limit;
            for (int attempt = 0; attempt < limit; attempt++) {
              if (attempt == 2 && limit > 4) {
                checksum += 1;
                continue;
              }
              checksum += attempt;
            }
            payload
                .append(deviceId)
                .append("~")
                .append(frameType)
                .append("~")
                .append(route)
                .append("~")
                .append(limit);
            break;
          default:
            frameType = "Z" + (globalFrame % 5);
            unknownFrames++;
            checksum += frameType.length();
            payload
                .append(deviceId)
                .append("~")
                .append(frameType)
                .append("~")
                .append(route)
                .append("~opaque");
            break;
        }
      }
    }
    expected.put("packetCount", (long) packetCount);
    expected.put("count", frameCount);
    expected.put("checksum", checksum);
    expected.put("reportFrames", reportFrames);
    expected.put("eventFrames", eventFrames);
    expected.put("controlFrames", controlFrames);
    expected.put("retryFrames", retryFrames);
    expected.put("unknownFrames", unknownFrames);
    expected.put("invalidFrames", invalidFrames);
    expected.put("pointCount", pointCount);
    expected.put("invalidPoints", invalidPoints);
    expected.put("onlineFrames", onlineFrames);
    expected.put("offlineFrames", offlineFrames);
    expected.put("unknownStateFrames", unknownStateFrames);
    expected.put("autoModeFrames", autoModeFrames);
    expected.put("manualModeFrames", manualModeFrames);
    expected.put("otherModeFrames", otherModeFrames);
    expected.put("northRoute", northRoute);
    expected.put("southRoute", southRoute);
    expected.put("edgeRoute", edgeRoute);
    expected.put("fallbackRoute", fallbackRoute);
    expected.put("analogPoints", analogPoints);
    expected.put("switchPoints", switchPoints);
    expected.put("alarmPoints", alarmPoints);
    expected.put("otherPoints", otherPoints);
    expected.put("switchOn", switchOn);
    expected.put("activeAlarms", activeAlarms);
    expected.put("goodQuality", goodQuality);
    expected.put("badQuality", badQuality);
    expected.put("degradedQuality", degradedQuality);
    expected.put("otherQuality", otherQuality);
    expected.put("criticalEvents", criticalEvents);
    expected.put("severityScore", severityScore);
    expected.put("ackFrames", ackFrames);
    expected.put("setFrames", setFrames);
    expected.put("otherControlFrames", otherControlFrames);
    expected.put("retryAttempts", retryAttempts);
    expected.put("sampleGatewayId", sampleGatewayId);
    expected.put("sampleDeviceId", sampleDeviceId);
    return new Workload(
        "网关批量入口场景，单包多帧、报告帧内多点位、事件严重度 fallthrough、控制命令与重试循环共存，贴近真实物联网入口分流。",
        script,
        payload.toString(),
        expected,
        frameCount,
        "帧",
        pointCount,
        "点位");
  }

  private static class Workload {

    private final String description;

    private final String script;

    private final String payload;

    private final Map<String, Object> expectedSummary;

    private final long primaryUnitsPerExecution;

    private final String primaryUnitLabel;

    private final long secondaryUnitsPerExecution;

    private final String secondaryUnitLabel;

    private Workload(
        String description,
        String script,
        String payload,
        Map<String, Object> expectedSummary,
        long primaryUnitsPerExecution,
        String primaryUnitLabel,
        long secondaryUnitsPerExecution,
        String secondaryUnitLabel) {
      this.description = description;
      this.script = script;
      this.payload = payload;
      this.expectedSummary = expectedSummary;
      this.primaryUnitsPerExecution = primaryUnitsPerExecution;
      this.primaryUnitLabel = primaryUnitLabel;
      this.secondaryUnitsPerExecution = secondaryUnitsPerExecution;
      this.secondaryUnitLabel = secondaryUnitLabel;
    }
  }

  private static class BenchmarkMetrics {

    private final String name;

    private final String description;

    private final long compileNanos;

    private final long firstExecuteNanos;

    private final long totalNanos;

    private final long[] latencies;

    private final int warmupIterations;

    private final int measureIterations;

    private final int scriptChars;

    private final int scriptLines;

    private final int payloadChars;

    private final int payloadBytes;

    private final long primaryUnitsPerExecution;

    private final String primaryUnitLabel;

    private final long measuredPrimaryUnits;

    private final long secondaryUnitsPerExecution;

    private final String secondaryUnitLabel;

    private final long measuredSecondaryUnits;

    private final long measuredChecksum;

    private final Map<String, Object> sampleSummary;

    private final boolean successful;

    private BenchmarkMetrics(
        String name,
        String description,
        long compileNanos,
        long firstExecuteNanos,
        long totalNanos,
        long[] latencies,
        int warmupIterations,
        int measureIterations,
        int scriptChars,
        int scriptLines,
        int payloadChars,
        int payloadBytes,
        long primaryUnitsPerExecution,
        String primaryUnitLabel,
        long measuredPrimaryUnits,
        long secondaryUnitsPerExecution,
        String secondaryUnitLabel,
        long measuredSecondaryUnits,
        long measuredChecksum,
        Map<String, Object> sampleSummary) {
      this.name = name;
      this.description = description;
      this.compileNanos = compileNanos;
      this.firstExecuteNanos = firstExecuteNanos;
      this.totalNanos = totalNanos;
      this.latencies = latencies;
      this.warmupIterations = warmupIterations;
      this.measureIterations = measureIterations;
      this.scriptChars = scriptChars;
      this.scriptLines = scriptLines;
      this.payloadChars = payloadChars;
      this.payloadBytes = payloadBytes;
      this.primaryUnitsPerExecution = primaryUnitsPerExecution;
      this.primaryUnitLabel = primaryUnitLabel;
      this.measuredPrimaryUnits = measuredPrimaryUnits;
      this.secondaryUnitsPerExecution = secondaryUnitsPerExecution;
      this.secondaryUnitLabel = secondaryUnitLabel;
      this.measuredSecondaryUnits = measuredSecondaryUnits;
      this.measuredChecksum = measuredChecksum;
      this.sampleSummary = sampleSummary;
      this.successful = true;
    }
  }
}
