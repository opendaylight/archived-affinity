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

import org.opendaylight.affinity.affinity.AffinityLink;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityLinkStatistics {

    private AffinityLink link;
    @XmlElement
    private long byteCount;
    @XmlElement
    private double bitRate;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private AffinityLinkStatistics() {
    }

    public AffinityLinkStatistics(AffinityLink link, long byteCount, double bitRate) {
        super();
        this.link = link;
        this.byteCount = byteCount;
        this.bitRate = bitRate;
    }

    public AffinityLink getLink() {
        return this.link;
    }

    public void setLink(AffinityLink link) {
        this.link = link;
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
