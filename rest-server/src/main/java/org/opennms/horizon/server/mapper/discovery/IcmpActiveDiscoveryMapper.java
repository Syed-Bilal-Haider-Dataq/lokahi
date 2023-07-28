/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.server.mapper.discovery;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscoveryCreate;
import org.opennms.horizon.server.model.inventory.discovery.SNMPConfig;

import java.util.List;

@Mapper(componentModel = "spring",
    uses = {TagMapper.class}, collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface IcmpActiveDiscoveryMapper {

    @Mapping(source = "readCommunityList", target = "readCommunities")
    @Mapping(source = "portsList", target = "ports")
    SNMPConfig snmpDtoToModel(SNMPConfigDTO snmpDto);

    @Mapping(source = "readCommunities", target = "readCommunityList")
    @Mapping(source = "ports", target = "portsList")
    SNMPConfigDTO snmpConfigToDTO(SNMPConfig snmpConfig);

    @Mapping(target = "ipAddressesList", source = "ipAddresses")
    @Mapping(target = "snmpConfig", source = "snmpConfig", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscoveryCreateDTO mapRequest(IcmpActiveDiscoveryCreate request);


    @Mapping(source = "ipAddressesList", target = "ipAddresses")
    @Mapping(target = "snmpConfig", source = "snmpConfig", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscovery dtoToIcmpActiveDiscovery(IcmpActiveDiscoveryDTO configDTO);

    List<IcmpActiveDiscovery> dtoListToIcmpActiveDiscoveryList(List<IcmpActiveDiscoveryDTO> dtoList);
}
