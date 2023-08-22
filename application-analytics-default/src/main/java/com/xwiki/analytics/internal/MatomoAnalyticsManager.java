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
import java.net.URI;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.databind.JsonNode;
import com.xwiki.analytics.AnalyticsManager;
import com.xwiki.analytics.JsonNormaliser;
import com.xwiki.analytics.configuration.AnalyticsConfiguration;

/**
 * The class would handle the Matomo request and return a json with the data.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("Matomo")
@Singleton
@Unstable
public class MatomoAnalyticsManager implements AnalyticsManager
{
    @Inject
    private Logger logger;

    @Inject
    @Named(MostViewedJsonNormaliser.HINT)
    private JsonNormaliser mostViewedNormaliser;

    @Inject
    @Named("RowEvolution")
    private JsonNormaliser rowEvolution;

    @Inject
    private AnalyticsConfiguration configuration;

    /**
     * Request specific data from Matomo and return an enhanced response.
     *
     * @param jsonNormaliserHint hint for the component that will alter the returned response
     * @param parameters a list of key, value pairs that will represent the parameters for the request
     * @return the altered Matomo request response, as JSON format
     */
    @Override
    public JsonNode requestData(Map<String, String> parameters, String jsonNormaliserHint)
        throws IOException, InterruptedException
    {
        parameters.put("idSite", configuration.getIdSite());
        parameters.put("token_auth", configuration.getAuthenticationToken());
        JsonNormaliser jsonNormaliser = this.getNormaliser(jsonNormaliserHint);
        if (jsonNormaliser == null) {
            logger.warn("There is no JSON normalizer associated with the [{}] hint you provided.", jsonNormaliserHint);
            throw new RuntimeException("Error occurred while retrieving Matomo statistic results.");
        }
        HttpClient client = HttpClients.createDefault();
        HttpGet request =  new HttpGet(buildURI(parameters));
        HttpResponse response = client.execute(request);
        String responseBody  = EntityUtils.toString(response.getEntity());
        return jsonNormaliser.normaliseData(responseBody);
    }

    /**
     * Create URI for Matomo request.
     *
     * @param parameterList List of the url parameters
     * @return The final URI in string format
     */
    private URI buildURI(Map<String, String> parameterList)
    {
        UriBuilder uriBuilder = UriBuilder.fromUri(configuration.getRequestAddress()).path("index.php");

        if (parameterList != null && !parameterList.isEmpty()) {
            for (Map.Entry<String, String> entry : parameterList.entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        return uriBuilder.build();
    }

    private JsonNormaliser getNormaliser(String hint)
    {
        switch (hint) {
            case "MostViewedPages":
                return this.mostViewedNormaliser;
            case "RowEvolution":
                return this.rowEvolution;
            default:
                return null;
        }
    }
}
