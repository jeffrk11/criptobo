package com.jeff.cripto.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jeff.cripto.config.ConfigLoader;
import com.jeff.cripto.exceptions.HttpHelperException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

public class HttpHelper {
    static Logger log = Logger.getLogger(HttpHelper.class.getName());

    public static JsonObject doGet(String url) throws HttpHelperException {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new HttpHelperException(e);
        }
    }

    public static JsonObject doSignedPost(String url, String... params) throws HttpHelperException {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String parameters = signParams(addParams(params));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url+"?"+parameters))
                    .header("X-MBX-APIKEY",ConfigLoader.get("binance.api_key"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200){
                log.severe("Error from binance :"+response.body());
                throw new HttpHelperException(response.body().toString());
            }

            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new HttpHelperException(e);
        }
    }
    public static JsonElement doSignedGet(String url, String... params) throws HttpHelperException {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String parameters = signParams(addParams(params));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url+"?"+parameters))
                    .header("X-MBX-APIKEY",ConfigLoader.get("binance.api_key"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body());
        } catch (Exception e) {
            throw new HttpHelperException(e);
        }
    }
    private static String signParams(String params) throws Exception {
        String signature = Signature.generateSignature(params, ConfigLoader.get("binance.secret_key"));
        return String.format("%s&signature=%s",params,signature);
    }
    private static String addParams(String... params){
        StringBuilder formatedParams = new StringBuilder();
        if(params == null ||  params.length == 0)
            return "";

        for (String param : params) {
            formatedParams.append(param);
            formatedParams.append("&");
        }
        return formatedParams.toString().substring(0, formatedParams.length()-1);
    }
}
