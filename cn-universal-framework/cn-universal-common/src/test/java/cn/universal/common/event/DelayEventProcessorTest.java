package cn.universal.common.event;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

public class DelayEventProcessorTest {

  @Test
  public void shouldSkipRedisScanAfterContextClose() throws Exception {
    RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate();
    DelayEventProcessor processor = new DelayEventProcessor();
    setField(processor, "redisTemplate", redisTemplate);

    processor.onContextClosed(null);
    processor.processDelayEvents();

    assertFalse(redisTemplate.accessed);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class RecordingRedisTemplate extends StringRedisTemplate {
    private boolean accessed;

    @Override
    public Cursor<String> scan(ScanOptions options) {
      accessed = true;
      return super.scan(options);
    }
  }
}
