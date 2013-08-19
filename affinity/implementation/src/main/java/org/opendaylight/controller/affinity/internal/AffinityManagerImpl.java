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
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;

import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.affinity.AffinityConfig;
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
    private String affinityConfigFileName = null;
    private ConcurrentMap<String, AffinityConfig> affinityConfigList;

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
        affinityConfigFileName = ROOT + "affinityConfig_" + this.getContainerName()
            + ".conf";

        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();

        /*
         * Read startup and build database if we have not already gotten the
         * configurations synced from another node
         */
        if (affinityConfigList.isEmpty()) {
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
                    "affinity.affinityConfigList",
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
            log.info("un-initialized clusterContainerService, can't create cache");
            return;
        }
        affinityConfigList = (ConcurrentMap<String, AffinityConfig>) clusterContainerService
            .getCache("affinity.affinityConfigList");
        if (affinityConfigList == null) {
            log.error("\nFailed to get cache for affinityConfigList");
        }
        configSaveEvent = (ConcurrentMap<Long, String>) clusterContainerService
            .getCache("affinity.configSaveEvent");
        if (configSaveEvent == null) {
            log.error("\nFailed to get cache for configSaveEvent");
        }
    }

    private void nonClusterObjectCreate() {
        affinityConfigList = new ConcurrentHashMap<String, AffinityConfig>();
        configSaveEvent = new ConcurrentHashMap<Long, String>();
    }

    @Override
    public List<AffinityConfig> getAffinityConfigList() {
        return new ArrayList<AffinityConfig>(affinityConfigList.values());
    }

    @Override
    public AffinityConfig getAffinityConfig(String affinity) {
        return affinityConfigList.get(affinity);
    }


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
        ConcurrentMap<String, AffinityConfig> confList = (ConcurrentMap<String, AffinityConfig>) objReader
                .read(this, affinityConfigFileName);

        if (confList == null) {
            return;
        }

        for (AffinityConfig conf : confList.values()) {
            updateAffinityConfig(conf);
        }
    }

    /* Add if absent. */
    @Override
    public Status updateAffinityConfig(AffinityConfig cfgObject) {
        // update default container only
        if (!isDefaultContainer) {
            return new Status(StatusCode.INTERNALERROR, "Not default container");
        }

        AffinityConfig ac = affinityConfigList.get(cfgObject.getName());
        if (ac == null) {
            if (affinityConfigList.putIfAbsent(cfgObject.getName(), cfgObject) != null) {
                return new Status(StatusCode.CONFLICT, "affinity configuration already exists" + cfgObject.getName());
            }
        } else {
            if (!affinityConfigList.replace(cfgObject.getName(), ac, cfgObject)) {
                return new Status(StatusCode.INTERNALERROR, "Failed to add affinity configuration.");
            }
        }
        return new Status(StatusCode.SUCCESS, "Updated affinity configuration " + cfgObject.getName());
    }


    /* Remove affinity config */
    @Override
    public Status removeAffinityConfig(String cfgName) {
        // update default container only
        if (!isDefaultContainer) {
            return new Status(StatusCode.INTERNALERROR, "Not default container");
        }
        AffinityConfig ac = affinityConfigList.get(cfgName);
        if (ac != null) {
            return removeAffinityConfigObject(ac);
        }
        return new Status(StatusCode.INTERNALERROR, "Missing affinity config" + cfgName);
    }
    /* Remove affinity config */
    @Override
    public Status removeAffinityConfigObject(AffinityConfig cfgObject) {
        // update default container only
        if (!isDefaultContainer) {
            return new Status(StatusCode.INTERNALERROR, "Not default container");
        }

        AffinityConfig ac = affinityConfigList.get(cfgObject.getName());
        if (ac != null) {
            if (affinityConfigList.remove(cfgObject.getName(), ac)) {
                return new Status(StatusCode.SUCCESS, "Configuration removed: " + cfgObject.getName());
            } else {
                String msg = "Remove failed " + cfgObject.getName();
                return new Status(StatusCode.INTERNALERROR, msg);
            }
        }
        return new Status(StatusCode.INTERNALERROR, "Remove failed: " + cfgObject.getName());
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

        retS = objWriter.write(new ConcurrentHashMap<String, AffinityConfig>(
                affinityConfigList), affinityConfigFileName);

        if (retS.equals(retP)) {
            if (retS.isSuccess()) {
                return retS;
            } else {
                return new Status(StatusCode.INTERNALERROR, "Save failed");
            }
        } else {
            return new Status(StatusCode.INTERNALERROR, "Partial save failure");
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
        log.debug("Cluster Service set for Statistics Mgr");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed for Statistics Mgr!");
            this.clusterContainerService = null;
        }
    }
}
