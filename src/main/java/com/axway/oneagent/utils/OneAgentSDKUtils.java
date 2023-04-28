package com.axway.oneagent.utils;

import com.axway.aspects.apim.TraceLoggingCallback;
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
import com.vordel.trace.Trace;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OneAgentSDKUtils {
    static OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();

    static {
        oneAgentSdk.setLoggingCallback(new TraceLoggingCallback());
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
        Trace.debug("Dynatrace :: Starting around producer");
        String orgName = "";
        String appName = "";
        String appId = "";
        Message message = null;
        HeaderSet headers = null;
        try {
            Field headersField = State.class.getDeclaredField("headers");
            headersField.setAccessible(true);
            headers = (HeaderSet) headersField.get(state);
            Trace.debug("Dynatrace :: Producer Headers before proceed: " + headers.toString());
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
        OutgoingWebRequestTracer outgoingWebRequestTracer = oneAgentSdk.traceOutgoingWebRequest(getRequestURL(message),
            getHTTPMethod(message));

        try {
            addOutgoingHeaders(outgoingWebRequestTracer, headers);
            outgoingWebRequestTracer.start();
            String outgoingTag = outgoingWebRequestTracer.getDynatraceStringTag();
            Trace.debug("Dynatrace :: outgoing x-dynatrace header " + outgoingTag);
            if (headers != null) {
                headers.setHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, outgoingTag);
            }
            addRequestAttributes(appName, orgName, appId, null);
            getAttributes(message);
            pjp.proceed();
        } catch (Throwable e) {
            Trace.error("Dynatrace :: around producer ", e);
            outgoingWebRequestTracer.error(e);
        } finally {
            outgoingWebRequestTracer.setStatusCode(getHTTPStatusCode(message));
            outgoingWebRequestTracer.end();
        }
        Trace.debug("Dynatrace :: Ending around producer");
    }

    public static Object aroundConsumer(ProceedingJoinPoint pjp, Message m, String apiName, String apiContextRoot, String appName, String orgName,
                                        String appId, ServerTransaction txn) {
        Trace.debug("Dynatrace :: Starting around consumer");
        Object pjpProceed = null;
        WebApplicationInfo wsInfo = oneAgentSdk.createWebApplicationInfo("Axway Gateway", apiName, apiContextRoot);
        HeaderSet headers = null;
        String correlationId = null;

        if (m != null) {
            headers = (HeaderSet) m.get("http.headers");
            correlationId = m.getIDBase().toString();
        } else if (txn != null) {
            headers = txn.getHeaders();
        }
        if (headers == null) {
            Trace.debug("Dynatrace :: NO Consumer Headers");
        } else {
            Trace.debug("Dynatrace :: Consumer Headers before proceed: " + headers);
        }
        IncomingWebRequestTracer tracer = createIncomingWebRequestTracer(m, txn, wsInfo);
        if (headers != null && headers.hasHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            String receivedTag = headers.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
            Trace.debug("Dynatrace :: X-Dynatrace-Header " + receivedTag);
            tracer.setDynatraceStringTag(receivedTag);
            addIncomingHeaders(tracer, headers);
            tracer.start();
            addRequestAttributes(appName, orgName, appId, correlationId);
            if (!receivedTag.startsWith("FW")) {
                int NA_index = receivedTag.indexOf("NA=");
                int SN_index = receivedTag.indexOf("SN=");
                int SI_index = receivedTag.indexOf("SI=");

                if (NA_index != -1 && SN_index != -1 && SI_index != -1) {
                    String afterNA = receivedTag.substring(NA_index);
                    int delimiter_index = afterNA.indexOf(';');
                    String NeoLoad_Transaction = receivedTag.substring(NA_index + 3, NA_index + delimiter_index);
                    NeoLoad_Transaction(NeoLoad_Transaction);
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
                Trace.error("Dynatrace :: around consumer ", e);
                tracer.error(e);
            }
        } else {
            addIncomingHeaders(tracer, headers);
            tracer.start();
            addRequestAttributes(appName, orgName, appId, correlationId);
            try {
                pjpProceed = pjp.proceed();
            } catch (Throwable e) {
                Trace.error("Dynatrace :: around consumer", e);
                tracer.error(e);
            }
        }
        tracer.setStatusCode(getHTTPStatusCode(m));
        tracer.end();
        Trace.debug("Dynatrace :: Ending around consumer");
        return pjpProceed;
    }

    private static IncomingWebRequestTracer createIncomingWebRequestTracer(Message m, ServerTransaction txn, WebApplicationInfo wsInfo) {
        IncomingWebRequestTracer tracer = null;
        if (m != null) {
            String httpURL = "https://" + readHostNameFromHttpHeader(m) + m.get("http.request.uri").toString();
            tracer = oneAgentSdk.traceIncomingWebRequest(wsInfo, httpURL, m.get("http.request.verb").toString());
        } else if (txn != null) {
            String httpURL = "https://" + txn.getHost() + txn.getRequestURI();
            tracer = oneAgentSdk.traceIncomingWebRequest(wsInfo, httpURL, txn.getMethod());
        }
        return tracer;
    }

    public static String readHostNameFromHttpHeader(Message message) {
        HeaderSet httpHeaders = (HeaderSet) message.get("http.headers");
        if (httpHeaders == null)
            return "0.0.0.0";
        String host = (String) httpHeaders.get("Host");
        if (host == null)
            return "0.0.0.0";
        if (host.contains(":")) {
            return host.split(":")[0];
        }
        return host;
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
        if (headers != null) {
            for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headers.entrySet()) {
                String value = getHeaderValues(entry);
                tracer.addRequestHeader(entry.getKey(), value);
            }
        }
    }

    public static String getHeaderValues(Map.Entry<String, HeaderSet.HeaderEntry> entry) {
        return entry.getValue().stream().
            map(Object::toString).
            collect(Collectors.joining(","));
    }

    public static void addOutgoingHeaders(OutgoingWebRequestTracer outgoingWebRequestTracer, HeaderSet headers) {
        if (headers != null) {
            for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headers.entrySet()) {
                String value = getHeaderValues(entry);
                outgoingWebRequestTracer.addRequestHeader(entry.getKey(), value);
            }
        }
    }


    public static void addRequestAttributes(String appName, String orgName, String appId, String correlationId) {
        Map<String, String> map = new HashMap<>();
        if (appName != null) {
            map.put("AxwayAppName", appName);
        }
        if (orgName != null) {
            map.put("AxwayOrgName", orgName);
        }
        if (appId != null) {
            map.put("AxwayAppId", appId);
        }
        if (correlationId != null) {
            map.put("AxwayCorrelationId", "Id-" + correlationId);
        }
        Trace.info("Dynatrace :: Application Id :" + appId + " - Application Name : " + appName);
        addRequestAttributes(map);
    }

    public static void addRequestAttributes(Map<String, String> attributes) {
        attributes.forEach((key, value) -> oneAgentSdk.addCustomRequestAttribute(key, value));
    }
}