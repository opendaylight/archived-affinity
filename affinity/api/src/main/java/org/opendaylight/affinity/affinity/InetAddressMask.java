/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.affinity.affinity;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.net.UnknownHostException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines an Inet address mask object.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class InetAddressMask implements Cloneable, Serializable {
    
    @XmlAttribute
    private String name;
    @XmlElement
    InetAddress networkAddress;
    @XmlAttribute
    Short mask;

    public InetAddressMask() {
    }
    /* String addrmask is in the a.b.c.d/m format. */
    public InetAddressMask(String addrmask) {
        this.networkAddress = getIPAddress(addrmask);
        this.mask = getIPMaskLen(addrmask);
    }

    public InetAddress getIPAddress(String ipmask) {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(ipmask.split("/")[0]);
        } catch (UnknownHostException e1) {
            return null;
        }
        return ip;
    }

    public Short getIPMaskLen(String ipmask) {
        Short maskLen = 0;
        String[] s = ipmask.split("/");
        maskLen = (s.length == 2) ? Short.valueOf(s[1]) : 32;
        return maskLen;
    }

    /**
     * Get the IP address portion of the sub-network of the static route.
     * @return InetAddress: the IP address portion of the sub-network of the static route
     */
    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    /**
     * Set the IP address portion of the sub-network of the static route.
     * @param networkAddress The IP address (InetAddress) to be set
     */
    public void setNetworkAddress(InetAddress networkAddress) {
        this.networkAddress = networkAddress;
    }

    /**
     * Get the mask of the sub-network of the static route.
     * @return mask: the mask  (InetAddress) of the sub-network of the static route
     */
    public Short getMask() {
        return mask;
    }

    /**
     * Set the sub-network's mask of the static route.
     * @param mask The mask (InetAddress) to be set
     */
    public void setMask(Short mask) {
        this.mask = mask;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mask == null) ? 0 : mask.hashCode());
        result = prime * result
                + ((networkAddress == null) ? 0 : networkAddress.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "InetAddressMask [networkAddress=" + networkAddress + ", mask=" + mask + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InetAddressMask other = (InetAddressMask) obj;
        if (!networkAddress.equals(other.networkAddress))
            return false;
        if (!mask.equals(other.mask))
            return false;
        return true;
    }
}
