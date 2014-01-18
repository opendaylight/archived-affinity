package org.opendaylight.affinity.affinity;

import org.opendaylight.controller.sal.utils.NetUtils;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

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

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityGroup implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String name;

    @XmlElement
    private final Map<String, AffinityIdentifier> elements;

    public AffinityGroup() {
        this("");
    }

    public AffinityGroup(String name) {
	this.name = name;
	elements = new HashMap<String, AffinityIdentifier>();
    }
    public AffinityGroup(String name, HashMap<String, AffinityIdentifier> elements) {
	this.name = name;
	this.elements = elements;
    }

    // Basic affinity element, IP address
    public Status add(String ipaddress) {
        AffinityIdentifier<InetAddress> elem = new AffinityIdentifier();
        
        elem.setName(ipaddress);
        if (NetUtils.isIPAddressValid(ipaddress)) {
            elem.set(NetUtils.parseInetAddress(ipaddress));
            elements.put(ipaddress, elem);
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.BADREQUEST);
        }
    }

    // Basic affinity element, IP prefix/mask
    public Status addInetMask(String addrmask) {
        InetAddressMask inetaddrmask = new InetAddressMask(addrmask);
	AffinityIdentifier<InetAddressMask> elem = new AffinityIdentifier();

        elem.setName(addrmask);
        elem.set(inetaddrmask);
        elements.put(addrmask, elem);
        return new Status(StatusCode.SUCCESS);
    }

    // Remove an affinity element given its IP address.
    public void remove(String ipaddress) {
	if (elements != null) {
	    elements.remove(ipaddress);
	}
    }
    public Integer size() {
	return (elements.size());
    }

    @Override
    public String toString() {
        String output = this.name;

	for (AffinityIdentifier value : elements.values()) {
	    output = output + ", " + value;
	}
        return output;
    }

    public String getName() {
	return name;
    }

    // TODO: This should not exist.  It's a replacement for a more
    // robust "is host h a member of this affinity group".
    @XmlElement(name="endpoints")
    public Set<String> getIPs() {
        return elements.keySet();
    }

    @JsonIgnore
    public ArrayList<AffinityIdentifier> getAllElements() {
        ArrayList<AffinityIdentifier> retvalues = new ArrayList<AffinityIdentifier>(elements.values());
	return retvalues;
    }
}

