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
import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ConnectionPoolException;
import com.ibm.as400.access.SocketProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import java.lang.management.ManagementFactory;

/**
 * Creates AS400 instance for PCML connector. Authenticates if user ID and password are provided. <p> Allows to set
 * socket properties for the AS400 connection. </p> <p> Supports AS400 connection pools as well. </p>
 */
public class AS400Initialize extends AbstractConnector {
    /**
     * A static map which stores {@link AS400ConnectionPool}s against a pool name. There should be only one map per ESB
     * node. The connections are stored in the memory.
     */
    private static Map<String, AS400ConnectionPool> as400ConnectionPoolMap = new ConcurrentHashMap<>();
    private Boolean isJmxEnabled = false;

    /**
     * {@inheritDoc} <p> Creates an AS400 instance and store it in the message context as {@link
     * AS400Constants#AS400_INSTANCE} property. Authentication occurs only when user ID and password are provided. AS400
     * connection pools can be creating using a {@link AS400ConnectionPool} if they are defined in the synapse. Supports
     * setting {@link SocketProperties} for AS400 connections. </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        AS400 as400 = null;
        String systemName = StringUtils.EMPTY;
        String userID = StringUtils.EMPTY;
        String password = StringUtils.EMPTY;
        try {
            // Get properties that are required for logging in.
            Object systemNameParameter = getParameter(messageContext, AS400Constants.AS400_INIT_SYSTEM_NAME);
            if (null != systemNameParameter) {
                systemName = (String) systemNameParameter;
            }

            Object userIDParameter = getParameter(messageContext, AS400Constants.AS400_INIT_USER_ID);
            if (null != userIDParameter) {
                userID = (String) userIDParameter;
            }

            Object passwordProperty = getParameter(messageContext, AS400Constants.AS400_INIT_PASSWORD);
            if (null != passwordProperty) {
                password = (String) passwordProperty;
            }

            AS400ConnectionPool connectionPool = this.getConnectionPool(messageContext, log);
            // If connection pool exist.
            if (null != connectionPool) {
                connectionPool.setSocketProperties(this.getSocketProperties(messageContext, log));

                log.auditLog("Getting an AS400 connection from connection pool.");

                // Getting the connection from pool.
                as400 = connectionPool.getConnection(systemName, userID, password);
            } else {
                log.auditLog("Creating an AS400 instance.");

                // Initializing as400 instance.
                as400 = new AS400(systemName, userID, password);

                // Updating/Modifying socket properties if they are provided.
                as400.setSocketProperties(this.getSocketProperties(messageContext, log));
            }

            // Disabling GUI feature.
            as400.setGuiAvailable(false);

            // Authenticating user if user ID and password are set.
            if (!userID.isEmpty() && !password.isEmpty()) {
                as400.authenticate(userID, password);
                log.auditLog("Authentication success...");
            }
            String isJmxEnabled = (String) getParameter(messageContext, AS400Constants.AS400_JMX_ENABLED);
            if (isJmxEnabled != null && !isJmxEnabled.isEmpty()) {
                this.isJmxEnabled = Boolean.parseBoolean(isJmxEnabled);
                if(this.isJmxEnabled) {
                    try {
                        ObjectName objectName = new ObjectName("org.wso2.carbon.connector.pcml:type=basic,name=as400");
                        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                        PCMLPoolMBean mbean = new PCMLPool(connectionPool, as400);
                        if(!server.isRegistered(objectName)) {
                            server.registerMBean(mbean, objectName);
                        }
                    } catch (MalformedObjectNameException | InstanceAlreadyExistsException |
                             MBeanRegistrationException | NotCompliantMBeanException e) {
                        log.auditError("Error while adding AS400ConnectionPoll to MBeanServer.");
                    }

                }
            }
        } catch (AS400SecurityException as400SecurityException) {
            String errorMessage = "Security or authorization error occurred: ";
            AS400Utils.setExceptionToPayload(errorMessage, as400SecurityException, "100", messageContext);
            handleException(errorMessage + as400SecurityException.getMessage(), as400SecurityException, messageContext);
        } catch (IOException ioException) {
            String errorMessage = "Error occurs when communicating to the AS400 server: ";
            AS400Utils.setExceptionToPayload(errorMessage, ioException, "101", messageContext);
            handleException(errorMessage + ioException.getMessage(), ioException, messageContext);
        } catch (PropertyVetoException guiDisableException) {
            String errorMessage = "Unable to disable GUI mode of JTOpen library for authenticating: ";
            AS400Utils.setExceptionToPayload(errorMessage, guiDisableException, "102", messageContext);
            handleException(errorMessage + guiDisableException.getMessage(), guiDisableException, messageContext);
        } catch (ConnectionPoolException conPoolException) {
            String errorMessage = "Error occurred while getting a connection from the pool: ";
            AS400Utils.setExceptionToPayload(errorMessage, conPoolException, "103", messageContext);
            handleException(errorMessage + conPoolException.getMessage(), conPoolException, messageContext);
        } catch (Exception exception) {
            String errorMessage = "Exception occurred while initializing the AS400 instance: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "199", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        } finally {
            // Adding the as400 object to message context.
            messageContext.setProperty(AS400Constants.AS400_INSTANCE, as400);
        }
    }

    /**
     * Gets a connection pool from {@link #as400ConnectionPoolMap} with a given pool name. If a connection pool does not
     * exists, then a new pool is created and stores in the {@link #as400ConnectionPoolMap} and then returned.
     *
     * @param messageContext The message context.
     * @param log            The logger object.
     * @return The connection pool.
     */
    private AS400ConnectionPool getConnectionPool(MessageContext messageContext, SynapseLog log) {
        AS400ConnectionPool as400ConnectionPool = null;
        try {
            Object poolNameParameter = getParameter(messageContext, AS400Constants.AS400_CONNECTION_POOL_NAME);
            if (null != poolNameParameter) {
                // Pool name taken from synapse.
                String poolName = (String) poolNameParameter;
                as400ConnectionPool = as400ConnectionPoolMap.get(poolName);

                // Creating new pool if a pool does not exists with the given pool name.
                if (null == as400ConnectionPool) {
                    as400ConnectionPool = new AS400ConnectionPool();

                    // Setting properties to the pool.
                    Object maxConnectionsParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_MAX_CONNECTIONS);
                    if (null != maxConnectionsParameter) {
                        String maxConnections = (String) maxConnectionsParameter;
                        as400ConnectionPool.setMaxConnections(Integer.parseInt(maxConnections));
                    }

                    Object maxInactivityParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_MAX_INACTIVITY);
                    if (null != maxInactivityParameter) {
                        String maxInactivity = (String) maxInactivityParameter;
                        as400ConnectionPool.setMaxInactivity(Long.parseLong(maxInactivity));
                    }

                    Object maxLifetimeParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_MAX_LIFETIME);
                    if (null != maxLifetimeParameter) {
                        String maxLifetime = (String) maxLifetimeParameter;
                        as400ConnectionPool.setMaxLifetime(Long.parseLong(maxLifetime));
                    }

                    Object maxUseCountParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_MAX_USE_COUNT);
                    if (null != maxUseCountParameter) {
                        String maxUseCount = (String) maxUseCountParameter;
                        as400ConnectionPool.setMaxUseCount(Integer.parseInt(maxUseCount));
                    }

