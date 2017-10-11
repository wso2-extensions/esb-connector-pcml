package org.wso2.carbon.connector.pcml.test.unit;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.template.TemplateContext;
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

public class AS400InitializeUnitTest {
    private AS400Initialize as400Initialize;
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;

    @BeforeMethod
    public void setUp() throws Exception {
        as400Initialize = new AS400Initialize();
        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        messageContext = new Axis2MessageContext(mc, config, env);
        org.apache.axiom.soap.SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);
        envelope.getBody().addChild(createOMElement("<a>test</a>"));
        messageContext.setEnvelope(envelope);
        templateContext = new TemplateContext("AS400Init", null);
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_SYSTEM_NAME, "AS400_SystemName");
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_USER_ID, "MyUserID");
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_PASSWORD, "MyPassword");
        functionStack = new Stack();
    }

    @Test(description = "Test Initialize with unknown Host")
    public void testAS400InitWithUnknownHost() throws Exception {
        templateContext.getMappedValues().put(AS400Constants.AS400_INIT_PASSWORD, "MyPassword");
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

