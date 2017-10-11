package org.wso2.carbon.connector.pcml.test.unit;

import com.ibm.as400.access.AS400;
import com.ibm.as400.data.ProgramCallDocument;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.pcml.AS400CallProgram;
import org.wso2.carbon.connector.pcml.AS400Constants;
import org.wso2.carbon.connector.pcml.AS400Initialize;
import org.wso2.carbon.mediation.registry.WSO2Registry;
import org.wso2.carbon.registry.core.Resource;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Stack;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;

@PrepareForTest(AS400CallProgram.class)
public class AS400CallProgramUnitTest {
    private AS400CallProgram as400CallProgram;
    private AS400Initialize as400Initialize;
    private MessageContext messageContext;
    private TemplateContext templateContext;
    private Stack functionStack;

    @BeforeMethod
    public void setUp() throws Exception {
        as400Initialize = new AS400Initialize();
        as400CallProgram = new AS400CallProgram();
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
        functionStack = new Stack();
        initMocks(this);
    }

    @Test(description = "Test AS400 Call Program")
    public void testAS400CallProgram() throws Exception {
        templateContext.getMappedValues()
                .put(AS400Constants.AS400_PCML_FILE_LOCATION, "conf:/pcml/PcmlNumberAddition.pcml");
        templateContext.getMappedValues().put(AS400Constants.AS400_PCML_PROGRAM_NAME, "Addition");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        WSO2Registry registry = PowerMockito.mock(WSO2Registry.class);
        Resource resource = PowerMockito.mock(Resource.class);
        InputStream inputStream = PowerMockito.mock(InputStream.class);
        PowerMockito.when(registry.getResource(anyString())).thenReturn(resource);
        PowerMockito.when(resource.getContentStream()).thenReturn(inputStream);
        messageContext.getConfiguration().setRegistry(registry);
        ProgramCallDocument programCallDocument = PowerMockito.mock(ProgramCallDocument.class);
        as400Initialize.connect(messageContext);
        PowerMockito.whenNew(ProgramCallDocument.class)
                .withArguments(any(AS400.class), anyString(), any(InputStream.class), any(ClassLoader.class),
                        any(InputStream.class), anyInt()).thenReturn(programCallDocument);
        as400CallProgram.connect(messageContext);
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
