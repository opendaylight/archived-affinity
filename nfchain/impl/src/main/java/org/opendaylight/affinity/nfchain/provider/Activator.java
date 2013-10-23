/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 */

package org.opendaylight.affinity.nfchain.provider;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;

import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;

import org.opendaylight.yang.gen.v1.urn.opendaylight.affinity.nfchain.rev131020.NfchainService;

public class Activator extends AbstractBindingAwareProvider {
    protected static final Logger log = LoggerFactory
        .getLogger(Activator.class);
    
    private ProviderContext providerContext;
    private NfchainManager nfcmgr;

    public Activator() {
        nfcmgr = new NfchainManager();
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        log.info("Provider session initialized");

        this.providerContext = session;
        nfcmgr.setNotificationProvider(session.getSALService(NotificationProviderService.class));
        providerContext.addRpcImplementation(NfchainService.class, nfcmgr);
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }
}
