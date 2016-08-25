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
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A connector component that calls an AS400 program using PCML.
 */
public class AS400CallProgram extends AbstractConnector {

    /**
     * {@inheritDoc}
     * <p>
     *     Calls a program in the AS400 server using PCML. The input parameters are taken through the soap body of the
     *     message context.
     * </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        AS400 as400 = null;
        try {
            Object as400InstanceProperty = messageContext.getProperty(AS400Constants.AS400_INSTANCE);
            if (null != as400InstanceProperty) {
                as400 = (AS400) as400InstanceProperty;
            } else {
                throw new AS400PCMLConnectorException("Unable to find an AS400 instance to call program. Use the " +
                                                                    "'init' mediator to create an AS400 instance.");
            }

            Object pcmlFileNameParameter = getParameter(messageContext, AS400Constants.AS400_PCML_FILE_NAME);
            if (null == pcmlFileNameParameter) {
                throw new AS400PCMLConnectorException("A PCML file name could not be found as a parameter to call a " +
                                                                                                            "program.");
            }

            // Get PCML source file name
            String pcmlFileName = (String) pcmlFileNameParameter;

            Object pcmlProgramNameParameter = getParameter(messageContext, AS400Constants.AS400_PCML_PROGRAM_NAME);
            if (null == pcmlProgramNameParameter) {
                throw new AS400PCMLConnectorException("A program name was not specified to call.");
            }

            // Get program name to call
            String programName = (String) pcmlProgramNameParameter;

            // Create program document with the given PCML source file
            ProgramCallDocument pcmlDocument = new ProgramCallDocument(as400, pcmlFileName);

            // Get input parameters to pass to the PCML document
            List<PCMLInputParam> inputParams = AS400Utils.getInputParameters(messageContext, log);
            // Apply input parameters
            if (!inputParams.isEmpty()) {
                for (PCMLInputParam inputParam : inputParams) {
                    if (null == inputParam.getIndices()) {
                        pcmlDocument.setValue(inputParam.getQualifiedName(), inputParam.getValue());
                    } else {
                        pcmlDocument.setValue(inputParam.getQualifiedName(), inputParam.getIndices(),
                                                                                                inputParam.getValue());
                    }
                }
            }

            log.auditLog("Calling program '" + programName + "' in file '" + pcmlFileName + "'.");

            // Call the AS400 program
            boolean success = pcmlDocument.callProgram(programName);
            if (!success) {
                // When the call is unsuccessful, throw an exception with the list of messages received from AS400
                // server.
                AS400Message[] msgs = pcmlDocument.getMessageList(programName);
                StringBuffer errorMessage = new StringBuffer();
                for (AS400Message message : msgs) {
                    errorMessage = errorMessage
                            .append(message.getID())
                            .append(" - ")
                            .append(message.getText())
                            .append(System.lineSeparator());
                }
                throw new AS400PCMLConnectorException("Calling program '" + programName +
                                                                                        "' was not successful.", msgs);
            } else {
                log.auditLog("Calling program '" + programName + "' is successful.");
                // Generate the XPCML document which consists of all input and output data
                ByteArrayOutputStream xpcmlOutputStream = new ByteArrayOutputStream();
                pcmlDocument.generateXPCML(programName, xpcmlOutputStream);
                OMElement omElement = AXIOMUtil.stringToOM(xpcmlOutputStream.toString(
                                                                                    StandardCharsets.UTF_8.toString()));

                // Adding output content to soap body
                messageContext.getEnvelope().getBody().addChild(omElement);
            }
        } catch (PcmlException pcmlException) {
            // Unable to connect to AS400 server
            log.error(pcmlException);
            AS400Utils.handleException(pcmlException, "300", messageContext);
            throw new SynapseException(pcmlException);
        } catch (AS400PCMLConnectorException pcmlException) {
            // Error occurred while processing message context. May occur due to invalid data.
            log.error(pcmlException);
            AS400Utils.handleException(pcmlException, "301", messageContext);
            throw new SynapseException(pcmlException);
        } catch (XmlException xmlException) {
            // Error occurred while processing the output payload
            log.error(xmlException);
            AS400Utils.handleException(xmlException, "302", messageContext);
            throw new SynapseException(xmlException);
        } catch (IOException ioException) {
            // Error occurred while writing data to output payload
            log.error(ioException);
            AS400Utils.handleException(ioException, "303", messageContext);
            throw new SynapseException(ioException);
        } catch (XMLStreamException xmlStreamException) {
            // Error converting XPCML to payload
            log.error(xmlStreamException);
            AS400Utils.handleException(xmlStreamException, "304", messageContext);
            throw new SynapseException(xmlStreamException);
        } catch (ExtendedIllegalArgumentException extendedIllegalArgumentException) {
            // Invalid arguments are passed to the input parameters
            log.error(extendedIllegalArgumentException);
            AS400Utils.handleException(extendedIllegalArgumentException, "305", messageContext);
            throw new SynapseException(extendedIllegalArgumentException);
        } catch (Exception exception) {
            // Error occurred while calling the AS400 program
            log.error(exception);
            AS400Utils.handleException(exception, "399", messageContext);
            throw new SynapseException(exception);
        } finally {
            if (null != as400 && as400.isConnected()) {
                log.auditLog("Disconnecting from all AS400 services.");
                as400.disconnectAllServices();
            }
        }
    }
}
