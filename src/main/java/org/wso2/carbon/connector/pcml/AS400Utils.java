/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.pcml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.util.ConnectorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for the connector.
 */
public class AS400Utils {
    public static final OMFactory OM_FACTORY = OMAbstractFactory.getOMFactory();
    public static final OMNamespace OM_NAMESPACE = OM_FACTORY.createOMNamespace("http://wso2.org/pcml/adaptor", "pcml");

    /**
     * Gets the input parameters from the soap body and converts them to a list of {@link PCMLInputParam}s.
     *
     * @param messageContext The message context.
     * @param log            The logger object for logging.
     * @return A list of {@link PCMLInputParam}.
     */
    public static List<PCMLInputParam> getInputParameters(MessageContext messageContext, SynapseLog log) throws
            AS400PCMLConnectorException {
        List<PCMLInputParam> inputParameters = new ArrayList<>();
        try {
            String strPCMLObjects = (String) ConnectorUtils.lookupTemplateParamater(messageContext, AS400Constants
                    .AS400_PCML_PROGRAM_INPUTS);
            if (null != strPCMLObjects && !strPCMLObjects.isEmpty()) {
                OMElement sObjects = AXIOMUtil.stringToOM(strPCMLObjects);
                if (null != sObjects) {
                    Iterator pcmlObjects = sObjects.getChildElements();
                    while (pcmlObjects.hasNext()) {
                        OMElement pcmlObject = (OMElement) pcmlObjects.next();
                        if (AS400Constants.AS400_PCML_PROGRAM_INPUT.equals(pcmlObject.getLocalName())) {
                            // qualifiedName is a required attribute.
                            String qualifiedName = pcmlObject.getAttributeValue(new QName(AS400Constants
                                    .AS400_PCML_PROGRAM_INPUT_QUALIFIED_NAME));
                            if (null == qualifiedName || qualifiedName.trim().isEmpty()) {
                                log.auditWarn("'" + AS400Constants.AS400_PCML_PROGRAM_INPUT_QUALIFIED_NAME + "' " +
                                              "attribute not found for a " + AS400Constants.AS400_PCML_PROGRAM_INPUT +
                                              ". Hence ignoring this input parameter.");
                                continue;
                            }
                            // indices is not a required attribute.
                            int[] indices = getIndices(pcmlObject.getAttributeValue(new QName(AS400Constants
                                    .AS400_PCML_PROGRAM_INPUT_INDICES)), log);
                            //Fix for the https://wso2.org/jira/browse/ESBCONNECT-216
                            String value;
                            if (pcmlObject.getFirstElement() != null) { // checking whether value is a xml body or not
                                value = pcmlObject.getFirstElement().toString();

                            } else {
                                value = pcmlObject.getText();
                            }
                            inputParameters.add(new PCMLInputParam(qualifiedName, indices, value));
                        } else {
                            log.auditWarn("Invalid element found when parsing children of '" +
                                          AS400Constants.AS400_PCML_PROGRAM_INPUTS +
                                          "'. Input parameters must have start with '" +
                                          AS400Constants.AS400_PCML_PROGRAM_INPUT +
                                          "' elements. But found '" + pcmlObject.getLocalName() +
                                          "'. Hence ignoring this input parameter.");
                        }
                    }
                } else if (log.isTraceOrDebugEnabled()) {
                    log.traceOrDebug("Input payload found but no input parameters found.");
                }
            }
        } catch (XMLStreamException e) {
            throw new AS400PCMLConnectorException("Unable to convert the input payload to an XML.", e);
        }

        if (log.isTraceOrDebugEnabled()) {
            log.traceOrDebug("Input parameters found: " + inputParameters);
        }

        return inputParameters;
    }

    /**
     * Gets the indices from a string separated by commas.
     *
     * @param indicesAsString The string with indices.
     * @param logger          The logger object for logging.
     * @return An array of indices.
     */
    private static int[] getIndices(String indicesAsString, SynapseLog logger) {
        int[] indices = null;
        try {
            if (null != indicesAsString) {
                String[] split = indicesAsString.split(",");
                indices = new int[split.length];
                for (int i = 0; i < split.length; i++) {
                    indices[i] = Integer.parseInt(split[i].trim());
                }
            }
        } catch (NumberFormatException e) {
            logger.auditWarn("Invalid content found for indices. Make sure that indices attribute consists of " +
                             "integers separated by commas. Found '" + indicesAsString + "'.");
            indices = null;
        }

        return indices;
    }

    /**
     * Removing the existing soap body and adding an payload to the soap body.
     *
     * @param messageContext The message context.
     * @param element        New payload body.
     */
    public static void preparePayload(MessageContext messageContext, OMElement element) {
        SOAPBody soapBody = messageContext.getEnvelope().getBody();
        for (Iterator itr = soapBody.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            child.detach();
        }
        for (Iterator itr = element.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            soapBody.addChild(child);
        }
    }

    /**
     * Preparing payload for an exception.
     *
     * @param messageContext   The message context.
     * @param exceptionMessage The exception message.
     */
    public static void preparePayload(MessageContext messageContext, String exceptionMessage) {
        OMElement omElement = OM_FACTORY.createOMElement("error", OM_NAMESPACE);
        OMElement subValue = OM_FACTORY.createOMElement("errorMessage", OM_NAMESPACE);
        subValue.addChild(OM_FACTORY.createOMText(omElement, exceptionMessage));
        omElement.addChild(subValue);
        preparePayload(messageContext, omElement);
    }

    /**
     * Handling exceptions by adding exception content to message context and setting it as a payload to soap body.
     *
     * @param errorMessage   Error message prefix.
     * @param exception      The occurred exception.
     * @param errorCode      The error code for the exception.
     * @param messageContext The message context.
     */
    public static void setExceptionToPayload(String errorMessage, Exception exception, String errorCode,
                                             MessageContext messageContext) {
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, exception);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage + exception.getMessage());
        messageContext.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        messageContext.setProperty(SynapseConstants.ERROR_DETAIL, ExceptionUtils.getStackTrace(exception));

        preparePayload(messageContext, errorMessage + exception.getMessage());
    }
}
