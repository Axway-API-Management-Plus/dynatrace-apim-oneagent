package com.axway.oneagent.utils;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.vordel.circuit.Message;
import com.vordel.dwe.CorrelationID;
import com.vordel.mime.HeaderSet;
import com.vordel.trace.Trace;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Trace.class, CorrelationID.class, OneAgentSDKUtils.class})
@SuppressStaticInitializationFor({
    "com.vordel.trace.Trace",
    "com.vordel.dwe.CorrelationID",
    "com.vordel.mime.Body",
    "com.vordel.circuit.Message",
    "com.vordel.circuit.format.MessagePropertiesBodyFormatter",
    "com.vordel.circuit.format.MessagePropertiesFormatterRegistration",
    "com.axway.oneagent.utils.OneAgentSDKUtils"
})

public class OneAgentSDKUtilsTest {


    @Test
    public void readHostNameFromHttpHeader() {
        Message message = mock(Message.class);
        HeaderSet headerSet = new HeaderSet();
        headerSet.addHeader("Host", "10.129.61.129:8075");
        when(message.get("http.headers")).thenReturn(headerSet);
        String host = OneAgentSDKUtils.readHostNameFromHttpHeader(message);
        Assert.assertEquals("10.129.61.129", host);
    }

    @Test
    public void readHostNameFromHttpHeaderWithoutPort() {
        Message message = mock(Message.class);
        HeaderSet headerSet = new HeaderSet();
        headerSet.addHeader("Host", "10.129.61.129");
        when(message.get("http.headers")).thenReturn(headerSet);
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
        if (headerSet.containsKey(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME)) {
            headerSet.remove(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME);
        }
        headerSet.addHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME, "FW12356");
        Assert.assertEquals("FW12356", headerSet.getHeader(OneAgentSDK.DYNATRACE_HTTP_HEADERNAME));
    }

    @Test
    public void testResponseCode() {
        Message message = mock(Message.class);
        when(message.getOrDefault("http.response.status", 500)).thenReturn(200);
        Assert.assertEquals(200, OneAgentSDKUtils.getHTTPStatusCode(message));
        when(message.getOrDefault("http.response.status", 500)).thenReturn(500);
        Assert.assertEquals(500, OneAgentSDKUtils.getHTTPStatusCode(message));
    }

    @Test
    public void checkURL() {
        Message message = mock(Message.class);
        Assert.assertEquals("/", OneAgentSDKUtils.getRequestURL(message));
    }


    @Test
    public void checkURI() {
        Message message = mock(Message.class);
        when(message.get("http.request.uri")).thenReturn(URI.create("https://example.com/test"));
        Assert.assertEquals("https://example.com/test", OneAgentSDKUtils.getRequestURL(message));
    }

}
