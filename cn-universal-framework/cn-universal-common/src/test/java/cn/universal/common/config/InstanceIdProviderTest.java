package cn.universal.common.config;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class InstanceIdProviderTest {

  @Test
  public void shouldSkipHeartbeatAfterContextClose() throws Exception {
    RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate();
    InstanceIdProvider provider = new InstanceIdProvider();
    setField(provider, "redisTemplate", redisTemplate);

    provider.onContextClosed(null);
    provider.updateHeartbeat();

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
    public ValueOperations<String, String> opsForValue() {
      accessed = true;
      return super.opsForValue();
    }
  }
}
