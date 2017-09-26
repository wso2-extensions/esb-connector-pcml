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
import com.ibm.as400.access.AS400ConnectionPool;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.util.Map;

/**
 * This connector component allows to return an AS400 connection back into the connection pool.
 */
public class AS400ReturnPool extends AbstractConnector {

    /**
     * {@inheritDoc} <p> Returns the AS400 connection back into the connection pool. </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        try {
            Object poolNameParameter = getParameter(messageContext, AS400Constants.AS400_RETURN_POOL_NAME);
            if (null != poolNameParameter) {
                // Pool name taken from synapse.
                String poolName = (String) poolNameParameter;
                Map<String, AS400ConnectionPool> connectionPoolMap = AS400Initialize.getAS400ConnectionPoolMap();
                AS400ConnectionPool as400ConnectionPool = connectionPoolMap.get(poolName);
                if (null != as400ConnectionPool) {
                    Object as400InstanceProperty = messageContext.getProperty(AS400Constants.AS400_INSTANCE);
                    if (null != as400InstanceProperty) {
                        log.auditLog("Returning AS400 connection to connection pool : " + poolName);
                        as400ConnectionPool.returnConnectionToPool((AS400) as400InstanceProperty);
                    } else {
                        // An AS400 instance could not be found in the mediation flow to be returned to the pool.
                        log.auditWarn("Unable to find an AS400 instance in the mediation flow. Make sure that an " +
                                      "AS400 instance was created earlier using 'init'.");
                    }
                } else {
                    // Unable to find a connection pool with the given name.
                    log.auditWarn("Unable to find a connection pool with the given pool name : " + poolName);
                }
            } else {
                // Throwing an error when pool name does not exists.
                throw new AS400PCMLConnectorException("Unable to find pool name to return the as400 instance.");
            }
        } catch (AS400PCMLConnectorException connectorException) {
            String errorMessage = "Error occurred while processing synapse: ";
            AS400Utils.setExceptionToPayload(errorMessage, connectorException, "300", messageContext);
            handleException(errorMessage + connectorException.getMessage(), connectorException, messageContext);
        } catch (Exception exception) {
            String errorMessage = "Error occurred while return the AS400 connection to the pool: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "399", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        }
    }
}
