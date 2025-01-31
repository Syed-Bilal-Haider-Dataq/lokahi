/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class InvalidPacketException extends Exception {

    public InvalidPacketException(final ByteBuf buffer, final String fmt, final Object... args) {
        super(appendPosition(String.format(fmt, args), buffer));
    }

    public InvalidPacketException(final ByteBuf buffer, final String message, final Throwable cause) {
        super(appendPosition(message, buffer), cause);
    }

    private static String appendPosition(final String message, final ByteBuf buffer) {
        // we want to hex-dump the whole PDU, wo we need to get the unsliced buffer
        final ByteBuf unwrappedBuffer = Unpooled.wrappedUnmodifiableBuffer(buffer.unwrap() != null ? buffer.unwrap() : buffer).resetReaderIndex();
        // compare the readableBytes() to determine the adjustment
        final int delta = unwrappedBuffer.readableBytes() - (buffer.readableBytes() + buffer.readerIndex());
        // compute the offset for which this exception had occurred
        final int offset = buffer.readerIndex() + delta;

        return String.format("%s, Offset: [0x%04X], Payload:\n%s", message, offset, ByteBufUtil.prettyHexDump(unwrappedBuffer));
    }
}
