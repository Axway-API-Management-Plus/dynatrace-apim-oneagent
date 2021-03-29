//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.axway.oneagent.utils;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import com.vordel.circuit.Message;
import com.vordel.circuit.net.State;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.HTTPProtocol;
import com.vordel.dwe.http.ServerTransaction;
import com.vordel.mime.HeaderSet;
import com.vordel.trace.Trace;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;

public class OneAgentSDKUtils {
    static OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();

    static {
        switch(oneAgentSdk.getCurrentState()) {
        case ACTIVE:
            System.out.println("SDK is active and capturing.");
            break;
        case TEMPORARILY_INACTIVE:
            System.err.println("SDK is TEMPORARY_INACTIVE; OneAgent has been deactivated - check OneAgent configuration.");
            break;
        case PERMANENTLY_INACTIVE:
            System.err.println("SDK is PERMANENT_INACTIVE; Probably no OneAgent injected or OneAgent is incompatible with SDK.");
            break;
        default:
            System.err.println("SDK is in unknown state.");
        }

    }

    public OneAgentSDKUtils() {
    }

    public static void aroundProducer(ProceedingJoinPoint pjp, State state) throws Throwable {
        String host = "host";
        String port = "port";
        Message message = null;
        HeaderSet headers = null;

        try {
            Field hostField = State.class.getDeclaredField("host");
            hostField.setAccessible(true);
            host = (String)hostField.get(state);
            Field portField = State.class.getDeclaredField("port");
            portField.setAccessible(true);
            port = (String)portField.get(state);
            Field headersField = State.class.getDeclaredField("headers");
            headersField.setAccessible(true);
            headers = (HeaderSet)headersField.get(state);
            Field messageField = State.class.getDeclaredField("message");
            messageField.setAccessible(true);
            message = (Message)messageField.get(state);
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        String concatenatedHost = host + ":" + port;
        if (headers != null) {
            OutgoingRemoteCallTracer outgoingRemoteCall = oneAgentSdk.traceOutgoingRemoteCall(getRequestPath(message), "http.axway", host, ChannelType.TCP_IP, concatenatedHost);
            outgoingRemoteCall.setProtocolName("RMI/custom");
            outgoingRemoteCall.start();
            String outgoingTag = outgoingRemoteCall.getDynatraceStringTag();
            headers.setHeader("x-dynaTrace", outgoingTag);

            try {
                if (message != null) {
                    getAttributes(message);
                }

                pjp.proceed();
            } catch (Throwable var11) {
                outgoingRemoteCall.error(var11);
                throw var11;
            }

            outgoingRemoteCall.end();
        }
    }

    public static void aroundConsumer(ProceedingJoinPoint pjp, HTTPProtocol protocol, HTTPProtocol handler, ServerTransaction txn, CorrelationID id, Map<String, Object> loopbackMessage) throws Throwable {
        HeaderSet headers = txn.getHeaders();
        if (headers.hasHeader("x-dynaTrace")) {
            String receivedTag = headers.getHeader("x-dynaTrace");
            IncomingRemoteCallTracer incomingRemoteCall = oneAgentSdk.traceIncomingRemoteCall(txn.getRequestURI(), "http.axway", "endpoint");
            if (receivedTag.startsWith("FW")) {
                incomingRemoteCall.setDynatraceStringTag(receivedTag);
                incomingRemoteCall.setProtocolName("RMI/custom");
                incomingRemoteCall.start();
            } else {
                incomingRemoteCall.setDynatraceStringTag(receivedTag);
                incomingRemoteCall.setProtocolName("RMI/custom");
                incomingRemoteCall.start();
                int NA_index = receivedTag.indexOf("NA=");
                int SN_index = receivedTag.indexOf("SN=");
                int SI_index = receivedTag.indexOf("SI=");
                if (NA_index != -1 && SN_index != -1 && SI_index != -1) {
                    String afterNA = receivedTag.substring(NA_index);
                    int delimiter_index = afterNA.indexOf(59);
                    String NeoLoad_Transaction = receivedTag.substring(NA_index + 3, NA_index + delimiter_index);
                    NeoLoad_Transaction(NeoLoad_Transaction);
                    Trace.info("NeoLoad_Transaction = " + NeoLoad_Transaction);
                    String afterSN = receivedTag.substring(SN_index);
                    delimiter_index = afterSN.indexOf(59);
                    String NeoLoad_UserPath = receivedTag.substring(SN_index + 3, SN_index + delimiter_index);
                    NeoLoad_UserPath(NeoLoad_UserPath);
                    String afterSI = receivedTag.substring(SI_index);
                    delimiter_index = afterSI.indexOf(59);
                    String Neoload_Traffic = receivedTag.substring(SI_index + 3, SI_index + delimiter_index);
                    Neoload_Traffic(Neoload_Traffic);
                }
            }

            try {
                pjp.proceed();
            } catch (Throwable var20) {
                incomingRemoteCall.error(var20);
                throw var20;
            }

            Trace.info("aroundConsumer : after processing request");
            incomingRemoteCall.end();
        } else {
            IncomingRemoteCallTracer incomingRemoteCall = oneAgentSdk.traceIncomingRemoteCall(txn.getRequestURI(), "http.axway", "endpoint");
            incomingRemoteCall.start();

            try {
                pjp.proceed();
            } catch (Throwable var19) {
                incomingRemoteCall.error(var19);
                throw var19;
            }

            incomingRemoteCall.end();
        }

    }

    public static void getAttributes(Message message) throws IOException {
        HeaderSet httpHeaders = (HeaderSet)message.get("http.headers");
        if (httpHeaders != null) {
            String keyId = (String)httpHeaders.get("KeyId");
            if (keyId != null) {
                getKEYID(keyId);
            }

            String clientName = (String)message.get("message.client.name");
            if (clientName != null) {
                getClientName(clientName);
            }

        }
    }

    public static void NeoLoad_Transaction(String value) {
        oneAgentSdk.addCustomRequestAttribute("NeoLoad_Transaction", value);
    }

    public static void NeoLoad_UserPath(String value) {
        oneAgentSdk.addCustomRequestAttribute("NeoLoad_UserPath", value);
    }

    public static void Neoload_Traffic(String value) {
        oneAgentSdk.addCustomRequestAttribute("Neoload_Traffic", value);
    }

    public static String getRequestPath(Message message) {
        return (String)message.get("http.request.path");
    }

    public static void getClientName(String clientName) {
        oneAgentSdk.addCustomRequestAttribute("ClientName", clientName);
    }

    public static void getKEYID(String keyId) {
        oneAgentSdk.addCustomRequestAttribute("KEYID", keyId);
    }
}
