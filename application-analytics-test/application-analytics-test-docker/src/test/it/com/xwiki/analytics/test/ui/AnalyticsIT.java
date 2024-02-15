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

import com.xwiki.analytics.test.po.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

import com.xwiki.analytics.test.ui.config.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class AnalyticsIT {
    /**
     * Creates the Admin user
     */
    public void setupUsers(TestUtils testUtils) {
        testUtils.loginAsSuperAdmin();
        //TODO remove this line when upgrading the parent to >=15.10 because it was added to createAdminUser function
        testUtils.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        testUtils.createAdminUser();
        testUtils.loginAsAdmin();

    }

    /**
     * Start the matomo and sql containers, rename the matomo.js to a random int to force the browser to load the
     * tracking script without explicit settings and generate a new auth token for matomo to be used in the tests.
     */
    public void setupContainers(XWikiWebDriver driver, TestConfiguration testConfiguration) throws Exception {
        GenericContainer<?> sqlContainer = startDb(testConfiguration);
        GenericContainer<?> matomoContainer = startMatomo(testConfiguration, sqlContainer);
        // Modify the name of the matomo.js to make sure that the browser doesn't block it.
        matomoContainer.execInContainer("sh", "-c",
                "grep -rl 'matomo.js' /var/www/html/ | xargs -d '\\n' -I {} sed -i 's/matomo.js/36011373.js/g' \"{}\"");
        matomoContainer.execInContainer("sh", "-c", "mv /var/www/html/matomo.js /var/www/html/36011373.js");
        Config.MATOMO_AUTH_TOKEN =
                MatomoTestUtils.createToken("http://" + Config.ADDRESS + ":" + matomoContainer.getMappedPort(80), driver);
    }

    /**
     * Import the platform.html.head UIExtension point to make the tracking code work in the test environment.
     */
    private void setupUIExtension(TestUtils testUtils) throws Exception {
        testUtils.setWikiPreference("meta",
                "#foreach($uix in $services.uix.getExtensions(\"org.xwiki.platform.html.head\","
                        + " {'sortByParameter' : 'order'}))\n" + "  $services.rendering.render($uix.execute(), 'xhtml/1.0')\n"
                        + "#end");
    }

    @BeforeAll
    void setup(XWikiWebDriver driver, TestConfiguration testConfiguration, TestUtils testUtils) throws Exception {
        setupContainers(driver, testConfiguration);
        setupUIExtension(testUtils);
        setupUsers(testUtils);
        AnalyticsViewPage.gotoPage();
    }

    /**
     * Check that the Save button displays the correct messages when the configurations provided by the
     * users are incorrect.
     */
    @Test
    @Order(1)
    void checkWrongConfigs(XWikiWebDriver driver) throws InterruptedException {
        AnalyticsAdministrationSectionPage analyticsConfigViewPage = new AnalyticsAdministrationSectionPage();
        analyticsConfigViewPage.gotoPage();
        analyticsConfigViewPage.setTrackingCode("").setAuthTokenId(Config.MATOMO_AUTH_TOKEN).setIdSiteId("1")
                .setRequestAddressId(Config.ADDRESS + ":" + Config.MATOMO_BRIDGE_PORT).saveConfigs();

        analyticsConfigViewPage.waitForNotificationInProgressMessage("Saving...");
        analyticsConfigViewPage.waitForNotificationSuccessMessage("Saved");
        analyticsConfigViewPage.waitForNotificationInProgressMessage("Checking connection to Matomo.");
        analyticsConfigViewPage.waitForNotificationErrorMessage("Failed to connect to Matomo. Please check your configuration " + "values.");
        AnalyticsViewPage.gotoPage();
    }

    /**
     * Check that the Save button displays the correct messages when the configurations provided by the
     * users are correct.
     */
    @Test
    @Order(2)
    void checkValidConfigs(XWikiWebDriver driver) throws InterruptedException {

        AnalyticsAdministrationSectionPage analyticsConfigViewPage = new AnalyticsAdministrationSectionPage();

        analyticsConfigViewPage.gotoPage();
        analyticsConfigViewPage.setTrackingCode(Config.getTrackingCode()).setAuthTokenId(Config.MATOMO_AUTH_TOKEN)
                .setIdSiteId("1").setRequestAddressId("http://" + Config.ADDRESS + ":" + Config.MATOMO_BRIDGE_PORT + "/")
                .saveConfigs();
        analyticsConfigViewPage.waitForNotificationInProgressMessage("Saving...");
        analyticsConfigViewPage.waitForNotificationSuccessMessage("Saved");
        analyticsConfigViewPage.waitForNotificationInProgressMessage("Checking connection to Matomo.");
        analyticsConfigViewPage.waitForNotificationSuccessMessage("Test connection succeeded!");
        AnalyticsViewPage.gotoPage();
    }


    /**
     * Checks that the description is loaded properly for a macro.
     */
    @Test
    @Order(3)
    void checkMacroDescription() throws InterruptedException {
        AnalyticsViewPage.gotoPage();
        MostViewedPagesElement mostViewedPagesELement = new MostViewedPagesElement("gadget_1");
        assertEquals("When visitors search on your website, they are looking for a particular page, content, product,"
                        + " or service. This report lists the pages that were clicked the most after an internal search.",
                mostViewedPagesELement.getMacroDescription());
    }


    /**
     * Checks if the admin has edit permissions in the home page of the application.
     */
    @Test
    @Order(3)
    void checkEditPermissions(XWikiWebDriver driver) throws InterruptedException {

        AnalyticsViewPage analyticsViewPage = AnalyticsViewPage.gotoPage();
        // Add a gadget to the dashboard.
        AnalyticsEditPage.gotoPage().addNewMacro("Search Categories").saveDashboard();
        driver.waitUntilCondition(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(".gadget-content"),
                2));
        assertEquals(3, analyticsViewPage.getGadgetCount());
    }

    /**
     * Checks that the Row Evolution modal is loaded properly.
     */
    @Test
    @Order(5)
    void checkRowEvolutionModal() {
        AnalyticsViewPage.gotoPage();
        RowEvolutionModal rowEvolutionModal = new RowEvolutionModal(By.cssSelector(".modal-dialog.modal-lg"));
        rowEvolutionModal.openModal();
        assertTrue(rowEvolutionModal.isDisplayed());
    }

    /**
     * Create and start a container with the database.
     */
    private MySQLContainer startDb(TestConfiguration testConfiguration) throws Exception {
        // Since the MySQL container is derived from the official MySQL image I have to mark the image as compatible
        // with MySQLContainers.
        DockerImageName sqlContainer =
                DockerImageName.parse(Config.DB_CONTAINER_NAME).asCompatibleSubstituteFor("mysql");
        MySQLContainer<?> mysqlContainer =
                new MySQLContainer<>(sqlContainer).withDatabaseName(Config.DB_NAME).withUsername(Config.DB_USERNAME)
                        .withPassword(Config.DB_PASSWORD).withExposedPorts(3306);
        mysqlContainer.setPortBindings(
                Collections.singletonList(String.format("%d:%d", Config.DB_BRIDGE_PORT, Config.DB_CONTAINER_EXPOSED_PORT)));
        DockerTestUtils.startContainer(mysqlContainer, testConfiguration);
        return mysqlContainer;
    }

    /**
     * Creates&starts the Matomo container.
     *
     * @param testConfiguration test configuration
     * @param dbContainer       reference to the db container
     */
    private GenericContainer startMatomo(TestConfiguration testConfiguration, GenericContainer dbContainer)
            throws Exception {
        GenericContainer<?> matomoContainer = new GenericContainer<>(Config.MATOMO_CONTAINER_NAME).withExposedPorts(80)
                .withEnv("MATOMO_DATABASE_HOST",
                        Config.ADDRESS + ":" + dbContainer.getMappedPort(Config.DB_CONTAINER_EXPOSED_PORT))
                .withFileSystemBind("src/test/it/resources/config.ini.php", Config.MATOMO_CONFIG_FILE_PATH);
        matomoContainer.setPortBindings(Collections.singletonList(String.format("%d:80", Config.MATOMO_BRIDGE_PORT)));
        DockerTestUtils.startContainer(matomoContainer, testConfiguration);
        return matomoContainer;
    }
}
