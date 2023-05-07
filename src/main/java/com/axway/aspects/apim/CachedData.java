package com.axway.aspects.apim;

import com.vordel.mime.HeaderSet;

public class CachedData {

    private final HeaderSet requestHeaderSet;
    private final HeaderSet responseHeaderSet;
    private final String requestUrl;
    private final String httpVerb;

    private final int httpStatusCode;

    public CachedData(HeaderSet requestHeaderSet,HeaderSet responseHeaderSet, String requestUrl, String httpVerb, int httpStatusCode) {
        this.requestHeaderSet = requestHeaderSet;
        this.responseHeaderSet = responseHeaderSet;
        this.requestUrl = requestUrl;
        this.httpVerb = httpVerb;
        this.httpStatusCode = httpStatusCode;
    }

    public HeaderSet getRequestHeaderSet() {
        return requestHeaderSet;
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

    public HeaderSet getResponseHeaderSet() {
        return responseHeaderSet;
    }

    @Override
    public String toString() {
        return "CachedData{" +
            "requestHeaderSet=" + requestHeaderSet +
            ", responseHeaderSet=" + responseHeaderSet +
            ", requestUrl='" + requestUrl + '\'' +
            ", httpVerb='" + httpVerb + '\'' +
            ", httpStatusCode=" + httpStatusCode +
            '}';
    }
}
