/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.analytics.test.ui;

import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

import com.xwiki.analytics.test.po.AnalyticsViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class AnalyticsIT
{
    @BeforeAll
    void setup(XWikiWebDriver driver, TestConfiguration testConfiguration, TestUtils testUtils) throws Exception
    {
        createAdminUser(testUtils);
    }

    /**
     * Checks if a user has view permissions in the home page of the application.
     */
    @Test
    @Order(4)
    void checkEditPermisionForUser(XWikiWebDriver driver, TestUtils testUtils) throws InterruptedException
    {
        testUtils.createUser("test", "test", null);
        // Logout from the admin account
        testUtils.setSession(null);
        testUtils.login("test", "test");
        AnalyticsViewPage analyticsViewPage = AnalyticsViewPage.gotoPage();
        assertEquals("You are not allowed to view this page or perform this action.",
            driver.findElement(By.cssSelector("p.xwikimessage")).getText());
        // Login as the admin to run the next test
        testUtils.setSession(null);
        testUtils.loginAsAdmin();
    }


    /**
     * Creates the Admin user
     */
    private void createAdminUser(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        // TODO: remove this line after upgrading the XWiki parent to a version >= 15.10, because it was added as part
        //  of the createAdminUser method
        testUtils.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        testUtils.createAdminUser();
        testUtils.loginAsAdmin();
    }
}
