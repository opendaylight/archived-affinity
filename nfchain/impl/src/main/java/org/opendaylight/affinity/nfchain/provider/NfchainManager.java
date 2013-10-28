
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.nfchain.provider;

import java.util.Collections;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;

import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfchainService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfchainData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfdbBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.ChainId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.Gateway;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.Nfdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfdbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.AddInput;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;

/**
 * NfchainManager -- sends flow programming rules to flow programming service. 
 */
public class NfchainManager implements NfchainService, NfchainData {
    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(NfchainManager.class);
    
    private NotificationProviderService notificationProvider; 
    private final ExecutorService executor;
    private Future<RpcResult<Void>> currentTask;

    private IFlowProgrammerService programmer = null;
    private ISwitchManager switchManager = null;

    public NfchainManager() {
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public Nfdb getNfdb() {
        NfdbBuilder builder = new NfdbBuilder();
        return builder.build();
    }

    @Override
    public Future<RpcResult<Void>> add(AddInput input) {
        // TODO Auto-generated method stub
        log.info("add gateway - Received input chain = {}, gateway = {}.", input.getChain(), input.getGateway());
        if (currentTask != null) {
            return inProgressError();
        }
        currentTask = executor.submit(new addGatewayTask(input));
        return currentTask;
    }

    @Override
    public Future<RpcResult<Void>> list() {
        log.info("List command received");
        RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Collections.<RpcError> emptySet());
        return Futures.immediateFuture(result);
    }

    private Future<RpcResult<Void>> inProgressError() {
        RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Collections.<RpcError> emptySet());
        return Futures.immediateFuture(result);
    }

    private void cancel() {
        currentTask.cancel(true);
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
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

    void setSwitchManager(ISwitchManager s)
    {
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            this.switchManager = null;
        }
    }


    /* Include this once dependencies are correctly established in osgi. 
    /** 
     * Fetch all node connectors. Each switch port will receive a flow
     * rule. Do not stop on error. Pass in the waypointMAC address so
     * that the correct output port can be determined.
     */
    public Status pushFlowRule(InetAddress from, InetAddress to, byte [] waypointMAC) {
        /* Get all node connectors. */
        Set<Node> nodes = switchManager.getNodes();
        Status success = new Status(StatusCode.SUCCESS);
        Status notfound = new Status(StatusCode.NOTFOUND);

        if (nodes == null) {
            log.debug("No nodes in network.");
            return success;
        } 

        /* Send this flow rule to all nodes in the network. */
        for (Node node: nodes) {
            List<Action> actions = new ArrayList<Action>();
            Match match = new Match();
            match.setField(new MatchField(MatchType.NW_SRC, from, null));
            match.setField(new MatchField(MatchType.NW_DST, to, null));
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());  
        
            Flow f = new Flow(match, actions);
            f.setMatch(match);
            f.setPriority(REDIRECT_IPSWITCH_PRIORITY);

            /* Look up the output port leading to the waypoint. */
            NodeConnector dst_connector = l2agent.lookup_output_port(node, waypointMAC);

            log.debug("Waypoint direction: node {} and connector {}", node, dst_connector);
            if (dst_connector != null) {
                f.setActions(actions);
                f.addAction(new Output(dst_connector));
                log.debug("flow push flow = {} to node = {} ", f, node);
                /*                Status status = installRedirectionFlow(node, flow);*/
                Status status = programmer.addFlow(node, f);
                if (!status.isSuccess()) {
                    log.debug("Error during addFlow: {} on {}. The failure is: {}",
                              f, node, status.getDescription());
                }
            }
        }
        return success;
    }
    */

    private class addGatewayTask implements Callable<RpcResult<Void>> {

        final AddInput input;

        public addGatewayTask(AddInput input) {
            this.input = input;
        }

        @Override
        public RpcResult<Void> call() throws Exception {
            Thread.sleep(1000);
            log.info("add gateway returning");
            currentTask = null;
            return Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        }
    }
}
