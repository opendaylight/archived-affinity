
/*
 * Copyright (c) 2013 Plexxi Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.flatl2;

import java.util.Hashtable;
import java.util.Dictionary;
import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.l2agent.IfL2Agent;

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
        Object[] res = { FlatL2AffinityImpl.class };
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
        if (imp.equals(FlatL2AffinityImpl.class)) {
            // export the services
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "FlatL2AffinityImpl");
            c.setInterface(new String[] { IfNewHostNotify.class.getName(),                    
                                          FlatL2AffinityImpl.class.getName() }, props);

            // register dependent modules
            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks("setSwitchManager",
                    "unsetSwitchManager").setRequired(true));

            // If using a layer 3 forwarding service such as simpleforwarding. 
            c.add(createContainerServiceDependency(containerName).setService(
                    IRouting.class).setCallbacks("setRouting", "unsetRouting")
                    .setRequired(false));

            // If using a layer 2 forwarding service such as l2agent. 
            c.add(createContainerServiceDependency(containerName)
                  .setService(IfL2Agent.class)
                  .setCallbacks("setL2Agent", "unsetL2Agent")
                  .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IfIptoHost.class).setCallbacks("setHostTracker",
                    "unsetHostTracker").setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IForwardingRulesManager.class).setCallbacks(
                    "setForwardingRulesManager", "unsetForwardingRulesManager")
                  .setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(IAffinityManager.class).setCallbacks(                                                                                                                  "setAffinityManager", "unsetAffinityManager")
                  .setRequired(true));
        }
    }
}
