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
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MatomoAnalyticsManager}
 *
 * @version $Id$
 */
@ComponentTest
public class MostViewedJsonNormalizerTest extends JsonNormalizerTest
{
    @InjectMockComponents
    private MostViewedJsonNormaliser mostViewedJsonNormaliser;

    @MockComponent
    @Named("resource/standardURL")
    private EntityReferenceResolver<String> urlToReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private Logger logger;

    /**
     * Will test if the normaliser works properly when the response from Matomo is an object.
     */
    @Test
    void normalizeDataWithObjectResponseWithoutFilters() throws Exception
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizeDataWithObjectResponseWithoutFilters.json");

        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    /**
     * Will test if the normaliser works properly when the response from Matomo is an array of jsons and because there
     * is no filters applied the json stays the same.
     */
    @Test
    void normalizeDataWithArrayResponseWithoutFilters() throws Exception
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizeDataWithArrayResponseWithoutFilters.json");
        assertEquals(node.get("Response"), mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    /**
     * Will test if the filtering works properly with an exact match.
     */
    @Test
    void normalizeDataWithExactMatchFilter() throws Exception
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizationWithOneFilter.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("label", "/xwiki/bin/view/Analytics/Code/MostViewedPages");
        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    /**
     * Will test if the filtering works properly with partial matching.
     */
    @Test
    void normalizeDataWithPartialMatchFilter() throws Exception
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizeDataWithMultipleFilters.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("nb_hits", "27");
        filters.put("label", "MostViewedPage?editor=wiki");
        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    /**
     * Will test if the normaliser works properly when the response from Matomo is an object.
     */
    @Test
    void normalizeDataWithMultipleFilters() throws Exception
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizeDataWithPartialMatchFilter.json");
        HashMap<String, String> filters = new HashMap<>();
        filters.put("nb_hits", "27");
        filters.put("nb_hits", "27");

        assertEquals(node.get("Response"),
            mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), filters));
    }

    /**
     * Will test if the normaliser works properly when the response from Matomo is an array of jsons.
     */
    @Test
    void normalizeDataJsonWithoutURL() throws IOException
    {
        JsonNode node = getTestJSONS("/mostViewedPages/normalizeDataJsonWithoutURL.json");
        assertEquals(node.get("JSON"), mostViewedJsonNormaliser.normaliseData(node.get("JSON").toString(), null));
    }

    @BeforeEach
    void setupAnyURL() throws Exception
    {
        EntityReference parentReference = mock(EntityReference.class);

        when(urlToReferenceResolver.resolve(any(), any())).thenReturn(
            new EntityReference("label", EntityType.DOCUMENT, parentReference, Collections.emptyMap()));
        when(contextualAuthorizationManager.hasAccess(any(), any())).thenReturn(true);
    }
}
