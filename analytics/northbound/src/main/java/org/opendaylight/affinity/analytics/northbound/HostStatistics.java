/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class HostStatistics {
    @XmlElement
    private String hostIP;
    @XmlElement
    private Long byteCount;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private HostStatistics() {
    }

    public HostStatistics(InetAddress hostIP, Long byteCount) {
        this.hostIP = hostIP.toString();
        this.byteCount = byteCount;
    }

    public String getHostIP() {
        return this.hostIP;
    }

    public void setHostIP(String hostIP) {
        this.hostIP = hostIP;
    }

    public Long getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(Long byteCount) {
        this.byteCount = byteCount;
    }
}
