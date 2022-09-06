package com.axway.oneagent.utils;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingWebRequestTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingWebRequestTracer;
import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import com.dynatrace.oneagent.sdk.api.infos.WebApplicationInfo;
import com.vordel.circuit.Message;
import com.vordel.circuit.net.State;
import com.vordel.dwe.http.ServerTransaction;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.HeaderSet.HeaderEntry;
import com.vordel.trace.Trace;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map.Entry;

public class OneAgentSDKUtils {
    static OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();

    static {
        if (oneAgentSdk.getCurrentState() == null) {

            System.out.println("SDK is active and capturing.");
        } else if (SDKState.PERMANENTLY_INACTIVE == oneAgentSdk.getCurrentState()) {
            System.err.println("SDK is PERMANENT_INACTIVE; Probably no OneAgent injected or OneAgent is incompatible with SDK.");
        } else if (SDKState.TEMPORARILY_INACTIVE == oneAgentSdk.getCurrentState()) {
            System.err.println("SDK is TEMPORARY_INACTIVE; OneAgent has been deactivated - check OneAgent configuration.");
        }
        System.err.println("SDK is in unknown state.");
    }

    public static void aroundProducer(ProceedingJoinPoint pjp, State state) {
        String host = "host";
        String port = "port";
        String orgName = "";
        String appName = "";
        Message message = null;
        HeaderSet headers = null;
        try {
            Field hostField = State.class.getDeclaredField("host");
            hostField.setAccessible(true);
            host = (String) hostField.get(state);
            Field portField = State.class.getDeclaredField("port");
            portField.setAccessible(true);
            port = (String) portField.get(state);
            Field headersField = State.class.getDeclaredField("headers");
            headersField.setAccessible(true);
            headers = (HeaderSet) headersField.get(state);
            Field messageField = State.class.getDeclaredField("message");
            messageField.setAccessible(true);
            message = (Message) messageField.get(state);

            if (message.get("authentication.application.name") != null) {
                appName = message.get("authentication.application.name").toString();
            }
            if (message.get("authentication.organization.name") != null) {
                orgName = message.get("authentication.organization.name").toString();
            }
        } catch (Exception e) {
            Trace.error("around producer ", e);
        }
        OutgoingWebRequestTracer outgoingWebRequestTracer = oneAgentSdk.traceOutgoingWebRequest(getRequestURL(message), getHTTPMethod(message));
        try {
            addoutgoingHeaders(outgoingWebRequestTracer, headers);
            outgoingWebRequestTracer.start();
            addRequestAttributes(appName, orgName);
            String outgoingTag = outgoingWebRequestTracer.getDynatraceStringTag();
            Trace.info("Dynatrace :: outgoing x-dynatrace header " + outgoingTag);
            headers.setHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, outgoingTag);
            for (Entry<String, HeaderEntry> entry : headers.entrySet()) {
                outgoingWebRequestTracer.addRequestHeader(entry.getKey(), entry.getValue().toString());
            }
            if (message != null) {
                getAttributes(message);
            }
            pjp.proceed();
        } catch (Throwable e) {
            Trace.error("Dynatrace :: around producer ", e);
            outgoingWebRequestTracer.error(e);
        }
        outgoingWebRequestTracer.setStatusCode(getHTTPStatusCode(message));
        outgoingWebRequestTracer.end();
    }

    public static Object aroundConsumer(ProceedingJoinPoint pjp, Message m, String apiName, String apiContextRoot, String appName, String orgName,
                                        ServerTransaction txn) {

        Object pjpProceed = null;
        WebApplicationInfo wsInfo = oneAgentSdk.createWebApplicationInfo("serverNameTest", apiName, apiContextRoot);

        HeaderSet headers = null;
        if (m != null) {
            headers = (HeaderSet) m.get("http.headers");
        } else if (txn != null) {
            headers = txn.getHeaders();
        }

        if (headers.hasHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            String receivedTag = headers.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
            Trace.info("Dynatrace :: X-Dynatrace-Header " + receivedTag);
            IncomingWebRequestTracer tracer = createIncomingWebRequestTracer(m, txn, wsInfo);
            if (receivedTag.startsWith("FW")) {
                tracer.setDynatraceStringTag(receivedTag);
                tracer.start();
                addIncomingHeaders(tracer, headers);
                addRequestAttributes(appName, orgName);
            } else {
                tracer.setDynatraceStringTag(receivedTag);
                tracer.start();
                addIncomingHeaders(tracer, headers);
                addRequestAttributes(appName, orgName);
                int NA_index = receivedTag.indexOf("NA=");
                int SN_index = receivedTag.indexOf("SN=");
                int SI_index = receivedTag.indexOf("SI=");

                if (NA_index != -1 && SN_index != -1 && SI_index != -1) {
                    String afterNA = receivedTag.substring(NA_index);
                    int delimiter_index = afterNA.indexOf(';');
                    String NeoLoad_Transaction = receivedTag.substring(NA_index + 3, NA_index + delimiter_index);
                    NeoLoad_Transaction(NeoLoad_Transaction);
                    //Trace.info("NeoLoad_Transaction = " + NeoLoad_Transaction);
                    String afterSN = receivedTag.substring(SN_index);
                    delimiter_index = afterSN.indexOf(';');
                    String NeoLoad_UserPath = receivedTag.substring(SN_index + 3, SN_index + delimiter_index);
                    NeoLoad_UserPath(NeoLoad_UserPath);

                    String afterSI = receivedTag.substring(SI_index);
                    delimiter_index = afterSI.indexOf(';');
                    String Neoload_Traffic = receivedTag.substring(SI_index + 3, SI_index + delimiter_index);
                    Neoload_Traffic(Neoload_Traffic);
                }
            }

            try {
                pjpProceed = pjp.proceed();
            } catch (Throwable e) {
                Trace.error("around consumer: if block ", e);
                tracer.error(e);
            }

            Trace.debug("Dynatrace :: aroundConsumer : after processing request");
            tracer.setStatusCode(getHTTPStatusCode(m));
            tracer.end();
        } else {
            IncomingWebRequestTracer tracer = createIncomingWebRequestTracer(m, txn, wsInfo);
            tracer.start();
            addIncomingHeaders(tracer, headers);
            addRequestAttributes(appName, orgName);
            try {
                pjpProceed = pjp.proceed();
            } catch (Throwable e) {
                Trace.error("Dynatrace :: around consumer in else ", e);
                tracer.error(e);
            }
            tracer.setStatusCode(getHTTPStatusCode(m));
            tracer.end();
        }

        return pjpProceed;
    }

    private static IncomingWebRequestTracer createIncomingWebRequestTracer(Message m, ServerTransaction txn, WebApplicationInfo wsInfo) {
        IncomingWebRequestTracer tracer = null;

        if (m != null) {
            String httpURL = "https://" + m.get("http.request.hostname").toString() + m.get("http.request.uri").toString();
            tracer = oneAgentSdk.traceIncomingWebRequest(wsInfo, httpURL, m.get("http.request.verb").toString());
        } else if (txn != null) {
            String httpURL = "https://" + txn.getHost() + txn.getRequestURI();
            tracer = oneAgentSdk.traceIncomingWebRequest(wsInfo, httpURL, txn.getMethod());
        }
        return tracer;
    }

    public static void getAttributes(Message message) {
        HeaderSet httpHeaders = (HeaderSet) message.get("http.headers");
        if (httpHeaders == null)
            return;
        String keyId = (String) httpHeaders.get("KeyId");
        if (keyId != null)
            getKEYID(keyId);

        String clientName = (String) message.get("message.client.name");
        if (clientName != null)
            getClientName(clientName);

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
        return (String) message.get("http.request.path");
    }

    public static void getClientName(String clientName) {
        oneAgentSdk.addCustomRequestAttribute("ClientName", clientName);
    }

    public static void getKEYID(String keyId) {
        oneAgentSdk.addCustomRequestAttribute("KEYID", keyId);
    }


    public static String getHTTPMethod(Message message) {
        return (String) message.get("http.request.verb");
    }

    public static String getRequestURL(Message message) {

        try {
            return message.get("http.request.uri").toString();
        } catch (Exception ex) {
            Trace.error("in Request url ", ex);
            return "(null)";
        }
    }

    public static int getHTTPStatusCode(Message message) {
        try {
            return Integer.parseInt(message.get("http.response.status").toString());
        } catch (Exception ex) {
            return 0;
        }
    }

    public static void addIncomingHeaders(IncomingWebRequestTracer tracer, HeaderSet headers) {
        Iterator<String> iterator = headers.getNames();
        if (iterator.hasNext()) {
            String header = iterator.next();
            tracer.addRequestHeader(header, headers.getHeader(header));
        }
    }

    public static void addoutgoingHeaders(OutgoingWebRequestTracer tracer, HeaderSet headers) {
        Iterator<String> iterator = headers.getNames();
        if (iterator.hasNext()) {
            String header = iterator.next();
            tracer.addRequestHeader(header, headers.getHeader(header));
        }
    }

    public static void addRequestAttributes(String appName, String orgName) {
        oneAgentSdk.addCustomRequestAttribute("AxwayAppName", appName);
        oneAgentSdk.addCustomRequestAttribute("AxwayOrgName", orgName);
    }

}
