
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
import java.util.concurrent.ConcurrentMap;
import java.util.Set;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;

import org.opendaylight.controller.affinity.AffinityLink;

/**
 * Primary purpose of this interface is to provide methods for
 * applications to set affinity configuration.
 */
public interface IAffinityManager {

    public Status addAffinityLink(AffinityLink al);
    public Status removeAffinityLink(AffinityLink al);
    public Status removeAffinityLink(String linkName);

    public AffinityLink getAffinityLink(String name);    
    public List<AffinityLink> getAllAffinityLinks();

    public Status addAffinityGroup(AffinityGroup ag);
    public Status removeAffinityGroup(String name);
    
    public AffinityGroup getAffinityGroup(String name);
    public List<AffinityGroup> getAllAffinityGroups();

    /* Save all configs to their respective files. */
    public Status saveAffinityConfig();
}
