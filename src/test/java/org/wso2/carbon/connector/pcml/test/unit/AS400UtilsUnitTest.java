/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.connector.pcml.test.unit;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.template.TemplateContext;
import org.powermock.api.mockito.PowerMockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.pcml.AS400Constants;
import org.wso2.carbon.connector.pcml.AS400PCMLConnectorException;
import org.wso2.carbon.connector.pcml.AS400Utils;
import org.wso2.carbon.connector.pcml.PCMLInputParam;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.powermock.api.mockito.PowerMockito.when;

public class AS400UtilsUnitTest {
    private AS400Utils as400Utils;
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;

    @BeforeMethod
    public void setUp() throws Exception {
        as400Utils = new AS400Utils();
        messageContext = createMessageContext();
        templateContext = new TemplateContext("AS400Init", null);
        functionStack = new Stack();
    }

    @Test(description = "Test AS400Utils preparePayload")
    public void testAS400UtilsPreparePayload() {
        OMElement omElement = createOMElement(
                "<elements>" + "<element1>1</element1>" + "<element2>2</element2>" + "</elements>");
        as400Utils.preparePayload(messageContext, omElement);
        Assert.assertEquals((messageContext.getEnvelope().getBody().getFirstElement()).toString(),
                "<element1>1</element1>");
    }

    @Test(description = "Test AS400Utils preparePayload for ExceptionMessage")
    public void testAS400UtilsPreparePayloadForException() {
        as400Utils.preparePayload(messageContext, "This is an error Message");
        Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
        String s1 = ((OMElement) error.next()).getText();
        Assert.assertEquals(s1, "This is an error Message");
    }

    @Test(description = "Test AS400Utils preparePayload for ExceptionMessage")
    public void testAS400UtilsGetInputParameters() throws AS400PCMLConnectorException {
        SynapseLog log = PowerMockito.mock(SynapseLog.class);
        when(log.isTraceOrDebugEnabled()).thenReturn(true);
        templateContext.getMappedValues().put(AS400Constants.AS400_PCML_PROGRAM_INPUTS,
                "<pcml:pcmlInputs xmlns:pcml=\"pcml\">"
                        + "<pcml:pcmlInput qualifiedName=\"Addition.inputValue1\" indices=\"1,3\">5</pcml:pcmlInput>"
                        + "<pcml:pcmlInput qualifiedName=\"Addition.inputValue2\"><num>6</num></pcml:pcmlInput>"
                        + "<pcml:pcmlInput>12</pcml:pcmlInput>" + "<pcml:Wrong>12</pcml:Wrong>" + "</pcml:pcmlInputs>");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        List<PCMLInputParam> inputParameters = as400Utils.getInputParameters(messageContext, log);
        System.out.println(inputParameters);
        Assert.assertEquals(inputParameters.size(), 2);
        Assert.assertEquals(inputParameters.get(0).getQualifiedName(), "Addition.inputValue1");
        Assert.assertEquals(inputParameters.get(0).getValue(), "5");
        Assert.assertEquals(inputParameters.get(0).getIndices().length, 2);
        Assert.assertEquals(inputParameters.get(1).getQualifiedName(), "Addition.inputValue2");
        Assert.assertEquals(inputParameters.get(1).getValue(), "<num>6</num>");
        Assert.assertNull(inputParameters.get(1).getIndices());
    }

    public MessageContext createMessageContext() throws AxisFault {
        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        MessageContext messageContext = new Axis2MessageContext(mc, config, env);
        org.apache.axiom.soap.SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);
        envelope.getBody().addChild(createOMElement("<a>test</a>"));
        messageContext.setEnvelope(envelope);
        return messageContext;
    }

    public static OMElement createOMElement(String xml) {
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            return builder.getDocumentElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}