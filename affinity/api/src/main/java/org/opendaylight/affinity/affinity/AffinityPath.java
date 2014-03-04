/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.affinity.affinity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

/**
 * Represents the attribute associated with an affinity link. 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AffinityPath implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AffinityPath.class);
    @XmlElement
    private transient boolean isValid = true;
    
    // Default path, leading to the destination. Each element is a sub-path of the total default path. 
    HostNodeConnector src;
    HostNodeConnector dst;
    List<HostPairPath> defaultPath;
    HashMap<HostNodeConnector, Path> tapPaths;

    /* Dummy constructor for JAXB */
    public AffinityPath(HostNodeConnector srcHnc, HostNodeConnector dstHnc) {
        this.src = srcHnc;
        this.dst = dstHnc;
        this.defaultPath = new ArrayList<HostPairPath>();
        this.tapPaths = new HashMap<HostNodeConnector, Path>();
    }

    public HostNodeConnector getSrc() {
        return this.src;
    }
    public HostNodeConnector getDst() {
        return this.dst;
    }
    public List<HostPairPath> getDefaultPath() {
        return defaultPath;
    }
    public void setDefaultPath(List<HostPairPath> subpaths) {
        defaultPath = subpaths;
    }
    
    public void setTapPath(HostNodeConnector dst, Path path) {
        tapPaths.put(dst, path);
    }

    public HashMap<HostNodeConnector, Path> getTapPaths() {
        return tapPaths;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AffinityPath other = (AffinityPath) obj;
        if (this.src != other.src || this.dst != other.dst) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String string = "affinity-path: \n";
        
        string = string + "src: " + src.toString() + "\n";
        string = string + "dst: " + dst.toString() + "\n";
        string = string + "defPath: " + "\n";
        for (HostPairPath hp: defaultPath) {
            string = string + hp.toString() + "\n";
        }
        for (HostNodeConnector k: tapPaths.keySet()) {
            string = string + "tapdst: " + k.toString() + "\n" + "path: " + tapPaths.get(k).toString() + "\n";
        }
        return string;
    }

}
