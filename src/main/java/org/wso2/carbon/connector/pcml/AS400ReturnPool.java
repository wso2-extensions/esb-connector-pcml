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
     * {@inheritDoc}
     * <p>
     *     Returns the AS400 connection back into the connection pool.
     * </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        try {
            Object poolNameParameter = getParameter(messageContext, AS400Constants.AS400_CONNECTION_POOL_NAME);
            if (null != poolNameParameter) {
                // Pool name taken from synapse.
                String poolName = (String) poolNameParameter;
                Object connectionPoolMapProperty = messageContext.getProperty(AS400Constants.AS400_CONNECTION_POOL_MAP);
                if (null != connectionPoolMapProperty) {
                    Map<String, AS400ConnectionPool> connectionPoolMap =
                                                        (Map<String, AS400ConnectionPool>) connectionPoolMapProperty;
                    AS400ConnectionPool as400ConnectionPool = connectionPoolMap.get(poolName);
                    if (null != as400ConnectionPool) {
                        Object as400InstanceProperty = messageContext.getProperty(AS400Constants.AS400_INSTANCE);
                        if (null != as400InstanceProperty) {
                            log.auditLog("Returning AS400 connection to connection pool : " + poolName);
                            as400ConnectionPool.removeFromPool((AS400) as400InstanceProperty);
                        } else {
                            // An AS400 instance could not be found in the mediation flow to be returned to the pool.
                            throw new AS400PCMLConnectorException("Unable to find an AS400 instance in the mediation " +
                                    "flow. Make sure that an AS400 instance was created earlier using 'init'.");
                        }
                    } else {
                        // Unable to find a connection pool with the given name.
                        throw new AS400PCMLConnectorException("Unable to find a connection pool with the given pool " +
                                "name : " + poolName);
                    }
                } else {
                    // The static connection pool map is empty.
                    throw new AS400PCMLConnectorException("Unable to find a connection pool in the message context. " +
                            "Make sure that 'pool' mediator was called earlier.");
                }
            } else {
                // Throwing an error when pool name does not exists.
                throw new AS400PCMLConnectorException("Pool name does not exist to return the AS400 connection back " +
                        "into the pool.");
            }
        } catch (AS400PCMLConnectorException connectorException) {
            // Error occurred in the synapse. Probably due to invalid data.
            this.log.error(connectorException);
            AS400Utils.handleException(connectorException, "600", messageContext);
        } catch (Exception exception) {
            // Unable to find pool name to create a connection pool.
            this.log.error(exception);
            AS400Utils.handleException(exception, "699", messageContext);
        }
    }
}
