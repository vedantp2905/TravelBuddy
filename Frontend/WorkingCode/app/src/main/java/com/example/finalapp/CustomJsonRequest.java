package com.example.finalapp;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class CustomJsonRequest extends Request<JSONObject> {
    private static final String PROTOCOL_CHARSET = "utf-8";

    private final Response.Listener<JSONObject> listener;
    private final String requestBody;

    public CustomJsonRequest(int method, String url, JSONObject jsonRequest,
                           Response.Listener<JSONObject> listener,
                           Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        this.requestBody = jsonRequest != null ? jsonRequest.toString() : null;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            
            JSONObject result;
            if (jsonString.trim().startsWith("{")) {
                result = new JSONObject(jsonString);
            } else {
                result = new JSONObject();
                result.put("message", jsonString.trim());
            }
            
            return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        listener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=" + PROTOCOL_CHARSET;
    }

    @Override
    public byte[] getBody() {
        try {
            return requestBody == null ? null : requestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }
}