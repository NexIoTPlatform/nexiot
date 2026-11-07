package cn.universal.dm.device.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class UPPushDispatcherTest {

  @Test
  void shouldPreserveOrderForSameDevice() throws Exception {
    UPPushDispatcher dispatcher =
        new UPPushDispatcher(new SimpleMeterRegistry(), 4, 128, 10_000, 1_000);
    dispatcher.start();
    List<Integer> values = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch completed = new CountDownLatch(20);

    for (int i = 0; i < 20; i++) {
      int value = i;
      assertTrue(
          dispatcher.dispatch(
              "iot-1",
              1,
              () -> {
                values.add(value);
                completed.countDown();
              }));
    }

    assertTrue(completed.await(2, TimeUnit.SECONDS));
    assertEquals(java.util.stream.IntStream.range(0, 20).boxed().toList(), values);
    dispatcher.shutdown();
  }

  @Test
  void shouldDropImmediatelyWhenStripeQueueIsFull() throws Exception {
    UPPushDispatcher dispatcher =
        new UPPushDispatcher(new SimpleMeterRegistry(), 1, 1, 10_000, 1_000);
    dispatcher.start();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    assertTrue(
        dispatcher.dispatch(
            "iot-1",
            1,
            () -> {
              entered.countDown();
              try {
                release.await(2, TimeUnit.SECONDS);
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              }
            }));
    assertTrue(entered.await(1, TimeUnit.SECONDS));
    assertTrue(dispatcher.dispatch("iot-1", 1, () -> {}));

    long start = System.nanoTime();
    assertFalse(dispatcher.dispatch("iot-1", 1, () -> {}));
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    assertTrue(elapsedMs < 100);
    release.countDown();
    dispatcher.shutdown();
  }
}
