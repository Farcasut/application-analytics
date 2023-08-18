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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

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
    @Named("MostViewedPages")
    private JsonNormaliser mostViewedNormaliser;

    @Inject
    @Named("RowEvolution")
    private JsonNormaliser rowEvolution;

    @Inject
    private AnalyticsConfiguration configuration;

    /**
     * This method will handle all the request made by the user and return a proper json.
     *
     * @param jsonNormaliserHint hint to select the json normaliser.
     * @param parameters a list of key, value pairs that will represent the parameters for the request.
     * @return will return a json with all the data returned by Matomo.
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
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder(buildURI(configuration.getRequestAddress(), parameters))
            .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return jsonNormaliser.normaliseData(response.body());
    }

    /**
     * This function will create the URI for the Matomo request.
     *
     * @param address Address of the Matomo server.
     * @param parameterList List of the url parameters.
     * @return The final URI in string format.
     */
    private URI buildURI(String address, Map<String, String> parameterList)
    {
        UriBuilder uriBuilder = UriBuilder.fromUri(address).path("index.php");

        if (parameterList != null && !parameterList.isEmpty()) {
            parameterList.forEach((k, v) -> uriBuilder.queryParam(k, v));
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
