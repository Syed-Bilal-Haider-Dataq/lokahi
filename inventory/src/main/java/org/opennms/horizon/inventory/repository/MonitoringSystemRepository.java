package org.opennms.horizon.inventory.repository;

import java.util.Optional;

import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitoringSystemRepository extends JpaRepository<MonitoringSystem, Long> {
    List<MonitoringSystem> findByTenantId(UUID tenantId);
    Optional<MonitoringSystem> findBySystemId(String systemId);
    Optional<MonitoringSystem> findBySystemIdAndTenantId(String systemId, UUID tenantId);
}
