/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.utils.IPProtocols;

public class HostStats {

    private Map<Byte, Long> byteCounts;
    private Map<Byte, Double> durations;

    public HostStats() {
        this.byteCounts = new HashMap<Byte, Long>();
        this.durations = new HashMap<Byte, Double>();
    }

    public long getByteCount() {
        long totalByteCount = 0;
        for (Byte protocol : this.byteCounts.keySet())
            totalByteCount += this.byteCounts.get(protocol);
        return totalByteCount;
    }

    public double getDuration() {
        return Collections.max(this.durations.values());
    }

    public void setStatsFromFlow(FlowOnNode flow) {
        MatchField protocolField = flow.getFlow().getMatch().getField(MatchType.NW_PROTO);
        Byte protocolNumber;
        if (protocolField == null)
            protocolNumber = IPProtocols.ANY.byteValue();
        else
            protocolNumber = (Byte) protocolField.getValue();

        // Prevent stats from getting overwritten by zero-byte flows.
        Long currentByteCount = this.byteCounts.get(protocolNumber);
        Long thisByteCount = flow.getByteCount();
        if (thisByteCount > 0 && (currentByteCount == null || currentByteCount <= thisByteCount)) {
            this.byteCounts.put(protocolNumber, thisByteCount);
            this.durations.put(protocolNumber, flow.getDurationSeconds() + .000000001 * flow.getDurationNanoseconds());
        }
    }

    public double getBitRate() {
        return (getByteCount() * 8)/getDuration();
    }
}
