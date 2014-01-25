/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.affinity.affinity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents the attribute associated with an affinity link. 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AffinityAttribute implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AffinityAttribute.class);
    @XmlElement
    protected AffinityAttributeType type;
    private transient boolean isValid = true;

    /* Dummy constructor for JAXB */
    public AffinityAttribute() {
    }

    public AffinityAttributeType getType() {
        return type;
    }

    /**
     * Returns the id of this action
     *
     * @return String
     */
    public String getId() {
        return type.getId();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.calculateConsistentHashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AffinityAttribute other = (AffinityAttribute) obj;
        if (type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type.toString();
    }

}
