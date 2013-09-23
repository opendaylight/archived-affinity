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
    @XmlElement 
    String affinityAttribute;
    @XmlElement 
    String affinityWaypoint;

    public AffinityLink() {
    }
    public AffinityLink(String name, AffinityGroup fromGroup, AffinityGroup toGroup) {
	this.name = name;
	this.fromGroup = fromGroup;
	this.toGroup = toGroup;
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
    public void setAttribute(String attribute) {
	this.affinityAttribute = attribute;
    }

    /* Set the waypoint address, if the attribute is "redirect" */
    public void setWaypoint(String wpaddr) {
	this.affinityWaypoint = wpaddr;
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
}

