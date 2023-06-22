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

import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.AS400;

public class PCMLPool implements PCMLPoolMBean {

    AS400ConnectionPool as400ConnectionPool;
    AS400 as400;
    int maxConnections;
    long maxInactivity;
    int maxLifetime;
    int maxUseCount;
    int maxUseTime;
    boolean isRunMaintenance;
    boolean isThreadUsed;
    int cleanupInterval;
    boolean pretestConnections;
    int activeConnectionCount;


    public PCMLPool(AS400ConnectionPool connectionPool, AS400 as400) {

        this.as400ConnectionPool = connectionPool;
        this.as400 = as400;
    }

    @Override
    public int getMaxConnections() {
        return this.getAs400ConnectionPool().getMaxConnections();
    }

    public AS400ConnectionPool getAs400ConnectionPool() {
        return as400ConnectionPool;
    }

    public long getMaxInactivity() {
        return this.getAs400ConnectionPool().getMaxInactivity();
    }

    public long getMaxLifetime() {
        return this.getAs400ConnectionPool().getMaxLifetime();
    }

    public int getMaxUseCount() {
        return this.getAs400ConnectionPool().getMaxUseCount();
    }

    public long getMaxUseTime() {
        return this.getAs400ConnectionPool().getMaxUseTime();
    }

    public boolean isRunMaintenance() {
        return this.getAs400ConnectionPool().isRunMaintenance();
    }

    public boolean isThreadUsed() {
        return this.getAs400ConnectionPool().isThreadUsed();
    }

    public long getCleanupInterval() {
        return this.getAs400ConnectionPool().getCleanupInterval();
    }

    public boolean isPretestConnections() {
        return this.getAs400ConnectionPool().isPretestConnections();
    }

    @Override
    public int getActiveConnectionCount() {
        return this.getAs400ConnectionPool().getActiveConnectionCount(as400.getSystemName(), as400.getUserId());
    }

}
