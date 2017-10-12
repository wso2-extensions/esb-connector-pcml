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

import com.ibm.as400.access.Trace;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.pcml.AS400Constants;
import org.wso2.carbon.connector.pcml.AS400Trace;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class AS400TraceUnitTest {
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;
    private AS400Trace as400Trace;
    private static final String CARBON_HOME = "carbon.home";

    @BeforeMethod
    public void setUp() throws Exception {
        as400Trace = new AS400Trace();
        messageContext = createMessageContext();
        templateContext = new TemplateContext("AS400Init", null);
        functionStack = new Stack();
    }

    public static void setCarbonHome() {
        Path carbonHome = Paths.get("");
        carbonHome = Paths.get(carbonHome.toString());
        System.setProperty(CARBON_HOME, carbonHome.toAbsolutePath().toString());
        Path create = Paths.get(carbonHome.toString(), "repository", "logs");
        File file = new File(create.toAbsolutePath().toString());
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Test(description = "Test AS400Trace Connect")
    public void testAs400TraceConnecct() throws ConnectException {
        setCarbonHome();
        setTraceProperties();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        as400Trace.connect(messageContext);
        Assert.assertEquals(Trace.getFileName(),
                "/home/jananithangavel/Desktop/new_unitTest/esb-connector-pcml/target/repository/logs/pcml-connector-logs.log");
        Assert.assertFalse(Trace.isTraceAllOn());
    }

    public void setTraceProperties() {
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_CONVERSION, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_DATASTREAM, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_DIAGNOSTICS, "false");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_ERROR, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_INFORMATION, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_PCML, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_WARNING, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_PROXY, "false");
        templateContext.getMappedValues().put(AS400Constants.AS400_TRACE_ALL, "false");
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