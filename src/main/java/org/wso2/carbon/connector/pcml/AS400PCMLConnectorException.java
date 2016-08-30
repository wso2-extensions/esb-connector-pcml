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

import com.ibm.as400.access.AS400Message;

import java.util.Arrays;

/**
 * Exception class for the connector.
 */
public class AS400PCMLConnectorException extends Exception {

    private static final long serialVersionUID = 3998762751313397184L;
    
    /**
     * A list of messages received when an as400 program call is unsuccessful.
     */
    String as400messages;

    /**
     * Creates exception with a text and exception.
     *
     * @param message The text for the exception message.
     * @param cause   The exception
     */
    public AS400PCMLConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with AS400 messages received by calling a program.
     *
     * @param message       The text message for the exception.
     * @param as400messages The list of messages created by the calling the server.
     */
    public AS400PCMLConnectorException(String message, AS400Message[] as400messages) {
        super(message);
        this.as400messages = Arrays.toString(as400messages);
    }

    /**
     * Creates an exception with a message.
     *
     * @param message The message content.
     */
    public AS400PCMLConnectorException(String message) {
        super(message);
    }

    public String getAS400messages() {
        return as400messages;
    }

    public void setAS400messages(AS400Message[] as400messages) {
        this.as400messages = Arrays.toString(as400messages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AS400PCMLConnectorException{" +
                "as400messages='" + as400messages + '\'' +
                '}';
    }
}
