package com.axway.oneagent.utils;

import com.vordel.circuit.Message;
import com.vordel.dwe.CorrelationID;
import com.vordel.mime.HeaderSet;
import com.vordel.trace.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Trace.class, CorrelationID.class})
@SuppressStaticInitializationFor({"com.vordel.trace.Trace","com.vordel.dwe.CorrelationID"})
public class OneAgentSDKUtilsTest {


    @Test
    public void readHostNameFromHttpHeader() {
        Message message = new Message(PowerMockito.mock(CorrelationID.class), null);
        HeaderSet headerSet = new HeaderSet();
        message.put("http.headers",headerSet);
        headerSet.addHeader("Host", "10.129.61.129:8075");
        String host = OneAgentSDKUtils.readHostNameFromHttpHeader(message);
        Assert.assertEquals("10.129.61.129", host);
    }

    @Test
    public void readHostNameFromHttpHeaderWithoutPort() {
        Message message = new Message(PowerMockito.mock(CorrelationID.class), null);
        HeaderSet headerSet = new HeaderSet();
        message.put("http.headers",headerSet);
        headerSet.addHeader("Host", "10.129.61.129");
        String host = OneAgentSDKUtils.readHostNameFromHttpHeader(message);
        Assert.assertEquals("10.129.61.129", host);
    }

}
