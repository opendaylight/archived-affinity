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
    
    // xxx 
    @XmlElement 
    String affinityAttribute;
    @XmlElement 
    String affinityWaypoint;

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
            System.out.println("Printing affinity attribute: " + attr.type);
            attrlist.put(attr.type, attr);
        }
    }
    public HashMap<AffinityAttributeType, AffinityAttribute> getAttributeList() {
	return this.attrlist;
    }

    /* Set the waypoint address, if the attribute is "redirect" */
    public void setAttribute(String attribute) {
	this.affinityAttribute = attribute;
    }
    
    // Create a service chain of one waypoint. 
    public void setWaypoint(String wpaddr) {
        SetPathRedirect redirect = new SetPathRedirect();
        redirect.addWaypoint(NetUtils.parseInetAddress(wpaddr));
        
        /* Add this service chain to this affinity link. */
        addAttribute((AffinityAttribute) redirect);
    }

    // Unset the waypoint address.
    public void unsetWaypoint() {        
        attrlist.remove(AffinityAttributeType.SET_PATH_REDIRECT);
    }

    public AffinityAttribute getWaypoint() {
	return attrlist.get(AffinityAttributeType.SET_PATH_REDIRECT);
    }
    
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
    public String getAttribute() {
	return this.affinityAttribute;
    }
    public AffinityGroup getFromGroup() {
	return this.fromGroup;
    }
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

