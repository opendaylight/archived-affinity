/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Host;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.NONE)
public class IncomingHostData {
    @XmlElement
    List<HostStatistics> stats;

    @SuppressWarnings("unused") // To satisfy JAXB
    private IncomingHostData() {}

    public IncomingHostData(Map<Host, Long> hostData) {
        this.stats = new ArrayList<HostStatistics>();
        for (Host h : hostData.keySet())
            this.stats.add(new HostStatistics(h.getNetworkAddress(), hostData.get(h)));
    }

    public List<HostStatistics> getStats() {
        return this.stats;
    }

    public void setStats(List<HostStatistics> stats) {
        this.stats = stats;
    }
}
