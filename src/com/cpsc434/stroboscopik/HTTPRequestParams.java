package com.cpsc434.stroboscopik;

import java.util.List;

import org.apache.http.NameValuePair;

public class HTTPRequestParams {
	String url;
	List<NameValuePair> data;
	String success;
	String failure;

    HTTPRequestParams(String u, List<NameValuePair> d, String s, String f) {
        this.url = u;
        this.data = d;
        this.success = s;
        this.failure = f;
    }
}
