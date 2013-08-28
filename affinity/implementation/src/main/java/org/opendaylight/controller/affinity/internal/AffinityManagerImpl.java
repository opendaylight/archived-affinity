/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.affinity.AffinityGroup;
import org.opendaylight.controller.affinity.AffinityLink;
import org.opendaylight.controller.affinity.IAffinityManager;
import org.opendaylight.controller.affinity.IAffinityManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class caches latest network nodes statistics as notified by reader
 * services and provides API to retrieve them.
 */
public class AffinityManagerImpl implements IAffinityManager, IConfigurationContainerAware, IObjectReader, ICacheUpdateAware<Long, String> {
    private static final Logger log = LoggerFactory.getLogger(AffinityManagerImpl.class);

    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    private static final String SAVE = "Save";
    private String affinityLinkFileName = null;
    private String affinityGroupFileName = null;

    private ConcurrentMap<String, AffinityGroup> affinityGroupList;
    private ConcurrentMap<String, AffinityLink> affinityLinkList;
    private ConcurrentMap<Long, String> configSaveEvent;

    private final Set<IAffinityManagerAware> affinityManagerAware = Collections
            .synchronizedSet(new HashSet<IAffinityManagerAware>());

    private byte[] MAC;
    private static boolean hostRefresh = true;
    private int hostRetryCount = 5;
    private IClusterContainerServices clusterContainerService = null;
    private String containerName = null;
    private boolean isDefaultContainer = true;
    private static final int REPLACE_RETRY = 1;

    public enum ReasonCode {
        SUCCESS("Success"), FAILURE("Failure"), INVALID_CONF(
                "Invalid Configuration"), EXIST("Entry Already Exist"), CONFLICT(
                        "Configuration Conflict with Existing Entry");

        private final String name;

        private ReasonCode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /* Only default container. */
    public String getContainerName() {
        return containerName;
    }

    public void startUp() {
        // Initialize configuration file names
        affinityLinkFileName = ROOT + "affinityConfig_link" + this.getContainerName()
            + ".conf";
        affinityGroupFileName = ROOT + "affinityConfig_group" + this.getContainerName()
            + ".conf";

        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        /*
         * Read startup and build database if we have not already gotten the
         * configurations synced from another node
         */
        if (affinityGroupList.isEmpty() || affinityLinkList.isEmpty()) {
            loadAffinityConfiguration();
        }
    }

