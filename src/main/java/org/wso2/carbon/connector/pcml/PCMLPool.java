package org.wso2.carbon.connector.pcml;

import com.ibm.as400.access.AS400ConnectionPool;

public class PCMLPool implements PCMLPoolMBean {

    AS400ConnectionPool as400ConnectionPool;
    int maxConnections;
    long maxInactivity;
    int maxLifetime;
    int maxUseCount;
    int maxUseTime;
    boolean isRunMaintenance;
    boolean isThreadUsed;
    int cleanupInterval;
    boolean pretestConnections;


    public PCMLPool(AS400ConnectionPool connectionPool) {
        this.as400ConnectionPool = connectionPool;
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

}
