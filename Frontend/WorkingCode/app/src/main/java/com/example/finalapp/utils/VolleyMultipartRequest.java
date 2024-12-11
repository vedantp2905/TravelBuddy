package com.example.finalapp.utils;

import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {
    private final Map<String, String> mHeaders = new HashMap<>();
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private Map<String, String> mParams;
    private Map<String, DataPart> mByteData;

    public VolleyMultipartRequest(int method, String url,
                                Response.Listener<NetworkResponse> listener,
                                Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
    }

    @Override
    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    protected Map<String, String> getParams() {
        return mParams;
    }

    protected Map<String, DataPart> getByteData() {
        return mByteData;
    }

    public static class DataPart {
        private String fileName;
        private byte[] content;

        public DataPart(String name, byte[] data) {
            fileName = name;
            content = data;
        }
    }
} 