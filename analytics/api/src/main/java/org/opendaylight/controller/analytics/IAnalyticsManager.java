/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.analytics;

import org.opendaylight.controller.sal.core.Host;

public interface IAnalyticsManager {

    long getByteCountBetweenHosts(Host src, Host dst);

    double getBitRateBetweenHosts(Host src, Host dst);
}
