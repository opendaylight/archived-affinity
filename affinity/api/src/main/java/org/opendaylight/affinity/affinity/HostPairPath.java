
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity;

import java.io.Serializable;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Path;

/**
 * Class that represent a pair of {Host, Path}, the intent of it is to
 * be used to represent a sub-path of a waypoint path. See
 * AffinityPath for details.
 */

public class HostPairPath implements Serializable {
    private static final long serialVersionUID = 1L;
    private HostNodeConnector src;
    private HostNodeConnector dst;
    private Path path;

    public HostPairPath(HostNodeConnector src, HostNodeConnector dst, Path p) {
        setPath(p);
        setSource(src);
        setDestination(dst);
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path p) {
        this.path = p;
    }

    public HostNodeConnector getSource() {
        return src;
    }

    public void setSource(HostNodeConnector host) {
        this.src = host;
    }

    public HostNodeConnector getDestination() {
        return dst;
    }

    public void setDestination(HostNodeConnector host) {
        this.dst = host;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostPairPath other = (HostPairPath) obj;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        if (dst == null) {
            if (other.dst != null)
                return false;
        } else if (!dst.equals(other.dst))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    @Override
    public String toString() {
        String string;
        if (path == null) {
            string = "HostPairPath [src=" + src + "dst=" + dst + ", path=" + "(null)" + "]";
        } else {
            string = "HostPairPath [src=" + src + "dst=" + dst + ", path=" + path.toString() + "]";
        } 
        return string;
    }
}
