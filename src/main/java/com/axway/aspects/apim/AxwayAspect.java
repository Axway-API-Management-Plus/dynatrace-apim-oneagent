package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.coreapireg.runtime.PathResolverResult;
import com.vordel.coreapireg.runtime.broker.ApiShunt;
import com.vordel.coreapireg.runtime.broker.InvokableMethod;
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
            return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, "/");
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

    @Pointcut("execution(* com.vordel.coreapireg.runtime.APIBroker.invokeMethod(..)) && args (m,  runMethod, resolvedMethod,  currentApiCallStatus)")
    public void invokeMethodPointcut(Message m, InvokableMethod runMethod,
                                     PathResolverResult resolvedMethod,
                                     ApiShunt currentApiCallStatus) {
    }

    /**
     * Captures api manager traffic
     *
     * @param pjp                  pjp
     * @param m                    message
     * @param runMethod            runMethod
     * @param resolvedMethod       resolvedMethod
     * @param currentApiCallStatus currentApiCallStatus
     * @return pjp object
     * @throws Throwable
     */
    @Around("invokeMethodPointcut( m,  runMethod, resolvedMethod, currentApiCallStatus)")
    public Object invokeMethodAroundAdvice(ProceedingJoinPoint pjp, Message m,
                                           InvokableMethod runMethod,
                                           PathResolverResult resolvedMethod, ApiShunt currentApiCallStatus) throws Throwable {
        String[] uriSplit = OneAgentSDKUtils.getRequestURL(m).split("/");
        String apiName;
        String apiContextRoot = "/";
        apiName = (String) m.getOrDefault("api.name", uriSplit[1]);
        apiContextRoot = (String) m.getOrDefault("api.path", apiContextRoot);
        return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, apiContextRoot);
    }

}
