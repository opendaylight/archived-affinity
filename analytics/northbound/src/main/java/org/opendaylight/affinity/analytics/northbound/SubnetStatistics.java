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
public class SubnetStatistics {
    @XmlElement
    private long byteCount;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private SubnetStatistics() {
    }

    public SubnetStatistics(long byteCount) {
        super();
        this.byteCount = byteCount;
    }

    public long getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }
}
