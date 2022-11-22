/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.service.taskset;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManagerUtil;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitorTaskSetService {
    private static final Logger log = LoggerFactory.getLogger(MonitorTaskSetService.class);
    private static final String DEFAULT_SCHEDULE = "60000";
    private static final int DEFAULT_SNMP_TIMEOUT = 18000;
    private static final int DEFAULT_SNMP_RETRIES = 2;
    private final TaskSetManagerUtil taskSetManagerUtil;
    private final TaskSetManager taskSetManager;
    private final TaskSetPublisher taskSetPublisher;

    public void sendMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface) {
        addMonitorTask(location, monitorType, ipInterface);
        sendTaskSet(location);
    }

    private void addMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = ipInterface.getIpAddress().getAddress();

        String name = String.format("%s-monitor", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sMonitor", monitorTypeValue);

        switch (monitorType) {
            case ICMP: {
                //todo: add request
                taskSetManagerUtil.addTask(location, ipAddress, name, TaskType.MONITOR, pluginName);
                break;
            }
            case SNMP: {
                Any configuration =
                    Any.pack(SnmpMonitorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(DEFAULT_SNMP_TIMEOUT)
                        .setRetries(DEFAULT_SNMP_RETRIES)
                        .build());

                taskSetManagerUtil.addTask(location, ipAddress, name, TaskType.MONITOR, pluginName, DEFAULT_SCHEDULE, configuration);
                break;
            }
            case UNRECOGNIZED: {
                log.warn("Unrecognized monitor type");
                break;
            }
            case UNKNOWN: {
                log.warn("Unknown monitor type");
                break;
            }
        }
    }

    private void sendTaskSet(String location) {
        TaskSet taskSet = taskSetManager.getTaskSet(location);
        taskSetPublisher.publishTaskSet(location, taskSet);
    }
}