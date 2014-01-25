/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.affinity.affinity.northbound;

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
import org.opendaylight.controller.sal.core.Host;


/** 
 * AffinityGroupHosts class to return identifiers by MAC
 * address. This is created and populated by the northbound and
 * manager classes.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityGroupHosts implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    @XmlAttribute
    private String name;

    @XmlElement
    private final List<Host> hostlist;

    public AffinityGroupHosts() {
        hostlist = new ArrayList<Host>();
    }

    public AffinityGroupHosts(String name, List<Host> hostlist) {
	this.name = name;
	this.hostlist = hostlist;
    }
    @Override
    public String toString() {
        String output = this.name;
        return output;
    }

    public String getName() {
	return name;
    }

    public List<Host> getHosts() {
        return hostlist;
    }
}

