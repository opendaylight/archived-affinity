/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.flatl2.northbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;

import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.*;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;

import org.opendaylight.affinity.flatl2.FlatL2AffinityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Northbound APIs that provides FlatL2 methods available to control
 * the affinity aspects of a flat L2 network. This is an example
 * service and should ultimately be implemented by components
 * providing basic forwarding services.
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default. Administrator can enable it in
 * tomcat-server.xml after adding a proper keystore / SSL certificate from a
 * trusted authority.<br>
 * More info :
 * http://tomcat.apache.org/tomcat-7.0-doc/ssl-howto.html#Configuration
 *
 */
@Path("/")
public class FlatL2Northbound {

    private String username;

    private static final Logger log = LoggerFactory.getLogger(FlatL2Northbound.class);

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private FlatL2AffinityImpl getFlatL2AffinityService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null)
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames)
            if (cName.trim().equalsIgnoreCase(containerName.trim()))
                found = true;
        if (found == false)
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());

        FlatL2AffinityImpl l2mgr = (FlatL2AffinityImpl) ServiceHelper.getInstance(FlatL2AffinityImpl.class, containerName, this);
        if (l2mgr == null)
            throw new ServiceUnavailableException("FlatL2AffinityImpl " + RestMessages.SERVICEUNAVAILABLE.toString());
        return l2mgr;
    }

    @Path("/{containerName}/enableaffinitylink/{affinityLinkName}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response enableAffinityLink(
            @PathParam("containerName") String containerName,
            @PathParam("affinityLinkName") String affinityLinkName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }
        
        FlatL2AffinityImpl l2affmgr = getFlatL2AffinityService(containerName);
        log.info("Enable " + affinityLinkName);
        try {
            l2affmgr.enableAffinityLink(affinityLinkName);
        } catch (Exception e) {
            String message = "An error occurred during flow programming.";
            log.error(message, e);
        }
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/{containerName}/disableaffinitylink/{affinityLinkName}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response disableLink(
            @PathParam("containerName") String containerName,
            @PathParam("affinityLinkName") String affinityLinkName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }



        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }
        
        FlatL2AffinityImpl l2affmgr = getFlatL2AffinityService(containerName);
        log.info("Disable " + affinityLinkName);
        try {
            l2affmgr.disableAffinityLink(affinityLinkName);
        } catch (Exception e) {
            String message = "An error occurred during flow programming.";
            log.error(message, e);
        }
        return Response.status(Response.Status.CREATED).build();
    }

    
    @Path("/{containerName}/disableaffinity")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response disableAffinity(@PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }
        
        FlatL2AffinityImpl l2affmgr = getFlatL2AffinityService(containerName);
        log.info("Remove all affinity rules.");
        try {
            l2affmgr.disableAllAffinityLinks(); // Disable the currently programmed affinity. 
        } catch (Exception e) {
            String message = "An error occurred during flow programming.";
            log.error(message, e);
        }
        return Response.status(Response.Status.CREATED).build();
    }


    @Path("/{containerName}/enableaffinity")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response enableAffinity(@PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }
        
        FlatL2AffinityImpl l2affmgr = getFlatL2AffinityService(containerName);
        log.info("Push all affinity rules.");
        try {
            l2affmgr.enableAllAffinityLinks(); // Read a new snapshot of affinity config and push flow rules for it. 
        } catch (Exception e) {
            String message = "An error occurred during flow programming.";
            log.error(message, e);
        }
        return Response.status(Response.Status.CREATED).build();
    }

}
