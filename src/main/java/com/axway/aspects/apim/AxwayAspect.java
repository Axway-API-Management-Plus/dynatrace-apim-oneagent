package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.HTTPProtocol;
import com.vordel.dwe.http.ServerTransaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;

@Aspect
public class AxwayAspect {

    public static final boolean isAPIManager = Boolean.parseBoolean(System.getProperty("apimanager", "true"));

    public AxwayAspect() {
    }


    @Pointcut("execution(public boolean com.vordel.circuit.net.ConnectionProcessor.invoke(..)) && args (c, m)")
    public void invokeConnectToUrl(Circuit c, Message m) {

    }
    @After("invokeConnectToUrl(c, m)")
    public void invokeConnectToUrlAroundAdvice(Circuit c, Message m) {
        OneAgentSDKUtils.aroundProducer(m, c);
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
            String orgName = "defaultFrontend";
            String appName = "defaultFrontend";
            String appId = "defaultFrontend";
            OneAgentSDKUtils.aroundConsumer(pjp, null, apiName, apiContextRoot, appName, orgName, appId, txn);
        } else {
            pjp.proceed();
        }
    }
    @Pointcut("call(* com.vordel.coreapireg.runtime.broker.InvokableMethod.invoke(..)) && args (txn, m, lastChance)")
    public void invokeDisposePointcut(ServerTransaction txn, Message m, MessageProcessor lastChance) {

    }
    @Around("invokeDisposePointcut(txn, m, lastChance)")
    public Object invokeAroundAdvice(ProceedingJoinPoint pjp, ServerTransaction txn, Message m,
                                     MessageProcessor lastChance) {
        String[] uriSplit = OneAgentSDKUtils.getRequestURL(m).split("/");
        String apiName;
        String apiContextRoot = "/";
        String orgName = "default";
        String appName = "default";
        String appId = "default";
        appName = (String) m.getOrDefault("authentication.application.name", appName);
        orgName = (String) m.getOrDefault("authentication.organization.name", orgName);
        apiName = (String) m.getOrDefault("api.name", uriSplit[1]);
        apiContextRoot = (String) m.getOrDefault("api.path", apiContextRoot);
        appId = (String) m.getOrDefault("authentication.subject.id", appId);
        return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, apiContextRoot, appName, orgName, appId, null);
    }
}
