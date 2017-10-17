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

import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.SocketProperties;
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
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.pcml.AS400Constants;
import org.wso2.carbon.connector.pcml.AS400Initialize;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Stack;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.powermock.api.mockito.PowerMockito.when;

public class AS400InitializeUnitTest {
    private AS400Initialize as400Initialize;
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;

    @BeforeMethod
    public void setUp() throws Exception {
        as400Initialize = new AS400Initialize();
        messageContext = createMessageContext();
        templateContext = new TemplateContext("AS400Init", null);
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_SYSTEM_NAME, "AS400_SystemName");
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_USER_ID, "MyUserID");
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_PASSWORD, "MyPassword");
        functionStack = new Stack();
    }

    @Test(description = "Test AS400Initialize connect with unknown Host")
    public void testConnectWithUnknownHost() throws Exception {
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        try {
            as400Initialize.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurs when communicating to the AS400 server: AS400_SystemName";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400Initialize connect with unknown Pool")
    public void testConnectWithUnknownPool() throws Exception {
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_NAME, "pool");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        try {
            as400Initialize.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurred while getting a connection from the pool: AS400_SystemName";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400Initialize getConnectionPool")
    public void testGetConnectionPool() throws Exception {
        setConnectionProperties();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        SynapseLog log = PowerMockito.mock(SynapseLog.class);
        when(log.isTraceOrDebugEnabled()).thenReturn(true);
        AS400ConnectionPool result = Whitebox.invokeMethod(as400Initialize, "getConnectionPool", messageContext, log);
        Assert.assertEquals(result.getMaxUseCount(), -1);
        Assert.assertEquals(result.getCleanupInterval(), 300000);
    }

    @Test(description = "Test AS400Initialize getConnectionPool")
    public void testGetSocketProperties() throws Exception {
        setSocketProperties();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        SynapseLog log = PowerMockito.mock(SynapseLog.class);
        when(log.isTraceOrDebugEnabled()).thenReturn(true);
        SocketProperties result = Whitebox.invokeMethod(as400Initialize, "getSocketProperties", messageContext, log);
        Assert.assertEquals(result.getLoginTimeout(), 10000);
        Assert.assertEquals(result.getReceiveBufferSize(), 87380);
    }

    public void setConnectionProperties() {
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_NAME, "pool");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_MAX_CONNECTIONS, "50");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_MAX_LIFETIME, "600000");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_MAX_USE_COUNT, "-1");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_MAX_USE_TIME, "300000");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_RUN_MAINTENANCE, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_THREAD_USED, "true");
        templateContext.getMappedValues().put(AS400Constants.AS400_CONNECTION_POOL_CLEANUP_INTERVAL, "300000");
    }

    public void setSocketProperties() {
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_KEEP_ALIVE, "false");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_LOGIN_TIMEOUT, "10000");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_RECEIVE_BUFFER_SIZE, "87380");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_SEND_BUFFER_SIZE, "16384");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_SOCKET_LINGER, "0");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_SOCKET_TIMEOUT, "15000");
        templateContext.getMappedValues().put(AS400Constants.AS400_SOCKET_PROPERTY_TCP_NO_DELAY, "false");
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

