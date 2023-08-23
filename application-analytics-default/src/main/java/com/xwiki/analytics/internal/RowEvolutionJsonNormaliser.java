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

import java.util.Iterator;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xwiki.analytics.JsonNormaliser;

/**
 * The normaliser for the RowEvolution.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("RowEvolution")
@Unstable
@Singleton
public class RowEvolutionJsonNormaliser implements JsonNormaliser
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATE = "date";

    /**
     * This method will normalise the jsons returned by Matomo into a single format.
     *
     * @param jsonString the json provided by Matomo.
     * @return the normalised json as a JsonNode.
     */
    public JsonNode normaliseData(String jsonString, String filteringField, String filterValue) throws JsonProcessingException
    {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
        return processObjectNode(jsonNode);
    }

    /**
     * Transforming the json object returned by Matomo into an array of jsons to make it easier to use in javascript.
     *
     * @param jsonNode the JSON node to be processed.
     * @return the processed and transformed JSON node.
     * @throws JsonProcessingException if any error occurs during JSON processing.
     */
    private ArrayNode processObjectNode(JsonNode jsonNode) throws JsonProcessingException
    {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        Iterator<String> fieldNames = jsonNode.fieldNames();

        while (fieldNames.hasNext()) {
            String date = fieldNames.next();
            JsonNode childNode = jsonNode.get(date);
            JsonNode node = childNode.get(0);

            if (node == null) {
                node = OBJECT_MAPPER.readTree(String.format("{\"%s\"  : \"%s\"}", DATE, date));
            } else {
                ((ObjectNode) node).put(DATE, date);
            }
            arrayNode.add(node);
        }
        return arrayNode;
    }
}
