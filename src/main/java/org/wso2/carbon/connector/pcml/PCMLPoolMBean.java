package org.wso2.carbon.connector.pcml;

public interface PCMLPoolMBean {

    int	getMaxConnections();
    long getMaxInactivity();
    long getMaxLifetime();
    int getMaxUseCount();
    long getMaxUseTime();
    boolean isRunMaintenance();
    boolean isThreadUsed();
    long getCleanupInterval();
    boolean isPretestConnections();

}