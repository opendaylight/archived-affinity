/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity.northbound;

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
import org.opendaylight.controller.affinity.IAffinityManager;
import org.opendaylight.controller.affinity.AffinityLink;
import org.opendaylight.controller.affinity.AffinityGroup;

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

/*
 * getAllAffinities()
 * getAffinity(String name)
 * addAffinity(name, ip1, ip2, type)
 * removeAffinity(String name)
 */
    /**
     * Retrieve a list of all affinities in this container.
     */
    @Path("/{containerName}/affinities")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Affinities.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Affinities getAllAffinities(@PathParam("containerName") String containerName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        IAffinityManager affinityManager = getIfAffinityManagerService(containerName);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return new Affinities(affinityManager.getAffinityConfigList());
    }
    /**
     * Returns details of affinityName affinity.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param affinityName
     *            Name of the affinity being retrieved.
     * @return affinity configuration that matches the affinity name.
     */
    @Path("/{containerName}/{affinityName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AffinityConfig.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The containerName is not found"),
            @ResponseCode(code = 415, condition = "Affinity name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AffinityConfig getAffinityDetails(
            @PathParam("containerName") String containerName,
            @PathParam("affinityName") String affinityName) {
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

        AffinityConfig ac = affinityManager.getAffinityConfig(affinityName);
        if (ac == null) {
            throw new ResourceNotFoundException(RestMessages.SERVICEUNAVAILABLE.toString());
        } else {
            return ac;
        }
    }
    /**
     * Delete an affinity
     *
     * @param containerName
     *            Name of the Container
     * @param affinityName
     *            affinity name 'String'
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/{affinityName}")
    @DELETE
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response deleteAffinity(
            @PathParam("containerName") String containerName,
            @PathParam("affinityName") String affinityName) {

        if (!NorthboundUtils.isAuthorized(
                getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException(
                    "User is not authorized to perform this operation on container "
                            + containerName);
        }

        IAffinityManager affinityManager = getIfAffinityManagerService(containerName);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity Manager "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Status ret = affinityManager.removeAffinityConfig(affinityName);
        if (ret.isSuccess()) {
            return Response.ok().build();
        }
        throw new ResourceNotFoundException(ret.getDescription());
    }

    /**
     * Add an affinity to the configuration database
     *
     * @param containerName
     *            Name of the Container
     * @param affinityName
     *            Name of the new affinity being added
     * @param networkAddress1
     *            IP address of the flow source
     * @param networkAddress2
     *            IP address of the flow destination
     * @param affinityAttribute
     *            Type of affinity being added
     * @return Response as dictated by the HTTP Response Status code
     */

    @Path("/{containerName}/{affinityName}/{networkAddress1}/{networkAddress2}/{affinityAttribute}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(Response.class)
    @StatusCodes({
            @ResponseCode(code = 200, condition = "Operation successful"),
            @ResponseCode(code = 404, condition = "The Container Name or nodeId or configuration name is not found"),
            @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response addAffinity(
            @PathParam("containerName") String containerName,
            @PathParam("affinityName") String affinityName,
            @PathParam("networkAddress1") String networkAddress1,
            @PathParam("networkAddress2") String networkAddress2,
            @PathParam("affinityAttribute") String affinityAttribute) {

        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                                            + containerName);
        }

        IAffinityManager affinityManager = getIfAffinityManagerService(containerName);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity Manager "
                                                  + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        AffinityConfig ac = new AffinityConfig(affinityName, networkAddress1, networkAddress2, affinityAttribute);
        Status ret = affinityManager.updateAffinityConfig(ac);
        if (ret.isSuccess()) {
            return Response.status(Response.Status.CREATED).build();
        }
        throw new InternalServerErrorException(ret.getDescription());
    }
}
