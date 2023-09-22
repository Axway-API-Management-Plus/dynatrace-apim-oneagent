package com.axway.oneagent.utils;

import com.axway.aspects.apim.TraceLoggingCallback;
import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingWebRequestTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingWebRequestTracer;
import com.dynatrace.oneagent.sdk.api.infos.WebApplicationInfo;
import com.vordel.circuit.Message;
import com.vordel.config.Circuit;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.ServerTransaction;
import com.vordel.mime.HeaderSet;
import com.vordel.trace.Trace;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OneAgentSDKUtils {
    private static final OneAgentSDK oneAgentSdk = OneAgentSDKFactory.createInstance();
    private static final String DEFAULT = "default";

    static {
        oneAgentSdk.setLoggingCallback(new TraceLoggingCallback());
        switch (oneAgentSdk.getCurrentState()) {
            case ACTIVE:
                System.out.println("SDK is active and capturing.");
                break;
            case PERMANENTLY_INACTIVE:
                System.err.println("SDK is PERMANENTLY_INACTIVE; Probably no OneAgent injected or OneAgent is incompatible with SDK.");
                break;
            case TEMPORARILY_INACTIVE:
                System.err.println("SDK is TEMPORARILY_INACTIVE; OneAgent has been deactivated - check OneAgent configuration.");
                break;
            default:
                System.err.println("SDK is in unknown state.");
                break;
        }
    }

    public static Object aroundProducer(ProceedingJoinPoint pjp, Message message, Circuit circuit, HeaderSet requestHeaders, String httpVerb) throws Throwable {
        Trace.debug("Dynatrace :: Starting around producer for Policy " + circuit.getName());
        Object object;
        String requestUrl = getRequestURL(message);
        Trace.debug("Request url :" + requestUrl + " httpVerb " + httpVerb);
        OutgoingWebRequestTracer outgoingWebRequestTracer = oneAgentSdk.traceOutgoingWebRequest(requestUrl, httpVerb);
        try {
            String appName = (String) message.getOrDefault("authentication.application.name", DEFAULT);
            String orgName = (String) message.getOrDefault("authentication.organization.name", DEFAULT);
            String appId = (String) message.getOrDefault("authentication.subject.id", DEFAULT);
            addOutgoingRequestHeaders(outgoingWebRequestTracer, requestHeaders);
            outgoingWebRequestTracer.start();
            addRequestAttributes(appName, orgName, appId, message.getIDBase());
            String outgoingTag = outgoingWebRequestTracer.getDynatraceStringTag();
            Trace.debug("Dynatrace :: outgoing x-dynatrace header " + outgoingTag);
            if (requestHeaders != null) {
                if (requestHeaders.containsKey(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
                    requestHeaders.remove(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
                }
                requestHeaders.setHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, outgoingTag);
            }
            addAttributes(message);
            object = pjp.proceed();
            HeaderSet responseHeaders = (HeaderSet) message.get("http.headers");
            addOutgoingResponseHeaders(outgoingWebRequestTracer, responseHeaders);
            int httpStatusCode = getHTTPStatusCode(message);
            outgoingWebRequestTracer.setStatusCode(httpStatusCode);
            Trace.debug("Dynatrace :: Backend response code " + httpStatusCode);
        } catch (Throwable e) {
            Trace.error("Dynatrace :: around producer ", e);
            outgoingWebRequestTracer.setStatusCode(500);
            outgoingWebRequestTracer.error(e);
            throw e;
        } finally {
            outgoingWebRequestTracer.end();
        }
        return object;
    }

    public static Object aroundConsumer(ProceedingJoinPoint pjp, Message message, String apiName, String apiContextRoot, ServerTransaction txn, boolean proceed) throws Throwable {
        Trace.debug("Dynatrace :: Starting around consumer");
        Object pjpProceed = null;
        WebApplicationInfo wsInfo = oneAgentSdk.createWebApplicationInfo("Axway Gateway", apiName, apiContextRoot);
        HeaderSet headers = null;
        String correlationId = null;
        if (message != null) {
            headers = (HeaderSet) message.get("http.headers");
            correlationId = message.getIDBase().toString();
        } else if (txn != null) {
            headers = txn.getHeaders();
        }
        Trace.debug("Dynatrace :: Consumer Headers before proceed: " + headers);
        IncomingWebRequestTracer tracer = createIncomingWebRequestTracer(message, txn, wsInfo);
        if (headers != null && headers.hasHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            String receivedTag = headers.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
            Trace.debug("Dynatrace :: X-Dynatrace-Header " + receivedTag);
            tracer.setDynatraceStringTag(receivedTag);
            addIncomingHeaders(tracer, headers);
            tracer.start();
            if (correlationId != null)
                oneAgentSdk.addCustomRequestAttribute("AxwayCorrelationId", "Id-" + correlationId);
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
        } else {
            addIncomingHeaders(tracer, headers);
            tracer.start();
            oneAgentSdk.addCustomRequestAttribute("AxwayCorrelationId", "Id-" + correlationId);
        }
        try {
            if (proceed)
                pjpProceed = pjp.proceed();
        } catch (Throwable e) {
            Trace.error("Dynatrace :: around consumer", e);
            tracer.error(e);
            throw e;
        } finally {
            if (message != null) {
                String appName = (String) message.getOrDefault("authentication.application.name", DEFAULT);
                String orgName = (String) message.getOrDefault("authentication.organization.name", DEFAULT);
                String appId = (String) message.getOrDefault("authentication.subject.id", DEFAULT);
                addRequestAttributes(appName, orgName, appId, message.getIDBase());
            }
            tracer.setStatusCode(getHTTPStatusCode(message));
            tracer.end();
            Trace.debug("Dynatrace :: Ending around consumer");
        }
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

    public static void addAttributes(Message message) {
        String clientName = (String) message.get("message.client.name");
        if (clientName != null)
            addClientName(clientName);
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

    public static void addClientName(String clientName) {
        oneAgentSdk.addCustomRequestAttribute("ClientName", clientName);
    }

    public static String getRequestURL(Message message) {
        return message.getOrDefault("http.request.uri", "(null)").toString();
    }

    public static int getHTTPStatusCode(Message message) {
        if (message == null)
            return 0;
        return (int) message.getOrDefault("http.response.status", 0);
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

    public static void addOutgoingRequestHeaders(OutgoingWebRequestTracer outgoingWebRequestTracer, HeaderSet headers) {
        if (headers != null) {
            for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headers.entrySet()) {
                String value = getHeaderValues(entry);
                outgoingWebRequestTracer.addRequestHeader(entry.getKey(), value);
            }
        }
    }

    public static void addOutgoingResponseHeaders(OutgoingWebRequestTracer outgoingWebRequestTracer, HeaderSet headers) {
        if (headers != null) {
            for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headers.entrySet()) {
                String value = getHeaderValues(entry);
                outgoingWebRequestTracer.addResponseHeader(entry.getKey(), value);
            }
        }
    }

    public static void addRequestAttributes(String appName, String orgName, String appId, CorrelationID correlationId) {
        Map<String, String> map = new HashMap<>();
        if (appName != null && !appName.equals(DEFAULT)) {
            map.put("AxwayAppName", appName);
        }
        if (orgName != null && !orgName.equals(DEFAULT)) {
            map.put("AxwayOrgName", orgName);
        }
        if (appId != null && !appId.equals(DEFAULT)) {
            map.put("AxwayAppId", appId);
        }
        if (correlationId != null) {
            map.put("AxwayCorrelationId", "Id-" + correlationId);
        }
        Trace.info("Dynatrace :: Application Id :" + appId + " - Application Name : " + appName);
        addRequestAttributes(map);
    }

    public static void addRequestAttributes(Map<String, String> attributes) {
        attributes.forEach(oneAgentSdk::addCustomRequestAttribute);
    }
}
