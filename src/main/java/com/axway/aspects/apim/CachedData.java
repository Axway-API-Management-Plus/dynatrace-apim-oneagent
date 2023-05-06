package com.axway.aspects.apim;

import com.vordel.mime.HeaderSet;

public class CachedData {

    private final HeaderSet headerSet;
    private final String requestUrl;
    private final String httpVerb;

    private final int httpStatusCode;

    public CachedData(HeaderSet headerSet, String requestUrl, String httpVerb, int httpStatusCode) {
        this.headerSet = headerSet;
        this.requestUrl = requestUrl;
        this.httpVerb = httpVerb;
        this.httpStatusCode = httpStatusCode;
    }

    public HeaderSet getHeaderSet() {
        return headerSet;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getHttpVerb() {
        return httpVerb;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
