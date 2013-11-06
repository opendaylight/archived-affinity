/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.NONE)
public class AllHosts {
    @XmlElement
    Map<String, Long> hosts;
    // TODO: There is a better way to serialize a map

    @SuppressWarnings("unused") // To satisfy JAXB
    private AllHosts() {}

    public AllHosts(Map<String, Long> hostData) {
        this.hosts = hostData;
    }

    public Map<String, Long> getHosts() {
        return this.hosts;
    }
}