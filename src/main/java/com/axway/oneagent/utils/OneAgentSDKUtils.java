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
import java.util.Iterator;
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

            final Message finalMessage = message;
            String mapAsString = message.keySet().stream()
                    .map(key -> key + "=" + finalMessage.get(key))
                    .collect(Collectors.joining(", ", "{", "}"));

            Trace.debug("Dynatrace :: message keys: " + mapAsString);

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
            Trace.debug("Dynatrace :: Add outgoing headers");
            addOutgoingHeaders(outgoingWebRequestTracer, headers);
            Trace.debug("Dynatrace :: Start Outgoing Tracer");
            outgoingWebRequestTracer.start();
            Trace.debug("Dynatrace :: Add Request Attributes");
            addRequestAttributes(appName, orgName, appId);
            String outgoingTag = outgoingWebRequestTracer.getDynatraceStringTag();
            Trace.info("Dynatrace :: outgoing x-dynatrace header " + outgoingTag);
            Trace.debug("Dynatrace :: Set outgoing header");

            headers.setHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, outgoingTag);

            if (message != null) {
                Trace.debug("Dynatrace :: Get Message Attributes");
                getAttributes(message);
            }
            pjp.proceed();

            final Message finalMessage1 = message;
            String postMapAsString = message.keySet().stream()
                    .map(key -> key + "=" + finalMessage1.get(key))
                    .collect(Collectors.joining(", ", "{", "}"));
            Trace.debug("Dynatrace :: message keys after process " + postMapAsString);
        } catch (Throwable e) {
            Trace.error("Dynatrace :: around producer ", e);
            outgoingWebRequestTracer.error(e);
        }
        finally {
            outgoingWebRequestTracer.setStatusCode(getHTTPStatusCode(message));
            outgoingWebRequestTracer.end();
        }
        Trace.debug("Dynatrace :: Ending around producer");

    }

    public static Object aroundConsumer(ProceedingJoinPoint pjp, Message m, String apiName, String apiContextRoot, String appName, String orgName,
                                        String appId, ServerTransaction txn) {
        Trace.debug("Dynatrace :: Starting around consumer");

        Object pjpProceed = null;
        WebApplicationInfo wsInfo = oneAgentSdk.createWebApplicationInfo("serverNameTest", apiName, apiContextRoot);

        HeaderSet headers = null;
        if (m != null) {
            headers = (HeaderSet) m.get("http.headers");
        } else if (txn != null) {
            headers = txn.getHeaders();
        }
        IncomingWebRequestTracer tracer = createIncomingWebRequestTracer(m, txn, wsInfo);
        addIncomingHeaders(tracer, headers);

        if (headers != null && headers.hasHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            String receivedTag = headers.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
            Trace.info("Dynatrace :: X-Dynatrace-Header " + receivedTag);
            tracer.setDynatraceStringTag(receivedTag);
            tracer.start();
            addRequestAttributes(appName, orgName, appId);

            if (!receivedTag.startsWith("FW")) {
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
        } else {
            tracer.start();
            addRequestAttributes(appName, orgName, appId);
            try {
                pjpProceed = pjp.proceed();
            } catch (Throwable e) {
                Trace.error("Dynatrace :: around consumer in else ", e);
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

    public static String readHostNameFromHttpHeader(Message message){
        HeaderSet httpHeaders = (HeaderSet) message.get("http.headers");
        if (httpHeaders == null)
            return "0.0.0.0";
        String host = (String) httpHeaders.get("Host");
        if(host == null)
            return "0.0.0.0";
        if(host.contains(":")){
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
            Iterator<String> iterator = headers.getNames();
            if (iterator.hasNext()) {
                String header = iterator.next();
                tracer.addRequestHeader(header, headers.getHeader(header));
            }
        }
    }

    public static void addOutgoingHeaders(OutgoingWebRequestTracer tracer, HeaderSet headers) {
        if (headers != null) {
            Iterator<String> iterator = headers.getNames();
            if (iterator.hasNext()) {
                String header = iterator.next();
                tracer.addRequestHeader(header, headers.getHeader(header));
            }
        }
    }

    public static void addRequestAttributes(String appName, String orgName) {
        addRequestAttributes(appName, orgName, null);
    }

    public static void addRequestAttributes(String appName, String orgName, String appId) {
        Map<String, String> map;
        map = new HashMap<>();
        map.put("AxwayAppName", appName);
        map.put("AxwayOrgName", orgName);
        if (appId != null) {
            map.put("AxwayAppId", appId);
        }
        addRequestAttributes(map);
    }

    public static void addRequestAttributes(Map<String, String> attributes) {
        attributes.forEach((key, value) -> oneAgentSdk.addCustomRequestAttribute(key, value));
    }

}
