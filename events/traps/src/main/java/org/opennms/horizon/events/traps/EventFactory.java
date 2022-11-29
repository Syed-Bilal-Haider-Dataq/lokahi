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

package org.opennms.horizon.events.traps;

import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.conf.xml.Event;
import org.opennms.horizon.events.conf.xml.LogDestType;
import org.opennms.horizon.events.conf.xml.Logmsg;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapIdentity;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.snmp.api.SnmpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Date;
import java.util.Optional;

import static org.opennms.horizon.events.EventConstants.OID_SNMP_IFINDEX_STRING;
import static org.opennms.horizon.shared.utils.InetAddressUtils.str;

public class EventFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EventFactory.class);

    private static final SnmpObjId OID_SNMP_IFINDEX = SnmpObjId.get(OID_SNMP_IFINDEX_STRING);

    private final EventConfDao eventConfDao;
    private final SnmpHelper snmpHelper;

    public EventFactory(EventConfDao eventConfDao, SnmpHelper snmpHelper) {
        this.eventConfDao = eventConfDao;
        this.snmpHelper = snmpHelper;
    }

    public org.opennms.horizon.events.xml.Event createEventFrom(final TrapDTO trapDTO,
                                                                final String systemId,
                                                                final String location,
                                                                final InetAddress trapAddress,
                                                                String tenantId) {
        LOG.debug("{} trap - trapInterface: {}", trapDTO.getVersion(), trapDTO.getAgentAddress());

        // Set event data
        final EventBuilder eventBuilder = new EventBuilder(null, "trapd");
        eventBuilder.setTime(new Date(trapDTO.getCreationTime()));
        eventBuilder.setCommunity(trapDTO.getCommunity());
        eventBuilder.setSnmpTimeStamp(trapDTO.getTimestamp());
        eventBuilder.setSnmpVersion(trapDTO.getVersion());
        eventBuilder.setSnmpHost(str(trapAddress));
        eventBuilder.setInterface(trapAddress);
        eventBuilder.setHost(trapDTO.getAgentAddress());

        // Handle trap identity
        final TrapIdentity trapIdentity = trapDTO.getTrapIdentity();
        LOG.debug("Trap Identity {}", trapIdentity);
        eventBuilder.setGeneric(trapIdentity.getGeneric());
        eventBuilder.setSpecific(trapIdentity.getSpecific());
        eventBuilder.setEnterpriseId(trapIdentity.getEnterpriseId());
        eventBuilder.setTrapOID(trapIdentity.getTrapOID());

        // Handle var bindings
        for (SnmpResult eachResult : trapDTO.getSnmpResultsList()) {
            final SnmpObjId name = SnmpObjId.get(eachResult.getBase());
            final SnmpValue value = snmpHelper.getValueFactory().getValue(eachResult.getValue().getTypeValue(),
                eachResult.getValue().getValue().toByteArray());
            SyntaxToEvent.processSyntax(name.toString(), value).ifPresent(eventBuilder::addParam);
            if (OID_SNMP_IFINDEX.isPrefixOf(name)) {
                eventBuilder.setIfIndex(value.toInt());
            }
        }

        // Resolve Node id and set, if known by OpenNMS
        resolveNodeId(location, trapAddress, tenantId)
            .ifPresent(eventBuilder::setNodeid);

        // Note: Filling in Location instead of SystemId. Do we really need to know about system id ?
        if (systemId != null) {
            eventBuilder.setDistPoller(location);
        }

        // Get event template and set uei, if unknown
        final org.opennms.horizon.events.xml.Event event = eventBuilder.getEvent();
        final Event econf = eventConfDao.findByEvent(event);
        if (econf == null || econf.getUei() == null) {
            event.setUei("uei.opennms.org/default/trap");
        } else {
            event.setUei(econf.getUei());
        }
        if (shouldDiscard(econf)) {
            LOG.debug("Trap discarded due to matching event having logmsg dest == discardtraps");
            return null;
        }
        return event;
    }

    private boolean shouldDiscard(Event econf) {
        if (econf != null) {
            final Logmsg logmsg = econf.getLogmsg();
            return logmsg != null && LogDestType.DISCARDTRAPS.equals(logmsg.getDest());
        }
        return false;
    }

    private Optional<Integer> resolveNodeId(String location, InetAddress trapAddress, String tenantId) {
        // TODO: Query inventory service for node id
        return Optional.empty();
    }
}