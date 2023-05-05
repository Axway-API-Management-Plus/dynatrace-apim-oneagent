package com.axway.aspects.apim;

import com.vordel.mime.HeaderSet;

public class CachedData {

    private final HeaderSet headerSet;
    private final String requestUrl;
    private final String httpVerb;

    public CachedData(HeaderSet headerSet, String requestUrl, String httpVerb) {
        this.headerSet = headerSet;
        this.requestUrl = requestUrl;
        this.httpVerb = httpVerb;
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
}
