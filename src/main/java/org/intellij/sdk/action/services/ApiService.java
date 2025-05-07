package org.intellij.sdk.action.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.sdk.action.dto.ApiResponse;
import org.intellij.sdk.action.dto.ExtractBaseClassResponse;
import org.intellij.sdk.action.dto.MergeClassResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ApiService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ApiResponse genUnitTest(String key, String functionName, String code, String category, String accessToken, String generatedTests) throws IOException {
        String url = ApiConfig.API_ENDPOINTS.GENERATE_UNIT_TEST;

        Map<String, String> payload = new HashMap<>();
        payload.put("key", key);
        payload.put("functionName", functionName);
        payload.put("sourceCode", code);
        payload.put("category", category);
        payload.put("generatedTests", generatedTests);

        return postRequest(url, payload, accessToken, ApiResponse.class);
    }

    public static ExtractBaseClassResponse extractBaseClass(String code, String accessToken) throws IOException {
        String url = ApiConfig.API_ENDPOINTS.EXTRACT_BASE_CLASS;

        Map<String, String> payload = new HashMap<>();
        payload.put("sourceCode", code);

        return postRequest(url, payload, accessToken, ExtractBaseClassResponse.class);
    }

    public static MergeClassResponse mergeClass(String code, String accessToken) throws IOException {
        String url = ApiConfig.API_ENDPOINTS.MERGE_CLASS;

        Map<String, String> payload = new HashMap<>();
        payload.put("sourceCode", code);

        return postRequest(url, payload, accessToken, MergeClassResponse.class);
    }

    public static boolean registerToken(String token) {
        try {
            String mac = getMacAddress();
            Map<String, String> payload = new HashMap<>();
            payload.put("token", token);
            payload.put("assignedTo", mac != null ? mac : "");

            HttpURLConnection conn = createConnection(ApiConfig.API_ENDPOINTS.REGISTER_USER_TOKEN, "POST", null);
            writeBody(conn, payload);

            int status = conn.getResponseCode();
            return status >= 200 && status < 300;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getAccessToken(String userToken) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("token", userToken);

            HttpURLConnection conn = createConnection(ApiConfig.API_ENDPOINTS.GENERATE_USER_TOKEN, "POST", null);
            writeBody(conn, payload);

            if (conn.getResponseCode() == 200) {
                return readString(conn.getInputStream());
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static boolean checkToken(String accessToken) {
        try {
            HttpURLConnection conn = createConnection(ApiConfig.API_ENDPOINTS.CHECK_ACCESS_TOKEN, "GET", accessToken);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    // --- Utility Methods ---

    private static <T> T postRequest(String urlString, Map<String, ?> payload, String accessToken, Class<T> responseClass) throws IOException {
        HttpURLConnection conn = createConnection(urlString, "POST", accessToken);
        writeBody(conn, payload);

        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            // Read the response stream
            InputStream inputStream = conn.getInputStream();
            String responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            // Parse responseBody into the responseClass
            return objectMapper.readValue(responseBody, responseClass);
        } else {
            throw new IOException("Error " + conn.getResponseCode() + " - " + conn.getResponseMessage());
        }
    }

    private static HttpURLConnection createConnection(String urlString, String method, String accessToken) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (accessToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        }
        conn.setDoOutput(true);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, Map<String, ?> payload) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsBytes(payload);
            os.write(input);
        }
    }

    private static String readString(InputStream stream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            return br.readLine();
        }
    }

    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                if (!network.isLoopback() && network.getHardwareAddress() != null) {
                    byte[] mac = network.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
