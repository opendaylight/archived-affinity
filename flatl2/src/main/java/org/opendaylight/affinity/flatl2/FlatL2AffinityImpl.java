/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.flatl2;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.utils.IPProtocols;

import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.core.Path;

import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NetUtils;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.GlobalConstants;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.hosttracker.HostIdFactory;
import org.opendaylight.controller.hosttracker.IHostId;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.affinity.affinity.InetAddressMask;

import org.opendaylight.controller.hosttracker.HostIdFactory;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.affinity.AffinityAttributeType;
import org.opendaylight.affinity.affinity.AffinityAttribute;
import org.opendaylight.affinity.affinity.AffinityPath;
import org.opendaylight.affinity.affinity.HostPairPath;
import org.opendaylight.affinity.affinity.SetDeny;
import org.opendaylight.affinity.affinity.SetPathIsolate;
import org.opendaylight.affinity.affinity.SetPathRedirect;
import org.opendaylight.affinity.affinity.SetTap;
import org.opendaylight.affinity.l2agent.IfL2Agent;
import org.opendaylight.affinity.l2agent.L2Agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Affinity rules engine for flat L2 network. Currently implements tap
 * and path redirect (waypoint routing). Uses the AffinityPath as a
 * container for routes to use for programming flows.
 */
public class FlatL2AffinityImpl implements IfNewHostNotify {
    private static final Logger log = LoggerFactory.getLogger(FlatL2AffinityImpl.class);

    private ISwitchManager switchManager = null;
    private IAffinityManager am = null;
    private IfIptoHost hostTracker;
    private IForwardingRulesManager ruleManager;
    private IRouting routing;
    private IfL2Agent l2agent;

    private String containerName = GlobalConstants.DEFAULT.toString();
    private boolean isDefaultContainer = true;
    private static final int REPLACE_RETRY = 1;
    private static short AFFINITY_RULE_PRIORITY = 3;

    HashMap<String, List<Flow>> allfgroups;
    HashMap<String, HashMap<AffinityAttributeType, AffinityAttribute>> attribs;

    Set<Node> nodelist;
    
    public enum ReasonCode {
        SUCCESS("Success"), FAILURE("Failure"), INVALID_CONF(
                "Invalid Configuration"), EXIST("Entry Already Exist"), CONFLICT(
                        "Configuration Conflict with Existing Entry");

        private final String name;

        private ReasonCode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /* Only default container. */
    public String getContainerName() {
        return containerName;
    }

    void setAffinityManager(IAffinityManager mgr) {
        log.info("Setting affinity manager {}", mgr);
        this.am = mgr;
    }

    void unsetAffinityManager(IAffinityManager mgr) {
        if (this.am.equals(mgr)) {
            this.am = null;
        }
    }
    void setHostTracker(IfIptoHost h) {
        log.info("Setting hosttracker {}", h);
        this.hostTracker = h;
    }

    void unsetHostTracker(IfIptoHost h) {
        if (this.hostTracker.equals(h)) {
            this.hostTracker = null;
        }
    }
    
    public void setForwardingRulesManager(IForwardingRulesManager forwardingRulesManager) {
        log.debug("Setting ForwardingRulesManager");
        this.ruleManager = forwardingRulesManager;
    }
    
