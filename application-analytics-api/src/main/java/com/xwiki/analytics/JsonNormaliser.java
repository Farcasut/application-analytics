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
package com.xwiki.analytics;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The interface for the json normalisation classes.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Unstable
public interface JsonNormaliser
{
    /**
     * This function will normalise the data returned to have only one format.
     *
     * @param jsonString A string that has a proper json format.
     * @return Returns the json in string format
     * @throws JsonProcessingException Throws this error when the jsonString param is not a proper json.
     */
<<<<<<< HEAD
    JsonNode normaliseData(String jsonString) throws JsonProcessingException;

=======
    JsonNode normaliseData(String jsonString)
        throws JsonProcessingException;
>>>>>>> RowEvolution
}
