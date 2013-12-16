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


package org.opendaylight.affinity.l2agent;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Node;

public interface IfL2Agent {

    public NodeConnector lookup_output_port(Node node, byte [] dstMAC);

}
