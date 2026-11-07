package cn.universal.core.engine;

import cn.hutool.json.JSONObject;
import cn.universal.core.engine.extend.UnivFunctions;
import cn.universal.core.engine.extend.method.JSONObjectExtension;
import cn.universal.core.engine.reflection.JavaReflection;
import org.junit.Test;

/**
 * @author liulihai
 * @version 1.0
 * @since 2025/12/8 17:24
 */
public class ExtendionTest {

  @Test
  public void test1() {
    JavaReflection.registerMethodExtension(JSONObject.class, new JSONObjectExtension());
    JavaReflection.registerFunction(new UnivFunctions());
    String ms =
        """
        var val = '{"params":{"length":{"time":1100000,"value":1.61903}}}';
                          System.out.println(val.getClass());

                           var jsx= toJson(val);
                           System.out.println(jsx.getClass());
              var maps= jsx.toMap();
                System.out.println("maps.params"+maps.params.getClass());

              var tp=jsx.params.toMap();
              System.out.println(" tp"+jsx.params.getClass());

              for( k,v in tp){
                 System.out.println("k= "+k);
                                  System.out.println("v= "+v);

              }
                 return  maps;
      """;

    long t = System.currentTimeMillis();
    MagicScript script = MagicScript.create(ms, null);
    script.compile();
    System.out.println("编译耗时：" + (System.currentTimeMillis() - t) + "ms");
    t = System.currentTimeMillis();
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName("b.ms");
    Object value = script.execute(context);
    System.out.println("执行耗时：" + (System.currentTimeMillis() - t) + "ms");
    System.out.println("执行结果：" + value);
  }

  @Test
  public void test2() {
    JavaReflection.registerMethodExtension(JSONObject.class, new JSONObjectExtension());
    JavaReflection.registerFunction(new UnivFunctions());
    String ms =
        """
        var val = {"params":{"length":{"time":1100000,"value":1.61903}}};
                          System.out.println(val.getClass());

                           var jsx= toJson(val);
                           System.out.println(jsx.getClass());

              for( k,v in val.params){
                 System.out.println("k= "+k);
                                  System.out.println("v= "+v);

              }
                 return  maps;
      """;

    long t = System.currentTimeMillis();
    MagicScript script = MagicScript.create(ms, null);
    script.compile();
    System.out.println("编译耗时：" + (System.currentTimeMillis() - t) + "ms");
    t = System.currentTimeMillis();
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName("b.ms");
    Object value = script.execute(context);
    System.out.println("执行耗时：" + (System.currentTimeMillis() - t) + "ms");
    System.out.println("执行结果：" + value);
  }
}
