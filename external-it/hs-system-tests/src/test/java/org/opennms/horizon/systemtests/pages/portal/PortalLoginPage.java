/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/


package org.opennms.horizon.systemtests.pages.portal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class PortalLoginPage {

    private static final SelenideElement acceptBtn = $("#cookie-header-accept");
    private static final SelenideElement usernameInp = $("#idp-discovery-username");
    private static final SelenideElement passwordInp = $("#okta-signin-password");
    private static final SelenideElement nextBtn = $("#idp-discovery-submit");
    private static final SelenideElement signInBtn = $("#okta-signin-submit");

    public static void closeCookieHeader() {
        acceptBtn.shouldBe(Condition.enabled).click();
    }

    public static void setUsername(String username) {
        usernameInp.shouldBe(Condition.enabled).setValue(username);
    }

    public static void setPassword(String password) {
        passwordInp.shouldBe(Condition.enabled).setValue(password);
    }

    public static void clickNext() {
        nextBtn.shouldBe(Condition.enabled).click();
    }

    public static void clickSignIn() {
        signInBtn.shouldBe(Condition.enabled).click();
    }


}
