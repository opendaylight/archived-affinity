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
import java.util.HashMap;
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
 * Interface to class for maintaining affinity configuration.
 */
public interface IAffinityManager {

    public Status addAffinityGroup(AffinityGroup ag);
    public Status removeAffinityGroup(String name);
    
    public AffinityGroup getAffinityGroup(String name);
    public List<AffinityGroup> getAllAffinityGroups();

    public Status addAffinityLink(AffinityLink al);
    public Status removeAffinityLink(AffinityLink al);
    public Status removeAffinityLink(String linkName);

    public AffinityLink getAffinityLink(String name);    
    public List<AffinityLink> getAllAffinityLinks();

    /* Save all configs to their respective files. */
    public Status saveAffinityConfig();

    public ArrayList<AffinityIdentifier> getAllElementsByAffinityIdentifier(AffinityGroup ag);
    public List<Host> getAllElementsByHost(AffinityGroup ag);
    public List<Entry<Host, Host>> getAllFlowsByHost(AffinityLink al);
    public List<Entry<AffinityIdentifier, AffinityIdentifier>> getAllFlowsByAffinityIdentifier(AffinityLink al);

    /** 
     * Returns a map of groupname, derived from affinity link name, to
     * the list of flow objects corresponding to that link. This
     * should be a consistent snapshot of all configured objects.
     */
    public HashMap<String, List<Flow>>getAllFlowGroups();

    // For each flowgroup, there is a list of attributes. This api
    // call fetches this as a hashmap. Key of the outer hashmap is the
    // name of the affinity link (aka flowgroup). Key for the inner
    // hashmap is the affinity attribute type.
    public HashMap<String, HashMap<AffinityAttributeType,AffinityAttribute>>getAllAttributes();

    public List<Flow> getFlowlist(AffinityLink al);
}
