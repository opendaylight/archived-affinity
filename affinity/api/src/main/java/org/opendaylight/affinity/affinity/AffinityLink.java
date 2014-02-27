/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.affinity.affinity;

import org.opendaylight.controller.sal.utils.NetUtils;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.affinity.affinity.AffinityAttribute;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityLink implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String name;
    @XmlElement
    AffinityGroup fromGroup;
    @XmlElement
    AffinityGroup toGroup;

    // Keep at most one affinity attribute per type. 
    @XmlElement
    private HashMap<AffinityAttributeType, AffinityAttribute> attrlist;
    
    public AffinityLink() {
        attrlist = new HashMap<AffinityAttributeType, AffinityAttribute>();
    }
    public AffinityLink(String name, AffinityGroup fromGroup, AffinityGroup toGroup) {
	this.name = name;
	this.fromGroup = fromGroup;
	this.toGroup = toGroup;
        attrlist = new HashMap<AffinityAttributeType, AffinityAttribute>();
    }
    public String getName() {
	return this.name;
    }
    public void setName(String name) {
	this.name = name;
    }
    public void setFromGroup(AffinityGroup fromGroup) {
	this.fromGroup = fromGroup;
    }
    public void setToGroup(AffinityGroup toGroup) {
	this.toGroup = toGroup;
    }
    public void addAttribute(AffinityAttribute attr) {
        if (attr != null) {
            attrlist.put(attr.type, attr);
        }
    }
    @JsonIgnore
    public HashMap<AffinityAttributeType, AffinityAttribute> getAttributeList() {
	return this.attrlist;
    }
    
    /* Set the waypoint address, if the attribute is "redirect" */
    public void setWaypoint(String wpaddr) {
        SetPathRedirect redirect = new SetPathRedirect();
        redirect.addWaypoint(NetUtils.parseInetAddress(wpaddr));
        
        /* Add this service chain to this affinity link. */
        addAttribute((AffinityAttribute) redirect);
    }

    /* Get the waypoint address */
    @XmlElement(name="waypoint")
    public AffinityAttribute getWaypoint() {
	return attrlist.get(AffinityAttributeType.SET_PATH_REDIRECT);
    }
    
    // Unset the waypoint attribute.
    public void unsetWaypoint() {
        attrlist.remove(AffinityAttributeType.SET_PATH_REDIRECT);
    }

    // Add tap attribute. 
    public void addTap(String ipaddr) {
        // Check if a tap attribute is already available on this link. 
        // If not, create one and add IP address to it. 
        AffinityAttributeType aatype = AffinityAttributeType.SET_TAP;
        SetTap tap = (SetTap) attrlist.get(aatype);

        if (tap == null) {
            tap = new SetTap();
        }
        // add a tap server
        tap.addTap(NetUtils.parseInetAddress(ipaddr));
        
        /* Add this tap set to the affinity link. */
        addAttribute((AffinityAttribute) tap);
    }

    // Add tap configuration. 
    public void removeTap(String ipaddr) {
        // Check if a tap attribute is already available on this link. 
        AffinityAttributeType aatype = AffinityAttributeType.SET_TAP;
        SetTap tap = (SetTap) attrlist.get(aatype);
        
        // tap attribute exists. Remove IP address from its list of IP adresses. 
        if (tap != null) {
            tap.removeTap(NetUtils.parseInetAddress(ipaddr));
        }
    }

    /* tbd requires nb method. */
    @XmlElement(name="tapList")
    public List<InetAddress> getTapList() {
        // Check if a tap attribute is already available on this link. 
        SetTap tap = (SetTap) attrlist.get(AffinityAttributeType.SET_TAP);
        if (tap != null) {
            return tap.getTapList();
        }
        return null;
    }
    
    @XmlElement(name="deny")
    public boolean isDeny() {
        return attrlist.containsKey(AffinityAttributeType.SET_DENY);
    }

    // Mark this with "deny"
    public void setDeny() {
        SetDeny deny = new SetDeny();
        addAttribute(deny);
    }

    // Remove "deny" marking if it exists
    public void unsetDeny() {
        attrlist.remove(AffinityAttributeType.SET_DENY);
    }

    @XmlElement(name="isolate")
    public boolean isIsolate() {
        return attrlist.containsKey(AffinityAttributeType.SET_PATH_ISOLATE);
    }

    // Mark this with "isolate"
    public void setIsolate() {
        SetPathIsolate iso = new SetPathIsolate();
        addAttribute(iso);
        SetMaxTputPath mtp = new SetMaxTputPath();
        addAttribute(mtp);
    }
    public void unsetIsolate() {
        attrlist.remove(AffinityAttributeType.SET_PATH_ISOLATE);
    }

    @XmlElement(name="fromGroup")
    public AffinityGroup getFromGroup() {
	return this.fromGroup;
    }
    @XmlElement(name="toGroup")
    public AffinityGroup getToGroup() {
	return this.toGroup;
    }
    @Override
    public String toString() {
        String output = this.name;

        if (attrlist != null) {
            for (AffinityAttribute a: attrlist.values()) {
                output = output + "attr: " + a.toString() + "; ";
            }
        }
        return output;
    }
}

