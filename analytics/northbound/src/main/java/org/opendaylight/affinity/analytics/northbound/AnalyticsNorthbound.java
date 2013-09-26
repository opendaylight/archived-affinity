/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;

import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.analytics.IAnalyticsManager;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.*;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Northbound APIs that returns various Analytics exposed by the Southbound
 * plugins such as Openflow.
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
public class AnalyticsNorthbound {

    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        username = context.getUserPrincipal().getName();
    }

    protected String getUserName() {
        return username;
    }

    private IAnalyticsManager getAnalyticsService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
        }

        IAnalyticsManager analyticsManager = (IAnalyticsManager) ServiceHelper.getInstance(IAnalyticsManager.class, containerName, this);
        if (analyticsManager == null) {
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return analyticsManager;
    }

    /**
     * Returns Host Statistics for a (src, dst) pair
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param dataLayerAddr
     *            DataLayerAddress for the host
     * @param networkAddr
     *            NetworkAddress for the host
     * @return Host Statistics for a given Node.
     */
    @Path("/{containerName}/hoststats/{srcNetworkAddr}/{dstNetworkAddr}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public HostStatistics getHostStatistics(
        @PathParam("containerName") String containerName,
        @PathParam("srcNetworkAddr") String srcNetworkAddr,
        @PathParam("dstNetworkAddr") String dstNetworkAddr) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        }
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null) {
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Host srcHost = handleHostAvailability(containerName, srcNetworkAddr);
        Host dstHost = handleHostAvailability(containerName, dstNetworkAddr);
        long byteCount = analyticsManager.getByteCountBetweenHosts(srcHost, dstHost);
        double bitRate = analyticsManager.getBitRateBetweenHosts(srcHost, dstHost);

        return new HostStatistics(srcHost, dstHost, byteCount, bitRate);
    }

    /**
     * Returns the affinity link statistics for a given link.
     *
     * @param containerName
     *            Name of the Container. The Container name for the base
     *            controller is "default".
     * @param linkName
     *            AffinityLink name.
     * @return List of Affinity Link Statistics for a given link.
     */
    @Path("/{containerName}/affinitylinkstats/{linkName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AffinityLinkStatistics getAffinityLinkStatistics(
        @PathParam("containerName") String containerName,
        @PathParam("linkName") String affinityLinkName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        }
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null) {
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        AffinityLink al = handleAffinityLinkAvailability(containerName, affinityLinkName);
        long byteCount = analyticsManager.getByteCountOnAffinityLink(al);
        double bitRate = analyticsManager.getBitRateOnAffinityLink(al);

        return new AffinityLinkStatistics(al, byteCount, bitRate);
    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
        }

        if (containerName.equals(GlobalConstants.DEFAULT.toString()) && containerManager.hasNonDefaultContainer()) {
            throw new ResourceConflictException(RestMessages.DEFAULTDISABLED.toString());
        }
    }

    private AffinityLink handleAffinityLinkAvailability(String containerName, String linkName) {

        IAffinityManager affinityManager = (IAffinityManager) ServiceHelper.getInstance(IAffinityManager.class, containerName, this);
        if (affinityManager == null) {
            throw new ServiceUnavailableException("Affinity manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        AffinityLink al = affinityManager.getAffinityLink(linkName);
        if (al == null) {
            throw new ResourceNotFoundException(linkName + " : AffinityLink does not exist");
        }

        return al;
    }


    private Host handleHostAvailability(String containerName, String networkAddr) {

        IfIptoHost hostTracker = (IfIptoHost) ServiceHelper.getInstance(IfIptoHost.class, containerName, this);
        if (hostTracker == null) {
            throw new ServiceUnavailableException("Host tracker " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Set<HostNodeConnector> allHosts = hostTracker.getAllHosts();
        if (allHosts == null) {
            throw new ResourceNotFoundException(networkAddr + " : " + RestMessages.NOHOST.toString());
        }

        Host host = null;
        try {
            InetAddress networkAddress = InetAddress.getByName(networkAddr);
            for (Host h : allHosts) {
                if (h.getNetworkAddress().equals(networkAddress)) {
                    host = h;
                    break;
                }
            }
        } catch (UnknownHostException e) {
        }

        if (host == null) {
            throw new ResourceNotFoundException(networkAddr + " : " + RestMessages.NOHOST.toString());
        }

        return host;
    }
}
