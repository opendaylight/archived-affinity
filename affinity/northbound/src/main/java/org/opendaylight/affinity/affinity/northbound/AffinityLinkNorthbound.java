/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.northbound;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.affinity.affinity.AffinityAttribute;
import org.opendaylight.affinity.affinity.AffinityAttributeType;

// API object to be returned in GET calls.
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityLinkNorthbound {
        @XmlAttribute 
        private String name;
        @XmlElement
        private List<AffinityAttribute> attrlist;
    
        public AffinityLinkNorthbound() {
        }
        public AffinityLinkNorthbound(AffinityLink al) {
            HashMap<AffinityAttributeType, AffinityAttribute> attrs = al.getAttributeList();

            attrlist = new ArrayList<AffinityAttribute>();

            if (attrs != null) {
                for (AffinityAttribute a: attrs.values()) {
                    this.attrlist.add(a);
                }
            }
        }
}
