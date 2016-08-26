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

import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;

/**
 * Constants for the connector.
 */
public class AS400Constants {
    // Properties stored in the message context.
    public static final String AS400_INSTANCE = "as400Instance";
    public static final String AS400_SOCKET_PROPERTIES = "as400SocketProperties";
    public static final String AS400_CONNECTION_POOL_MAP = "as400ConnectionPoolMap";
    // Parameters of the initializing mediator.
    public static final String AS400_INIT_SYSTEM_NAME = "systemName";
    public static final String AS400_INIT_USER_ID = "userID";
    public static final String AS400_INIT_PASSWORD = "password";
    public static final String AS400_INIT_PROXY = "proxy";
    public static final String AS400_INIT_POOL_NAME = "pool";
    // Parameters of the call program mediator.
    public static final String AS400_PCML_FILE_NAME = "pcmlFileName";
    public static final String AS400_PCML_PROGRAM_NAME = "programName";
    public static final String AS400_PCML_PROGRAM_INPUTS = "pcmlInputs";
    // Payload elements and attributes of the input parameters.
    public static final String AS400_PCML_PROGRAM_INPUT = "pcmlInput";
    public static final String AS400_PCML_PROGRAM_INPUT_QUALIFIED_NAME = "qualifiedName";
    public static final String AS400_PCML_PROGRAM_INPUT_INDICES = "indices";
    // Parameters for socket properties of the AS400 connection.
    public static final String AS400_SOCKET_PROPERTY_KEEP_ALIVE = "keepAlive";
    public static final String AS400_SOCKET_PROPERTY_RECEIVE_BUFFER_SIZE = "receiveBufferSize";
    public static final String AS400_SOCKET_PROPERTY_LOGIN_TIMEOUT = "loginTimeout";
    public static final String AS400_SOCKET_PROPERTY_SEND_BUFFER_SIZE = "sendBufferSize";
    public static final String AS400_SOCKET_PROPERTY_SOCKET_LINGER = "soLinger";
    public static final String AS400_SOCKET_PROPERTY_SOCKET_TIMEOUT = "soTimeout";
    public static final String AS400_SOCKET_PROPERTY_TCP_NO_DELAY = "tcpNoDelay";
    // Parameters for AS400 tracing.
    public static final String AS400_TRACE_CONVERSION = "conversion";
    public static final String AS400_TRACE_DATASTREAM = "datastream";
    public static final String AS400_TRACE_DIAGNOSTICS = "diagnostics";
    public static final String AS400_TRACE_ERROR = "error";
    public static final String AS400_TRACE_INFORMATION = "information";
    public static final String AS400_TRACE_PCML = "pcml";
    public static final String AS400_TRACE_WARNING = "warning";
    public static final String AS400_TRACE_PROXY = "proxy";
    public static final String AS400_TRACE_ALL = "all";
    // Parameters for AS400 connection pool.
    public static final String AS400_CONNECTION_POOL_NAME = "poolName";
    public static final String AS400_CONNECTION_POOL_MAX_CONNECTIONS = "maxConnections";
    public static final String AS400_CONNECTION_POOL_MAX_INACTIVITY = "maxInactivity";
    public static final String AS400_CONNECTION_POOL_MAX_LIFETIME = "maxLifetime";
    public static final String AS400_CONNECTION_POOL_MAX_USE_COUNT = "maxUseCount";
    public static final String AS400_CONNECTION_POOL_MAX_USE_TIME = "maxUseTime";
    public static final String AS400_CONNECTION_POOL_RUN_MAINTENANCE = "runMaintenance";
    public static final String AS400_CONNECTION_POOL_THREAD_USED = "threadUsed";
    public static final String AS400_CONNECTION_POOL_CLEANUP_INTERVAL = "cleanupInterval";
    // Default log file path.
    public static final String AS400_DEFAULT_LOG_PATH = CarbonUtils.getCarbonLogsPath() + File.separator +
                                                                                            "pcml-connector-logs.log";
}
