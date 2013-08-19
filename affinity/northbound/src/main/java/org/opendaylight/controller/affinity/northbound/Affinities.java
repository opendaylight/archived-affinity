/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.affinity.AffinityConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Affinities {
        @XmlElement (name="affinity")
        List<AffinityConfig> affinityList;

        public Affinities() {
        }
        public Affinities (List<AffinityConfig> aff) {
            this.affinityList = aff;
        }
        public List<AffinityConfig> getAffinityList() {
                return affinityList;
        }
        public void setAffinityList(List<AffinityConfig> aff) {
                this.affinityList = aff;
        }
}
