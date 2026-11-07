package cn.universal.core.engine;

import org.junit.Assert;
import org.junit.Test;

public class MagicScriptSyntaxFeatureTest {

  @Test
  public void shouldSupportClassicForLoopWithUpdateAndContinue() {
    Object value =
        execute(
            """
            var sum = 0;
            for (let i = 0; i < 5; i++) {
              if (i === 1) {
                i += 1;
                continue;
              }
              sum += i;
            }
            return sum;
            """);

    Assert.assertEquals(7, ((Number) value).intValue());
  }

  @Test
  public void shouldKeepIteratorForLoopWorking() {
    Object value =
        execute(
            """
            var count = 0;
            for (i in range(0, 2)) {
              count += i;
            }
            return count;
            """);

    Assert.assertEquals(3, ((Number) value).intValue());
  }

  @Test
  public void shouldBreakCurrentSwitchWithoutBreakingOuterLoop() {
    Object value =
        execute(
            """
            var result = "";
            for (let i = 0; i < 3; i++) {
              switch (i) {
                case 1:
                  result += "a";
                  break;
                default:
                  result += "b";
              }
            }
            return result;
            """);

    Assert.assertEquals("bab", value);
  }

  @Test
  public void shouldSupportSwitchFallthrough() {
    Object value =
        execute(
            """
            var result = "";
            switch (2) {
              case 1:
                result += "1";
                break;
              case 2:
                result += "2";
              case 3:
                result += "3";
                break;
              default:
                result += "d";
            }
            return result;
            """);

    Assert.assertEquals("23", value);
  }

  @Test
  public void shouldContinueOuterLoopFromSwitchCase() {
    Object value =
        execute(
            """
            var result = "";
            for (let i = 0; i < 4; i++) {
              switch (i) {
                case 1:
                  result += "x";
                  continue;
                default:
                  result += i;
              }
            }
            return result;
            """);

    Assert.assertEquals("0x23", value);
  }

  private Object execute(String script) {
    MagicScript magicScript = MagicScript.create(script, null);
    magicScript.compile();
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName("syntax-feature.ms");
    return magicScript.execute(context);
  }
}
