/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.affinity.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.UnknownHostException;
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
import java.util.AbstractMap;
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
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;

import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.NetUtils;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.affinity.affinity.AffinityGroup;
import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.affinity.affinity.AffinityIdentifier;
import org.opendaylight.affinity.affinity.IAffinityManager;
import org.opendaylight.affinity.affinity.IAffinityManagerAware;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.affinity.l2agent.L2Agent;

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
    private IFlowProgrammerService fps = null;
    private ISwitchManager switchManager = null;
    private L2Agent l2agent = null;

    private ConcurrentMap<String, AffinityGroup> affinityGroupList;
    private ConcurrentMap<String, AffinityLink> affinityLinkList;
    private ConcurrentMap<Long, String> configSaveEvent;

    private IfIptoHost hostTracker;

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


    void setHostTracker(IfIptoHost h) {
        this.hostTracker = h;
    }

    void unsetHostTracker(IfIptoHost h) {
        if (this.hostTracker.equals(h)) {
            this.hostTracker = null;
        }
    }
    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.fps = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.fps == s) {
            this.fps = null;
        }
    }
    public void setL2Agent(L2Agent s)
    {
        this.l2agent = s;
    }

    public void unsetL2Agent(L2Agent s) {
        if (this.l2agent == s) {
            this.l2agent = null;
        }
    }

    /*
    public void setForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        this.ruleManager = forwardingRulesManager;
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardingRulesManager) {
        if (this.ruleManager == forwardingRulesManager) {
            this.ruleManager = null;
        }
    }
    */
    
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


    /** 
     * Fetch all node connectors. Each switch port will receive a flow rule. Do not stop on error.
     */
    public Status pushFlowRule(Flow flow) {
        /* Get all node connectors. */
        Set<Node> nodes = switchManager.getNodes();
        Status success = new Status(StatusCode.SUCCESS);
        Status notfound = new Status(StatusCode.NOTFOUND);

        if (nodes == null) {
            log.debug("No nodes in network.");
            return success;
        } 
        for (Node node: nodes) {
            Set<NodeConnector> ncs = switchManager.getNodeConnectors(node);
            if (ncs == null) {
                continue;
            }
            Status status = fps.addFlow(node, flow);
            if (!status.isSuccess()) {
                log.debug("Error during addFlow: {} on {}. The failure is: {}",
                          flow, node, status.getDescription());
            }
        }
        return success;
    }

    /** 
     * add flow rules for each node connector.
     */
    public Status addFlowRulesForRedirect(AffinityLink al) throws Exception {
        Match match = new Match();
        List<Action> actions = new ArrayList<Action>();

        InetAddress address1, address2;
        InetAddress mask;
        mask = InetAddress.getByName("255.255.255.255");

        Flow f = new Flow(match, actions);

        List<Entry<Host,Host>> hostPairList= getAllFlowsByHost(al);
        for (Entry<Host,Host> hostPair : hostPairList) {
            /* Create a match for each host pair in the affinity link. */

            Host host1 = hostPair.getKey();
            Host host2 = hostPair.getValue();
            address1 = host1.getNetworkAddress();
            address2 = host2.getNetworkAddress();
            
            match.setField(MatchType.NW_SRC, address1, mask);
            match.setField(MatchType.NW_DST, address2, mask);
            

            /* For each end point, discover the mac address of the
             * host. Then lookup the L2 table to find the port to send
             * this flow along. Program the flow. */

            byte [] mac = ((HostNodeConnector) host1).getDataLayerAddressBytes();
            NodeConnector dst_connector = l2agent.lookupMacAddress(mac);
            actions.add(new Output(dst_connector));
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
    public ArrayList<AffinityIdentifier> getAllElementsByAffinityIdentifier(AffinityGroup ag) {
	return ag.getAllElements();
    }
 
    @Override 
    public List<Host> getAllElementsByHost(AffinityGroup ag) {
	List<Host> hostList= new ArrayList<Host>();

	for (AffinityIdentifier h : ag.getAllElements()) {
	    /* TBD: Do not assume this to be an InetAddress. */ 
	    h.print();
	    if (hostTracker != null) {
		Host host1 = hostTracker.hostFind((InetAddress) h.get());
		hostList.add(host1);
	    }
	}
	return hostList;
    }

    @Override
    public List<Entry<Host, Host>> getAllFlowsByHost(AffinityLink al) {
	List<Entry<Host,Host>> hostPairList= new ArrayList<Entry<Host, Host>>();

	AffinityGroup fromGroup = al.getFromGroup();
	AffinityGroup toGroup = al.getToGroup();
	
	for (AffinityIdentifier h1 : fromGroup.getAllElements()) {
	    for (AffinityIdentifier h2 : toGroup.getAllElements()) {
		if (hostTracker != null) {
		    Host host1 = hostTracker.hostFind((InetAddress) h1.get());
		    Host host2 = hostTracker.hostFind((InetAddress) h2.get());
		    Entry<Host, Host> hp1=new AbstractMap.SimpleEntry<Host, Host>(host1, host2);
		    hostPairList.add(hp1);
		}
	    }
	}
	return hostPairList;
    }

    @Override 
    public List<Entry<AffinityIdentifier, AffinityIdentifier>> getAllFlowsByAffinityIdentifier(AffinityLink al) {
	List<Entry<AffinityIdentifier, AffinityIdentifier>> hostPairList= new ArrayList<Entry<AffinityIdentifier, AffinityIdentifier>>();

	AffinityGroup fromGroup = al.getFromGroup();
	AffinityGroup toGroup = al.getToGroup();
	
	for (AffinityIdentifier h1 : fromGroup.getAllElements()) {
	    for (AffinityIdentifier h2 : toGroup.getAllElements()) {
		Entry<AffinityIdentifier, AffinityIdentifier> hp1=new AbstractMap.SimpleEntry<AffinityIdentifier, AffinityIdentifier>(h1, h2);
		hostPairList.add(hp1);
	    }
	}
	return hostPairList;
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
