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
public class ProtocolStatistics {
    @XmlElement
    private byte protocol;
    @XmlElement
    private Statistics stat;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private ProtocolStatistics() {
    }

    public ProtocolStatistics(byte protocol, Statistics stat) {
        super();
        this.protocol = protocol;
        this.stat = stat;
    }

    public long getProtocol() {
        return this.protocol;
    }

    public void setProtocol(Byte protocol) {
        this.protocol = protocol;
    }

    public Statistics getStat() {
        return this.stat;
    }

    public void setStat(Statistics stat) {
        this.stat = stat;
    }
}