    public void unsetForwardingRulesManager(IForwardingRulesManager forwardingRulesManager) {
        if (this.ruleManager == forwardingRulesManager) {
            this.ruleManager = null;
        }
    }
    void setSwitchManager(ISwitchManager s)
    {
        this.switchManager = s;
    }
    
    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            this.switchManager = null;
        }
    }

    /**
     * Redirect port lookup requires access to L2agent or Routing. For
     * the time being, only one is assumed to be active.
     */
    void setL2Agent(IfL2Agent s)
    {
        log.info("Setting l2agent {}", s);
        this.l2agent = s;
    }

    void unsetL2Agent(IfL2Agent s) {
        if (this.l2agent == s) {
            this.l2agent = null;
        }
    }
    public void setRouting(IRouting routing) {
        this.routing = routing;
        // Init max throughput edge weights
        this.routing.initMaxThroughput(null);
    }

    public void unsetRouting(IRouting routing) {
        if (this.routing == routing) {
            this.routing = null;
        }
    }
    
    private void notifyHostUpdate(HostNodeConnector host, boolean added) {
        if (host == null) {
            return;
        }
        log.info("Host update received (new = {}).", added);
    }

    @Override
    public void notifyHTClient(HostNodeConnector host) {
        notifyHostUpdate(host, true);
    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        notifyHostUpdate(host, false);
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("flat L2 implementation INIT called!");
        containerName = GlobalConstants.DEFAULT.toString();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        log.debug("START called!");
    }

    /**
     * Function called after registering the service in OSGi service registry.
     */
    void started() {
        log.debug("FlatL2AffinityImpl started!");
    }

    /**
     * Clear all flows.
     */
    public boolean clearAllFlowGroups(List<String> groupnames) {
        for (String groupName: groupnames) {
            ruleManager.uninstallFlowEntryGroup(groupName);
        } 
        return true;
    }
    
    // Called via northbound API -- push all affinities. 
    public boolean enableAllAffinityLinks() {
        this.nodelist = switchManager.getNodes();
        log.info("Enable all affinity links.");
        if (this.nodelist == null) {
            log.debug("No nodes in network.");
            return true;
        }
        
        // Get all flow groups and attribs from the affinity manager. 
        this.allfgroups = am.getAllFlowGroups();
        this.attribs = am.getAllAttributes();
        
        // Calculate affinity path per src-dst pair using the merged set of affinity attributes.         
        HashMap<Flow, HashMap<AffinityAttributeType, AffinityAttribute>> pfa = mergeAffinityAttributesPerFlow();
        HashMap<Flow, AffinityPath> flowpaths = new HashMap<Flow, AffinityPath>();
        AffinityPath ap;

        for (Flow f: pfa.keySet()) {
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            ap = calcAffinityPath(srcIp, dstIp, pfa.get(f));
            log.debug("Adding to pfa flow={}, ap={}", f, ap);
            flowpaths.put(f, ap);
        }

        // tap forwarding rules. The match field is the 2-tuple NW_SRC, NW_DST. 
        for (Flow f: flowpaths.keySet()) {
            // Re-use the match field for each "flow" in the tapflows
            // set. tapflows only contains actions.
            HashMap<Node, List<Action>> tapflows = calcTapForwardingActions(flowpaths.get(f));
            log.debug("Adding tap forwarding actions.");
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            printTaprules(srcIp, dstIp, tapflows);
            for (Node n: tapflows.keySet()) {
                programTapFlows(n, f, tapflows.get(n));
            }
        }

        // redirect forwarding rules.  The match field is the 4-tuple NW_SRC, NW_DST, DL_SRC, DL_DST. 
        for (Flow g: flowpaths.keySet()) {
            log.debug("Adding redirect forwarding actions. flow {}, ap {}", g, flowpaths.get(g));
            HashMap<Node, List<Flow>> flowmap = calcRedirectForwardingActions(flowpaths.get(g));
            printRedirectRules(flowmap);
            for (Node n: flowmap.keySet()) {
                programRedirectFlows(n, flowmap.get(n));
            }
        }
        return true;
    }

    /** 
     * Calculate paths for this src-dst pair after applying: 
     *  -- default routing and exception/waypoint routing
     *  -- tap destinations.
     * Return a list of Paths.
     */
    
    public AffinityPath calcAffinityPath(InetAddress src, InetAddress dst, 
                                         HashMap<AffinityAttributeType, AffinityAttribute> attribs) {

        boolean maxTputPath = false;
        AffinityPath ap;

        log.info("calc paths: src = {}, dst = {}", src, dst);

        AffinityAttributeType aatype;

        // Apply drop
        aatype = AffinityAttributeType.SET_DENY;
        if (attribs.get(aatype) != null) {
            return null;
        }

        // Apply isolate (no-op now), and continue to add other affinity types to the forwarding actions list.
        aatype = AffinityAttributeType.SET_PATH_ISOLATE;
        if (attribs.get(aatype) != null) {
            log.info("Found a path isolate setting.");
        }

        // Apply MTP path, set the type of default path to compute.
        aatype = AffinityAttributeType.SET_MAX_TPUT_PATH;
        if (attribs.get(aatype) != null) {
            log.info("Found a max tput setting.");
            maxTputPath = true;
        }
        // Compute the default path, after applying waypoints and add it to the list. 
        // List<HostPairPath> subpaths = new ArrayList<HostPairPath>();
        HostNodeConnector srcNC = getHostNodeConnector(src);
        HostNodeConnector dstNC = getHostNodeConnector(dst);
        if (srcNC == null || dstNC == null) {
            log.info("src or destination does not have a HostNodeConnector. src={}, dst={}", src, dst);
            return null;
        }
        Node srcNode = srcNC.getnodeconnectorNode();
        Node dstNode = dstNC.getnodeconnectorNode();
        ap = new AffinityPath(srcNC, dstNC);

        log.debug("from node: {}", srcNC.toString());
        log.debug("dst node: {}", dstNC.toString());
        
        // Apply redirect 
        aatype = AffinityAttributeType.SET_PATH_REDIRECT;

        SetPathRedirect rdrct = (SetPathRedirect) attribs.get(aatype);

        // No redirects were added, so calculate the defaultPath by
        // looking up the appropriate type of route in the routing
        // service.
        List<HostPairPath> route = new ArrayList<HostPairPath>();
        if (rdrct == null) {
            Path defPath;
            if (maxTputPath == true) {
                defPath = this.routing.getMaxThroughputRoute(srcNode, dstNode);
            } else {
                defPath = this.routing.getRoute(srcNode, dstNode);
            }
            route.add(new HostPairPath(srcNC, dstNC, defPath));
        } else {
            log.info("Found a path redirect setting. Calculating subpaths 1, 2");
            List<InetAddress> wplist = rdrct.getWaypointList();
            if (wplist != null) {
                // Only one waypoint server in list. 
                InetAddress wp = wplist.get(0);
                log.info("waypoint information = {}", wplist.get(0));
                HostNodeConnector wpNC = getHostNodeConnector(wp);
                Node wpNode = wpNC.getnodeconnectorNode();
                Path subpath1;
                Path subpath2;
                subpath1 = this.routing.getRoute(srcNode, wpNode);
                subpath2 = this.routing.getRoute(wpNode, dstNode);
                log.debug("subpath1 is: {}", subpath1);
                log.debug("subpath2 is: {}", subpath2);

                route.add(new HostPairPath(srcNC, wpNC, subpath1));
                route.add(new HostPairPath(wpNC, dstNC, subpath2));
            }
        }
        if (route.size() > 0) {
            log.debug("Adding default path to ap src {}, dst {}, route {}", src, dst, route.get(0));
            ap.setDefaultPath(route);
        }
        
        // Apply tap, calculate paths to each tap destination and add to AffinityPath.
        aatype = AffinityAttributeType.SET_TAP;

        SetTap tap = (SetTap) attribs.get(aatype);

        if (tap != null) {
            log.info("Applying tap affinity.");
            List<InetAddress> taplist = tap.getTapList();
            if (taplist != null) {
                // Add a new rule with original destination + tap destinations. 
                for (InetAddress tapip: taplist) {
                    log.info("Adding tap path to destination = {}", tapip);

                    Path tapPath;
                    HostNodeConnector tapNC = getHostNodeConnector(tapip);
                    Node tapNode = tapNC.getnodeconnectorNode();
                    tapPath = this.routing.getRoute(srcNode, tapNode);
                    ap.setTapPath(tapNC, tapPath);
                }
            }
        }

        log.debug("calcAffinityPath: {}", ap.toString());
        return ap;
    }

    // Merge all affinity links into a single result. This result is a
    // collection that maps Flow (src-dst pair) -> combined set of all
    // attribs applied to that src-dst pair.
    public HashMap<Flow, HashMap<AffinityAttributeType, AffinityAttribute>> mergeAffinityAttributesPerFlow() {
        // per-flow attributes
        HashMap<Flow, HashMap<AffinityAttributeType, AffinityAttribute>> pfa = new HashMap<Flow, HashMap<AffinityAttributeType, AffinityAttribute>>();

        for (String linkname: this.allfgroups.keySet()) {
            log.debug("Adding new affinity link", linkname);
            List<Flow> newflows = this.allfgroups.get(linkname);
            HashMap<AffinityAttributeType, AffinityAttribute> newattribs = this.attribs.get(linkname);
            
            for (Flow f: newflows) {
                if (!pfa.containsKey(f)) {
                    // Create the initial record for this flow (src-dst pair). 
                    pfa.put(f, newattribs);
                } else {
                    // Merge attribs to the key that already exists. 
                    pfa.put(f, merge(pfa.get(f), newattribs));
                }
            }
        }
        return pfa;
    }

    // tbd: This attribute map should become a class. 
    // Overwriting merge of two atribute HashMaps. 
    public HashMap<AffinityAttributeType, AffinityAttribute> merge(HashMap<AffinityAttributeType, AffinityAttribute> a, 
                                                                   HashMap<AffinityAttributeType, AffinityAttribute> b) {
        HashMap<AffinityAttributeType, AffinityAttribute> result = new HashMap<AffinityAttributeType, AffinityAttribute>();

        for (AffinityAttributeType at: a.keySet()) {
            result.put(at, a.get(at));
        }
        for (AffinityAttributeType at: b.keySet()) {
            result.put(at, b.get(at));
        }
        return result;
    }


    /** Redirect forwarding actions per node in path. */
    public HashMap<Node, List<Flow>> calcRedirectForwardingActions(AffinityPath ap) {
        
        // Redirect rules
        HashMap<Node, List<Flow>> rules = new HashMap<Node, List<Flow>>();
        log.debug("calcRedir {}", ap);
        HostNodeConnector srcHnc = ap.getSrc();
        HostNodeConnector dstHnc = ap.getDst();

        Node srcNode = srcHnc.getnodeconnectorNode();
        Node dstNode = dstHnc.getnodeconnectorNode();

        // Process each segment of the default path, where each
        // segment is created by a redirect/waypoint. xxx extend
        // affinitypath to contain the reverse default path with
        // waypoints for return traffic.
        for (HostPairPath p: ap.getDefaultPath()) {
            rules = addRedirectRules(srcHnc, p.getSource(), p.getDestination(), dstHnc, p.getPath(), rules);
        }
        return rules;
    }

    /** 
     * Translate the path (edges + nodes) into a set of per-node
     * forwarding actions.  Coalesce them with the existing set of
     * rules for this affinity path.  Path p is the subpath for which
     * rules are being computed. Path contains a series of edges and
     * nodes.  srcHnc is always the starting host for this
     * subpath. dstHnc is the final destination, and wpHnc is the next
     * hop destination host, and also the destination of this
     * path. The match fields for rules depends on which subpath we're
     * on -- since we take L2 + L3 fields.
     * 
     * Returns a per-node list of rules (match + action) that include
     * both the "to waypoint" and "from waypoint" segments.
     *
     **/
    public HashMap<Node, List<Flow>> addRedirectRules(HostNodeConnector srcHnc, 
                                                      HostNodeConnector pathsrcHnc, 
                                                      HostNodeConnector pathdstHnc, 
                                                      HostNodeConnector dstHnc, 
                                                      Path p, 
                                                      HashMap<Node, List<Flow>> rulesDB) {
        HashMap<Node, List<Flow>> rules = rulesDB;
        NodeConnector forwardPort;
        
        InetAddress srcIP = srcHnc.getNetworkAddress();
        InetAddress dstIP = dstHnc.getNetworkAddress();
        HostNodeConnector wpHnc = pathdstHnc;

        byte[] srcMAC = pathsrcHnc.getDataLayerAddressBytes();
        byte[] dstMAC = dstHnc.getDataLayerAddressBytes();
        byte[] wpmac = wpHnc.getDataLayerAddressBytes();

        // If path to the hnc is null. Two cases to consider: 
        // (a) source and destination are attached to the same node. Use this node in addrules. 
        // (b) no path between source and destination. Do not call addrules. 
        
        /** 
         * Source node is also wp node. 
         */
        if (pathsrcHnc.getnodeconnectorNode().getNodeIDString().equals(pathdstHnc.getnodeconnectorNode().getNodeIDString())) {
            log.debug("Both source and waypoint are connected to same switch nodes. output port is {}",
                      wpHnc.getnodeConnector());
            return addflowrule(srcIP, dstIP, srcMAC, dstMAC, new Output(wpHnc.getnodeConnector()), wpmac, rules, wpHnc.getnodeconnectorNode());
        } 
        if (p == null) {
            log.debug("No edges in path, returning.");
            return rules;
        }

        /** Source and wp nodes are separated by a path, p. */
        Edge lastedge = null;
        for (Edge e: p.getEdges()) {
            NodeConnector op = e.getTailNodeConnector();
            Node node = e.getTailNodeConnector().getNode();
            List<Flow> flowlist = rules.get(node);
            // Calculate the output and setDlDst actions
            rules = addflowrule(srcIP, dstIP, srcMAC, dstMAC, new Output(op), wpmac, rules, node);
        }
        return addflowrule(srcIP, dstIP, srcMAC, dstMAC, new Output(wpHnc.getnodeConnector()), wpmac, rules, wpHnc.getnodeconnectorNode());
    }

    public HashMap<Node, List<Flow>> addflowrule(InetAddress srcIP, InetAddress dstIP, byte [] srcMAC, byte [] dstMAC, 
                                                 Output forwardPort, byte [] wpmac, HashMap<Node, List<Flow>> rules, Node node) {
        HashMap<Node, List<Flow>> ruleDB = rules;

        Match match = new Match();
        match.setField(new MatchField(MatchType.NW_SRC, srcIP, null));
        match.setField(new MatchField(MatchType.NW_DST, dstIP, null));
        match.setField(new MatchField(MatchType.DL_SRC, srcMAC, null));
        match.setField(new MatchField(MatchType.DL_DST, dstMAC, null));
        match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());  
        
        Output output = forwardPort;
        SetDlDst dldst = new SetDlDst(wpmac);
        List<Action> actions = new ArrayList<Action>();
        actions.add(output);
        actions.add(dldst);
        
        Flow flow = new Flow(match, actions);
        flow.setPriority(AFFINITY_RULE_PRIORITY);
        List<Flow> flowlist = rules.get(node);
        if (flowlist == null) {
            flowlist = new ArrayList<Flow>();
        }
        flowlist.add(flow);
        rules.put(node, flowlist);
        return rules;
    }

    
    /** Program 4-tuple flows where the match is NW_SRC, NW_DST, DL_SRC, DL_DST. This is used for redirect affinity. */ 
    public void programRedirectFlows(Node n, List<Flow> flows) {
        // Update flow with actions. 
        for (Flow f: flows) {
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            byte [] srcMAC = (byte []) f.getMatch().getField(MatchType.DL_SRC).getValue();
            byte [] dstMAC = (byte []) f.getMatch().getField(MatchType.DL_DST).getValue();
            String flowName = "[" + srcIp + "->" + dstIp + " " + srcMAC + "->" + dstMAC + "]";
            
            FlowEntry fEntry = new FlowEntry("redirect", flowName, f, n);
            log.info("Install flow entry {} on node {}", fEntry.toString(), n.toString());
            installFlowEntry(fEntry);
        }
    }
    

    public void printRedirectRules(HashMap<Node, List<Flow>> flows) {
        log.debug("redirect rules");
        for (Node n: flows.keySet()) {
            String astr = " ";
            for (Flow a: flows.get(n)) {
                astr = astr + "; " + a.toString();
            }
            log.debug("Node: {}, Output: {}", n, astr);
        }
    }


    // Tap affinity related methods. 
    // xxx Compute the set of output actions for each node in this AffinityPath. 
    public HashMap<Node, List<Action>> calcTapForwardingActions(AffinityPath ap) {

        HashMap<Node, List<Action>> actionMap;
        // Final set of rules to push into the nodes.
        actionMap = new HashMap<Node, List<Action>>();
        
        // Add output ports for each node in the tapPath list. Include
        // the host node connector of the destination server too.
        HashMap<HostNodeConnector, Path> tapPaths = ap.getTapPaths();
        for (HostNodeConnector tapDest: tapPaths.keySet()) {
            Path tp = tapPaths.get(tapDest);
            actionMap = addTaprules(ap.getSrc(), tapDest, tp, actionMap);
        }
        return actionMap;
    }


    /** Program 2-tuple flows where the match is NW_SRC, NW_DST. This is used for tap affinity. */ 
    public void programTapFlows(Node n, Flow f, List<Action> actions) {
        // Update flow with actions. 
        Flow flow = f.clone();
        if (actions.size() > 0) {
            log.info("Adding actions {} to flow {}", actions, flow);
            flow.setActions(actions);
            InetAddress srcIp = (InetAddress) flow.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) flow.getMatch().getField(MatchType.NW_DST).getValue();
            String flowName = "[" + srcIp + "->" + dstIp + "]";
            
            FlowEntry fEntry = new FlowEntry("tap", flowName, flow, n);
            log.info("Install flow entry {} on node {}", fEntry.toString(), n.toString());
            installFlowEntry(fEntry);
        }
    }

    public HashMap<Node, List<Action>> addLastHopTapRules(HostNodeConnector dstHnc, 
                                                          HashMap<Node, List<Action>> actionMap) {
        HashMap<Node, List<Action>> rules = actionMap;
        Node destNode = dstHnc.getnodeconnectorNode();
        NodeConnector forwardPort = dstHnc.getnodeConnector();
        rules.put(destNode, merge(rules.get(destNode), new Output(forwardPort)));
        return rules;
    }

    // Translate the path (edges + nodes) into a set of per-node forwarding actions. 
    // Coalesce them with the existing set of rules for this affinity path. 
    public HashMap<Node, List<Action>> addTaprules(HostNodeConnector srcHnc, HostNodeConnector dstHnc, Path p, 
                                                   HashMap<Node, List<Action>> actionMap) {
        HashMap<Node, List<Action>> rules = actionMap;

        if (srcHnc.getnodeconnectorNode().getNodeIDString().equals(dstHnc.getnodeconnectorNode().getNodeIDString())) {
            log.debug("Both source and destination are connected to same switch nodes. output port is {}",
                      dstHnc.getnodeConnector());
            return addLastHopTapRules(dstHnc, actionMap);
        } 
        if (p == null) {
            log.debug("No edges in path, returning.");
            return rules;
        }
        Edge lastedge = null;
        for (Edge e: p.getEdges()) {
            NodeConnector op = e.getTailNodeConnector();
            Node node = e.getTailNodeConnector().getNode();
            List<Action> actions = rules.get(node);
            rules.put(node, merge(actions, new Output(op)));
            lastedge = e;
        }
        // Add the last hop of the path using dstHnc
        return addLastHopTapRules(dstHnc, rules);
   }

    public void printTaprules(InetAddress src, InetAddress dst, HashMap<Node, List<Action>> taprules) {
        log.debug("source: {}, destination: {}", src, dst);
        for (Node n: taprules.keySet()) {
            String astr = " ";
            for (Action a: taprules.get(n)) {
                astr = astr + "; " + a.toString();
            }
            log.debug("Node: {}, Output: {}", n, astr);
        }
    }
    
    public List<Action> merge(List<Action> fwdactions, Action a) {
        if (fwdactions == null) {
            fwdactions = new ArrayList<Action>();
            fwdactions.add(a);
        } else if (!fwdactions.contains(a)) {
            fwdactions.add(a);
        }
        return fwdactions;
    }


    public boolean isHostActive(InetAddress ipaddr) {
        Set<HostNodeConnector> activeStaticHosts = hostTracker.getActiveStaticHosts();
        for (HostNodeConnector h : activeStaticHosts) {
            InetAddress networkAddress = h.getNetworkAddress();
            log.info("Checking match {} vs. {}", networkAddress, ipaddr);
            if (networkAddress == ipaddr) {
                log.debug("networkaddress found {} = {}", ipaddr, networkAddress);
                return true;
            }
        }
        return false;
    }

    public boolean isHostKnown(InetAddress ipaddr) {
        Set<HostNodeConnector> knownHosts = hostTracker.getAllHosts();
        for (HostNodeConnector h : knownHosts) {
            InetAddress networkAddress = h.getNetworkAddress();
            log.info("Checking match {} vs. {}", networkAddress, ipaddr);
            if (networkAddress.equals(ipaddr)) {
                log.debug("networkaddress found {} = {}", ipaddr, networkAddress);
                return true;
            }
        }
        return false;
    }

    public boolean isHostInactive(InetAddress ipaddr) {
        Set<HostNodeConnector> inactiveStaticHosts = hostTracker.getInactiveStaticHosts();
        for (HostNodeConnector h : inactiveStaticHosts) {
            InetAddress networkAddress = h.getNetworkAddress();
            log.info("Checking match {} vs. {}", networkAddress, ipaddr);
            if (networkAddress.equals(ipaddr)) {
                return true;
            }
        }
        return false;
    }

    public HostNodeConnector getInactiveHost(InetAddress ipaddr) {
        Set<HostNodeConnector> inactiveStaticHosts = hostTracker.getInactiveStaticHosts();
        for (HostNodeConnector h : inactiveStaticHosts) {
            InetAddress networkAddress = h.getNetworkAddress();
            log.info("Checking match {} vs. {}", networkAddress, ipaddr);
            if (networkAddress.equals(ipaddr)) {
                return h;
            }
        }
        return null;
    }

    public HostNodeConnector getHostNodeConnector(InetAddress ipaddr) {
        /** 
         * This host may be active, inactive/static or not present in the hosts DB.
         */
        HostNodeConnector hnConnector;      
        hnConnector = null;
        log.info("Lookup hostTracker for this host");
        
        // Check inactive hosts.
        if (isHostInactive(ipaddr)) {
            log.info("host is from inactive DB");
            hnConnector = getInactiveHost(ipaddr);
        } else if (isHostKnown(ipaddr)) {
            log.info("host is known to hostTracker, attempt a hostfind");
            IHostId id = HostIdFactory.create(ipaddr, null);
            hnConnector = this.hostTracker.hostFind(id);
        }
        return hnConnector;
    }

    /**
     * Install this flow entry object. 
     */
    public boolean installFlowEntry(FlowEntry fEntry) {
        if (!this.ruleManager.checkFlowEntryConflict(fEntry)) {
            if (this.ruleManager.installFlowEntry(fEntry).isSuccess()) {
                return true;
            } else {
                log.error("Error in installing flow entry {} to node : {}", fEntry.toString(), fEntry.getNode());
            }
        } else {
            log.error("Conflicting flow entry exists : {}", fEntry.toString());
        }
        return true;
    }

    public void disableAllAffinityLinks() {
        if (this.allfgroups != null) {
            for (String s: this.allfgroups.keySet()) {
                log.info("Clearing all flowrules for " + s);
                ruleManager.uninstallFlowEntryGroup(s);
            }
        }
    }

    public void enableAffinityLink(String affinityLinkName) {
        log.debug("No incremental add/delete of affinitylink yet.");
    }
    
    public void disableAffinityLink(String affinityLinkName) {
        log.debug("No incremental add/delete of affinitylink yet.");
    }



}
