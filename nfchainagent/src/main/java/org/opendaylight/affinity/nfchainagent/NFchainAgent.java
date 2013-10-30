/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.affinity.nfchainagent;

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

import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;

import org.opendaylight.affinity.l2agent.IfL2Agent;
import org.opendaylight.controller.switchmanager.ISwitchManager;

import java.io.Serializable;

public class NFchainAgent implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(NFchainAgent.class);
    private IFlowProgrammerService programmer = null;    
    private IfL2Agent l2agent = null;
    private IfIptoHost hostTracker = null;
    private ISwitchManager switchManager = null;
    
    private HashMap<String, List<NFchainconfig>> allconfigs;

    void init() {
        log.debug("INIT called!");
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

    void setHostTracker(IfIptoHost h) {
        log.info("Setting hosttracker {}", h);
        this.hostTracker = h;
    }

    void unsetHostTracker(IfIptoHost h) {
        if (this.hostTracker.equals(h)) {
            this.hostTracker = null;
        }
    }
    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.programmer = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.programmer == s) {
            this.programmer = null;
        }
    }
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

    void setSwitchManager(ISwitchManager s)
    {
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            this.switchManager = null;
        }
    }

    public Status addNfchain(String key, List<NFchainconfig> nfclist) {
	String name;

        if (allconfigs == null) {
            allconfigs = new HashMap<String, List<NFchainconfig>>();
        }
        /* xxx compute changelist and push flow changes. */
	if (allconfigs.containsKey(key)) {
	    return new Status(StatusCode.CONFLICT,
			      "NFchain with the specified name already configured.");
	} 
	List<NFchainconfig> oldcfg = allconfigs.get(key);
	if (oldcfg == null) {
	    if (allconfigs.put(key, nfclist) == null) {
                return new Status(StatusCode.SUCCESS); 
	    } 
	}
        return new Status(StatusCode.CONFLICT,
                          "Unknown error during addNFchain.");
    }

    /** 
     * add flow rules for set of flows in nfchainconfig. Do this for
     * each node connector in the network proactively.
     */
    public Status addrules(Node node, NFchainconfig nfcc) throws Exception {
        List<Flow> flowlist = nfcc.getFlowList();
        for (Flow f: flowlist) {
            HostNodeConnector wphost = (HostNodeConnector) hostTracker.hostFind(nfcc.getWaypointIP()); 
            List<Action> actions = new ArrayList<Action>();
            /* Look up the output port leading to the waypoint. */
            NodeConnector dst_connector = l2agent.lookup_output_port(node, wphost.getDataLayerAddressBytes());

            log.debug("Waypoint direction added: node {} and connector {}", node, dst_connector);
            if (dst_connector != null) {
                f.setActions(actions);
                f.addAction(new Output(dst_connector));
                log.debug("flow push add flow = {} to node = {} ", f, node);
                Status status = programmer.addFlow(node, f);
                if (!status.isSuccess()) {
                    log.debug("Error during addFlow: {} on {}. The failure is: {}",
                              f, node, status.getDescription());
                }
            }
        }
        return new Status(StatusCode.SUCCESS); 
    }



    /** 
     * remove flow rules for set of flows in nfchainconfig. Do this for
     * each node connector in the network proactively.
     */
    public Status removerules(Node node, NFchainconfig nfcc) throws Exception {
        List<Flow> flowlist = nfcc.getFlowList();
        for (Flow f: flowlist) {
            HostNodeConnector wphost = (HostNodeConnector) hostTracker.hostFind(nfcc.getWaypointIP()); 
            List<Action> actions = new ArrayList<Action>();
            /* Look up the output port leading to the waypoint. */
            NodeConnector dst_connector = l2agent.lookup_output_port(node, wphost.getDataLayerAddressBytes());

            log.debug("Waypoint settings removed: node {} and connector {}", node, dst_connector);
            if (dst_connector != null) {
                f.setActions(actions);
                f.addAction(new Output(dst_connector));
                log.debug("flow push remove flow = {} to node = {} ", f, node);
                Status status = programmer.removeFlow(node, f);
                if (!status.isSuccess()) {
                    log.debug("Error during removeFlow: {} on {}. The failure is: {}",
                              f, node, status.getDescription());
                }
            }
        }
        return new Status(StatusCode.SUCCESS); 
    }


    public Status removeNfchain(String key) {
        if (allconfigs != null) {
            allconfigs.remove(key);
        }
        return new Status(StatusCode.SUCCESS); 
    }

    /** 
     * Enable the nfchain by programming flow rules on its behalf. 
     */
    public Status enable(String cfgname) throws Exception {
        /* Get all node connectors. */
        Set<Node> nodes = switchManager.getNodes();
        NFchainconfig cfg = allconfigs.get(cfgname).get(0);

        Status success = new Status(StatusCode.SUCCESS);
        Status notfound = new Status(StatusCode.NOTFOUND);
        Status ret;

        if (nodes == null) {
            log.debug("No nodes in network.");
            return success;
        } 

        /* Send this flow rule to all nodes in the network. */
        for (Node node: nodes) {
            ret = addrules(node, cfg);
        }
        return new Status(StatusCode.SUCCESS);         
    }

    /** 
     * Remove openflow rules added earlier. Restore default routing via standard L2 learning methods. 
     */
    public Status disable(String cfgname) throws Exception {
        /* Get all node connectors. */
        Set<Node> nodes = switchManager.getNodes();
        NFchainconfig cfg = allconfigs.get(cfgname).get(0);

        Status success = new Status(StatusCode.SUCCESS);
        Status notfound = new Status(StatusCode.NOTFOUND);
        Status ret;

        if (nodes == null) {
            log.debug("No nodes in network.");
            return success;
        } 

        /* Send this flow rule to all nodes in the network. */
        for (Node node: nodes) {
            ret = removerules(node, cfg);
        }
        return new Status(StatusCode.SUCCESS);         
    }
}

