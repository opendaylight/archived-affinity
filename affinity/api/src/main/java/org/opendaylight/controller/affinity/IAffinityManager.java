
/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;

import org.opendaylight.controller.affinity.AffinityConfig;

/**
 * Primary purpose of this interface is to provide methods for
 * applications to set affinity configuration.
 */
public interface IAffinityManager {

    /**
     * Remove an affinity configuration
     *
     * @param  configObject refer to {@link Open Declaration org.opendaylight.controller.affinitymanager.AffinityConfig}
     * @return "Success" or failure reason
     */
    public Status removeAffinityConfigObject(AffinityConfig configObject);

    /**
     * Remove an affinity configuration given the name
     *
     * @param   name      affinity name
     * @return  "Success" or failure reason
     */
    public Status removeAffinityConfig(String name);

    /**
     * Save the current affinity configurations
     *
     * @return the status code
     */
    public Status saveAffinityConfig();

    /**
     * Update Switch specific configuration such as Affinity name and type. Add if absent.
     *
     * @param cfgConfig refer to {@link Open Declaration org.opendaylight.controller.affinity.AffinityConfig}
     */
    public Status updateAffinityConfig(AffinityConfig cfgObject);

    public List<AffinityConfig> getAffinityConfigList();
    public AffinityConfig getAffinityConfig(String affinity);
}
