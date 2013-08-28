/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity;

/**
 * The interface which describes the methods forwarding rules manager will call
 * for notifying the listeners of policy installation updates.
 */
public interface IAffinityManagerAware {

    /**
     * Inform the listeners that specified affinity was updated.
     *
     * @param aff
     *            the affinity config that was added or removed
     * @param add true if add; false otherwise
     */
    public void affinityLinkNotify(AffinityLink aff, boolean add);
    public void affinityGroupNotify(AffinityGroup aff, boolean add);

    /**
     * Inform listeners that the network node has notified us about a failure in
     * executing the controller generated asynchronous request identified by the
     * passed unique id.
     *
     * @param requestId
     *            the unique id associated with the request which failed to be
     *            executed on the network node
     * @param error
     *            the string describing the error reported by the network node
     */
    public void requestFailed(long requestId, String error);

}
