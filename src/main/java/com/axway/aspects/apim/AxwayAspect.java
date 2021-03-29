//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.axway.aspects.apim;

import com.axway.oneagent.utils.OneAgentSDKUtils;
import com.vordel.circuit.net.State;
import com.vordel.dwe.CorrelationID;
import com.vordel.dwe.http.HTTPProtocol;
import com.vordel.dwe.http.ServerTransaction;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AxwayAspect {
    public AxwayAspect() {
    }

    @Pointcut("call (* com.vordel.circuit.net.State.tryTransaction()) && target(t)")
    public void tryTransactionPointCut(State t) {
    }

    @Around("tryTransactionPointCut(t)")
    public void tryTransactionAroundAdvice(ProceedingJoinPoint pjp, State t) throws Throwable {
        OneAgentSDKUtils.aroundProducer(pjp, t);
    }

    @Pointcut("call(* com.vordel.dwe.http.HTTPPlugin.invokeDispose(..)) && args (protocol, handler, txn, id, loopbackMessage)")
    public void invokeDisposePointcut(HTTPProtocol protocol, HTTPProtocol handler, ServerTransaction txn, CorrelationID id, Map<String, Object> loopbackMessage) {
    }

    @Around("invokeDisposePointcut(protocol, handler, txn, id, loopbackMessage)")
    public void invokeDisposeAroundAdvice(ProceedingJoinPoint pjp, HTTPProtocol protocol, HTTPProtocol handler, ServerTransaction txn, CorrelationID id, Map<String, Object> loopbackMessage) throws Throwable {
        OneAgentSDKUtils.aroundConsumer(pjp, protocol, handler, txn, id, loopbackMessage);
    }
}
