package com.example.finalapp;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

public class VolleyMultipartRequest extends Request<NetworkResponse> {
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;

    public VolleyMultipartRequest(int method, String url,
                                Response.Listener<NetworkResponse> listener,
                                Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = new HashMap<>();
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

    protected Map<String, DataPart> getByteData() {
        return null;
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