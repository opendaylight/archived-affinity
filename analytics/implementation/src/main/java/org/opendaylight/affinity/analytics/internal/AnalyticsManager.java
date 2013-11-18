/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.internal;

import java.lang.Short;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.affinity.affinity.AffinityGroup;
import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.analytics.IAnalyticsManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.Status;

public class AnalyticsManager implements IReadServiceListener, IAnalyticsManager {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsManager.class);

    private IAffinityManager affinityManager;
    private IfIptoHost hostTracker;

    private Map<MatchField, Host> destinationHostCache;
    private Map<MatchField, Host> sourceHostCache;
    private Map<Host, Map<Host, HostStats>> hostsToStats;

    /* Initialize data structures */
    void init() {
        this.destinationHostCache = new HashMap<MatchField, Host>();
        this.sourceHostCache = new HashMap<MatchField, Host>();
        this.hostsToStats = new HashMap<Host, Map<Host, HostStats>>();
    }

    void destroy() {
    }

    void start() {
    }

    void started(){
    }

    void stop() {
    }

    void setAffinityManager(IAffinityManager a) {
        this.affinityManager = a;
    }

    void unsetAffinityManager(IAffinityManager a) {
        if (this.affinityManager.equals(a))
            this.affinityManager = null;
    }

    void setHostTracker(IfIptoHost h) {
        this.hostTracker = h;
    }

    void unsetHostTracker(IfIptoHost h) {
        if (this.hostTracker.equals(h))
            this.hostTracker = null;
    }

    /* Returns the destination host associated with this flow, if one
     * exists.  Returns null otherwise. */
    protected Host getDestinationHostFromFlow(Flow flow, Set<HostNodeConnector> hosts) {
        Match match = flow.getMatch();
        MatchField dst = null;

        // Flow has to have DL_DST field or NW_DST field to proceed
        if (match.isPresent(MatchType.DL_DST))
            dst = match.getField(MatchType.DL_DST);
        else if (match.isPresent(MatchType.NW_DST))
            dst = match.getField(MatchType.NW_DST);
        else
            return null;

        // Check cache
        Host cacheHit = this.destinationHostCache.get(dst);
        if (cacheHit != null) 
            return cacheHit;

        // Find the destination host
        Host dstHost = null;
        for (HostNodeConnector h : hosts) {
            
            // DL_DST => compare on MAC address strings
            if (match.isPresent(MatchType.DL_DST)) {
                String dstMac = MatchType.DL_DST.stringify(dst.getValue());
                String hostMac = ((EthernetAddress) h.getDataLayerAddress()).getMacAddress();
                if (dstMac.equals(hostMac)) {
                    dstHost = h;
                    this.destinationHostCache.put(dst, dstHost); // Add to cache
                    break;
                }
            }
          
            // NW_DST => compare on IP address (of type InetAddress)
            else if (match.isPresent(MatchType.NW_DST)) {
                InetAddress hostIP = h.getNetworkAddress();
                if (dst.getValue().equals(hostIP)) {
                    dstHost = h;
                    this.destinationHostCache.put(dst, dstHost); // Add to cache
                    break;
                }
            }
        }

        return dstHost;
    }

    /* Returns the source Host associated with this flow, if one
     * exists.  Returns null otherwise. */
    protected Host getSourceHostFromFlow(Flow flow, Set<HostNodeConnector> hosts) {

        Host srcHost = null;
        Match match = flow.getMatch();

        // Flow must have IN_PORT field (DL_SRC rarely (never?)
        // exists).
        if (match.isPresent(MatchType.IN_PORT)) {
            MatchField inPort = match.getField(MatchType.IN_PORT);

            // Check cache
            Host cacheHit = this.sourceHostCache.get(inPort);
            if (cacheHit != null)
                return cacheHit;

            // Find the source host by comparing the NodeConnectors
            NodeConnector inPortNc = (NodeConnector) inPort.getValue();
            for (HostNodeConnector h : hosts) {
                NodeConnector hostNc = h.getnodeConnector();
                if (hostNc.equals(inPortNc)) {
                    srcHost = h;
                    this.sourceHostCache.put(inPort, h); // Add to cache
                    break;
                }
            }
        }
        return srcHost;
    }

    /* These are all basic getters/setters, most of which are required
     * by IAnalyticsManager */
    public long getByteCount(Host src, Host dst) {
        return getByteCountBetweenHostsInternal(src, dst, null);
    }

    public long getByteCount(Host src, Host dst, Byte protocol) {
        return getByteCountBetweenHostsInternal(src, dst, protocol);
    }

    public Map<Byte, Long> getAllByteCounts(Host src, Host dst) {
        if (this.hostsToStats.get(src) == null ||
            this.hostsToStats.get(src).get(dst) == null)
            return new HashMap<Byte, Long>();
        return this.hostsToStats.get(src).get(dst).getAllByteCounts();
    }

    public double getBitRate(Host src, Host dst) {
        return getBitRateBetweenHostsInternal(src, dst, null);
    }

    public double getBitRate(Host src, Host dst, Byte protocol) {
        return getBitRateBetweenHostsInternal(src, dst, protocol);
    }

    public Map<Byte, Double> getAllBitRates(Host src, Host dst) {
        if (this.hostsToStats.get(src) == null ||
            this.hostsToStats.get(src).get(dst) == null)
            return new HashMap<Byte, Double>();
        return this.hostsToStats.get(src).get(dst).getAllBitRates();
    }

    public Set<Byte> getProtocols(Host src, Host dst) {
        if (this.hostsToStats.get(src) == null ||
            this.hostsToStats.get(src).get(dst) == null)
            return new HashSet<Byte>();
        return this.hostsToStats.get(src).get(dst).getProtocols();
    }

    public long getByteCount(AffinityLink al) {
        return getByteCountOnAffinityLinkInternal(al, null);
    }

    public long getByteCount(AffinityLink al, Byte protocol) {
        return getByteCountOnAffinityLinkInternal(al, protocol);
    }

    public Set<Byte> getProtocols(AffinityLink al) {
        Set<Byte> protocols = new HashSet<Byte>();
        for (Entry<Host, Host> flow : this.affinityManager.getAllFlowsByHost(al)) {
            Host h1 = flow.getKey();
            Host h2 = flow.getValue();
            Set<Byte> thisProtocols = getProtocols(h1, h2);
            protocols.addAll(thisProtocols);
        }
        return protocols;
    }

    public Map<Byte, Long> getAllByteCounts(AffinityLink al) {
        Map<Byte, Long> byteCounts = new HashMap<Byte, Long>();
        Set<Byte> protocols = getProtocols(al);
        for (Byte protocol : protocols) {
            Long thisByteCounts = getByteCount(al, protocol);
            byteCounts.put(protocol, thisByteCounts);
        }
        return byteCounts;
    }

    public double getBitRate(AffinityLink al) {
        return getBitRateOnAffinityLinkInternal(al, null);
    }

    public double getBitRate(AffinityLink al, Byte protocol) {
        return getBitRateOnAffinityLinkInternal(al, protocol);
    }

    public Map<Byte, Double> getAllBitRates(AffinityLink al) {
        Map<Byte, Double> bitRates = new HashMap<Byte, Double>();
        Set<Byte> protocols = getProtocols(al);
        for (Byte protocol : protocols)
            bitRates.put(protocol, getBitRate(al, protocol));
        return bitRates;
    }

    public long getByteCount(String srcSubnet, String dstSubnet) {
        return getByteCountBySubnetInternal(srcSubnet, dstSubnet, null);
    }

    public long getByteCount(String srcSubnet, String dstSubnet, Byte protocol) {
        return getByteCountBySubnetInternal(srcSubnet, dstSubnet, protocol);
    }

    public Map<Byte, Long> getAllByteCounts(String srcSubnet, String dstSubnet) {
        Map<Byte, Long> byteCounts = new HashMap<Byte, Long>();
        Set<Byte> protocols = getProtocols(srcSubnet, dstSubnet);
        for (Byte protocol : protocols) {
            byteCounts.put(protocol, getByteCount(srcSubnet, dstSubnet, protocol));
        }
        return byteCounts;
    }

    public double getBitRate(String srcSubnet, String dstSubnet) {
        return getBitRateBySubnetInternal(srcSubnet, dstSubnet, null);
    }

    public double getBitRate(String srcSubnet, String dstSubnet, Byte protocol) {
        return getBitRateBySubnetInternal(srcSubnet, dstSubnet, protocol);
    }

    public Map<Byte, Double> getAllBitRates(String srcSubnet, String dstSubnet) {
        Map<Byte, Double> bitRates = new HashMap<Byte, Double>();
        Set<Byte> protocols = getProtocols(srcSubnet, dstSubnet);
        for (Byte protocol : protocols)
            bitRates.put(protocol, getBitRate(srcSubnet, dstSubnet, protocol));
        return bitRates;
    }

    public Set<Byte> getProtocols(String srcSubnet, String dstSubnet) {
        if (srcSubnet == null && dstSubnet == null) {
            log.debug("Source and destination subnets cannot both be null.");
            return null;
        }
        Set<Byte> protocols = new HashSet<Byte>();
        Set<Host> srcHosts;
        Set<Host> dstHosts;
        if (srcSubnet == null) {
            dstHosts = getHostsInSubnet(dstSubnet);
            srcHosts = getHostsNotInSubnet(dstSubnet);
        } else if (dstSubnet == null) {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsNotInSubnet(srcSubnet);
        } else {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsInSubnet(dstSubnet);
        }

        for (Host srcHost : srcHosts)
            for (Host dstHost : dstHosts)
                protocols.addAll(getProtocols(srcHost, dstHost));
        return protocols;
    }

    public Map<Host, Long> getIncomingHostByteCounts(String subnet) {
        return getIncomingHostByteCountsInternal(subnet, null);
    }

    public Map<Host, Long> getIncomingHostByteCounts(String subnet, Byte protocol) {
        return getIncomingHostByteCountsInternal(subnet, protocol);
    }

    /* Return byte count between two hosts, either per-protocol or not */
    private long getByteCountBetweenHostsInternal(Host src, Host dst, Byte protocol) {
        long byteCount = 0;
        if (this.hostsToStats.get(src) != null &&
            this.hostsToStats.get(src).get(dst) != null) {
            if (protocol == null)
                byteCount = this.hostsToStats.get(src).get(dst).getByteCount();
            else
                byteCount = this.hostsToStats.get(src).get(dst).getByteCount(protocol);
        }
        return byteCount;
    }

    /* Return the total bit rate between two hosts, either per-protocol or not */
    private double getBitRateBetweenHostsInternal(Host src, Host dst, Byte protocol) {
        double bitRate = 0;
        if (this.hostsToStats.get(src) != null &&
            this.hostsToStats.get(src).get(dst) != null) {
            if (protocol == null)
                bitRate = this.hostsToStats.get(src).get(dst).getBitRate();
            else
                bitRate = this.hostsToStats.get(src).get(dst).getBitRate(protocol);
        }
        return bitRate;
    }

    /* Return the duration between two hosts, either per-protocol or not */
    private double getDurationBetweenHostsInternal(Host src, Host dst, Byte protocol) {
        double duration = 0.0;
        if (this.hostsToStats.get(src) != null &&
            this.hostsToStats.get(src).get(dst) !=null) {
            if (protocol == null)
                duration = this.hostsToStats.get(src).get(dst).getDuration();
            else
                duration = this.hostsToStats.get(src).get(dst).getDuration(protocol);
        }
        return duration;
    }

    /* Return the byte count on an affinity link, per-protocol or not */
    private long getByteCountOnAffinityLinkInternal(AffinityLink al, Byte protocol) {
        long b = 0;
        List<Entry<Host, Host>> flows = this.affinityManager.getAllFlowsByHost(al);
        for (Entry<Host, Host> flow : flows) {
            Host h1 = flow.getKey();
            Host h2 = flow.getValue();
            // This will handle protocol being null
            b += getByteCountBetweenHostsInternal(h1, h2, protocol);
        }
        return b;
    }

    /* Returns bit rate in bits-per-second on an affinity link, per-protocol or not */
    private double getBitRateOnAffinityLinkInternal(AffinityLink al, Byte protocol) {
        double duration = getDurationOnAffinityLinkInternal(al, protocol);
        long totalBytes = getByteCountOnAffinityLinkInternal(al, protocol);
        if (duration == 0.0)
            return 0.0;
        return (totalBytes * 8.0) / duration;
    }

    /* Returns the duration of communication on an affinity link, per-protocol or not */
    private double getDurationOnAffinityLinkInternal(AffinityLink al, Byte protocol) {
        double maxDuration = 0.0;
        for (Entry<Host, Host> flow : this.affinityManager.getAllFlowsByHost(al)) {
            Host h1 = flow.getKey();
            Host h2 = flow.getValue();
            // This will handle protocol being null
            double duration = getDurationBetweenHostsInternal(h1, h2, protocol);
            if (duration > maxDuration)
                maxDuration = duration;
        }
        return maxDuration;
    }

    /* Return the total bytes for a particular protocol between these subnets. */
    private long getByteCountBySubnetInternal(String srcSubnet, String dstSubnet, Byte protocol) {
        long totalBytes = 0;
        if (srcSubnet == null && dstSubnet == null) {
            log.debug("Source and destination subnets cannot both be null.");
            return totalBytes;
        }
        Set<Host> srcHosts;
        Set<Host> dstHosts;
        if (srcSubnet == null) {
            dstHosts = getHostsInSubnet(dstSubnet);
            srcHosts = getHostsNotInSubnet(dstSubnet);
        } else if (dstSubnet == null) {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsNotInSubnet(srcSubnet);
        } else {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsInSubnet(dstSubnet);
        }

        for (Host srcHost : srcHosts)
            for (Host dstHost : dstHosts)
                totalBytes += getByteCount(srcHost, dstHost, protocol);
        return totalBytes;
    }

    /* Returns the duration of communication between two subnetes, per-protocol or not */
    private double getDurationBySubnetInternal(String srcSubnet, String dstSubnet, Byte protocol) {
        double maxDuration = 0.0;
        if (srcSubnet == null && dstSubnet == null) {
            log.debug("Source and destination subnet cannot both be null.");
            return maxDuration;
        }
        Set<Host> srcHosts;
        Set<Host> dstHosts;
        if (srcSubnet == null) {
            dstHosts = getHostsInSubnet(dstSubnet);
            srcHosts = getHostsNotInSubnet(dstSubnet);
        } else if (dstSubnet == null) {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsNotInSubnet(srcSubnet);
        } else {
            srcHosts = getHostsInSubnet(srcSubnet);
            dstHosts = getHostsInSubnet(dstSubnet);
        }
        for (Host srcHost : srcHosts) {
            for (Host dstHost : dstHosts) {
                double duration = getDurationBetweenHostsInternal(srcHost, dstHost, protocol);
                if (duration > maxDuration)
                    maxDuration = duration;
            }
        }
        return maxDuration;
    }

    /* Returns the bit rate between these subnects, per-protocol or not. */
    private double getBitRateBySubnetInternal(String srcSubnet, String dstSubnet, Byte protocol) {
        double duration = getDurationBySubnetInternal(srcSubnet, dstSubnet, protocol);
        long totalBytes = getByteCountBySubnetInternal(srcSubnet, dstSubnet, protocol);
        if (duration == 0.0)
            return 0.0;
        return (totalBytes * 8.0) / duration;
    }

    /* Returns all hosts that transferred data into this subnet. */
    private Map<Host, Long> getIncomingHostByteCountsInternal(String subnet, Byte protocol) {
        Map<Host, Long> hosts = new HashMap<Host, Long>();
        Set<Host> dstHosts = getHostsInSubnet(subnet);
        Set<Host> otherHosts = getHostsNotInSubnet(subnet);
        for (Host host : otherHosts) {
            for (Host targetHost : dstHosts) {
                Long byteCount = getByteCount(host, targetHost, protocol);
                if (byteCount > 0)
                    hosts.put(host, byteCount);
            }
        }
        return hosts;
    }

    private Set<Host> getHostsNotInSubnet(String subnet) {
        Set<Host> hostsInSubnet = getHostsInSubnet(subnet);
        Set<HostNodeConnector> otherHosts = this.hostTracker.getAllHosts();
        otherHosts.removeAll(hostsInSubnet);
        Set<Host> hostsNotInSubnet = new HashSet<Host>();
        for (Host h : otherHosts)
            hostsNotInSubnet.add(h);
        return hostsNotInSubnet;
    }

    private Set<Host> getHostsInSubnet(String subnet) {
        InetAddress ip;
        Short mask;
        Set<Host> hosts = new HashSet<Host>();

        // Split 1.2.3.4/5 format into the subnet (1.2.3.4) and the mask (5)
        try {
            String[] splitSubnet = subnet.split("/");
            ip = InetAddress.getByName(splitSubnet[0]);
            mask = (splitSubnet.length == 2) ? Short.valueOf(splitSubnet[1]) : 32;
        } catch (UnknownHostException e) {
            log.debug("Incorrect subnet/mask format: " + subnet);
            return hosts;
        }

        // Match on subnetes
        InetAddress targetSubnet = getSubnet(ip, mask);
        Set<HostNodeConnector> allHosts = this.hostTracker.getAllHosts();
        for (HostNodeConnector host : allHosts) {
            InetAddress hostSubnet = getSubnet(host.getNetworkAddress(), mask);
            if (hostSubnet.equals(targetSubnet))
                hosts.add(host);
        }
        return hosts;
    }

    private InetAddress getSubnet(InetAddress ip, Short mask) {
        byte[] prefix = ip.getAddress();
        InetAddress newIP = null;
        try {
            int bits = (32 - mask) % 8;
            int bytes = 4 - ((int) mask / 8);
            if (bits > 0)
                bytes--;
            // zero out the bytes
            for (int i = 1; i <= bytes; i++)
                prefix[prefix.length - i] = 0x0;
            // zero out the bits
            if (bits > 0)
                prefix[prefix.length - bytes - 1] &= (0xFF << bits);
            newIP = InetAddress.getByAddress(prefix);
        } catch (UnknownHostException e) {
        }
        return newIP;
    }

    @Override
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList) {
        Set<HostNodeConnector> allHosts = this.hostTracker.getAllHosts();

        for (FlowOnNode f : flowStatsList) {
            Host srcHost = getSourceHostFromFlow(f.getFlow(), allHosts);
            Host dstHost = getDestinationHostFromFlow(f.getFlow(), allHosts);

            // Source host being null is okay; it indicates that the
            // source of this particular flow is a switch, not a host.
            //
            // TODO: It would be useful, at least for debugging
            // output, to differentiate between when the source is a
            // switch and when it's a host that the hosttracker
            // doesn't know about. The latter would be an error.
            if (dstHost == null) {
                log.debug("Error: Destination host is null for Flow " + f.getFlow());
                continue;
            }
            else if (srcHost == null) {
                log.debug("Source host is null for Flow " + f.getFlow() + ". This is NOT necessarily an error.");
                continue;
            }

            if (this.hostsToStats.get(srcHost) == null)
                this.hostsToStats.put(srcHost, new HashMap<Host, HostStats>());
            if (this.hostsToStats.get(srcHost).get(dstHost) == null)
                this.hostsToStats.get(srcHost).put(dstHost, new HostStats());
            this.hostsToStats.get(srcHost).get(dstHost).setStatsFromFlow(f);
        }
    }

    @Override
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList) {
        // Not interested in this update
    }

    @Override
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList) {
        // Not interested in this update
    }

    @Override
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription) {
        // Not interested in this update
    }
}
