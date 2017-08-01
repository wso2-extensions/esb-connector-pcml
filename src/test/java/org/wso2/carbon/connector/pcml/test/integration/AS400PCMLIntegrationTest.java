/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.connector.pcml.test.integration;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.connector.integration.test.base.ConnectorIntegrationTestBase;

/**
 * Integration test for the PCML connector.
 */
public class AS400PCMLIntegrationTest extends ConnectorIntegrationTestBase {

    /**
     * Initializes test.
     * @throws Exception
     */
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        init("pcml-connector-1.0.2");
    }

    /**
     * Executes a program that adds 2 values in the AS400 using the connector.
     * @throws Exception
     */
    @Test(groups = {"wso2.esb"}, description = "Check if number addition test works.")
    public void callAdditionProgramTestCase() throws Exception {
        SOAPEnvelope soapRequest = sendSOAPRequest(proxyUrl, "invoker.xml");
        OMElement xpcmlElement = soapRequest.getBody().getFirstElement();
        Assert.assertEquals(xpcmlElement.toString(),
                "<xpcml version=\"6.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:noNamespaceSchemaLocation='xpcml.xsd'><program name=\"Addition\" " +
                        "path=\"/QSYS.LIB/%LIBL%.LIB/ADDITION.PGM\">" +
                        "<parameterList>" +
                        "<intParm name=\"inputValue1\" passDirection=\"in\">5</intParm>" +
                        "<intParm name=\"inputValue2\" passDirection=\"in\">10</intParm>" +
                        "<intParm name=\"outputValue\" passDirection=\"out\">15<intParm>" +
                        "</parameterList>" +
                        "</program></xpcml>", "Output value does not match.");
    }
}
