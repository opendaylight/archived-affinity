package org.opendaylight.affinity.affinity;

import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetPathIsolate extends AffinityAttribute {
    private static final long serialVersionUID = 1L;

    public SetPathIsolate() {
        type = AffinityAttributeType.SET_PATH_ISOLATE;
    }
}



