
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.nfchain.provider;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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

import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.chain.Gateway;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.Nfdb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfdbBuilder;


import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.AddchainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.EnablechainInput;

import org.opendaylight.affinity.nfchainagent.NFchainconfig;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;


import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.addchain.input.Chain;
import java.net.InetAddress;
import org.opendaylight.controller.sal.compability.ToSalConversionsUtils;

import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.chain.Flow;
   
/**
 * NfchainManager -- sends flow programming rules to flow programming service. 
 */
public class NfchainManager implements NfchainService, NfchainData {
    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(NfchainManager.class);
    
    private NotificationProviderService notificationProvider; 
    private Future<RpcResult<Void>> currentTask;

    public NfchainManager() {
    }

    @Override
    public Nfdb getNfdb() {
        NfdbBuilder builder = new NfdbBuilder();
        return builder.build();
    }

    /**
     * Convert API Flow objects to flow programmer flow objects. 
     */
    List<org.opendaylight.controller.sal.flowprogrammer.Flow> fromAPIFlowlist(List<Flow> fl) {
        List<org.opendaylight.controller.sal.flowprogrammer.Flow> flowlist = new ArrayList<org.opendaylight.controller.sal.flowprogrammer.Flow>();
        
        for (Flow f: fl) {
            org.opendaylight.controller.sal.flowprogrammer.Flow fp_flow = ToSalConversionsUtils.toFlow(f);
            flowlist.add(fp_flow);
        }
        return flowlist;
    }
    
    /**
     * addchain synchronous. 
     */
    @Override
    public Future<RpcResult<Void>> addchain(AddchainInput input) {
        // TODO Auto-generated method stub
        Chain chain = input.getChain();

        List<org.opendaylight.controller.sal.flowprogrammer.Flow> flowlist = fromAPIFlowlist(chain.getFlow());
        List<Gateway> gatewaylist = chain.getGateway();
        String name = chain.getName();

        if (gatewaylist.size() > 1) {
            log.info("addNfchain function chain has {} elements", gatewaylist.size());
        } else {

            log.info("add gateway - Received input chain = {}, gateway = {}.", input.getChain(), chain.getGateway());
            Gateway gw = gatewaylist.get(0);
            InetAddress ip = ToSalConversionsUtils.inetAddressFrom(gw.getLocation());
            NFchainconfig nfcc = new NFchainconfig(name, flowlist, ip);
            /*        nfchainagent.addNfchain(); */
        }
        RpcResult<Void> result = Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        return Futures.immediateFuture(result);
    }

    @Override
    public Future<RpcResult<java.lang.Void>> enablechain(EnablechainInput input) {
        log.info("enable chain");

        RpcResult<Void> result = Rpcs.<Void> getRpcResult(true, null, Collections.<RpcError> emptySet());
        return Futures.immediateFuture(result);
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
    }
}
