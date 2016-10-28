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

import com.ibm.as400.access.Trace;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.io.IOException;

/**
 * A connector component that enables/disables different log levels in AS400 communication.
 */
public class AS400Trace extends AbstractConnector {

    /**
     * {@inheritDoc} <p> Updated different levels of logs in AS400 server communication. </p>
     */
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        SynapseLog log = getLog(messageContext);
        try {
            String logFilePath = AS400Constants.AS400_DEFAULT_LOG_PATH;
            if (null != Trace.getFileName() && !logFilePath.equals(Trace.getFileName())) {
                log.auditWarn("Changing AS400 logs path to : " + logFilePath);
            } else {
                Trace.setFileName(logFilePath);
            }

            Object conversionLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_CONVERSION);
            if (null != conversionLevel) {
                Trace.setTraceConversionOn(Boolean.parseBoolean((String) conversionLevel));
            }

            Object datastreamLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_DATASTREAM);
            if (null != datastreamLevel) {
                Trace.setTraceDatastreamOn(Boolean.parseBoolean((String) datastreamLevel));
            }

            Object diagnosticsLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_DIAGNOSTICS);
            if (null != diagnosticsLevel) {
                Trace.setTraceDiagnosticOn(Boolean.parseBoolean((String) diagnosticsLevel));
            }

            Object errorLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_ERROR);
            if (null != errorLevel) {
                Trace.setTraceErrorOn(Boolean.parseBoolean((String) errorLevel));
            }

            Object informationLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_INFORMATION);
            if (null != informationLevel) {
                Trace.setTraceInformationOn(Boolean.parseBoolean((String) informationLevel));
            }

            Object pcmlLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_PCML);
            if (null != pcmlLevel) {
                Trace.setTracePCMLOn(Boolean.parseBoolean((String) pcmlLevel));
            }

            Object warningLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_WARNING);
            if (null != warningLevel) {
                Trace.setTraceWarningOn(Boolean.parseBoolean((String) warningLevel));
            }

            Object proxyLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_PROXY);
            if (null != proxyLevel) {
                Trace.setTraceProxyOn(Boolean.parseBoolean((String) proxyLevel));
            }

            Object allLevel = getParameter(messageContext, AS400Constants.AS400_TRACE_ALL);
            if (null != allLevel) {
                Trace.setTraceConversionOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceDatastreamOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceDiagnosticOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceErrorOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceInformationOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTracePCMLOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceWarningOn(Boolean.parseBoolean((String) allLevel));
                Trace.setTraceProxyOn(Boolean.parseBoolean((String) allLevel));
            }

            Trace.setTraceOn(true);

            // Logging for debugging.
            if (log.isTraceOrDebugEnabled()) {
                log.traceOrDebug("Log level Conversion : " + Trace.isTraceConversionOn());
                log.traceOrDebug("Log level Datastream : " + Trace.isTraceDatastreamOn());
                log.traceOrDebug("Log level Diagnostics : " + Trace.isTraceDiagnosticOn());
                log.traceOrDebug("Log level Error : " + Trace.isTraceErrorOn());
                log.traceOrDebug("Log level Information : " + Trace.isTraceInformationOn());
                log.traceOrDebug("Log level PCML : " + Trace.isTracePCMLOn());
                log.traceOrDebug("Log level Warning : " + Trace.isTraceWarningOn());
                log.traceOrDebug("Log level Proxy : " + Trace.isTraceProxyOn());
            }
        } catch (IOException ioException) {
            String errorMessage = "Error occurred when setting logging file path: ";
            AS400Utils.setExceptionToPayload(errorMessage, ioException, "400", messageContext);
            handleException(errorMessage + ioException.getMessage(), ioException, messageContext);
        } catch (Exception exception) {
            String errorMessage = "Error occurred when setting trace properties: ";
            AS400Utils.setExceptionToPayload(errorMessage, exception, "499", messageContext);
            handleException(errorMessage + exception.getMessage(), exception, messageContext);
        }
    }
}
