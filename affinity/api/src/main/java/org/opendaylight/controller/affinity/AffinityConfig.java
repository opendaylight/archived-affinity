
/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

//import org.opendaylight.controller.sal.utils;

/**
 * The class represents an affinity configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityConfig implements Cloneable, Serializable {
    //static fields are by default excluded by Gson parser
    private static final long serialVersionUID = 1L;

    /*    private static final String affinityFields[] = { GUIField.NAME.toString(),
						     GUIField.AFFINITYFROM.toString(), 
						     GUIField.AFFINITYTO.toString(), 
						     GUIField.AFFINITYTYPE.toString() };
    */

    // Order matters: JSP file expects following fields in the
    // following order
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String fromIp; // A.B.C.D
    private String toIp; // A.B.C.D
    private String affinityType;

    public AffinityConfig() {
    }

    public AffinityConfig(String desc, String from, String to, String aType) {
        name = desc;
        fromIp = from;
        toIp = to;
        affinityType = aType;
    }

    public AffinityConfig(AffinityConfig ac) {
        name = ac.name;
        fromIp = ac.fromIp;
        toIp = ac.toIp;
        affinityType = ac.affinityType;
    }

    public String getName() {
        return name;
    }
    public InetAddress getFromIP() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(fromIp);
        } catch (UnknownHostException e1) {
            return null;
        }
        return ip;
    }

    public InetAddress getToIP() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(toIp);
        } catch (UnknownHostException e1) {
            return null;
        }
        return ip;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        /*
         * Configuration will be stored in collection only if it is valid
         * Hence we don't check here for uninitialized fields
         */
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AffinityConfig that = (AffinityConfig) obj;
        if (this.fromIp.equals(that.fromIp) && this.toIp.equals(that.toIp) && (this.affinityType.equals(that.affinityType))) {
            return true;
        }
        return false;
    }
    /*
    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : affinityFields) {
            fieldList.add(str);
        }
        return fieldList;
    }
    */
    @Override
    public String toString() {
        return ("AffinityConfig [Description=" + name + ", From=" + fromIp
                + ", To=" + toIp + ", Type=" + affinityType + "]");
    }

    /**
     * Implement clonable interface
     */
    @Override
    public AffinityConfig clone() {
        return new AffinityConfig(this);
    }

}
