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
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.affinity.affinity.InetAddressMask;

import org.opendaylight.controller.hosttracker.HostIdFactory;
import org.opendaylight.controller.hosttracker.IHostId;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.affinity.AffinityAttributeType;
import org.opendaylight.affinity.affinity.AffinityAttribute;
import org.opendaylight.affinity.affinity.SetDeny;
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

    private static short REDIRECT_IPSWITCH_PRIORITY = 3;

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

        for (Node node: this.nodelist) {
            programFlowGroupsOnNode(this.allfgroups, this.attribs, node);
        }
        return true;
    }
    
    /** 
     * Add flow groups to forwarding rules manager as FlowEntry
     * objects. Each flow group corresponds to a policy group in the
     * forwarding rules manager. actions represent the forwarding
     * actions to be applied to each flow group. Forwarding actions
     * may be REDIRECT, DROP, or TAP. 
     */
    public boolean programFlowGroupsOnNode(HashMap<String, List<Flow>>flowgroups, 
                                          HashMap<String, HashMap<AffinityAttributeType, AffinityAttribute>>attribs, 
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
                log.info("Adding actions {} to flow {}", actions, f);
                f.setActions(actions);
                // Make a flowEntry object. groupName is the policy name, from the affinity link name. Same for all flows in this bundle. 
                FlowEntry fEntry = new FlowEntry(groupName, flowName, f, node);
                log.info("Install flow entry {} on node {}", fEntry.toString(), node.toString());
                installFlowEntry(fEntry);
            }
        }
        return true; // error checking
    }
    /** 
     * Calculate forwarding actions per node. Inputs are the node
     * (switch) and the list of configured actions.
     */

    public List<Action> calcForwardingActions(Node node, InetAddress src, InetAddress dst, HashMap<AffinityAttributeType, AffinityAttribute> attribs) {
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
                fwdactions.add(output);
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
                // Only one waypoint server in list. 
                for (InetAddress tapip: taplist) {
                    log.info("tap information = {}", tapip);
                    // Lookup output port on this node for this destination. 
                    
                    // Using L2agent
                    Output output1 = getOutputPortL2Agent(node, tapip);
                    Output output2 = getOutputPortL2Agent(node, dst);
                    
                    fwdactions.add(output1);
                    fwdactions.add(output2);
                    
                    // Using simpleforwarding.
                    // Output output = getOutputPort(node, wp);
                    // Controller controller = new Controller();
                    // fwdactions.add(controller);
                }
            }
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
            log.info("output port on node {} toward host {}", node, host);
            NodeConnector dst_connector = l2agent.lookup_output_port(node, host.getDataLayerAddressBytes());
            if (dst_connector != null) {
                op = new Output(dst_connector);
            }
        } else {
            log.info("l2agent is not set!!!");
        }
        return op;
    }

    public Output getOutputPort(Node node, InetAddress wp) {
        IHostId id = HostIdFactory.create(wp, null);
        HostNodeConnector hnConnector = this.hostTracker.hostFind(id);
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
            Path route = this.routing.getRoute(node, destNode);
            log.info("Path between source and destination switch nodes : {}",
                        route.toString());
            forwardPort = route.getEdges().get(0).getTailNodeConnector();
        }
        return(new Output(forwardPort));
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
