/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.net.InetAddress;
import java.util.HashMap;
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
    Map<String, Long> data;
    // TODO: There is a better way to serialize a map

    @SuppressWarnings("unused") // To satisfy JAXB
    private IncomingHostData() {}

    public IncomingHostData(Map<Host, Long> hostData) {
        this.data = new HashMap<String, Long>();
        for (Host h : hostData.keySet())
            this.data.put(h.getNetworkAddress().toString(), hostData.get(h));
    }

    public Map<String, Long> getData() {
        return this.data;
    }
}