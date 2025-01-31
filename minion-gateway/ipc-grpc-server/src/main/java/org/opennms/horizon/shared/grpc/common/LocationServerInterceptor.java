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

package org.opennms.horizon.shared.grpc.common;

import org.opennms.horizon.shared.constants.GrpcConstants;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

/**
 * Location resolver which rely on grpc header.
 */
@Slf4j
public class LocationServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata headers, ServerCallHandler<ReqT, RespT> callHandler) {
        log.debug("Received metadata: {}", headers);
        String locationId = headers.get(GrpcConstants.LOCATION_ID_REQUEST_KEY);
         if (locationId == null) {
             //
             // FAILED
             //
             log.error("Missing location");

             serverCall.close(Status.UNAUTHENTICATED.withDescription("Missing location"), new Metadata());
             return new ServerCall.Listener<>() {};
         }

        // Write the tenant ID to the current GRPC context
        Context context = Context.current().withValue(GrpcConstants.LOCATION_ID_CONTEXT_KEY, locationId);
        return Contexts.interceptCall(context, serverCall, headers, callHandler);
    }

    public String readCurrentContextLocationId() {
        return GrpcConstants.LOCATION_ID_CONTEXT_KEY.get();
    }

    public String readContextLocation(Context context) {
        var locationId = GrpcConstants.LOCATION_ID_CONTEXT_KEY.get(context);
        var span = Span.current();
        if (span.isRecording()) {
            span.setAttribute("location-id", locationId);
        }
        return locationId;
    }


}
