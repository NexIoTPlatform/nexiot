package cn.universal.admin.system.service;

import cn.universal.persistence.entity.IoTTenantPackage;
import java.util.List;

public interface IIoTTenantPackageService {

  List<IoTTenantPackage> selectPackageList(IoTTenantPackage query);

  IoTTenantPackage selectPackageById(Long packageId);

  List<IoTTenantPackage> selectActivePackages();

  int insertPackage(IoTTenantPackage tenantPackage);

  int updatePackage(IoTTenantPackage tenantPackage);

  int updatePackageStatus(IoTTenantPackage tenantPackage);

  int deletePackageByIds(Long[] packageIds);

  Long getOrCreateDefaultPackageId();
}
