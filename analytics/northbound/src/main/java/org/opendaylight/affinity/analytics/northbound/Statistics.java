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

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Statistics {
    @XmlElement
    private long byteCount;
    @XmlElement
    private long packetCount;
    @XmlElement
    private double duration;
    @XmlElement
    private double bitRate;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private Statistics() {
    }

    public Statistics(long byteCount, long packetCount, double duration, double bitRate) {
        super();
        this.byteCount = byteCount;
        this.packetCount = packetCount;
        this.duration = duration;
        this.bitRate = bitRate;
    }

    public long getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    public long getPacketCount() {
        return this.packetCount;
    }

    public void setPacketCount(long packetCount) {
        this.packetCount = packetCount;
    }

    public double getDuration() {
        return this.duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getBitRate() {
        return this.bitRate;
    }

    public void setBitRate(double bitRate) {
        this.bitRate = bitRate;
    }
}
