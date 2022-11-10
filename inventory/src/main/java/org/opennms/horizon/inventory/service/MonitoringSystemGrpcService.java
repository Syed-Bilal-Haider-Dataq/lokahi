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

package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.opennms.horizon.inventory.dto.GetBySystemIdRequest;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemList;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;

import com.google.protobuf.StringValue;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonitoringSystemGrpcService extends MonitoringSystemServiceGrpc.MonitoringSystemServiceImplBase {
    private final MonitoringSystemRepository repository;
    private final MonitoringSystemMapper mapper;
    @Override
    public void listMonitoringSystem(StringValue tenantId, StreamObserver<MonitoringSystemList> responseObserver) {
        List<MonitoringSystemDTO> list = repository.findByTenantId(UUID.fromString(tenantId.getValue()))
            .stream().map(mapper::modelToDTO).collect(Collectors.toList());
        responseObserver.onNext(MonitoringSystemList.newBuilder().addAllList(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMonitoringSystemById(GetBySystemIdRequest request, StreamObserver<MonitoringSystemDTO> responseObserver) {
        Optional<MonitoringSystem> monitoringSystem = repository.findBySystemIdAndTenantId(request.getSystemId(), UUID.fromString(request.getTenantId()));
        if(monitoringSystem.isPresent()) {
            responseObserver.onNext(mapper.modelToDTO(monitoringSystem.get()));
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Monitor system with system id: " + request.getSystemId() + " doesn't exist")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
        responseObserver.onCompleted();
    }
}
