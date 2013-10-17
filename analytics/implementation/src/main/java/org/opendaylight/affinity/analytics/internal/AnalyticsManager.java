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
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;

// TODO: get "analytics" somewhere in this namespace
//import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.rev131016.HostStatistics;

public class AnalyticsManager implements IReadServiceListener, IAnalyticsManager {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsManager.class);

    private IAffinityManager affinityManager;
    private IStatisticsManager statisticsManager;
    private IfIptoHost hostTracker;

    private Map<MatchField, Host> destinationHostCache;
    private Map<MatchField, Host> sourceHostCache;
    private Map<Host, Map<Host, HostStats>> hostsToStats;

    void init() {
        log.debug("INIT called!");
        this.destinationHostCache = new HashMap<MatchField, Host>();
        this.sourceHostCache = new HashMap<MatchField, Host>();
        this.hostsToStats = new HashMap<Host, Map<Host, HostStats>>();
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");
    }

    void started(){
    }

    void stop() {
        log.debug("STOP called!");
    }

    void setAffinityManager(IAffinityManager a) {
        this.affinityManager = a;
    }

    void unsetAffinityManager(IAffinityManager a) {
        if (this.affinityManager.equals(a)) {
            this.affinityManager = null;
        }
    }

    void setStatisticsManager(IStatisticsManager s) {
        this.statisticsManager = s;
    }

    void unsetStatisticsManager(IStatisticsManager s) {
        if (this.statisticsManager.equals(s)) {
            this.statisticsManager = null;
        }
    }

    void setHostTracker(IfIptoHost h) {
        this.hostTracker = h;
    }

    void unsetHostTracker(IfIptoHost h) {
        if (this.hostTracker.equals(h)) {
            this.hostTracker = null;
        }
    }

    /* Returns the destination host associated with this flow, if one
     * exists.  Returns null otherwise.
     */
    protected Host getDestinationHostFromFlow(Flow flow, Set<HostNodeConnector> hosts) {
        Match match = flow.getMatch();
        MatchField dst = null;

        // Flow has to have DL_DST field or NW_DST field to proceed
        if (match.isPresent(MatchType.DL_DST)) {
            dst = match.getField(MatchType.DL_DST);
        } else if (match.isPresent(MatchType.NW_DST)) {
            dst = match.getField(MatchType.NW_DST);
        } else { 
            return null;
        }

        // Check cache
        Host cacheHit = this.destinationHostCache.get(dst);
        if (cacheHit != null) {
            return cacheHit;
        }

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
     * exists.  Returns null otherwise.
     */
    protected Host getSourceHostFromFlow(Flow flow, Set<HostNodeConnector> hosts) {

        Host srcHost = null;
        Match match = flow.getMatch();

        // Flow must have IN_PORT field (DL_SRC rarely (never?)
        // exists).
        if (match.isPresent(MatchType.IN_PORT)) {
            MatchField inPort = match.getField(MatchType.IN_PORT);

            // Check cache
            Host cacheHit = this.sourceHostCache.get(inPort);
            if (cacheHit != null) {
                return cacheHit;
            }

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

    public long getByteCountBetweenHosts(Host src, Host dst) {

        long byteCount = 0;
        if (this.hostsToStats.get(src) != null &&
            this.hostsToStats.get(src).get(dst) != null) {
            byteCount = this.hostsToStats.get(src).get(dst).getByteCount();
        }

        return byteCount;
    }

    public double getBitRateBetweenHosts(Host src, Host dst) {
        double bitRate = 0;
        if (this.hostsToStats.get(src) != null &&
            this.hostsToStats.get(src).get(dst) != null) {
            bitRate = this.hostsToStats.get(src).get(dst).getBitRate();
        }

        return bitRate;
    }

    public double getBitRateOnAffinityLink(AffinityLink al) {
        // Returns bit rate in *bits-per-second*
        double maxDuration = 0;
        int totalBytes = 0;
        List<Entry<Host, Host>> flows = this.affinityManager.getAllFlowsByHost(al);
        for (Entry<Host, Host> flow : flows) {
            Host h1 = flow.getKey();
            Host h2 = flow.getValue();
            if (this.hostsToStats.get(h1) != null &&
                this.hostsToStats.get(h1).get(h2) != null) {
                totalBytes += getByteCountBetweenHosts(h1, h2);
                double duration = this.hostsToStats.get(h1).get(h2).getDuration();
                if (duration > maxDuration) {
                    maxDuration = duration;
                }
            }
        }
        if (maxDuration == 0.0) {
            return 0.0;
        } else {
            return (totalBytes * 8.0) / maxDuration;
        }
    }

    public long getByteCountOnAffinityLink(AffinityLink al) {
        long b = 0;
        List<Entry<Host, Host>> flows = this.affinityManager.getAllFlowsByHost(al);
        for (Entry<Host, Host> flow : flows) {
            Host h1 = flow.getKey();
            Host h2 = flow.getValue();
            b += getByteCountBetweenHosts(h1, h2);
        }

        return b;
    }

    private InetAddress getPrefix(InetAddress ip, Short mask) {
        byte[] prefix = ip.getAddress();
        InetAddress newIP = null;
        try {
            int bits = (32 - mask) % 8;
            int bytes = 4 - ((int) mask / 8);
            if (bits > 0) {
                bytes--;
            }
            // zero out the bytes
            for (int i = 1; i <= bytes; i++) {
                prefix[prefix.length - i] = 0x0;
            }
            // zero out the bits
            if (bits > 0) {
                prefix[prefix.length - bytes - 1] &= (0xFF << bits);
            }
            newIP = InetAddress.getByAddress(prefix);
        } catch (UnknownHostException e) {
            // TODO:
        }
        return newIP;
    }

    public long getByteCountIntoHost(Host targetHost) {
        long totalBytes = 0;
        // We're calculating bytes *into* the target host, not out of
        for (Host sourceHost : this.hostsToStats.keySet()) {
            if (this.hostsToStats.get(sourceHost).get(targetHost) != null) {
                totalBytes += this.hostsToStats.get(sourceHost).get(targetHost).getByteCount();
            }
        }
        return totalBytes;
    }

    public long getByteCountIntoPrefix(String prefixAndMask, Set <HostNodeConnector> allHosts) {
        long totalBytes = 0;
        InetAddress ip;
        Short mask;

        // Split 1.2.3.4/5 format into the prefix (1.2.3.4) and the mask (5)
        try {
            String[] splitPrefix = prefixAndMask.split("/");
            ip = InetAddress.getByName(splitPrefix[0]);
            mask = (splitPrefix.length == 2) ? Short.valueOf(splitPrefix[1]) : 32;
        } catch (UnknownHostException e) {
            log.debug("Incorrect prefix/mask format: " + prefixAndMask);
            return 0;
        }

        // Match on prefixes
        InetAddress targetPrefix = getPrefix(ip, mask);
        for (HostNodeConnector host : allHosts) {
            InetAddress hostPrefix = getPrefix(host.getNetworkAddress(), mask);
            if (hostPrefix.equals(targetPrefix)) {
                totalBytes += getByteCountIntoHost(host);
            }
        }

        return totalBytes;
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

            if (this.hostsToStats.get(srcHost) == null) {
                this.hostsToStats.put(srcHost, new HashMap<Host, HostStats>());
            }
            if (this.hostsToStats.get(srcHost).get(dstHost) == null) {
                this.hostsToStats.get(srcHost).put(dstHost, new HostStats());
            }
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
