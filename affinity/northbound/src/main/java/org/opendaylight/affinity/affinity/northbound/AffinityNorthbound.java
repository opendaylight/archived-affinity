/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.northbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.affinity.affinity.AffinityGroup;

/**
 * The class provides Northbound REST APIs to access affinity configuration.
 *
 */

@Path("/")
public class AffinityNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private IAffinityManager getIfAffinityManagerService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
                break;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        IAffinityManager affinityManager = (IAffinityManager) ServiceHelper
                .getInstance(IAffinityManager.class, containerName, this);

        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return affinityManager;
    }

    /**
     * Add an affinity to the configuration database
     *
     * @param containerName
     *            Name of the Container
     * @param affinityGroupName
     *            Name of the new affinity group being added
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/create/group/{affinityGroupName}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response createAffinityGroup(
            @PathParam("containerName") String containerName,
            @PathParam("affinityGroupName") String affinityGroupName) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }

        IAffinityManager affinityManager = getIfAffinityManagerService(containerName);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity Manager "
                                                  + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        AffinityGroup ag1 = new AffinityGroup(affinityGroupName);
        Status ret = affinityManager.addAffinityGroup(ag1);
        if (ret.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(ret.getDescription());
    }

    /**
     * Returns details of an affinity group.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param affinityGroupName
     *            Name of the affinity group being retrieved.
     * @return affinity configuration that matches the affinity name.
     */
    @Path("/{containerName}/group/{affinityGroupName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AffinityGroup.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 415, condition = "Affinity name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AffinityGroup getAffinityGroupDetails(
            @PathParam("containerName") String containerName,
            @PathParam("affinityGroupName") String affinityGroupName) {
        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }
        IAffinityManager affinityManager = getIfAffinityManagerService(containerName);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity "
                                                  + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        AffinityGroup ag = affinityManager.getAffinityGroup(affinityGroupName);
        if (ag == null) {
            throw new ResourceNotFoundException(RestMessages.SERVICEUNAVAILABLE.toString());
        } else {
            return ag;
        }
    }
}
