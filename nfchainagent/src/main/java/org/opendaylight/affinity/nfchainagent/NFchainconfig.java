package org.opendaylight.affinity.nfchainagent;

import org.opendaylight.controller.sal.utils.NetUtils;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;

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

import org.opendaylight.controller.sal.flowprogrammer.Flow;

/** 
 * Configuration object representing a network function chain. 
 * flowlist is the set of flows to be redirected. 
 * dstIP is the singleton waypoint, representing the waypoint server. 
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NFchainconfig implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String name;
    @XmlElement
    private final List<Flow> flowlist;
    private InetAddress dstIP;
    
    public NFchainconfig(String name) {
	this.name = name;
	flowlist = new ArrayList<Flow>();
        dstIP = null;
    }

    // Set the flowlist and destination IP of the network function. 
    public NFchainconfig(String name, List<Flow> flowlist, InetAddress dstIP) {
	this.name = name;
	this.flowlist = flowlist;
        this.dstIP = dstIP;
    }

    // add a flow to the flowlist. 
    public Status addFlow(Flow f) {
        flowlist.add(f);
        return new Status(StatusCode.SUCCESS);
    }

    public List<Flow> getFlowList() {
        return this.flowlist;
    }
    public InetAddress getWaypointIP() {
        return this.dstIP;
    }
    public void print() {
	System.out.println("Printing NFchain config " + this.name);
	for (Flow value : flowlist) {
	    System.out.println("flow is " + value);
	}
    }
    public String getName() {
	return name;
    }
}


