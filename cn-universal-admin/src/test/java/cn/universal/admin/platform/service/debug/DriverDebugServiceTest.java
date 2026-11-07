package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.exception.IoTException;
import cn.universal.core.engine.extend.ProtocolFunctions;
import cn.universal.core.engine.reflection.JavaReflection;
import cn.universal.core.protocol.magic.ProtocolCodecMagic;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import org.junit.jupiter.api.*;

class DriverDebugServiceTest {
  private DriverDebugService service;

  @BeforeEach
  void setup() {
    service = new DriverDebugService();
  }

  @AfterEach
  void cleanup() {
    service.close();
  }

  private DriverDebugService.RunRequest input(
      String source, String payload, boolean debug, Integer... lines) {
    return new DriverDebugService.RunRequest(
        source, payload, null, "decode", debug, List.of(lines));
  }

  private String start(String owner, DriverDebugService.RunRequest input) {
    return (String) service.start(owner, "driver", input).get("id");
  }

  private Map<String, Object> await(
      String id, String owner, Predicate<Map<String, Object>> condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      Map<String, Object> result = service.poll(id, owner, "driver");
      if (condition.test(result)) return result;
      Thread.sleep(10);
    }
    fail("未到达期望状态: " + service.poll(id, owner, "driver"));
    return null;
  }

  private Map<String, Object> state(String id, String owner, String state) throws Exception {
    return await(id, owner, value -> state.equals(value.get("state")));
  }

  @Test
  void runsDraftWithTheSameResultAsDeviceCodecAndPreservesLiveSource() throws Exception {
    String source = "var decode = (payload, context) => { return {value: payload}; };";
    ProtocolSupportDefinition live = new ProtocolSupportDefinition();
    live.setId("live-test");
    live.setProvider("live-test");
    live.setType("magic");
    live.setConfiguration(new HashMap<>(Map.of("provider", "live-test", "location", source)));
    ProtocolCodecMagic codec = ProtocolCodecMagic.getInstance();
    try {
      String expected = codec.decode(new ProtocolDecodeRequest(live, "hello", null));
      String id = start("user", input(source, "hello", false));
      assertEquals(expected, state(id, "user", "SUCCEEDED").get("result"));
      String modified =
          start("user", input("var decode = (p,c) => { return 'draft'; };", "hello", false));
      assertEquals("draft", state(modified, "user", "SUCCEEDED").get("result"));
      assertEquals(expected, codec.decode(new ProtocolDecodeRequest(live, "hello", null)));
    } finally {
      codec.remove("live-test");
    }
  }

  @Test
  void pausesShowsVariablesStepsAndContinues() throws Exception {
    String id =
        start(
            "user",
            input(
                "var decode = (payload, context) => {\n"
                    + "  var value = 1;\n"
                    + "  value = value + 1;\n"
                    + "  return value + '';\n"
                    + "};",
                "a",
                true,
                3));
    Map<String, Object> paused = state(id, "user", "PAUSED");
    assertEquals(3, paused.get("line"));
    assertTrue(paused.get("variables").toString().contains("value=1"));
    service.command(id, "user", "driver", new DriverDebugService.Command("step", List.of(3)));
    Map<String, Object> next =
        await(
            id,
            "user",
            v -> "PAUSED".equals(v.get("state")) && Integer.valueOf(4).equals(v.get("line")));
    assertTrue(next.get("variables").toString().contains("value=2"));
    service.command(id, "user", "driver", new DriverDebugService.Command("continue", List.of()));
    assertEquals("2", state(id, "user", "SUCCEEDED").get("result"));
  }

  @Test
  void editsBreakpointsWhilePausedWithoutResumingAndUsesNewLinesOnContinue() throws Exception {
    String id =
        start(
            "user",
            input(
                "var decode=(p,c)=>{\n var n=1;\n n=n+1;\n n=n+1;\n return n+'';\n};",
                "",
                true,
                2,
                3));
    assertEquals(2, state(id, "user", "PAUSED").get("line"));
    var updated =
        service.command(
            id, "user", "driver", new DriverDebugService.Command("breakpoints", List.of(4)));
    assertEquals("PAUSED", updated.get("state"));
    assertEquals(2, updated.get("line"));
    service.command(id, "user", "driver", new DriverDebugService.Command("continue", List.of(4)));
    assertEquals(4, state(id, "user", "PAUSED").get("line"));
    service.command(id, "user", "driver", new DriverDebugService.Command("continue", List.of()));
    assertEquals("3", state(id, "user", "SUCCEEDED").get("result"));
  }

  public static class BreakpointGate {
    final CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);

    @cn.universal.core.engine.annotation.Function
    public String awaitBreakpointUpdate() throws InterruptedException {
      entered.countDown();
      if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("gate timed out");
      return "ok";
    }
  }

  @Test
  void acceptsNewBreakpointsWhileScriptIsRunning() throws Exception {
    BreakpointGate gate = new BreakpointGate();
    JavaReflection.registerFunction(gate);
    try {
      String id =
          start(
              "user",
              input(
                  "var decode=(p,c)=>{\n var value=awaitBreakpointUpdate();\n return value;\n};",
                  "",
                  false));
      assertTrue(gate.entered.await(3, TimeUnit.SECONDS));
      assertEquals(
          "RUNNING",
          service
              .command(
                  id, "user", "driver", new DriverDebugService.Command("breakpoints", List.of(3)))
              .get("state"));
      gate.release.countDown();
      assertEquals(3, state(id, "user", "PAUSED").get("line"));
      assertThrows(
          IoTException.class,
          () ->
              service.command(
                  id,
                  "other-user",
                  "driver",
                  new DriverDebugService.Command("breakpoints", List.of())));
      service.command(id, "user", "driver", new DriverDebugService.Command("continue", List.of()));
      assertEquals("ok", state(id, "user", "SUCCEEDED").get("result"));
    } finally {
      gate.release.countDown();
      JavaReflection.getFunctions().removeIf(invoker -> invoker.getDefaultTarget() == gate);
    }
  }

  @Test
  void isolatesOwnersLogsAndProtocolBindings() throws Exception {
    String source = "var decode = (payload,context) => { println(payload); return payload; };";
    String a = start("a", input(source, "only-a", false));
    String b = start("b", input(source, "only-b", false));
    assertEquals(List.of("only-a"), state(a, "a", "SUCCEEDED").get("logs"));
    assertEquals(List.of("only-b"), state(b, "b", "SUCCEEDED").get("logs"));
    assertThrows(IoTException.class, () -> service.poll(a, "b", "driver"));
    assertThrows(IoTException.class, () -> service.poll(a, "a", "other-driver"));
    assertThrows(IoTException.class, () -> service.cancel(a, "b", "driver"));
  }

  @Test
  void cancelsPausedRunWithoutExecutingLaterStatements() throws Exception {
    String id =
        start(
            "a",
            input(
                "var decode = (p,c) => {\n println('should-not-run');\n return 1;\n};",
                "",
                true,
                2));
    state(id, "a", "PAUSED");
    service.cancel(id, "a", "driver");
    assertEquals(List.of(), state(id, "a", "CANCELLED").get("logs"));
    assertThrows(
        IoTException.class,
        () ->
            service.command(
                id, "a", "driver", new DriverDebugService.Command("continue", List.of())));
  }

  @Test
  void readingVariablesDoesNotConsumeExecutionBudgetButLostConnectionsExpire() throws Exception {
    String id = start("a", input("var decode=(p,c)=>{\n return p;\n};", "ok", true, 2));
    state(id, "a", "PAUSED");
    var sessions =
        (Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "sessions");
    Object session = sessions.get(id);
    long now = System.currentTimeMillis();
    org.springframework.test.util.ReflectionTestUtils.setField(session, "created", now - 180000);
    org.springframework.test.util.ReflectionTestUtils.setField(
        session, "pausedSince", now - 179000);
    org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "expire");
    assertEquals("PAUSED", service.poll(id, "a", "driver").get("state"));
    org.springframework.test.util.ReflectionTestUtils.setField(session, "touched", now - 31000);
    org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "expire");
    assertEquals("CANCELLED", service.poll(id, "a", "driver").get("state"));
  }

  @Test
  void emptyInfiniteLoopCanBeCancelledAtLoopCheckpoint() throws Exception {
    DriverDebugContext context = new DriverDebugContext(List.of(), 1, (row, vars) -> {});
    ExecutorService worker = Executors.newSingleThreadExecutor();
    try {
      Future<?> task =
          worker.submit(
              () ->
                  ProtocolCodecMagic.getInstance()
                      .executeDraft(
                          "var decode=(p,c)=>{ while(true){} };",
                          cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod
                              .decode,
                          context));
      context.cancel();
      assertThrows(ExecutionException.class, () -> task.get(2, TimeUnit.SECONDS));
    } finally {
      worker.shutdownNow();
    }
  }

  @Test
  void reportsScriptErrorsWithOriginalLineNumber() throws Exception {
    String id =
        start("a", input("var decode=(p,c)=>{\n return missing_function();\n};", "", false));
    assertEquals(2, state(id, "a", "FAILED").get("line"));
  }

  @Test
  void registeredProtocolFunctionsExecuteAndAppearInCatalog() throws Exception {
    JavaReflection.registerFunction(new ProtocolFunctions());
    var catalog =
        new cn.universal.admin.platform.service.impl.MagicFunctionCatalogService().listFunctions();
    var fn =
        catalog.stream().filter(f -> f.getStr("name").equals("cs_check")).findFirst().orElseThrow();
    assertEquals("ProtocolFunctions", fn.getStr("group"));
    assertTrue(fn.getJSONArray("params").toString().contains("hex"));
    String id = start("a", input("var decode=(p,c)=>{ return cs_check(p,256); };", "0102", false));
    assertEquals("03", state(id, "a", "SUCCEEDED").get("result"));
  }
}
