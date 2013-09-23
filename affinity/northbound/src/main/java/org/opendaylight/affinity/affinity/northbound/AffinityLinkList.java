/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.affinity.affinity.AffinityLink;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityLinkList {
        @XmlElement (name="affinity")
        List<AffinityLink> affinityLinkList;

        public AffinityLinkList() {
        }
        public AffinityLinkList (List<AffinityLink> aff) {
            this.affinityLinkList = aff;
        }
        public List<AffinityLink> getAffinityList() {
                return affinityLinkList;
        }
        public void setAffinityList(List<AffinityLink> aff) {
                this.affinityLinkList = aff;
        }
}
