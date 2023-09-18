package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.coreapireg.runtime.PathResolverResult;
import com.vordel.coreapireg.runtime.broker.ApiShunt;
import com.vordel.coreapireg.runtime.broker.InvokableMethod;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.HTTPProtocol;
import com.vordel.dwe.http.ServerTransaction;
import com.vordel.mime.Body;
import com.vordel.mime.HeaderSet;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;

@Aspect
public class AxwayAspect {

    public static final boolean isAPIManager = Boolean.parseBoolean(System.getProperty("apimanager", "true"));

    public AxwayAspect() {
    }


    @Pointcut("execution(* com.vordel.circuit.net.ConnectionProcessor.invoke(..)) && args (c, m, headers, verb, body)")
    public void invokeConnectToUrl(Circuit c, Message m, HeaderSet headers, String verb, Body body) {

    }

    @Around("invokeConnectToUrl(c, m, headers, verb, body)")
    public Object invokeConnectToUrlAroundAdvice(ProceedingJoinPoint pjp, Circuit c, Message m, HeaderSet headers, String verb, Body body) throws Throwable {
        return OneAgentSDKUtils.aroundProducer(pjp, m, c, headers, verb);
    }

    @Pointcut("call(* com.vordel.dwe.http.HTTPPlugin.invokeDispose(..)) && args (protocol, handler, txn, id, loopbackMessage)")
    public void invokeDisposePointcutGateway(HTTPProtocol protocol, HTTPProtocol handler, ServerTransaction txn, CorrelationID id, Map<String, Object> loopbackMessage) {

    }

    @Around("invokeDisposePointcutGateway(protocol, handler, txn, id, loopbackMessage)")
    public void invokeDisposeAroundAdvice(ProceedingJoinPoint pjp, HTTPProtocol protocol, HTTPProtocol handler,
                                          ServerTransaction txn, CorrelationID id, Map<String, Object> loopbackMessage) throws Throwable {
        if (!isAPIManager) {
            String[] uriSplit = txn.getRequestURI().split("/");
            String apiName = uriSplit[1];
            String apiContextRoot = "/";
            OneAgentSDKUtils.aroundConsumer(pjp, null, apiName, apiContextRoot, txn);
        } else {
            pjp.proceed();
        }
    }

    @Pointcut("execution(* com.vordel.coreapireg.runtime.CoreApiBroker.invokeMethod(..)) && args (txn, m, lastChanceHandler, runMethod, resolvedMethod, matchCount, httpMethod, apiPrefix, currentApiCallStatus)")
    public void invokeMethodPointcut(ServerTransaction txn, Message m,
                                     MessageProcessor lastChanceHandler, InvokableMethod runMethod,
                                     final PathResolverResult resolvedMethod, final int matchCount,
                                     String httpMethod, String apiPrefix, ApiShunt currentApiCallStatus) {

    }

    @Around("invokeMethodPointcut(txn, m, lastChanceHandler, runMethod, resolvedMethod, matchCount, httpMethod, apiPrefix, currentApiCallStatus)")
    public Object invokeMethodAroundAdvice(ProceedingJoinPoint pjp, ServerTransaction txn, Message m,
                                           MessageProcessor lastChanceHandler, InvokableMethod runMethod,
                                           final PathResolverResult resolvedMethod, final int matchCount,
                                           String httpMethod, String apiPrefix, ApiShunt currentApiCallStatus) throws Throwable{
        String[] uriSplit = OneAgentSDKUtils.getRequestURL(m).split("/");
        String apiName;
        String apiContextRoot = "/";
        apiName = (String) m.getOrDefault("api.name", uriSplit[1]);
        apiContextRoot = (String) m.getOrDefault("api.path", apiContextRoot);
        return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, apiContextRoot, txn);
    }
}
