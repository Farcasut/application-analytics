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
package com.xwiki.analytics.internal.configuration;

import javax.inject.Singleton;
import com.xwiki.analytics.configuration.AnalyticsConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Default implementation of {@AnalyticsConfiguration}.
 * @version $Id$
 */
@Component
@Singleton
public class DefaultAnalyticsConfiguration implements AnalyticsConfiguration
{
    @Inject
    @Named("analytics")
    private ConfigurationSource configDocument;
    @Override
    public String getRequestAddress()
    {
        return getProperty("requestAddress", "");
    }

    @Override
    public String getIdSite()
    {
        return  getProperty("siteId", "");
    }

    @Override
    public String getAuthenticationToken()
    {
        return getProperty("authToken", "");
    }

    private <T> T getProperty(String key, T defaultValue)
    {
        return this.configDocument.getProperty(key, defaultValue);
    }
}
