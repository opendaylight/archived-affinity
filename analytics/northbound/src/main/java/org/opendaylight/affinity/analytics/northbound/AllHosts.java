/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "list")
@XmlAccessorType(XmlAccessType.NONE)
public class AllHosts {
    @XmlElement
    List<String> hosts;

    @SuppressWarnings("unused") // To satisfy JAXB
    private AllHosts() {}

    public AllHosts(List<String> hostIPs) {
        this.hosts = hostIPs;
    }

    public List<String> getHosts() {
        return this.hosts;
    }
}
