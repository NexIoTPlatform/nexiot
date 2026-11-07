package cn.universal.admin.video.sync;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 设备同步事务执行器：用于按设备拆分独立事务，避免单条失败污染整批同步事务。 */
@Service
public class DeviceSyncTransactionExecutor {

  @FunctionalInterface
  public interface TransactionalSupplier<T> {
    T get() throws Exception;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public <T> T execute(TransactionalSupplier<T> supplier) throws Exception {
    // 通过独立 Spring Bean 进入代理，使 REQUIRES_NEW 生效；不能改成处理器内部自调用。
    return supplier.get();
  }
}
