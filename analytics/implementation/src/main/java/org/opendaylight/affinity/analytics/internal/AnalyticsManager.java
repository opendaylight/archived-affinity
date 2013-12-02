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
import java.util.Arrays;
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

    /* Return the number of bytes transferred between two sets,
     * per-protocol (across all protocols if protocol is null).*/
    protected long getByteCount(Set<Host> srcSet, Set<Host> dstSet, Byte protocol) {
        long byteCount = 0;
        for (Host src : srcSet) {
            for (Host dst : dstSet) {
                if (this.hostsToStats.get(src) != null &&
                    this.hostsToStats.get(src).get(dst) != null) {
                    if (protocol == null)
                        byteCount += this.hostsToStats.get(src).get(dst).getByteCount();
                    else
                        byteCount += this.hostsToStats.get(src).get(dst).getByteCount(protocol);
                }
            }
        }
        return byteCount;
    }

    /* Return all protocols used between two sets */
    protected Set<Byte> getProtocols(Set<Host> srcSet, Set<Host> dstSet) {
        Set<Byte> protocols = new HashSet<Byte>();
        for (Host src : srcSet) {
            for (Host dst : dstSet) {
                if (this.hostsToStats.get(src) != null &&
                    this.hostsToStats.get(src).get(dst) != null) {
                    protocols.addAll(this.hostsToStats.get(src).get(dst).getProtocols());
                }
            }
        }
        return protocols;
    }

    /* Returns a map of all byte counts between two sets */
    protected Map<Byte, Long> getAllByteCounts(Set<Host> srcSet, Set<Host> dstSet) {
        Map<Byte, Long> byteCounts = new HashMap<Byte, Long>();
        Set<Byte> protocols = getProtocols(srcSet, dstSet);
        for (Byte protocol : protocols)
            byteCounts.put(protocol, getByteCount(srcSet, dstSet, protocol));
        return byteCounts;
    }

    /* Returns the duration of communication between two sets (max
     * duration of all flows) for a particular protocol (across all
     * protocols if protocol is null) */
    protected double getDuration(Set<Host> srcSet, Set<Host> dstSet, Byte protocol) {
        double maxDuration = 0.0;
        for (Host src : srcSet) {
            if (this.hostsToStats.get(src) == null)
                continue;
            for (Host dst : dstSet) {
                double duration;
                if (this.hostsToStats.get(src).get(dst) == null)
                    continue;
                if (protocol == null)
                    duration = this.hostsToStats.get(src).get(dst).getDuration();
                else
                    duration = this.hostsToStats.get(src).get(dst).getDuration(protocol);
                if (duration > maxDuration)
                    maxDuration = duration;
            }
        }
        return maxDuration;
    }

    /* Returns the bit rate between two sets */
    protected double getBitRate(Set<Host> srcSet, Set<Host> dstSet, Byte protocol) {
        double duration = getDuration(srcSet, dstSet, protocol);
        long totalBytes = getByteCount(srcSet, dstSet, protocol);
        if (duration == 0.0)
            return 0.0;
        return (totalBytes * 8.0) / duration;
    }

    protected Map<Byte, Double> getAllBitRates(Set<Host> srcSet, Set<Host> dstSet) {
        Map<Byte, Double> bitRates = new HashMap<Byte, Double>();
        Set<Byte> protocols = getProtocols(srcSet, dstSet);
        for (Byte protocol : protocols)
            bitRates.put(protocol, getBitRate(srcSet, dstSet, protocol));
        return bitRates;
    }

    /* These are all basic getters/setters, most of which are required
     * by IAnalyticsManager */
    public long getByteCount(Host src, Host dst) {
        return getByteCount(src, dst, null);
    }

    public long getByteCount(Host src, Host dst, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(Arrays.asList(src));
        Set<Host> dstSet = new HashSet<Host>(Arrays.asList(dst));
        return getByteCount(srcSet, dstSet, protocol);
    }

    public Map<Byte, Long> getAllByteCounts(Host src, Host dst) {
        Set<Host> srcSet = new HashSet<Host>(Arrays.asList(src));
        Set<Host> dstSet = new HashSet<Host>(Arrays.asList(dst));
        return getAllByteCounts(srcSet, dstSet);
    }

    public double getDuration(Host src, Host dst) {
        return getDuration(src, dst, null);
    }

    public double getDuration(Host src, Host dst, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(Arrays.asList(src));
        Set<Host> dstSet = new HashSet<Host>(Arrays.asList(dst));
        return getDuration(srcSet, dstSet, protocol);
    }

    public double getBitRate(Host src, Host dst) {
        return getBitRate(src, dst, null);
    }

    public double getBitRate(Host src, Host dst, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(Arrays.asList(src));
        Set<Host> dstSet = new HashSet<Host>(Arrays.asList(dst));
        return getBitRate(srcSet, dstSet, protocol);
    }

    public Map<Byte, Double> getAllBitRates(Host src, Host dst) {
        Set<Host> srcSet = new HashSet<Host>(Arrays.asList(src));
        Set<Host> dstSet = new HashSet<Host>(Arrays.asList(dst));
        return getAllBitRates(srcSet, dstSet);
    }

    public long getByteCount(AffinityLink al) {
        return getByteCount(al, null);
    }

    public long getByteCount(AffinityLink al, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getToGroup()));
        Set<Host> dstSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getFromGroup()));
        return getByteCount(srcSet, dstSet, protocol);
    }

    public Map<Byte, Long> getAllByteCounts(AffinityLink al) {
        Set<Host> srcSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getToGroup()));
        Set<Host> dstSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getFromGroup()));
        return getAllByteCounts(srcSet, dstSet);
    }

    public double getDuration(AffinityLink al) {
        return getDuration(al, null);
    }

    public double getDuration(AffinityLink al, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getToGroup()));
        Set<Host> dstSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getFromGroup()));
        return getDuration(srcSet, dstSet, protocol);
    }

    public double getBitRate(AffinityLink al) {
        return getBitRate(al, null);
    }

    public double getBitRate(AffinityLink al, Byte protocol) {
        Set<Host> srcSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getToGroup()));
        Set<Host> dstSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getFromGroup()));
        return getBitRate(srcSet, dstSet, protocol);
    }

    public Map<Byte, Double> getAllBitRates(AffinityLink al) {
        Set<Host> srcSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getToGroup()));
        Set<Host> dstSet = new HashSet<Host>(this.affinityManager.getAllElementsByHost(al.getFromGroup()));
        return getAllBitRates(srcSet, dstSet);
    }

    public long getByteCount(String srcSubnet, String dstSubnet) {
        return getByteCount(srcSubnet, dstSubnet, null);
    }

    public long getByteCount(String srcSubnet, String dstSubnet, Byte protocol) {
        Set<Host> srcSet = getSrcHosts(srcSubnet, dstSubnet);
        Set<Host> dstSet = getSrcHosts(dstSubnet, srcSubnet); // reverse arguments
        return getByteCount(srcSet, dstSet, protocol);
    }

    public Map<Byte, Long> getAllByteCounts(String srcSubnet, String dstSubnet) {
        Set<Host> srcSet = getSrcHosts(srcSubnet, dstSubnet);
        Set<Host> dstSet = getSrcHosts(dstSubnet, srcSubnet); // reverse arguments
        return getAllByteCounts(srcSet, dstSet);
    }

    public double getDuration(String srcSubnet, String dstSubnet) {
        return getDuration(srcSubnet, dstSubnet, null);
    }

    public double getDuration(String srcSubnet, String dstSubnet, Byte protocol) {
        Set<Host> srcSet = getSrcHosts(srcSubnet, dstSubnet);
        Set<Host> dstSet = getSrcHosts(dstSubnet, srcSubnet); // reverse arguments
        return getDuration(srcSet, dstSet, protocol);
    }

    public double getBitRate(String srcSubnet, String dstSubnet) {
        return getBitRate(srcSubnet, dstSubnet, null);
    }

    public double getBitRate(String srcSubnet, String dstSubnet, Byte protocol) {
        Set<Host> srcSet = getSrcHosts(srcSubnet, dstSubnet);
        Set<Host> dstSet = getSrcHosts(dstSubnet, srcSubnet); // reverse arguments
        return getBitRate(srcSet, dstSet, protocol);
    }

    public Map<Byte, Double> getAllBitRates(String srcSubnet, String dstSubnet) {
        Set<Host> srcSet = getSrcHosts(srcSubnet, dstSubnet);
        Set<Host> dstSet = getSrcHosts(dstSubnet, srcSubnet); // reverse arguments
        return getAllBitRates(srcSet, dstSet);
    }

    public Map<Host, Long> getIncomingHostByteCounts(String subnet) {
        return getIncomingHostByteCounts(subnet, null);
    }

    public Map<Host, Long> getIncomingHostByteCounts(String subnet, Byte protocol) {
         Set<HostNodeConnector> allHosts = this.hostTracker.getAllHosts();
         return getIncomingHostByteCounts(subnet, protocol, allHosts);
    }

    /* Returns all hosts that transferred data into this subnet. */
    public Map<Host, Long> getIncomingHostByteCounts(String subnet, Byte protocol, Set<HostNodeConnector> allHosts) {
        Map<Host, Long> hosts = new HashMap<Host, Long>();
        Set<Host> dstHosts = getHostsInSubnet(subnet, allHosts);
        Set<Host> otherHosts = getHostsNotInSubnet(subnet, allHosts);
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
        return getHostsNotInSubnet(subnet, this.hostTracker.getAllHosts());
    }

    protected Set<Host> getHostsNotInSubnet(String subnet, Set<HostNodeConnector> allHosts) {
        Set<Host> hostsInSubnet = getHostsInSubnet(subnet, allHosts);
        Set<HostNodeConnector> otherHosts = new HashSet<HostNodeConnector>(allHosts); // copy constructor
        otherHosts.removeAll(hostsInSubnet);
        Set<Host> hostsNotInSubnet = new HashSet<Host>();
        for (Host h : otherHosts)
            hostsNotInSubnet.add(h);
        return hostsNotInSubnet;
    }

    // Handles null subnets
    private Set<Host> getSrcHosts(String srcSubnet, String dstSubnet) {
        if (srcSubnet == null && dstSubnet == null) {
            log.debug("Source and destination subnets cannot both be null.");
            return new HashSet<Host>();
        }
        if (srcSubnet == null)
            return getHostsNotInSubnet(dstSubnet);
        else
            return getHostsInSubnet(srcSubnet);
    }

    private Set<Host> getHostsInSubnet(String subnet) {
        return getHostsInSubnet(subnet, this.hostTracker.getAllHosts());
    }

    protected Set<Host> getHostsInSubnet(String subnet, Set<HostNodeConnector> allHosts) {
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
        nodeFlowStatisticsUpdated(node, flowStatsList, this.hostTracker.getAllHosts());
    }

    protected void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList, Set<HostNodeConnector> allHosts) {
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
