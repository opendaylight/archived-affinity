/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.Set;
import java.util.Collection;
import java.util.Map.Entry;

import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.flowprogrammer.Flow;

import org.opendaylight.affinity.affinity.AffinityLink;

/**
 * Program flows in a flat layer 2 domain. 
 */
public interface IFlatL2AffinityManager {

    public Status addNfchain(AffinityLink al);
    public Status enableRedirect(AffinityLink al) throws Exception;
    public Status disableRedirect(AffinityLink al) throws Exception;

}
