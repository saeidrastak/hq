package org.hyperic.hq.control.server.session;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertEquals;

@DirtiesContext
public class ControlManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private ControlManager controlManager;

    private Service service;

    @Before
    public void setUp() throws Exception {
        String agentToken = "agentToken123";
        createAgent("127.0.0.1", 2144, "authToken", agentToken, "4.5");
        flushSession();
        Platform platform = createPlatform(agentToken, "PluginTestPlatform", "TestPlatform1",
            "TestPlatform1", 2);
        ServerType serverType = serverManager.findServerTypeByName("PluginTestServer 1.0");
        Server server = createServer(platform, serverType, "Server1");
        ServiceType webModuleStats = serviceManager
            .findServiceTypeByName("PluginTestServer 1.0 Web Module Stats");
        service = createService(server.getId(), webModuleStats, "Service1", "", "");
    }

    @Test
    public void testGetActions() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("stop");
        expected.add("start");
        expected.add("reload");
        assertEquals(
            expected,
            new HashSet<String>(controlManager.getActions(authzSubjectManager.getOverlordPojo(),
                service.getEntityId())));
    }

    @Test
    public void testGetActionsCompatibleGroup() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("stop");
        expected.add("start");
        expected.add("reload");
        Set<Service> services = new HashSet<Service>();
        services.add(service);
        ResourceGroup group = createServiceResourceGroup(services, "Group1");
        assertEquals(
            expected,
            new HashSet<String>(controlManager.getActions(authzSubjectManager.getOverlordPojo(),
                AppdefUtil.newAppdefEntityId(group))));

    }
    
    @Test
    public void testGetActionsType() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("stop");
        expected.add("start");
        expected.add("reload");
        assertEquals(
            expected,
            new HashSet<String>(controlManager.getActions(authzSubjectManager.getOverlordPojo(),
               new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVICE,service.getId()))));
    }
}