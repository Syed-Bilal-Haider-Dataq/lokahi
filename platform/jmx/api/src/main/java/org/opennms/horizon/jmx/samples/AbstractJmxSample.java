/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
package org.opennms.horizon.jmx.samples;

import javax.management.Attribute;

import org.opennms.horizon.jmx.config.Mbean;

public abstract class AbstractJmxSample {
    /**
     * The MBean to which the attribute belongs.
     */
    private final Mbean mbean;

    /**
     * The collected attribute returned from MBeanServer.
     */
    private final Attribute attribute;

    public AbstractJmxSample(Mbean mbean, Attribute attribute) {
        this.mbean = mbean;
        this.attribute = attribute;
    }

    public Attribute getCollectedAttribute() {
        return attribute;
    }

    public Mbean getMbean() {
        return mbean;
    }

    public abstract String getCollectedValueAsString();

}