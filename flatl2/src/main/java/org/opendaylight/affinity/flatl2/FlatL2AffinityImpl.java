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
 * Affinity rules engine for flat L2 network.
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
        

        // New implementation using AffintyPath. 
        HashMap<Flow, AffinityPath> flowpaths = calcAffinityPathsForAllFlows(mergeAffinityAttributesPerFlow());
        for (Flow f: flowpaths.keySet()) {
            HashMap<Node, List<Action>> flowActions = calcForwardingActions(flowpaths.get(f));
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            printActionMap(srcIp, dstIp, flowActions);
            for (Node n: flowActions.keySet()) {
                programFlows(n, f, flowActions.get(n));
            }
        }

        /** Old implementation that does per-node programming. Demo-only.
        for (Node node: this.nodelist) {
            programFlowGroupsOnNode(this.allfgroups, this.attribs, node);
        }
        */
        return true;
    }

    public void programFlows(Node n, Flow f, List<Action> actions) {
        
        // Update flow with actions. 
        if (actions.size() > 0) {
            log.info("Adding actions {} to flow {}", actions, f);
            f.setActions(actions);
            // Make a flowEntry object. groupName is the policy name, 
            // from the affinity link name. Same for all flows in this bundle. 
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            String flowName = "[" + srcIp + "->" + dstIp + "]";
            
            FlowEntry fEntry = new FlowEntry("affinity", flowName, f, n);
            log.info("Install flow entry {} on node {}", fEntry.toString(), n.toString());
            installFlowEntry(fEntry);
        }
    }

    public HashMap<Node, List<Action>> mergeActions(HashMap<Node, List<Action>> a, HashMap<Node, List<Action>> b) {
        HashMap<Node, List<Action>> result = new HashMap<Node, List<Action>>();

        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        // Initialize with all elements of a.
        result = a;
        // Add elements from b, merging when necessary. 
        ArrayList<Action> allActions = new ArrayList<Action>();
        for (Node n: b.keySet()) {
            // This node is listed in both maps, merge the actions. 
            if (a.get(n) != null) {
                allActions.addAll(a.get(n));
                allActions.addAll(b.get(n));
                result.put(n, allActions);
            }
        }
        return result;
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

    // A "Flow" here is used to represent the source-destination pair. 
    // Function returns an affinity path object per src-dest pair. 
    public HashMap<Flow, AffinityPath> calcAffinityPathsForAllFlows(HashMap<Flow, HashMap<AffinityAttributeType, AffinityAttribute>>perFlowAttribs) {
        HashMap<Flow, AffinityPath> perFlowPaths = new HashMap<Flow, AffinityPath>();
        
        for (Flow f: perFlowAttribs.keySet()) {
            InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
            InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
            String flowName = "[" + srcIp + "->" + dstIp + "]";
            AffinityPath ap = calcAffinityPath(srcIp, dstIp, perFlowAttribs.get(f));
            perFlowPaths.put(f, ap);
        }
        return perFlowPaths;
    }

    // xxx Compute the set of output actions for each node in this AffinityPath. 
    public HashMap<Node, List<Action>> calcForwardingActions(AffinityPath ap) {

        HashMap<Node, List<Action>> actionMap;
        // Final set of rules to push into the nodes.
        actionMap = new HashMap<Node, List<Action>>();
        
        Node srcNode = ap.getSrc().getnodeconnectorNode();
        Node destNode = ap.getDst().getnodeconnectorNode();
        // Process each segment of the default path, where each
        // segment is created by a redirect/waypoint.
        for (HostPairPath p: ap.getDefaultPath()) {
            // If path to the hnc is null. Two cases to consider: 
            // (a) source and destination are attached to the same node. Use this node in addrules. 
            // (b) no path between source and destination. Do not call addrules. 
            actionMap = addrules(p.getSource(), p.getDestination(), p.getPath(), actionMap);
        }

        // Add output ports for each node in the tapPath list. Include
        // the host node connector of the destination server too.
        HashMap<HostNodeConnector, Path> tapPaths = ap.getTapPaths();
        for (HostNodeConnector tapDest: tapPaths.keySet()) {
            Path tp = tapPaths.get(tapDest);
            actionMap = addrules(ap.getSrc(), tapDest, tp, actionMap);
        }
        return actionMap;
    }

    // Translate the path (edges + nodes) into a set of per-node forwarding actions. 
    // Coalesce them with the existing set of rules for this affinity path. 
    public HashMap<Node, List<Action>> addrules(HostNodeConnector srcHnc, HostNodeConnector dstHnc, Path p, 
                                                HashMap<Node, List<Action>> actionMap) {
        HashMap<Node, List<Action>> rules = actionMap;
        NodeConnector forwardPort;

        if (srcHnc.getnodeconnectorNode().getNodeIDString().equals(dstHnc.getnodeconnectorNode().getNodeIDString())) {
            forwardPort = dstHnc.getnodeConnector();
            log.debug("Both source and destination are connected to same switch nodes. output port is {}",
                      forwardPort);
            Node destNode = dstHnc.getnodeconnectorNode();
            List<Action> actions = rules.get(destNode);
            rules.put(destNode, merge(actions, new Output(forwardPort)));
            return rules;
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
        // Add the edge from the lastnode to the destination host. 
        NodeConnector dstNC = dstHnc.getnodeConnector();
        Node lastnode = lastedge.getHeadNodeConnector().getNode();
        // lastnode is also the same as hnc.getnodeConnectorNode();
        List<Action> actions = rules.get(lastnode);
        rules.put(lastnode, merge(actions, new Output(dstNC)));
        return rules;
    }
    
    public void printActionMap(InetAddress src, InetAddress dst, HashMap<Node, List<Action>> aMap) {
        log.debug("source: {}, destination: {}", src, dst);
        for (Node n: aMap.keySet()) {
            String astr = " ";
            for (Action a: aMap.get(n)) {
                astr = astr + "; " + a.toString();
            }
            log.debug("Node: {}, Output: {}", n, astr);
        }
    }

    /** 
     * Add flow groups to forwarding rules manager as FlowEntry
     * objects. Each flow group corresponds to a policy group in the
     * forwarding rules manager. actions represent the forwarding
     * actions to be applied to each flow group. Forwarding actions
     * may be REDIRECT, DROP, or TAP. 
     */
    public boolean programFlowGroupsOnNode(HashMap<String, List<Flow>>flowgroups, 
                                           HashMap<String, HashMap<AffinityAttributeType, 
                                           AffinityAttribute>>attribs, 
                                           Node node) {
        for (String groupName: flowgroups.keySet()) {
            List<Flow> flowlist = flowgroups.get(groupName);
            log.info("flatl2: {} (#flows={})", groupName, flowgroups.get(groupName).size());
            log.info("flatl2: {} (#attribs={})", groupName, attribs.get(groupName).size());
            for (Flow f: flowlist) {
                // Set the flow name based on end points for this flow. 
                String flowName = null;
                InetAddress srcIp = (InetAddress) f.getMatch().getField(MatchType.NW_SRC).getValue();
                InetAddress dstIp = (InetAddress) f.getMatch().getField(MatchType.NW_DST).getValue();
                flowName = "[" + groupName + ":" + srcIp + ":" + dstIp + "]";
                List<Action> actions = calcForwardingActions(node, srcIp, dstIp, attribs.get(groupName));
                // Update flow with actions. 
                if (actions.size() > 0) {
                    log.info("Adding actions {} to flow {}", actions, f);
                    f.setActions(actions);
                    // Make a flowEntry object. groupName is the policy name, 
                    // from the affinity link name. Same for all flows in this bundle. 
                    FlowEntry fEntry = new FlowEntry(groupName, flowName, f, node);
                    log.info("Install flow entry {} on node {}", fEntry.toString(), node.toString());
                    installFlowEntry(fEntry);
                }
            }
        }
        return true; // error checking
    }
    /** 
     * Calculate forwarding actions per node. Inputs are the node
     * (switch) and the list of configured actions.
     */

    public List<Action> calcForwardingActions(Node node, InetAddress src, InetAddress dst, 
                                              HashMap<AffinityAttributeType, AffinityAttribute> attribs) {
        List<Action> fwdactions = new ArrayList<Action>();

        AffinityAttributeType aatype;
        log.info("calcforwardingactions: node = {}", node);

        // Apply drop
        aatype = AffinityAttributeType.SET_DENY;

        if (attribs.get(aatype) != null) {
            Action dropaction = new Drop();
            fwdactions.add(dropaction);
            return fwdactions;
        }

        // Apply isolate (no-op now), and continue to add other affinity types to the forwarding actions list.
        aatype = AffinityAttributeType.SET_PATH_ISOLATE;

        if (attribs.get(aatype) != null) {
            log.info("Found a path isolate setting.");
        }

        // Apply MTP path.
        aatype = AffinityAttributeType.SET_MAX_TPUT_PATH;

        if (attribs.get(aatype) != null) {
            log.info("Found a max tput setting.");
            Output output = getOutputPort(node, dst, true);
            if (output != null) {
                fwdactions.add(output);
            }
        }

        // Apply redirect 
        aatype = AffinityAttributeType.SET_PATH_REDIRECT;

        SetPathRedirect rdrct = (SetPathRedirect) attribs.get(aatype);
        
        if (rdrct != null) {
            log.info("Found a path redirect setting.");
            List<InetAddress> wplist = rdrct.getWaypointList();
            if (wplist != null) {
                // Only one waypoint server in list. 
                InetAddress wp = wplist.get(0);
                log.info("waypoint information = {}", wplist.get(0));
                // Lookup output port on this node for this destination. 

                // Using L2agent
                Output output = getOutputPortL2Agent(node, wp);
                // Using routing service. 
                // Output output = getOutputPort(node, wp, false);
                if (output != null) {
                    fwdactions.add(output);
                }
                // Using simpleforwarding.
                // Output output = getOutputPort(node, wp);
                // Controller controller = new Controller();
                // fwdactions.add(controller);
            }
        }

        // Apply tap 
        aatype = AffinityAttributeType.SET_TAP;

        SetTap tap = (SetTap) attribs.get(aatype);

        if (tap != null) {
            log.info("Applying tap affinity.");
            List<InetAddress> taplist = tap.getTapList();
            if (taplist != null) {
                // Add a new rule with original destination + tap destinations. 
                for (InetAddress tapip: taplist) {
                    log.info("tap information = {}", tapip);
                    Output output1 = getOutputPortL2Agent(node, tapip);
                    // Not using L2 agent, using routing service. 
                    // Output output1 = getOutputPort(node, tapip, false);
                    if (output1 != null) {
                        fwdactions = merge(fwdactions, output1);
                    }
                }
                Output output2 = getOutputPortL2Agent(node, dst);
                // Not using L2 agent, using routing service. 
                // Output output2 = getOutputPort(node, dst, false);
                if (output2 != null) {
                    fwdactions = merge(fwdactions, output2);
                }

                // Using simpleforwarding.
                // Output output = getOutputPort(node, wp);
                // Controller controller = new Controller();
                // fwdactions.add(controller);
            }
        }

        return fwdactions;
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
        HostNodeConnector srcNC, dstNC;
        srcNC = getHostNodeConnector(src);
        dstNC = getHostNodeConnector(dst);
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
    
    public List<Action> merge(List<Action> fwdactions, Action a) {
        if (fwdactions == null) {
            fwdactions = new ArrayList<Action>();
            fwdactions.add(a);
        } else if (!fwdactions.contains(a)) {
            fwdactions.add(a);
        }
        return fwdactions;
    }

    /** 
     * Using L2agent, get the output port toward this IP from this
     * node (switch).
     */
    public Output getOutputPortL2Agent(Node node, InetAddress ip) {
        Output op = null;

        if (l2agent != null) {
            /* Look up the output port leading to the waypoint. */
            HostNodeConnector host = (HostNodeConnector) hostTracker.hostFind(ip);
            if (host != null) {
                log.info("output port on node {} toward host {}", node, host);
                NodeConnector dst_connector = l2agent.lookup_output_port(node, host.getDataLayerAddressBytes());
                if (dst_connector != null) {
                    op = new Output(dst_connector);
                }
            }
        } else {
            log.info("l2agent is not set!!!");
        }

        // host node connector may be null, if this address is static
        // and not known to the l2agent which relies on learning.
        if (op == null && isHostInactive(ip)) {
            // Use routing.
            op = getOutputPort(node, ip, false);
        }
        return op;
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

    public Output getOutputPort(Node node, InetAddress ipaddr, boolean mtp) {
        HostNodeConnector hnConnector;
        hnConnector = getHostNodeConnector(ipaddr);
        if (hnConnector != null) {
            Node destNode = hnConnector.getnodeconnectorNode();
            
            log.debug("from node: {}", node.toString());
            log.debug("dest node: {}", destNode.toString());
            
            // Get path between both the nodes                                                                                                           
            NodeConnector forwardPort = null;
            if (node.getNodeIDString().equals(destNode.getNodeIDString())) {
                forwardPort = hnConnector.getnodeConnector();
                log.info("Both source and destination are connected to same switch nodes. output port is {}",
                         forwardPort);
            } else {
                Path route;
                if (mtp == true) {
                    log.info("Lookup max throughput route {} -> {}", node, destNode);
                    route = this.routing.getMaxThroughputRoute(node, destNode);
                } else {
                    route = this.routing.getRoute(node, destNode);
                }

                log.info("Path between source and destination switch nodes : {}",
                         route.toString());
                forwardPort = route.getEdges().get(0).getTailNodeConnector();                
            }
            log.info("output port {} on node {} toward host {}", forwardPort, node, hnConnector);
            return(new Output(forwardPort));
        } 
        return null;
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

    public void enableAffinityLink(String affinityLinkName) {
        List<Flow> flowgroup = this.allfgroups.get(affinityLinkName);
        HashMap<AffinityAttributeType, AffinityAttribute> attribset = this.attribs.get(affinityLinkName);
        
        // Make a hashmap with one key, value pair representing this
        // affinity link. Do this for flows and for attribs.
        HashMap<String, List<Flow>> linkflows = new HashMap<String, List<Flow>>();
        HashMap<String, HashMap<AffinityAttributeType, AffinityAttribute>> linkattribs = new HashMap<String, HashMap<AffinityAttributeType, AffinityAttribute>>();

        if (flowgroup != null && attribset != null) {
            linkflows.put(affinityLinkName, flowgroup);
            linkattribs.put(affinityLinkName, attribset);
            
            if (this.nodelist != null) {
                for (Node node: this.nodelist) {
                    programFlowGroupsOnNode(this.allfgroups, this.attribs, node);
                }
            }
        }
    }
    
    public void disableAffinityLink(String affinityLinkName) {
        ruleManager.uninstallFlowEntryGroup(affinityLinkName);
    }

    public void disableAllAffinityLinks() {
        if (this.allfgroups != null) {
            for (String s: this.allfgroups.keySet()) {
                log.info("Clearing all flowrules for " + s);
                ruleManager.uninstallFlowEntryGroup(s);
            }
        }
    }
}
