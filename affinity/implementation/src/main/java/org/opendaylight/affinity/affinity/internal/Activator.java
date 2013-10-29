/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.affinity.IAffinityManagerAware;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.affinity.l2agent.IfL2Agent;
import org.opendaylight.affinity.nfchainagent.NFchainAgent;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;

/**
 * AffinityManager Bundle Activator
 *
 *
 */
public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    public void init() {

    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
    public void destroy() {

    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    public Object[] getImplementations() {
        Object[] res = { AffinityManagerImpl.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(AffinityManagerImpl.class)) {
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();
            Set<String> propSet = new HashSet<String>();
            propSet.add("affinitymanager.configSaveEvent");
            props.put("cachenames", propSet);
            // export the service
            c.setInterface(new String[] {
                    IAffinityManager.class.getName(),
                    ICacheUpdateAware.class.getName(),
                    IfNewHostNotify.class.getName(),
                    IConfigurationContainerAware.class.getName() }, props);

            // Now lets add a service dependency to make sure the
            // provider of service exists
            /* L2agent dependency causes the service to fail activation. tbd. */
            c.add(createContainerServiceDependency(containerName)
                  .setService(IfL2Agent.class)
                  .setCallbacks("setL2Agent", "unsetL2Agent")
                  .setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    NFchainAgent.class).setCallbacks(
                    "setNFchainAgent", "unsetNFchainAgent")
                    .setRequired(true));
            c.add(createContainerServiceDependency(containerName)
                  .setService(IFlowProgrammerService.class)
                  .setCallbacks("setFlowProgrammerService", "unsetFlowProgrammerService")
                  .setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(IfIptoHost.class)
                  .setCallbacks("setHostTracker", "unsetHostTracker").setRequired(true));
            c.add(createContainerServiceDependency(containerName)
                    .setService(ISwitchManager.class)
                    .setCallbacks("setSwitchManager", "unsetSwitchManager")
                    .setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IAffinityManagerAware.class).setCallbacks(
                    "setAffinityManagerAware", "unsetAffinityManagerAware")
                    .setRequired(false));
        }
    }
}
