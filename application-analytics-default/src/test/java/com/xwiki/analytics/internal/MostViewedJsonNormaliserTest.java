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
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Mockito.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;

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
public class MostViewedJsonNormaliserTest
{
    @InjectMockComponents
    private MostViewedJsonNormaliser mostViewedJsonNormaliser;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    @MockComponent
    private Logger logger;

    /**
     * Will test if the normaliser works properly when the response from Matomo is an object.
     */
    @Test
    public void normalizeDataWithObjectResponseWithoutFilters() throws Exception
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataWithObjectResponseWithoutFilters.json");
        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    /**
     * Will test if the normaliser works properly when the response from Matomo is an array of jsons and because there
     * is no filters applied the json stays the same.
     */
    @Test
    public void normalizeDataWithArrayResponseWithoutFilters() throws Exception
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataWithArrayResponseWithoutFilters.json");
        assertEquals(node.get("JSON"), mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    /**
     * Will test if the filtering works properly with an exact match.
     */
    @Test
    public void normalizeDataWithExactMatchFilter() throws Exception
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizationWithOneFilter.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("label", "/xwiki/bin/view/Analytics/Code/MostViewedPages");
        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    /**
     * Will test if the filtering works properly with partial matching.
     */
    @Test
    public void normalizeDataWithPartialMatchFilter() throws Exception
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataWithMultipleFilters.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("nb_hits", "27");
        filters.put("label", "MostViewedPage?editor=wiki");
        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    @Test
    public void normalizeDataWithMultipleFilters() throws Exception
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataWithPartialMatchFilter.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("nb_hits", "27");
        filters.put("nb_hits", "27");

        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    /**
     * Will test the case when the url is invalid.
     */
    @Test
    public void normalizeDataWithMalformedUrl() throws Exception
    {
        ReflectionUtils.setFieldValue(this.mostViewedJsonNormaliser, "logger", this.logger);
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataWithMalformedUrl.json");
        HashMap<String, String> filters = new HashMap<>();
        assertEquals(node.get("JSON"), mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
        verify(logger).warn("Failed to get resource reference from URL: [{}]. Caused by [{}]",
            "htttp://localhost:8080/xwiki/bin/view/Analytics/Code/MostViwedPages",
            "MalformedURLException: unknown protocol: htttp");
    }

    @Test
    public void normalizeDataJsonWithoutURL() throws IOException
    {
        JsonNode node = readJSONS("/mostViewedPages/normalizeDataJsonWithoutURL.json");
        assertEquals(node.get("JSON"), mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    @BeforeEach
    private void setupAnyURL() throws Exception
    {
        when(resourceTypeResolver.resolve(any(ExtendedURL.class), eq(Collections.emptyMap()))).thenReturn(null);
    }

    private JsonNode readJSONS(String file) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream is = JsonReader.class.getResourceAsStream(file);
        JsonNode node = objectMapper.readTree(is);
        return node;
    }
}
