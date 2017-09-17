package com.avontell.resq_responder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * API hooks for the ResQ-Server
 */
public class ResQApi {

    public static final String url = "http://35.196.47.57";
    public static final String LOCATION = "/location";
    public static final String STATUS = "/api/status";
    public static final String RESPONDER = "/api/auth/firstresponder";
    public static final String TRIAGE = "/api/triage";
    public static final String USER = "/auth/user";

    public static final String ACCOUNT_INFO_KEY = "ACCOUNTINFOKEY";
    public static final String ACCOUNT_AUTH_KEY = "ACCOUNTAUTHKEY";
    public static final String SHARED_PREFS = "ACCOUNTAUTHKEY";

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static boolean createAccount(Context context, String name, boolean hasBoat, boolean hasCar, int physicality) {

        try {

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("hasBoat", hasBoat);
            data.put("hasCar", hasCar);
            data.put("physicality", physicality);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(JSON, data.toString());
            Request request = new Request.Builder()
                    .url(url + RESPONDER)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            String responseString = response.body().string();
            JSONObject result = new JSONObject(responseString);
            Log.e("ACCOUNT", responseString);
            if (result.has("success") && result.getString("success").equals("true")) {
                saveToPrefs(context, ACCOUNT_INFO_KEY, data.toString());
                saveToPrefs(context, ACCOUNT_AUTH_KEY, result.getString("authorizationKey"));
            } else {
                Log.e("ACCOUNT CREATION FAILED", "" + result);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public static JSONArray getTriage() {

        try {

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url + TRIAGE)
                    .build();

            Response response = client.newCall(request).execute();
            return new JSONArray(response.body().string());

        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONArray();
        }

    }

    public static JSONObject getStatus() {

        try {

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url + STATUS)
                    .build();

            Response response = client.newCall(request).execute();
            return new JSONObject(response.body().string());

        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject();
        }

    }

    private static void saveToPrefs(Context context, String key, String value) {

        SharedPreferences sharedPref = context.getSharedPreferences(
                SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putString(key, value);
        edit.apply();

    }

}
