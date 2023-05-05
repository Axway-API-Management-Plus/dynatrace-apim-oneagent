package com.axway.oneagent.utils;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
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

import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Trace.class, CorrelationID.class})
@SuppressStaticInitializationFor({"com.vordel.trace.Trace", "com.vordel.dwe.CorrelationID"})
public class OneAgentSDKUtilsTest {


    @Test
    public void readHostNameFromHttpHeader() {
        Message message = new Message(PowerMockito.mock(CorrelationID.class), null);
        HeaderSet headerSet = new HeaderSet();
        message.put("http.headers", headerSet);
        headerSet.addHeader("Host", "10.129.61.129:8075");
        String host = OneAgentSDKUtils.readHostNameFromHttpHeader(message);
        Assert.assertEquals("10.129.61.129", host);
    }

    @Test
    public void readHostNameFromHttpHeaderWithoutPort() {
        Message message = new Message(PowerMockito.mock(CorrelationID.class), null);
        HeaderSet headerSet = new HeaderSet();
        message.put("http.headers", headerSet);
        headerSet.addHeader("Host", "10.129.61.129");
        String host = OneAgentSDKUtils.readHostNameFromHttpHeader(message);
        Assert.assertEquals("10.129.61.129", host);
    }

    @Test
    public void testMultiValueHeader() {
        HeaderSet headerSet = new HeaderSet();
        headerSet.addHeader("Cf-Visitor", "scheme");
        headerSet.addHeader("Cf-Visitor", "https");
        for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headerSet.entrySet()) {
            String value = OneAgentSDKUtils.getHeaderValues(entry);
            Assert.assertEquals("scheme,https", value);
        }
    }

    @Test
    public void testSingleHeader() {
        HeaderSet headerSet = new HeaderSet();
        headerSet.addHeader("Host", "10.129.61.129");
        for (Map.Entry<String, HeaderSet.HeaderEntry> entry : headerSet.entrySet()) {
            String value = OneAgentSDKUtils.getHeaderValues(entry);
            Assert.assertEquals("10.129.61.129", value);
        }
    }

    @Test
    public void testDuplicateHeaderRemoval() {

        HeaderSet headerSet = new HeaderSet();
        headerSet.addHeader("Host", "10.129.61.129");
        headerSet.addHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, "FW123");
        if(headerSet.containsKey(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            headerSet.remove(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
        }
        headerSet.addHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, "FW12356");
        System.out.println(headerSet);
        Assert.assertEquals(headerSet.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME), "FW12356");
    }

    @Test
    public void testResponseCode(){
        Message message = new Message(PowerMockito.mock(CorrelationID.class), null);
        message.put("http.response.status", 200);
        Assert.assertEquals(OneAgentSDKUtils.getHTTPStatusCode(message), 200);
        message.put("http.response.status", null);
        Assert.assertEquals(OneAgentSDKUtils.getHTTPStatusCode(message), 0);

    }

}
