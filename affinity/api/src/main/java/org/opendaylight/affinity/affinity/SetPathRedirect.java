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
public class SetPathRedirect extends AffinityAttribute {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private List<InetAddress> waypointList;

    public SetPathRedirect() {
        type = AffinityAttributeType.SET_PATH_REDIRECT;
        waypointList = new ArrayList<InetAddress>();
    }

    public List<InetAddress> getWaypointList() {
        return this.waypointList;
    }

    public void addWaypoint(InetAddress ipaddr) {
        waypointList.add(ipaddr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        for (InetAddress address: waypointList) {
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
        SetPathRedirect other = (SetPathRedirect) obj;
        /* xxx check first element. */
        InetAddress address = waypointList.get(0);
        List<InetAddress> otherlist = other.getWaypointList();
        return waypointList.equals(otherlist);
    }

    @Override
    public String toString() {
        String string = type + "[";
        for (InetAddress address: waypointList) {
            string = string + " -> " + address.toString();
        }
        string = string +  "]";
        return string;
    }
}



