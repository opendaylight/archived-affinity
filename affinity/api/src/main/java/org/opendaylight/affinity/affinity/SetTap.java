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
public class SetTap extends AffinityAttribute {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private List<InetAddress> tapList;

    public SetTap() {
        type = AffinityAttributeType.SET_TAP;
        tapList = new ArrayList<InetAddress>();
    }

    public List<InetAddress> getTapList() {
        return this.tapList;
    }

    // Must be in the same L2 domain. 
    public void addTap(InetAddress ipaddr) {
        tapList.add(ipaddr);
    }

    // Must be in the same L2 domain. 
    public void removeTap(InetAddress ipaddr) {
        tapList.remove(ipaddr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        for (InetAddress address: tapList) {
            result = prime * result + ((address == null) ? 0 : address.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetTap other = (SetTap) obj;
        /* xxx check first element. */
        InetAddress address = tapList.get(0);
        List<InetAddress> otherlist = other.getTapList();
        return tapList.equals(otherlist);
    }

    @Override
    public String toString() {
        String string = type + "[";
        for (InetAddress address: tapList) {
            string = string + " -> " + address.toString();
        }
        string = string +  "]";
        return string;
    }
}



