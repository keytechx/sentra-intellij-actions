package org.intellij.sdk.action.services;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.intellij.sdk.action.storages.UserTokenStorage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class TokenService {
    public static boolean checkForToken() {
        String extensionId = "sentra.sentra-unit-test-generator";  // Placeholder for extension ID

        // Check if the extension is present
        if (isExtensionActive(extensionId)) {
            checkAndPromptForToken();
            return true;
        } else {
            System.out.println("Extension 'sentra.sentra' not found.");
            return false;
        }
    }

    public static void checkAndPromptForToken() {
        String storedToken = getStoredUserToken(); // Get the stored token
        if (storedToken == null || storedToken.isEmpty()) {
            System.out.println("No token found. Please enter one.");
            showTokenPanel();
        } else {
            // Optionally, send it to the backend for validation
            System.out.println("Token found.");
        }
    }

    public static void showTokenPanel() {
        RequestTokenDialog dialog = new RequestTokenDialog();
        dialog.show();
    }

    public static boolean generateAccessToken(String title) {
        UserTokenStorage tokenStorage = ServiceManager.getService(UserTokenStorage.class);
        String storedUserToken = tokenStorage.getUserToken();
        if (storedUserToken == null || storedUserToken.isEmpty()) {
            Messages.showMessageDialog(
                    "No token found. Please enter one.",
                    title,
                    Messages.getInformationIcon());
            showTokenPanel();
            return false;
        }

        String accessToken = tokenStorage.getAccessToken();
        if (accessToken != null && checkToken(accessToken)) {
            return true;
        }
        tokenStorage.setAccessToken(null);

        // Fetch new token using storedUserToken
        accessToken = getNewAccessToken(storedUserToken);
        if (accessToken != null) {
            tokenStorage.setAccessToken(accessToken);
            return true;
        }
        Messages.showMessageDialog(
                "Your token is invalid. Please enter a valid token.",
                title,
                Messages.getInformationIcon());
        tokenStorage.setUserToken(null);
        return false;
    }

    private static boolean checkToken(String accessToken) {
        try {
            URL url = new URL(ApiConfig.API_ENDPOINTS.CHECK_ACCESS_TOKEN);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {  // HTTP 200 OK
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getNewAccessToken(String userToken) {
        try {
            URL url = new URL(ApiConfig.API_ENDPOINTS.GENERATE_USER_TOKEN);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = "{\"token\":\"" + userToken + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED) {  // HTTP 200 OK
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();  // Returning the response text as the access token
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setAuthorizeHeader(Map<String, String> requestOptions) {
        String accessToken = getStoredAccessToken();
        if (accessToken != null) {
            requestOptions.put("Authorization", "Bearer " + accessToken);
        }
    }

    private static boolean isExtensionActive(String extensionId) {
        // Placeholder logic for checking extension activity
        return extensionId.equals("sentra.sentra-unit-test-generator");
    }

    public static String getStoredAccessToken() {
        UserTokenStorage tokenStorage = ServiceManager.getService(UserTokenStorage.class);
        return tokenStorage.getAccessToken();
    }

    public static String getStoredUserToken() {
        UserTokenStorage tokenStorage = ServiceManager.getService(UserTokenStorage.class);
        return tokenStorage.getUserToken();
    }
}
