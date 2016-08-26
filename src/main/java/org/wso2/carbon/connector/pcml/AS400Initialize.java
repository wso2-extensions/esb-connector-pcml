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
package org.wso2.carbon.connector.pcml;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.SocketProperties;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.beans.PropertyVetoException;
import java.io.IOException;

/**
 * Creates AS400 instance for PCML connector. Authenticates if user ID and password are provided.
 */
public class AS400Initialize extends AbstractConnector {

    /**
     * {@inheritDoc}
     * <p>
     *     Creates an AS400 instance and store it in the message context as {@link AS400Constants#AS400_INSTANCE}
     *     property. Authentication occurs only when user ID and password are provided.
     * </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        AS400 as400 = null;
        String systemName = "";
        String userID = "";
        String password = "";
        String proxy = "";
        try {
            // Get properties that are required for logging in.
            Object systemNameParameter = getParameter(messageContext, AS400Constants.AS400_INIT_SYSTEM_NAME);
            if (null != systemNameParameter) {
                systemName = (String)systemNameParameter;
            }

            Object userIDParameter = getParameter(messageContext, AS400Constants.AS400_INIT_USER_ID);
            if (null != userIDParameter) {
                userID = (String) userIDParameter;
            }

            Object passwordProperty = getParameter(messageContext, AS400Constants.AS400_INIT_PASSWORD);
            if (null != passwordProperty) {
                password = (String) passwordProperty;
            }

            Object proxyParameter = getParameter(messageContext, AS400Constants.AS400_INIT_PROXY);
            if (null != proxyParameter) {
                proxy = (String) proxyParameter;
            }

            log.auditLog("Creating an AS400 Instance.");

            // Initializing as400 instance.
            as400 = new AS400(systemName, userID, password, proxy);

            // Disabling GUI feature.
            as400.setGuiAvailable(false);

            // Setting socket properties to the as400 instance if socket properties are available.
            Object socketProperties = messageContext.getProperty(AS400Constants.AS400_SOCKET_PROPERTIES);
            if (null != socketProperties) {
                as400.setSocketProperties((SocketProperties)socketProperties);
            }

            // Authenticating user if user ID and password are set.
            if (!userID.isEmpty() && !password.isEmpty()) {
                log.auditLog("Authenticating with AS400 server...");
                as400.authenticate(userID, password);
                log.auditLog("Authentication success...");
            }

        } catch (AS400SecurityException as400SecurityException){
            // When a security or authority error occurs
            log.error(as400SecurityException);
            AS400Utils.handleException(as400SecurityException, "200", messageContext);
            throw new SynapseException(as400SecurityException);
        } catch (IOException ioException){
            // Error occurs when communicating to the AS400 server
            log.error(ioException);
            AS400Utils.handleException(ioException, "201", messageContext);
            throw new SynapseException(ioException);
        } catch (PropertyVetoException guiDisableException) {
            // Unable to disable GUI mode of JTOpen library for authenticating
            log.error(guiDisableException);
            AS400Utils.handleException(guiDisableException, "202", messageContext);
            throw new SynapseException(guiDisableException);
        } catch(Exception exception) {
            // Exception occurred while initializing the AS400 instance
            log.error(exception);
            AS400Utils.handleException(exception, "299", messageContext);
            throw new SynapseException(exception);
        } finally {
            // Adding the as400 object to message context.
            messageContext.setProperty(AS400Constants.AS400_INSTANCE, as400);
        }
    }
}