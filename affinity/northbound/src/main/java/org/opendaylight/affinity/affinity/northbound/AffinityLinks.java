/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.northbound;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.affinity.affinity.AffinityLink;

@XmlRootElement(name="list")
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityLinks {
    @XmlElement
    Set<AffinityLink> affinityLinks;
    
    // To satisfy JAXB
    @SuppressWarnings("unused")
    private AffinityLinks() {
    }
    public AffinityLinks (List<AffinityLink> aff) {
        this.affinityLinks = new HashSet<AffinityLink>();
        this.affinityLinks.addAll(aff);
    }
    public Set<AffinityLink> getAffinityLinks() {
        return this.affinityLinks;
    }
    public void setAffinityLinks(List<AffinityLink> aff) {
        if (this.affinityLinks != null) {
            this.affinityLinks.addAll(aff);
        } else {
            this.affinityLinks = new HashSet<AffinityLink>();
            affinityLinks.addAll(aff);
        }
        return;
    }
    public void setAffinityLinks(Set<AffinityLink> aff) {
        if (this.affinityLinks != null) {
            this.affinityLinks.addAll(aff);            
        } else {
            this.affinityLinks = aff;
        }
        return;
    }
}
