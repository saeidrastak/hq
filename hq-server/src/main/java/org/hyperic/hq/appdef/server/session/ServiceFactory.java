package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.auth.data.AuthzSubjectRepository;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.plugin.mgmt.data.PluginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceFactory {

    static final String SERVICE_RT = "ServiceRt";

    static final String MODIFIED_TIME = "ModifiedTime";

    static final String END_USER_RT = "EndUserRt";

    static final String CREATION_TIME = "CreationTime";

    static final String AUTO_INVENTORY_IDENTIFIER = "AutoInventoryIdentifier";

    static final String AUTO_DISCOVERY_ZOMBIE = "autoDiscoveryZombie";

    private ServerFactory serverFactory;

    private PlatformFactory platformFactory;
    
    private PluginRepository pluginRepository;
    
    private AuthzSubjectRepository authzSubjectRepository;

    @Autowired
    public ServiceFactory(ServerFactory serverFactory, PlatformFactory platformFactory, 
                          PluginRepository pluginRepository, AuthzSubjectRepository authzSubjectRepository) {
        this.serverFactory = serverFactory;
        this.platformFactory = platformFactory;
        this.pluginRepository = pluginRepository;
        this.authzSubjectRepository = authzSubjectRepository;
    }

    public Service createService(Resource resource) {
        Service service = new Service();
        service.setAutodiscoveryZombie((Boolean) resource.getProperty(AUTO_DISCOVERY_ZOMBIE));
        service
            .setAutoinventoryIdentifier((String) resource.getProperty(AUTO_INVENTORY_IDENTIFIER));
        service.setCreationTime((Long) resource.getProperty(CREATION_TIME));
        service.setDescription(resource.getDescription());
        service.setEndUserRt((Boolean) resource.getProperty(END_USER_RT));
        service.setId(resource.getId());
        service.setLocation(resource.getLocation());
        service.setModifiedBy(resource.getModifiedBy());
        service.setModifiedTime((Long) resource.getProperty(MODIFIED_TIME));
        service.setName(resource.getName());
        service.setOwnerName(authzSubjectRepository.findByOwnedResource(resource).getName());
        service.setResource(resource);
        Resource parent = resource.getResourceTo(RelationshipTypes.SERVICE);
        if(parent.getProperty(AppdefResourceType.APPDEF_TYPE_ID).equals(AppdefEntityConstants.APPDEF_TYPE_SERVER)) {
            service.setParent(serverFactory.createServer(parent));
        }else {
            service.setParent(platformFactory.createPlatform(parent));
        }
        service.setServiceRt((Boolean) resource.getProperty(SERVICE_RT));
        service.setServiceType(createServiceType(resource.getType()));
        service.setSortName((String) resource.getProperty(AppdefResource.SORT_NAME));
        return service;
    }

    public ServiceType createServiceType(ResourceType resourceType) {
        ServiceType serviceType = new ServiceType();
        // TODO
        // serviceType.setCreationTime(creationTime);
        // serviceType.setModifiedTime(modifiedTime);
        serviceType.setDescription(resourceType.getDescription());
        serviceType.setId(resourceType.getId());
        serviceType.setName(resourceType.getName());
        serviceType.setPlugin(pluginRepository.findByResourceType(resourceType).getName());
        // TODO for types, we just fake out sort name for now. Can't do
        // setProperty on ResourceType
        serviceType.setSortName(resourceType.getName().toUpperCase());
        return serviceType;
    }
}
