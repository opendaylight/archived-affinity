/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AllStatistics {
    @XmlElement
    private List<ProtocolStatistics> stats;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private AllStatistics() {
    }

    public AllStatistics(Map<Byte, Long> byteCounts, Map<Byte, Double> bitRates) {
        this.stats = new ArrayList<ProtocolStatistics>();
        for (Byte protocol : byteCounts.keySet()) {
            long byteCount = byteCounts.get(protocol);
            double bitRate = bitRates.get(protocol);
            this.stats.add(new ProtocolStatistics(protocol, new Statistics(byteCount, bitRate)));
        }
    }

    public List<ProtocolStatistics> getStats() {
        return this.stats;
    }

    public void setStats(List<ProtocolStatistics> stats) {
        this.stats = stats;
    }
}
