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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xwiki.analytics.JsonNormaliser;

import liquibase.repackaged.org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Implementation for {@link JsonNormaliser}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("MostViewedPages")
@Singleton
public class MostViewedJsonNormaliser implements JsonNormaliser
{
    /**
     * Hint for the MostViewedJsonNormaliser.
     */
    public static final String HINT = "MostViewedPages";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATE = "date";

    private static final String URL = "url";

    private static final String LABEL = "label";

    @Inject
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @Inject
    private Logger logger;

    @Inject
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /**
     * Normalize Matomo response for format consistency and add extra information needed by XWiki.
     *
     * @param jsonString the Matomo JSON response, in {@code String} format
     * @return the normalised json
     */
    public JsonNode normaliseData(String jsonString) throws JsonProcessingException
    {
        // Convert the string returned by Matomo in a JSON format to easily handle the processing of the nodes.
        JsonNode jsonRoot = OBJECT_MAPPER.readTree(jsonString);
        // Matomo may return several variants of JSON formats. In one scenario, when the period is set to
        // day/week/month/year, it returns a JSON object with keys representing dates. The corresponding value for
        // each key is an array of JSON objects, each of which represents a page. However, if the user sets the
        // period parameter to "range" Matomo returns an array of JSON objects, with each JSON object representing
        // a page. Due to these variations, the result needs to be processed to create a single format.
        // This normalized format is an array of JSON objects and each JSON object in this array will have a new
        // field called 'date'. This 'date' field will be set to 'N/A' when Matomo returns an array instead of an
        // object. For both type of formats, the label field is also altered in order to contain the full page name
        if (jsonRoot.isArray()) {
            processArrayNode(jsonRoot);
        } else {
            jsonRoot = processObjectNode(jsonRoot);
        }
        return jsonRoot;
    }

    /**
     * This method processes each entry to append an empty date to it.
     *
     * @param jsonNode an array of jsons
     */
    private void processArrayNode(JsonNode jsonNode)
    {
        for (JsonNode objNode : jsonNode) {
            if (objNode.isObject()) {
                ((ObjectNode) objNode).put(DATE, "");
                // Just in case that this normalsier wil be used in the future for other macros that do not have an url.
                if (objNode.has(URL)) {
                    this.handleURLNode((ObjectNode) objNode);
                }
            }
        }
    }

    /**
     * Handle the scenario where Matomo returns an object with dates as keys and arrays of JSON objects as values. This
     * function extracts the date from the key and adds it to each page. Ultimately, it returns an array of JSON objects
     * instead of a single JSON object.
     *
     * @param jsonNode json object
     * @return array of jsons
     */
    private ArrayNode processObjectNode(JsonNode jsonNode)
    {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while (fieldNames.hasNext()) {
            String date = fieldNames.next();
            JsonNode childNode = jsonNode.get(date);
            for (JsonNode objNode : childNode) {
                if (objNode.isObject()) {
                    ((ObjectNode) objNode).put(DATE, date);
                    // Just in case that this normalsier wil be used in the future for other macros that do not have an
                    // url.
                    if (objNode.has(URL)) {
                        this.handleURLNode((ObjectNode) objNode);
                        arrayNode.add(objNode);
                    }
                }
            }
        }
        return arrayNode;
    }

    /**
     * Process the URL of a page to obtain the documentReference. This is necessary to retrieve the document name for
     * display when rendering the table.
     *
     * @param resourceReferenceURL the URL of the page
     * @return the reference associated to the given URL, or {@code null} in case it couldn't be resolved
     */
    private ResourceReference getResourceReferenceFromStringURL(String resourceReferenceURL)
    {
        try {

            // The URL provided by Matomo is an unencoded string. For utilization with the resourceTypeResolver,
            // this URL needs proper encoding. This is achieved by creating a URL object to split the URL into its
            // respective components and after that the URL is encoded using the URI constructor.
            URL url = new URL(resourceReferenceURL);
            URL encodedUrl = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                url.getQuery(), url.getRef()).toURL();
            ExtendedURL extendedURL = new ExtendedURL(encodedUrl, null);
            ResourceType resourceType = this.resourceTypeResolver.resolve(extendedURL, Collections.emptyMap());
            return this.resourceReferenceResolver.resolve(extendedURL, resourceType, Collections.emptyMap());
        } catch (MalformedURLException | CreateResourceReferenceException | CreateResourceTypeException
                 | UnsupportedResourceReferenceException | URISyntaxException e) {
            logger.warn("Failed to get resource reference from URL: [{}].", resourceReferenceURL,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Change the label node to contain the actual page name instead of an URL.
     *
     * @param objNode a json object
     */
    private void handleURLNode(ObjectNode objNode)
    {
        EntityResourceReference entityResourceReference =
            (EntityResourceReference) this.getResourceReferenceFromStringURL(objNode.get(URL).asText());
        if (entityResourceReference != null) {
            objNode.put(LABEL, this.serializer.serialize(entityResourceReference.getEntityReference()));
        }
    }
}
