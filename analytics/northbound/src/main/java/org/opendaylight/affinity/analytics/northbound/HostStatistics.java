/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.controller.sal.core.Host;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class HostStatistics {
    @XmlElement
    private Host srcHost;
    @XmlElement
    private Host dstHost;
    @XmlElement(name="byteCount")
    private long byteCount;
    @XmlElement(name="bitRate")
    private double bitRate;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private HostStatistics() {
    }

    public HostStatistics(Host srcHost, Host dstHost, long byteCount, double bitRate) {
        super();
        this.srcHost = srcHost;
        this.dstHost = dstHost;
        this.byteCount = byteCount;
        this.bitRate = bitRate;
    }

    public Host getSrcHost() {
        return this.srcHost;
    }

    public void setSrcHost(Host host) {
        this.srcHost = host;
    }

    public Host getDstHost() {
        return this.dstHost;
    }

    public void setDstHost(Host host) {
        this.dstHost = host;
    }

    public long getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    public double getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(double bitRate) {
        this.bitRate = bitRate;
    }
}
