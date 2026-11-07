package cn.universal.persistence.service.device;

/**
 * Extension point for deleting module-owned data in the core device deletion transaction.
 *
 * <p>Implementations must be idempotent and must scope every query by the supplied tenant and
 * device identity. Throwing from {@link #cleanup(IoTDeviceDeleteContext)} rolls the transaction
 * back.
 */
public interface IoTDeviceDeleteParticipant {

  default IoTDeviceDeleteImpact preview(IoTDeviceDeleteContext context) {
    return IoTDeviceDeleteImpact.empty(getClass().getSimpleName());
  }

  void cleanup(IoTDeviceDeleteContext context);
}
