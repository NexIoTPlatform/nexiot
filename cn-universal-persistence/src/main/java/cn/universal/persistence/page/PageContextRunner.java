package cn.universal.persistence.page;

import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import java.util.function.Supplier;

/** 在辅助查询期间挂起并恢复 PageHelper 上下文，避免误消费当前分页。 */
public final class PageContextRunner {

  private PageContextRunner() {}

  public static <T> T runWithoutPaging(Supplier<T> supplier) {
    Page<?> currentPage = PageMethod.getLocalPage();
    if (currentPage == null) {
      return supplier.get();
    }
    PageMethod.clearPage();
    try {
      return supplier.get();
    } finally {
      PageMethod.clearPage();
      PageMethod.setLocalPage(currentPage);
    }
  }

  public static void runWithoutPaging(Runnable runnable) {
    runWithoutPaging(
        () -> {
          runnable.run();
          return null;
        });
  }
}
