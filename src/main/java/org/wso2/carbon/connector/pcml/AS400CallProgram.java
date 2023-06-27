/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.connector.pcml;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.ExtendedIllegalArgumentException;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;
import com.ibm.as400.data.XmlException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;
import org.wso2.micro.integrator.registry.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;

/**
 * A connector component that calls an AS400 program using PCML.
 */
public class AS400CallProgram extends AbstractConnector {

    /**
     * {@inheritDoc} <p> Calls a program in the AS400 server using PCML. The input parameters are taken through the soap
     * body of the message context. </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        AS400 as400 = null;
        InputStream pcmlFileContent = null;
        try {
            Object as400InstanceProperty = messageContext.getProperty(AS400Constants.AS400_INSTANCE);
            if (null != as400InstanceProperty) {
                as400 = (AS400) as400InstanceProperty;
            } else {
                throw new AS400PCMLConnectorException("Unable to find an AS400 instance to call program. Use the " +
                                                      "'init' mediator to create an AS400 instance.");
            }

            Object pcmlFileLocationParameter = getParameter(messageContext, AS400Constants.AS400_PCML_FILE_LOCATION);
            if (null == pcmlFileLocationParameter) {
                throw new AS400PCMLConnectorException("A PCML file name could not be found as a parameter to call a "
                                                      + "program. Make sure the registry path is correct.");
            }

            // Get PCML source file name
            String pcmlFileLocation = (String) pcmlFileLocationParameter;

            Object pcmlProgramNameParameter = getParameter(messageContext, AS400Constants.AS400_PCML_PROGRAM_NAME);
            if (null == pcmlProgramNameParameter) {
                throw new AS400PCMLConnectorException("A program name was not specified to call.");
            }

            // Get program name to call
            String programName = (String) pcmlProgramNameParameter;

            // Create program document by getting the PCML file from registry.
            MicroIntegratorRegistry registry = (MicroIntegratorRegistry) messageContext.getConfiguration().getRegistry();
            Resource pcmlFileResource = registry.getResource(pcmlFileLocation);
            pcmlFileContent = pcmlFileResource.getContentStream();

            ProgramCallDocument pcmlDocument = new ProgramCallDocument(as400, FilenameUtils.getBaseName
                    (pcmlFileLocation), pcmlFileContent, null, null, getFileType(FilenameUtils.getExtension
                    (pcmlFileLocation)));

            // Get input parameters to pass to the PCML document
            List<PCMLInputParam> inputParams = AS400Utils.getInputParameters(messageContext, log);
            // Apply input parameters
            if (!inputParams.isEmpty()) {
                for (PCMLInputParam inputParam : inputParams) {
                    if (null == inputParam.getIndices()) {
                        pcmlDocument.setValue(inputParam.getQualifiedName(), inputParam.getValue());
                    } else {
                        pcmlDocument.setValue(inputParam.getQualifiedName(), inputParam.getIndices(), inputParam
                                .getValue());
                    }
                }
            }

            // Call the AS400 program
            boolean success = pcmlDocument.callProgram(programName);
            if (!success) {
                // When the call is unsuccessful, throw an exception with the list of messages received from AS400
                // server.

                AS400Message[] messages = pcmlDocument.getMessageList(programName);

                OMElement as400MessagesElement = AS400Utils.OM_FACTORY.createOMElement("as400Messages", AS400Utils
                        .OM_NAMESPACE);

                for (AS400Message message : messages) {
                    OMElement messageIDElement = AS400Utils.OM_FACTORY.createOMElement("id", AS400Utils.OM_NAMESPACE);
                    messageIDElement.setText(message.getID());

                    OMElement messageContentElement = AS400Utils.OM_FACTORY.createOMElement("message", AS400Utils
                            .OM_NAMESPACE);
                    messageContentElement.setText(message.getText());

                    OMElement as400MessageElement = AS400Utils.OM_FACTORY.createOMElement("as400Message", AS400Utils
                            .OM_NAMESPACE);
                    as400MessageElement.addChild(messageIDElement);
                    as400MessageElement.addChild(messageContentElement);

                    as400MessagesElement.addChild(as400MessageElement);
                }

                // Adding AS400 messages content to soap body and considering it as an exception.
                AS400Utils.preparePayload(messageContext, as400MessagesElement);
                messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, "Calling program '" + programName +
                                                                           "' was not successful.");
                messageContext.setProperty(SynapseConstants.ERROR_CODE, "206");
                messageContext.setProperty(SynapseConstants.ERROR_DETAIL, Arrays.toString(messages));
                handleException("Calling program '" + programName + "' was not successful.", messageContext);
            } else {
                log.auditLog("Calling program '" + programName + "' in file '" + pcmlFileLocation + "' is successful.");
                // Generate the XPCML document which consists of all input and output data.
                // ByteArrayOutputStream are note required to be closed.
                ByteArrayOutputStream xpcmlOutputStream = new ByteArrayOutputStream();
                pcmlDocument.generateXPCML(programName, xpcmlOutputStream);
                OMElement omElement = AXIOMUtil.stringToOM(cleanText(xpcmlOutputStream.toString(
                        StandardCharsets.UTF_8.toString())));

                // Adding output content to soap body
                AS400Utils.preparePayload(messageContext, omElement);
            }
        } catch (PcmlException pcmlException) {
            String errorMessage = "Unable to connect to AS400 server: ";
            AS400Utils.setExceptionToPayload(errorMessage, pcmlException, "200", messageContext);
            handleException(errorMessage + pcmlException.getMessage(), pcmlException, messageContext);
        } catch (AS400PCMLConnectorException connectorException) {
            String errorMessage = "Error occurred while processing message context. May occur due to invalid data: ";
            AS400Utils.setExceptionToPayload(errorMessage, connectorException, "201", messageContext);
            handleException(errorMessage + connectorException.getMessage(), connectorException, messageContext);
        } catch (XmlException xmlException) {
            String errorMessage = "Error occurred while processing the output payload: ";
            AS400Utils.setExceptionToPayload(errorMessage, xmlException, "202", messageContext);
            handleException(errorMessage + xmlException.getMessage(), xmlException, messageContext);
        } catch (IOException ioException) {
            String errorMessage = "Error occurred while writing data to output payload: ";
            AS400Utils.setExceptionToPayload(errorMessage, ioException, "203", messageContext);
            handleException(errorMessage + ioException.getMessage(), ioException, messageContext);
        } catch (XMLStreamException xmlStreamException) {
            String errorMessage = "Error converting XPCML to payload: ";
            AS400Utils.setExceptionToPayload(errorMessage, xmlStreamException, "204", messageContext);
            handleException(errorMessage + xmlStreamException.getMessage(), xmlStreamException, messageContext);
        } catch (ExtendedIllegalArgumentException illegalArgException) {
            String errorMessage = "Invalid arguments are passed to the input parameters: ";
            AS400Utils.setExceptionToPayload(errorMessage, illegalArgException, "205", messageContext);
            handleException(errorMessage + illegalArgException.getMessage(), illegalArgException, messageContext);
        } catch (Exception exception) {
            String errorMessage = "Error occurred while calling the AS400 program: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "299", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        } finally {
            try {
                if (null != pcmlFileContent) {
                    pcmlFileContent.close();
                }
            } catch (IOException exception) {
                String errorMessage = "Error occurred while closing PCML file stream from registry: ";
                AS400Utils.setExceptionToPayload(errorMessage, exception, "299", messageContext);
                handleException(errorMessage + exception.getMessage(), exception, messageContext);
            }
        }
    }

    /**
     * Gets the PCML source file type using file extension. Serialized PCML files are not supported. <p> Extensions with
     * "pcml" or "pcmlsrc" are considered as PCML documents. </p> <p> Extensions with "xpcml" or "xpcmlsrc" are
     * considered as XPCML documents. </p>
     *
     * @param extension The extension.
     * @return The PCML source file type.
     */
    private int getFileType(String extension) throws AS400PCMLConnectorException {
        switch (extension.toLowerCase()) {
            case "pcml":
            case "pcmlsrc":
                return ProgramCallDocument.SOURCE_PCML;
            case "xpcml":
            case "xpcmlsrc":
                return ProgramCallDocument.SOURCE_XPCML;
        }

        throw new AS400PCMLConnectorException("Unsupported extension found for program calling. Following extensions " +
                                              "" + "are supported : pcml, pcmlsrc, xpcml, xpcmlsrc.");
    }

    /**
     * Removes all non-ASCII, ASCII control and non-printable characters from a string.
     *
     * @param text Text content.
     * @return Cleaned text.
     */
    private String cleanText(String text) {
        // Remove non-ASCII characters
        text = text.replaceAll("[^\\x00-\\xFF]", "");

        // Remove ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Remove non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }
}
