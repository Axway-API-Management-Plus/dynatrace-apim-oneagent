package com.axway.aspects.apim;

import com.dynatrace.oneagent.sdk.api.LoggingCallback;
import com.vordel.trace.Trace;

public class TraceLoggingCallback implements LoggingCallback {
    @Override
    public void warn(String message) {
        Trace.fatal(message);
    }

    @Override
    public void error(String message) {
        Trace.error(message);
    }
}