                    Object maxUseTimeParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_MAX_USE_TIME);
                    if (null != maxUseTimeParameter) {
                        String maxUseTime = (String) maxUseTimeParameter;
                        as400ConnectionPool.setMaxUseTime(Long.parseLong(maxUseTime));
                    }

                    Object runMaintenanceParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_RUN_MAINTENANCE);
                    if (null != runMaintenanceParameter) {
                        String runMaintenance = (String) runMaintenanceParameter;
                        as400ConnectionPool.setRunMaintenance(Boolean.parseBoolean(runMaintenance));
                    }

                    Object threadUsedParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_THREAD_USED);
                    if (null != threadUsedParameter) {
                        String threadUsed = (String) threadUsedParameter;
                        as400ConnectionPool.setThreadUsed(Boolean.parseBoolean(threadUsed));
                    }

                    Object cleanupIntervalParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_CLEANUP_INTERVAL);
                    if (null != cleanupIntervalParameter) {
                        String cleanupInterval = (String) cleanupIntervalParameter;
                        as400ConnectionPool.setCleanupInterval(Long.parseLong(cleanupInterval));
                    }

                    Object pretestConnectionsParameter = getParameter(messageContext, AS400Constants
                            .AS400_CONNECTION_POOL_PRETEST_CONNECTIONS);
                    if (null != pretestConnectionsParameter) {
                        String pretestConnections = (String) pretestConnectionsParameter;
                        as400ConnectionPool.setPretestConnections(Boolean.parseBoolean(pretestConnections));
                    }

                    // Logging for debugging.
                    if (log.isTraceOrDebugEnabled()) {
                        debugLogPoolProperties(poolName, as400ConnectionPool, log);
                    }

                    // Setting socket properties to the connection pool.
                    as400ConnectionPool.setSocketProperties(this.getSocketProperties(messageContext, log));

                    // Adding the created pool to the static pool map.
                    log.auditLog("Creating AS400 connection pool with name '" + poolName + "'.");
                    as400ConnectionPoolMap.put(poolName, as400ConnectionPool);
                } else {
                    // A pool already exists with the given pool name. Hence pool creation is ignored.

                    // Logging for debugging.
                    if (log.isTraceOrDebugEnabled()) {
                        log.traceOrDebug("AS400 Connection pool already exists with name '" + poolName + "'. Hence " +
                                         "ignoring pool creation.");
                        debugLogPoolProperties(poolName, as400ConnectionPoolMap.get(poolName), log);
                    }
                }
            }
        } catch (Exception exception) {
            String errorMessage = "Error occurred while creating AS400 connection pool: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "199", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        }

        return as400ConnectionPool;
    }

    /**
     * Creates socket properties of a given {@link SocketProperties} object according to synapse syntax.
     *
     * @param messageContext The message context.
     * @param log            The logger object.
     * @return The socket properties object.
     */
    private SocketProperties getSocketProperties(MessageContext messageContext, SynapseLog log) {
        SocketProperties socketProperties = new SocketProperties();
        try {
            // Sets keepalive value
            Object keepAlive = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_KEEP_ALIVE);
            if (null != keepAlive) {
                socketProperties.setKeepAlive(Boolean.parseBoolean((String) keepAlive));
            }

            // Sets login timeout
            Object loginTimeout = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_LOGIN_TIMEOUT);
            if (null != loginTimeout) {
                socketProperties.setLoginTimeout(Integer.parseInt((String) loginTimeout));
            }

            // Sets receive buffer size
            Object receiveBufferSize = getParameter(messageContext, AS400Constants
                    .AS400_SOCKET_PROPERTY_RECEIVE_BUFFER_SIZE);
            if (null != receiveBufferSize) {
                socketProperties.setReceiveBufferSize(Integer.parseInt((String) receiveBufferSize));
            }

            // Sets send buffer size
            Object sendBufferSize = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_SEND_BUFFER_SIZE);
            if (null != sendBufferSize) {
                socketProperties.setSendBufferSize(Integer.parseInt((String) sendBufferSize));
            }

            // Sets socket linger value
            Object socketLinger = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_SOCKET_LINGER);
            if (null != socketLinger) {
                socketProperties.setSoLinger(Integer.parseInt((String) socketLinger));
            }

            // Sets socket timeout value
            Object socketTimeout = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_SOCKET_TIMEOUT);
            if (null != socketTimeout) {
                socketProperties.setSoTimeout(Integer.parseInt((String) socketTimeout));
            }

            // Sets TCP no delay
            Object tcpNoDelay = getParameter(messageContext, AS400Constants.AS400_SOCKET_PROPERTY_TCP_NO_DELAY);
            if (null != tcpNoDelay) {
                socketProperties.setTcpNoDelay(Boolean.parseBoolean((String) tcpNoDelay));
            }

            // Logging for debugging.
            if (log.isTraceOrDebugEnabled()) {
                log.traceOrDebug("Socket - Keep Alive : " + socketProperties.isKeepAlive());
                log.traceOrDebug("Socket - Login Timeout(Milliseconds) : " + socketProperties.getLoginTimeout());
                log.traceOrDebug("Socket - Receive Buffer Size(Bytes) : " + socketProperties.getReceiveBufferSize());
                log.traceOrDebug("Socket - Send Buffer Size(Bytes) : " + socketProperties.getSendBufferSize());
                log.traceOrDebug("Socket - Linger(Seconds) : " + socketProperties.getSoLinger());
                log.traceOrDebug("Socket - Timeout(Milliseconds) : " + socketProperties.getSoTimeout());
                log.traceOrDebug("Socket - tcpNoDelay : " + socketProperties.isTcpNoDelay());
            }

        } catch (Exception exception) {
            String errorMessage = "Error occurred when setting socket properties: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "199", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        }

        return socketProperties;
    }

    /**
     * Logging properties of a {@link AS400ConnectionPool} as debug logs.
     *
     * @param poolName The name of the pool in which its stored against in {@link #as400ConnectionPoolMap}.
     * @param pool     The AS400 connection pool.
     * @param log      The logger for logging.
     */
    private void debugLogPoolProperties(String poolName, AS400ConnectionPool pool, SynapseLog log) {
        // Logging for debugging.
        if (log.isTraceOrDebugEnabled()) {
            log.traceOrDebug("Connection Pool - " + poolName + " - Max Connections : " + pool.getMaxConnections());
            log.traceOrDebug("Connection Pool - " + poolName + " - Max Inactivity : " + pool.getMaxInactivity());
            log.traceOrDebug("Connection Pool - " + poolName + " - Max Lifetime : " + pool.getMaxLifetime());
            log.traceOrDebug("Connection Pool - " + poolName + " - Max Use Count : " + pool.getMaxUseCount());
            log.traceOrDebug("Connection Pool - " + poolName + " - Max Use Time : " + pool.getMaxUseTime());
            log.traceOrDebug("Connection Pool - " + poolName + " - Run Maintenance : " + pool.isRunMaintenance());
            log.traceOrDebug("Connection Pool - " + poolName + " - Thread Used : " + pool.isThreadUsed());
            log.traceOrDebug("Connection Pool - " + poolName + " - Cleanup Interval : " + pool.getCleanupInterval());
        }
    }

    /**
     * Gets the static AS400 connection pool map.
     *
     * @return The connection pool map.
     */
    public static Map<String, AS400ConnectionPool> getAS400ConnectionPoolMap() {
        return as400ConnectionPoolMap;
    }
}