/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.northbound;

import java.lang.Long;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
import org.opendaylight.controller.sal.utils.IPProtocols;
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
        if (containerManager == null)
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames)
            if (cName.trim().equalsIgnoreCase(containerName.trim()))
                found = true;
        if (found == false)
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());

        IAnalyticsManager analyticsManager = (IAnalyticsManager) ServiceHelper.getInstance(IAnalyticsManager.class, containerName, this);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());
        return analyticsManager;
    }

    /**
     * Returns Host Statistics for a (src, dst) pair
     *
     * @param containerName: Name of the Container
     * @param dataLayerAddr: DataLayerAddress for the host
     * @param networkAddr: NetworkAddress for the host
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
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        Host srcHost = handleHostAvailability(containerName, srcNetworkAddr);
        Host dstHost = handleHostAvailability(containerName, dstNetworkAddr);
        long byteCount = analyticsManager.getByteCount(srcHost, dstHost);
        double bitRate = analyticsManager.getBitRate(srcHost, dstHost);

        return new HostStatistics(srcHost, dstHost, byteCount, bitRate);
    }

    /**
     * Returns Host Statistics for a (src, dst) pair and a particular protocol
     *
     * @param containerName: Name of the Container
     * @param srcIP: Source IP
     * @param dstIP: Destination IP
     * @param protocol: Protocol of interest
     * @return Host Statistics for a given Node.
     */
    @Path("/{containerName}/hoststats/{srcIP}/{dstIP}/{protocol}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(HostStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public HostStatistics getHostStatistics(
        @PathParam("containerName") String containerName,
        @PathParam("srcIP") String srcIP,
        @PathParam("dstIP") String dstIP,
        @PathParam("protocol") String protocol) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        Host srcHost = handleHostAvailability(containerName, srcIP);
        Host dstHost = handleHostAvailability(containerName, dstIP);
        long byteCount = analyticsManager.getByteCount(srcHost, dstHost, IPProtocols.getProtocolNumberByte(protocol));
        double bitRate = analyticsManager.getBitRate(srcHost, dstHost, IPProtocols.getProtocolNumberByte(protocol));

        return new HostStatistics(srcHost, dstHost, byteCount, bitRate);
    }

    /**
     * Returns all Host Statistics for a (src, dst) pair
     *
     * @param containerName: Name of the Container
     * @param srcIP: Source IP
     * @param dstIP: Destination IP
     * @return Host Statistics for a given Node.
     */
    @Path("/{containerName}/hoststats/{srcIP}/{dstIP}/all")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllHostStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllHostStatistics getAllHostStatistics(
        @PathParam("containerName") String containerName,
        @PathParam("srcIP") String srcIP,
        @PathParam("dstIP") String dstIP) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        Host srcHost = handleHostAvailability(containerName, srcIP);
        Host dstHost = handleHostAvailability(containerName, dstIP);
        Map<Byte, Long> byteCounts = analyticsManager.getAllByteCounts(srcHost, dstHost);
        Map<Byte, Double> bitRates = analyticsManager.getAllBitRates(srcHost, dstHost);
        AllHostStatistics allStats = new AllHostStatistics();
        for (Byte protocol : byteCounts.keySet())
            allStats.addHostStat(protocol, new HostStatistics(srcHost, dstHost, byteCounts.get(protocol), bitRates.get(protocol)));
        System.out.println(">>> " + allStats);
        return allStats;
    }

    /**
     * Returns the affinity link statistics for a given link.
     *
     * @param containerName: Name of the Container
     * @param linkName: AffinityLink name
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
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        AffinityLink al = handleAffinityLinkAvailability(containerName, affinityLinkName);
        long byteCount = analyticsManager.getByteCount(al);
        double bitRate = analyticsManager.getBitRate(al);

        return new AffinityLinkStatistics(al, byteCount, bitRate);
    }

    /**
     * Returns SubnetStatistics
     *
     * @param containerName: Name of the Container
     * @param srcIP: IP prefix
     * @param srcMask: Mask
     * @param dstIP: IP prefix
     * @param dstMask: Mask
     * @return SubnetStatistics for a particular subnet
     */
    @Path("/{containerName}/subnetstats/{srcIP}/{srcMask}/{dstIP}/{dstMask}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(SubnetStatistics.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public SubnetStatistics getSubnetStatistics(
        @PathParam("containerName") String containerName,
        @PathParam("srcIP") String srcIP,
        @PathParam("srcMask") String srcMask,
        @PathParam("dstIP") String dstIP,
        @PathParam("dstMask") String dstMask) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        long byteCount;
        // TODO: This is hardly the most elegant way to handle null prefixes
        if (srcIP.equals("null") && srcMask.equals("null"))
            byteCount = analyticsManager.getByteCount(null, dstIP + "/" + dstMask);
        else if (dstIP.equals("null") && dstMask.equals("null"))
            byteCount = analyticsManager.getByteCount(srcIP + "/" + srcMask, null);
        else
            byteCount = analyticsManager.getByteCount(srcIP + "/" + srcMask, dstIP + "/" + dstMask);
        return new SubnetStatistics(byteCount);
    }

    /**
     * Returns hosts that sent data into this prefix
     *
     * @param containerName: Name of the Container
     * @param ip: IP prefix
     * @param mask: Mask
     * @return AllHosts for the particular subnet
     */
    @Path("/{containerName}/subnetstats/incoming/{ip}/{mask}/")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @TypeHint(AllHosts.class)
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public AllHosts getIncomingHostByteCounts(
        @PathParam("containerName") String containerName,
        @PathParam("ip") String ip,
        @PathParam("mask") String mask) {
        // TODO: Change AllHosts class name to something better
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this))
            throw new UnauthorizedException("User is not authorized to perform this operation on container " + containerName);
        handleDefaultDisabled(containerName);

        IAnalyticsManager analyticsManager = getAnalyticsService(containerName);
        if (analyticsManager == null)
            throw new ServiceUnavailableException("Analytics " + RestMessages.SERVICEUNAVAILABLE.toString());

        Map<Host, Long> hosts = analyticsManager.getIncomingHostByteCounts(ip + "/" + mask);
        return new AllHosts(hosts);
    }

    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null)
            throw new InternalServerErrorException(RestMessages.INTERNALERROR.toString());
        if (containerName.equals(GlobalConstants.DEFAULT.toString()) && containerManager.hasNonDefaultContainer())
            throw new ResourceConflictException(RestMessages.DEFAULTDISABLED.toString());
    }

    private AffinityLink handleAffinityLinkAvailability(String containerName, String linkName) {
        IAffinityManager affinityManager = (IAffinityManager) ServiceHelper.getInstance(IAffinityManager.class, containerName, this);
        if (affinityManager == null)
            throw new ServiceUnavailableException("Affinity manager " + RestMessages.SERVICEUNAVAILABLE.toString());
        AffinityLink al = affinityManager.getAffinityLink(linkName);
        if (al == null)
            throw new ResourceNotFoundException(linkName + " : AffinityLink does not exist");
        return al;
    }


    private Host handleHostAvailability(String containerName, String networkAddr) {
        IfIptoHost hostTracker = (IfIptoHost) ServiceHelper.getInstance(IfIptoHost.class, containerName, this);
        if (hostTracker == null)
            throw new ServiceUnavailableException("Host tracker " + RestMessages.SERVICEUNAVAILABLE.toString());

        Set<HostNodeConnector> allHosts = hostTracker.getAllHosts();
        if (allHosts == null)
            throw new ResourceNotFoundException(networkAddr + " : " + RestMessages.NOHOST.toString());

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

        if (host == null)
            throw new ResourceNotFoundException(networkAddr + " : " + RestMessages.NOHOST.toString());
        return host;
    }
}