    public void shutDown() {
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            this.nonClusterObjectCreate();
            log.warn("un-initialized clusterContainerService, can't create cache");
            return;
        }
        try {
            clusterContainerService.createCache(
                    "affinity.affinityGroupList",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache(
                    "affinity.affinityLinkList",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            clusterContainerService.createCache(
                    "affinity.configSaveEvent",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            log.error("\nCache already exits - destroy and recreate if needed");
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }
        affinityGroupList = (ConcurrentMap<String, AffinityGroup>) clusterContainerService
            .getCache("affinity.affinityGroupList");
        if (affinityGroupList == null) {
            log.error("\nFailed to get cache for affinityGroupList");
        }
        affinityLinkList = (ConcurrentMap<String, AffinityLink>) clusterContainerService
            .getCache("affinity.affinityLinkList");
        if (affinityLinkList == null) {
            log.error("\nFailed to get cache for affinityLinkList");
        }

        configSaveEvent = (ConcurrentMap<Long, String>) clusterContainerService
            .getCache("affinity.configSaveEvent");
        if (configSaveEvent == null) {
            log.error("\nFailed to get cache for configSaveEvent");
        }
    }

    private void nonClusterObjectCreate() {
        affinityLinkList = new ConcurrentHashMap<String, AffinityLink>();
        affinityGroupList = new ConcurrentHashMap<String, AffinityGroup>();
        configSaveEvent = new ConcurrentHashMap<Long, String>();
    }


    public Status addAffinityLink(AffinityLink al) {
	boolean putNewLink = false;

	if (affinityLinkList.containsKey(al.getName())) {
	    return new Status(StatusCode.CONFLICT,
			      "AffinityLink with the specified name already configured.");
	}

	
	AffinityLink alCurr = affinityLinkList.get(al.getName());
	if (alCurr == null) {
	    if (affinityLinkList.putIfAbsent(al.getName(), al) == null) {
		putNewLink = true;
	    } 
	} else {
	    putNewLink = affinityLinkList.replace(al.getName(), alCurr, al);
	}

	if (!putNewLink) {
	    String msg = "Cluster conflict: Conflict while adding the subnet " + al.getName();
	    return new Status(StatusCode.CONFLICT, msg);
	}
	
        return new Status(StatusCode.SUCCESS);
    }

    public Status removeAffinityLink(String name) {
	affinityLinkList.remove(name);
	return new Status(StatusCode.SUCCESS);
    }

    public Status removeAffinityLink(AffinityLink al) {
	AffinityLink alCurr = affinityLinkList.get(al.getName());
	if (alCurr != null) {
	    affinityLinkList.remove(alCurr);
	    return new Status(StatusCode.SUCCESS);
	} else {
	    String msg = "Affinity Link with specified name does not exist." + al.getName();
	    return new Status(StatusCode.INTERNALERROR, msg);
	}
    }
    
    @Override
    public AffinityLink getAffinityLink(String linkName) {
        return affinityLinkList.get(linkName);
    }

    @Override
    public List<AffinityLink> getAllAffinityLinks() {
	return new ArrayList<AffinityLink>(affinityLinkList.values());
    }

    @Override
    public Status addAffinityGroup(AffinityGroup ag) {
	boolean putNewGroup = false;
	String name = ag.getName();
	if (affinityGroupList.containsKey(name)) {
	    return new Status(StatusCode.CONFLICT,
			      "AffinityGroup with the specified name already configured.");
	} 
	AffinityGroup agCurr = affinityGroupList.get(name);
	if (agCurr == null) {
	    if (affinityGroupList.putIfAbsent(name, ag) == null) {
		putNewGroup = true;
	    } 
	} else {
	    putNewGroup = affinityGroupList.replace(name, agCurr, ag);
	}

	if (!putNewGroup) {
	    String msg = "Cluster conflict: Conflict while adding the subnet " + name;
	    return new Status(StatusCode.CONFLICT, msg);
	}
	
        return new Status(StatusCode.SUCCESS);
    }

    /* Check for errors. */
    @Override
    public Status removeAffinityGroup(String name) {
	affinityGroupList.remove(name);
	return new Status(StatusCode.SUCCESS);
    }

    @Override
    public AffinityGroup getAffinityGroup(String groupName) {
        return affinityGroupList.get(groupName);
    }

    @Override
    public List<AffinityGroup> getAllAffinityGroups() {
        return new ArrayList<AffinityGroup>(affinityGroupList.values());
    }

    /* Find where this is used. */
    @Override
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package
        // where the class is defined
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    private void loadAffinityConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, AffinityGroup> groupList = (ConcurrentMap<String, AffinityGroup>) objReader.read(this, affinityGroupFileName);
        ConcurrentMap<String, AffinityLink> linkList = (ConcurrentMap<String, AffinityLink>) objReader.read(this, affinityLinkFileName);
	
	/* group list */
        if (groupList != null) {
	    for (AffinityGroup ag : groupList.values()) {
		addAffinityGroup(ag);
	    }
	}

	/* link list */
	if (linkList != null) {
	    for (AffinityLink al : linkList.values()) {
		addAffinityLink(al);
	    }
        }
    }
    @Override
    public Status saveConfiguration() {
        return saveAffinityConfig();
    }

    @Override
    public Status saveAffinityConfig() {
        // Publish the save config event to the cluster nodes
        configSaveEvent.put(new Date().getTime(), SAVE);
        return saveAffinityConfigInternal();
    }

    public Status saveAffinityConfigInternal() {
        Status retS = null, retP = null;
        ObjectWriter objWriter = new ObjectWriter();

        retS = objWriter.write(new ConcurrentHashMap<String, AffinityLink>(
                affinityLinkList), affinityLinkFileName);

        retP = objWriter.write(new ConcurrentHashMap<String, AffinityGroup>(
                affinityGroupList), affinityGroupFileName);

        if (retS.isSuccess() && retP.isSuccess()) {
	    return new Status(StatusCode.SUCCESS, "Configuration saved.");
	} else {
	    return new Status(StatusCode.INTERNALERROR, "Save failed");
	}
    }

    @Override
    public void entryCreated(Long key, String cacheName, boolean local) {
    }

    @Override
    public void entryUpdated(Long key, String new_value, String cacheName,
            boolean originLocal) {
        saveAffinityConfigInternal();
    }

    @Override
    public void entryDeleted(Long key, String cacheName, boolean originLocal) {
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        log.debug("INIT called!");
        containerName = GlobalConstants.DEFAULT.toString();
        startUp();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        log.debug("START called!");
    }

    /**
     * Function called after registering the service in OSGi service registry.
     */
    void started(){
        // Retrieve current statistics so we don't have to wait for next refresh
        IAffinityManager affinityManager = (IAffinityManager) ServiceHelper.getInstance(
                IAffinityManager.class, this.getContainerName(), this);
        if (affinityManager != null) {
            log.debug("STARTED method called!");
        }
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        log.debug("STOP called!");
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set for affinity mgr");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed for affinity mgr!");
            this.clusterContainerService = null;
        }
    }
}
