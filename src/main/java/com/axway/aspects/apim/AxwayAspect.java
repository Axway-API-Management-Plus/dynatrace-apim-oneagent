package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.circuit.net.State;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.HTTPProtocol;
import com.vordel.dwe.http.ServerTransaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;

@Aspect
public class AxwayAspect {

    private boolean isAPIManager;

    public AxwayAspect() {
        isAPIManager = Boolean.parseBoolean(System.getProperty("apimanager", "true"));
    }

    @Pointcut("call (* com.vordel.circuit.net.State.tryTransaction()) && target(t)")
    public void tryTransactionPointCut(State t) {
    }

    @Around("tryTransactionPointCut(t)")
    public void tryTransactionAroundAdvice(ProceedingJoinPoint pjp, State t) {
        OneAgentSDKUtils.aroundProducer(pjp, t);
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
            OneAgentSDKUtils.aroundConsumer(pjp, null, apiName, apiContextRoot, appName, orgName, txn);
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

        if (m.get("authentication.application.name") != null) {
            appName = m.get("authentication.application.name").toString();
        }

        if (m.get("authentication.organization.name") != null) {
            orgName = m.get("authentication.organization.name").toString();
        }

        if (m.get("api.name") != null) {
            apiName = m.get("api.name").toString();
        } else {
            apiName = uriSplit[1];
        }
        return OneAgentSDKUtils.aroundConsumer(pjp, m, apiName, apiContextRoot, appName, orgName, null);
    }
}
