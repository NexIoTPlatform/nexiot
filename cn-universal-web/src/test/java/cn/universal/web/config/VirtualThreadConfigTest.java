package cn.universal.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class VirtualThreadConfigTest {

  @AfterEach
  void tearDown() {
    MDC.clear();
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void taskExecutorShouldNotPropagateRequestAttributes() throws InterruptedException {
    ExecutorService delegate = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    VirtualThreadConfig.VirtualThreadContextExecutor executor =
        new VirtualThreadConfig.VirtualThreadContextExecutor(delegate);

    MDC.put("traceId", "trace-123");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user-1", "pwd", "ROLE_USER"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/task-center/dispatch");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> traceId = new AtomicReference<>();
    AtomicReference<String> principal = new AtomicReference<>();
    AtomicReference<Object> requestAttributes = new AtomicReference<>();

    try {
      executor.execute(
          () -> {
            traceId.set(MDC.get("traceId"));
            principal.set(
                SecurityContextHolder.getContext().getAuthentication() == null
                    ? null
                    : String.valueOf(
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
            requestAttributes.set(RequestContextHolder.getRequestAttributes());
            latch.countDown();
          });

      assertTrue(latch.await(5, TimeUnit.SECONDS));
      assertEquals("trace-123", traceId.get());
      assertEquals("user-1", principal.get());
      assertNull(requestAttributes.get());
    } finally {
      delegate.shutdownNow();
    }
  }

  @Test
  void taskDecoratorShouldNotPropagateRequestAttributes() {
    VirtualThreadConfig.VirtualThreadTaskDecorator decorator =
        new VirtualThreadConfig.VirtualThreadTaskDecorator();

    MDC.put("traceId", "trace-456");
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));

    AtomicReference<String> traceId = new AtomicReference<>();
    AtomicReference<Object> requestAttributes = new AtomicReference<>();

    Runnable decorated =
        decorator.decorate(
            () -> {
              traceId.set(MDC.get("traceId"));
              requestAttributes.set(RequestContextHolder.getRequestAttributes());
            });

    RequestContextHolder.resetRequestAttributes();
    decorated.run();

    assertEquals("trace-456", traceId.get());
    assertNull(requestAttributes.get());
  }

  @Test
  void saturatedTaskExecutorShouldRestoreCallerContext() {
    ExecutorService delegate = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    VirtualThreadConfig.VirtualThreadContextExecutor executor =
        new VirtualThreadConfig.VirtualThreadContextExecutor(delegate, 1);

    CountDownLatch firstTaskEntered = new CountDownLatch(1);
    CountDownLatch releaseFirstTask = new CountDownLatch(1);
    try {
      executor.execute(
          () -> {
            firstTaskEntered.countDown();
            try {
              releaseFirstTask.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          });
      assertTrue(firstTaskEntered.await(2, TimeUnit.SECONDS));

      MDC.put("traceId", "caller-trace");
      SecurityContextHolder.getContext()
          .setAuthentication(new TestingAuthenticationToken("caller", "pwd", "ROLE_USER"));

      executor.execute(
          () -> {
            assertEquals("caller-trace", MDC.get("traceId"));
            assertEquals(
                "caller", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
          });

      assertEquals("caller-trace", MDC.get("traceId"));
      assertEquals("caller", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      releaseFirstTask.countDown();
      delegate.shutdownNow();
    }
  }

  @Test
  void boundedVirtualExecutorShouldActAsExecutorService() throws Exception {
    ExecutorService delegate = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    ExecutorService executor =
        new VirtualThreadConfig.VirtualThreadContextExecutor(delegate, 1, "virtualThreadExecutor");

    CountDownLatch firstTaskEntered = new CountDownLatch(1);
    CountDownLatch releaseFirstTask = new CountDownLatch(1);
    try {
      executor.execute(
          () -> {
            firstTaskEntered.countDown();
            try {
              releaseFirstTask.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          });
      assertTrue(firstTaskEntered.await(2, TimeUnit.SECONDS));

      MDC.put("traceId", "background-trace");
      executor
          .submit(() -> assertEquals("background-trace", MDC.get("traceId")))
          .get(2, TimeUnit.SECONDS);
    } finally {
      releaseFirstTask.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void waitModeShouldHoldSecondTaskUntilPermitIsAvailable() throws Exception {
    ExecutorService delegate = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    ExecutorService executor =
        new VirtualThreadConfig.VirtualThreadContextExecutor(
            delegate, 1, "dbBoundTaskExecutor", true);

    CountDownLatch firstTaskEntered = new CountDownLatch(1);
    CountDownLatch releaseFirstTask = new CountDownLatch(1);
    CountDownLatch secondTaskRan = new CountDownLatch(1);
    try {
      executor.execute(
          () -> {
            firstTaskEntered.countDown();
            try {
              releaseFirstTask.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          });
      assertTrue(firstTaskEntered.await(2, TimeUnit.SECONDS));

      Thread submitter = new Thread(() -> executor.execute(secondTaskRan::countDown));
      submitter.start();

      assertEquals(1L, secondTaskRan.getCount());
      releaseFirstTask.countDown();
      submitter.join(2_000);
      assertTrue(secondTaskRan.await(2, TimeUnit.SECONDS));
    } finally {
      releaseFirstTask.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void uplinkExecutorRejectsWithoutBlockingOrRunningOnCaller() throws Exception {
    ExecutorService executor = VirtualThreadConfig.uplinkExecutor("test-uplink", 1, 1);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try {
      executor.execute(
          () -> {
            assertTrue(Thread.currentThread().isVirtual());
            entered.countDown();
            try {
              release.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      executor.execute(() -> {});
      org.junit.jupiter.api.Assertions.assertTimeout(
          java.time.Duration.ofMillis(250),
          () ->
              org.junit.jupiter.api.Assertions.assertThrows(
                  java.util.concurrent.RejectedExecutionException.class,
                  () ->
                      executor.execute(
                          () ->
                              org.junit.jupiter.api.Assertions.fail(
                                  "caller must not run uplink side effect"))));
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }
}
