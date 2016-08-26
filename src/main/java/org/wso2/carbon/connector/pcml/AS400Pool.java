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

import com.ibm.as400.access.AS400ConnectionPool;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.util.HashMap;
import java.util.Map;

/**
 * The following is class is invoked when creating an AS400 connection pool. A connection pool will be created in
 */
public class AS400Pool extends AbstractConnector {
    /**
     * A static map which stores {@link AS400ConnectionPool}s against a pool name. There should be only one map per ESB
     * node. The connections are stored in the memory.
     */
    private static Map<String, AS400ConnectionPool> as400ConnectionPoolMap;

    /**
     * {@inheritDoc}
     * <p>
     *  Creates an {@link AS400ConnectionPool} for a given name. The 'init' method can use the pool name to get AS400
     *  connections. The connection pool will be stored in {@link #as400ConnectionPoolMap} against a pool name.
     * </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);

        try {
            Object poolNameParameter = getParameter(messageContext, AS400Constants.AS400_CONNECTION_POOL_NAME);
            if (null != poolNameParameter) {

                // Create connection pool map only if it does not exist.
                if (null == as400ConnectionPoolMap) {
                    as400ConnectionPoolMap = new HashMap<>();
                }

                // Pool name taken from synapse.
                String poolName = (String) poolNameParameter;

                // Creating new pool if a pool does not exists with the given pool name.
                if (null == as400ConnectionPoolMap.get(poolName)) {

                    AS400ConnectionPool as400ConnectionPool = new AS400ConnectionPool();

                    // Setting properties to the pool.
                    Object maxConnectionsParameter = getParameter(messageContext,
                                                                AS400Constants.AS400_CONNECTION_POOL_MAX_CONNECTIONS);
                    if (null != maxConnectionsParameter) {
                        String maxConnections = (String) maxConnectionsParameter;
                        as400ConnectionPool.setMaxConnections(Integer.parseInt(maxConnections));
                    }

                    Object maxInactivityParameter = getParameter(messageContext,
                                                                AS400Constants.AS400_CONNECTION_POOL_MAX_INACTIVITY);
                    if (null != maxInactivityParameter) {
                        String maxInactivity = (String) maxInactivityParameter;
                        as400ConnectionPool.setMaxInactivity(Long.parseLong(maxInactivity));
                    }

                    Object maxLifetimeParameter = getParameter(messageContext,
                                                                    AS400Constants.AS400_CONNECTION_POOL_MAX_LIFETIME);
                    if (null != maxLifetimeParameter) {
                        String maxLifetime = (String) maxLifetimeParameter;
                        as400ConnectionPool.setMaxLifetime(Long.parseLong(maxLifetime));
                    }

                    Object maxUseCountParameter = getParameter(messageContext,
                                                                    AS400Constants.AS400_CONNECTION_POOL_MAX_USE_COUNT);
                    if (null != maxUseCountParameter) {
                        String maxUseCount = (String) maxUseCountParameter;
                        as400ConnectionPool.setMaxUseCount(Integer.parseInt(maxUseCount));
                    }

                    Object maxUseTimeParameter = getParameter(messageContext,
                                                                    AS400Constants.AS400_CONNECTION_POOL_MAX_USE_TIME);
                    if (null != maxUseTimeParameter) {
                        String maxUseTime = (String) maxUseTimeParameter;
                        as400ConnectionPool.setMaxUseTime(Long.parseLong(maxUseTime));
                    }

                    Object runMaintenanceParameter = getParameter(messageContext,
                                                                AS400Constants.AS400_CONNECTION_POOL_RUN_MAINTENANCE);
                    if (null != runMaintenanceParameter) {
                        String runMaintenance = (String) runMaintenanceParameter;
                        as400ConnectionPool.setRunMaintenance(Boolean.parseBoolean(runMaintenance));
                    }

                    Object threadUsedParameter = getParameter(messageContext,
                                                                    AS400Constants.AS400_CONNECTION_POOL_THREAD_USED);
                    if (null != threadUsedParameter) {
                        String threadUsed = (String) threadUsedParameter;
                        as400ConnectionPool.setThreadUsed(Boolean.parseBoolean(threadUsed));
                    }

                    Object cleanupIntervalParameter = getParameter(messageContext,
                                                                AS400Constants.AS400_CONNECTION_POOL_CLEANUP_INTERVAL);
                    if (null != cleanupIntervalParameter) {
                        String cleanupInterval = (String) cleanupIntervalParameter;
                        as400ConnectionPool.setCleanupInterval(Long.parseLong(cleanupInterval));
                    }

                    // Adding the created pool to the static pool map.
                    log.auditLog("Creating AS400 connection pool with name '" + poolName + "'.");
                    as400ConnectionPoolMap.put(poolName, as400ConnectionPool);
                } else {
                    // A pool already exists with the given pool name. Hence pool creation is ignored.
                    log.auditLog("AS400 Connection pool already exists with name '" + poolName + "'. Hence ignoring " +
                                                                                                    "pool creation.");
                }
            } else {
                // Throwing an error when pool name does not exists.
                throw new AS400PCMLConnectorException("Pool name does not exist to create a connection pool.");
            }
        } catch (AS400PCMLConnectorException connectorException) {
            // Unable to find pool name to create a connection pool.
            log.error(connectorException);
            AS400Utils.handleException(connectorException, "500", messageContext);
        } catch (Exception exception) {
            // Error occurred while creating AS400 connection pool.
            log.error(exception);
            AS400Utils.handleException(exception, "599", messageContext);
        } finally {
            if (null != as400ConnectionPoolMap) {
                if (log.isTraceOrDebugEnabled()) {
                    log.auditDebug("AS400 connection pool map : " + as400ConnectionPoolMap);
                }
                // Setting the static connection pool map to the message context so that it can be used by init synapse.
                messageContext.setProperty(AS400Constants.AS400_CONNECTION_POOL_MAP, as400ConnectionPoolMap);
            }
        }
    }
}
