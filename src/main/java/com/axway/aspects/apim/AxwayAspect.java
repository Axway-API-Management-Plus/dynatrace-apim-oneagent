package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.mime.HeaderSet;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AxwayAspect {

    public static final boolean ISAPIMANAGER = Boolean.parseBoolean(System.getProperty("apimanager", "true"));


    @Pointcut("execution(* com.vordel.circuit.SyntheticCircuitChainProcessor.invoke(..)) && args (m, lastChanceHandler, context)")
    public void invokeGateway(Message m, MessageProcessor lastChanceHandler, Object context) {
    }

    /**
     * Captures policies exposed via Listener and API manager UI traffics, it does not capture servlet traffic like api manger REST API
     *
     * @param m                 m
     * @param lastChanceHandler currentApiCallStatus
     * @param context           context
     * @return context object
     * @throws Throwable
     */
    @Around("invokeGateway(m, lastChanceHandler, context)")
    public Object invokePointcutGateway(ProceedingJoinPoint pjp, Message m, MessageProcessor lastChanceHandler, Object context) throws Throwable {
        if (!ISAPIMANAGER) {
            String requestPath = (String) m.get("http.request.path");
            String[] uriSplit = requestPath.split("/");
            String apiName = uriSplit.length == 0 ? "/" : uriSplit[1];
            return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, requestPath);
        } else {
            return pjp.proceed();
        }
    }

    @Pointcut("execution(* com.vordel.circuit.net.ConnectionProcessor.invoke(..)) && args (c, m)")
    public void invokeConnectToUrl(Circuit c, Message m) {
    }

    @Around("invokeConnectToUrl(c, m)")
    public Object invokeConnectToUrlAroundAdvice(ProceedingJoinPoint pjp, Circuit c, Message m) throws Throwable {
        HeaderSet headers = (HeaderSet) m.get("http.headers");
        String verb = (String) m.get("http.request.verb");
        return OneAgentSDKUtils.aroundProducer(pjp, m, c, headers, verb);
    }

    @Pointcut("execution(* com.vordel.coreapireg.runtime.APIBroker.processRequest(..)) && args (circuit, message)")
    public void invokeMethodPointcut(Circuit circuit, Message message) {
    }

    /**
     * Captures api manager traffic
     *
     * @param circuit circuit
     * @param message message
     * @return context object
     * @throws Throwable
     */
    @Around("invokeMethodPointcut(circuit, message)")
    public Object invokeMethodAroundAdvice(ProceedingJoinPoint pjp, Circuit circuit, Message message) throws Throwable {
        String requestPath = (String) message.get("http.request.path");
        String[] uriSplit = requestPath.split("/");
        String apiName;
        String apiContextRoot = "/";
        apiName = (String) message.getOrDefault("api.name", uriSplit[1]);
        apiContextRoot = (String) message.getOrDefault("api.path", apiContextRoot);
        return OneAgentSDKUtils.aroundConsumer(pjp, message, apiName, apiContextRoot);

    }

}
