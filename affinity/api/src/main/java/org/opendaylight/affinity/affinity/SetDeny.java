package org.opendaylight.affinity.affinity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represent the action of dropping matched flows. 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetDeny extends AffinityAttribute {
    private static final long serialVersionUID = 1L;

    public SetDeny() {
        type = AffinityAttributeType.SET_DENY;
    }
}
