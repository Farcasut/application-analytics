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
package com.xwiki.analytics.internal;

import java.io.IOException;
import java.util.HashMap;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.analytics.JsonNormaliser;
import com.xwiki.analytics.configuration.AnalyticsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MatomoAnalyticsManager}
 *
 * @version $Id$
 */
@ComponentTest
public class MatomoAnalyticsManagerTest
{
    @InjectMockComponents
    private MatomoAnalyticsManager matomoAnalyticsManager;

    @MockComponent
    @Named("MostViewedPages")
    private JsonNormaliser jsonNormaliser;

    @MockComponent
    private AnalyticsConfiguration configuration;

    @MockComponent
    private Logger logger;

    @Test
    public void requestDataWithCorrectHintForNormaliser() throws IOException, InterruptedException
    {
        when(this.configuration.getAuthenticationToken()).thenReturn("token");
        when(this.configuration.getRequestAddress()).thenReturn("http://130.61.233.19/matomo");
        when(this.configuration.getIdSite()).thenReturn("3");
        this.matomoAnalyticsManager.requestData(new HashMap<>(), MostViewedJsonNormaliser.HINT);
        verify(this.jsonNormaliser).normaliseData(any(String.class));
    }

    @Test
    public void requestDataWithInvalidNormaliser() throws IOException, InterruptedException
    {
        ReflectionUtils.setFieldValue(this.matomoAnalyticsManager, "logger", this.logger);
        when(this.configuration.getAuthenticationToken()).thenReturn("token");
        when(this.configuration.getRequestAddress()).thenReturn("http://130.61.233.19/matomo");
        when(this.configuration.getIdSite()).thenReturn("3");
        try {
            when(this.matomoAnalyticsManager.requestData(new HashMap<>(), "RANDOM_NORMALISER")).thenThrow(
                new RuntimeException());
            verify(this.logger).warn("There is no JSON normalizer associated with the [{}] hint you provided.",
                "RANDOM_NORMALISER");
        }
        catch (RuntimeException e)
        {
            assertEquals(e.getMessage(), "Error occurred while retrieving Matomo statistic results.");
        }

    }
}
