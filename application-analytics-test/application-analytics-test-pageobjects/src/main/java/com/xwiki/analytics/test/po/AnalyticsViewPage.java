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
package com.xwiki.analytics.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Responsible for interacting with the main page of the application when the page is in view mode.
 */
public class AnalyticsViewPage extends ViewPage
{
    public AnalyticsViewPage()
    {
    }

    public static AnalyticsViewPage gotoPage()
    {
        getUtil().gotoPage("Analytics", "WebHome");
        return new AnalyticsViewPage();
    }

    /**
     * Calculates the number of gadgets that are present on the homepage of the application.
     *
     * @return number of gadgets
     */
    public int getGadgetCount()
    {
        return getUtil().getDriver().findElements(By.className("gadget")).size();
    }

    public boolean hasEditButton()
    {
        return !getDriver().findElementsWithoutWaiting(By.id("tmEdit")).isEmpty();
    }
}
