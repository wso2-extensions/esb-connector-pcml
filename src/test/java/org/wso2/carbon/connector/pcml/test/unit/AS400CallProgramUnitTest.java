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

import com.ibm.as400.access.AS400;
import com.ibm.as400.data.ProgramCallDocument;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.pcml.AS400CallProgram;
import org.wso2.carbon.connector.pcml.AS400Constants;
import org.wso2.carbon.connector.pcml.AS400Initialize;
import org.wso2.carbon.connector.pcml.AS400PCMLConnectorException;
import org.wso2.carbon.mediation.registry.WSO2Registry;
import org.wso2.carbon.registry.core.Resource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Stack;

import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

@PrepareForTest({ AS400CallProgram.class })
public class AS400CallProgramUnitTest {
    AS400CallProgram as400CallProgram;
    AS400Initialize as400Initialize;
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;
    SynapseConfiguration synapseConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        as400Initialize = new AS400Initialize();
        as400CallProgram = new AS400CallProgram();
        messageContext = createMessageContext();
        templateContext = new TemplateContext("AS400Init", null);
        functionStack = new Stack();
    }

    @Test(description = "Test AS400 Call Program")
    public void testAS400CallProgram() throws Exception {
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_SYSTEM_NAME, "AS400_SystemName");
        templateContext.getMappedValues()
                .put(AS400Constants.AS400_PCML_FILE_LOCATION, "conf:/pcml/PcmlNumberAddition.pcml");
        templateContext.getMappedValues().put(AS400Constants.AS400_PCML_PROGRAM_NAME, "Addition");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        Stack a = PowerMockito.mock(Stack.class);
        whenNew(Stack.class).withNoArguments().thenReturn(a);
        WSO2Registry registry = PowerMockito.mock(WSO2Registry.class);
        Resource resource = PowerMockito.mock(Resource.class);
        InputStream inputStream = PowerMockito.mock(InputStream.class);
        when(registry.getResource(anyString())).thenReturn(resource);
        when(resource.getContentStream()).thenReturn(inputStream);
        messageContext.getConfiguration().setRegistry(registry);
        as400Initialize.connect(messageContext);
        ProgramCallDocument programCallDocument = PowerMockito.mock(ProgramCallDocument.class);
        whenNew(ProgramCallDocument.class)
                .withArguments(any(AS400.class), anyString(), any(InputStream.class), any(ClassLoader.class),
                        any(InputStream.class), anyInt()).thenReturn(programCallDocument);
        when(programCallDocument.callProgram(anyString())).thenReturn(true);
        ByteArrayOutputStream byteArrayOutputStream = PowerMockito.mock(ByteArrayOutputStream.class);
        whenNew(ByteArrayOutputStream.class).withNoArguments().thenReturn(byteArrayOutputStream);
        try {
            as400CallProgram.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurred while calling the AS400 program: null";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400 without AS400 instance")
    public void testAS400CallProgramWithoutInstance() throws Exception {
        try {
            as400CallProgram.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurred while processing message context. May occur due to invalid data:"
                    + " Unable to find an AS400 instance to call program."
                    + " Use the 'init' mediator to create an AS400 instance.";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400 without Pcml File Location")
    public void testAS400CallProgramWithoutPcmlFileLocationParameter() throws Exception {
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        as400Initialize.connect(messageContext);
        try {
            as400CallProgram.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurred while processing message context. May occur due to invalid data:"
                    + " A PCML file name could not be found as a parameter to call a program."
                    + " Make sure the registry path is correct.";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400 without Pcml program Name")
    public void testAS400WithoutPcmlProgramName() throws Exception {
        templateContext.getMappedValues().put(AS400Constants.AS400_PCML_FILE_LOCATION, "pcmlFileLocation");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        as400Initialize.connect(messageContext);
        try {
            as400CallProgram.connect(messageContext);
        } catch (Exception e) {
            Iterator error = messageContext.getEnvelope().getBody().getChildrenWithLocalName("errorMessage");
            String Message = "Error occurred while processing message context. May occur due to invalid data:"
                    + " A program name was not specified to call.";
            String s1 = ((OMElement) error.next()).getText();
            Assert.assertEquals(s1, Message);
        }
    }

    @Test(description = "Test AS400 Call Program getFileType with Unsupport extension",
          expectedExceptions = AS400PCMLConnectorException.class)
    public void testGetFileTypeWithUnSupportExtension() throws Exception {
        Whitebox.invokeMethod(as400CallProgram, "getFileType", "unSupport");
    }

    private MessageContext createMessageContext() throws AxisFault {
        org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
        Axis2MessageContext mc = new Axis2MessageContext(axis2MC, this.synapseConfig, null);
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        mc.setConfiguration(synapseConfiguration);
        return mc;
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }
}