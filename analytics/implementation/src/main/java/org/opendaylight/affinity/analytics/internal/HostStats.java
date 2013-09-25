/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.internal;

import org.opendaylight.controller.sal.reader.FlowOnNode;

public class HostStats {

    private long byteCount;
    private double duration;

    public HostStats() {
        this.byteCount = 0;
        this.duration = 0;
    }

    public long getByteCount() {
        return this.byteCount;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    public double getDuration() {
        return this.duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public void setStatsFromFlow(FlowOnNode flow) {
        // Prevent stats from getting overwritten by zero-byte flows.
        // TODO: Figure out why this happens
        if (flow.getByteCount() > this.byteCount) {
            this.byteCount = flow.getByteCount();
        }
        this.duration = flow.getDurationSeconds() + .000000001 * flow.getDurationNanoseconds();
    }

    public double getBitRate() {
        return (this.byteCount * 8)/(this.duration);
    }
}
