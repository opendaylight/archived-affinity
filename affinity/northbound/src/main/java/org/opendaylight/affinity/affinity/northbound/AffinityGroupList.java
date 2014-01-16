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

import org.opendaylight.affinity.affinity.AffinityGroup;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityGroupList {
        @XmlElement 
        List<AffinityGroup> affinityGroupList;

        public AffinityGroupList() {
        }
        public AffinityGroupList (List<AffinityGroup> aff) {
            this.affinityGroupList = aff;
        }
        public List<AffinityGroup> getAffinityList() {
                return affinityGroupList;
        }
        public void setAffinityList(List<AffinityGroup> aff) {
                this.affinityGroupList = aff;
        }
}
