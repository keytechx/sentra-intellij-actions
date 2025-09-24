package org.intellij.sdk.action.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.intellij.sdk.action.storages.UserTokenStorage;

import javax.swing.*;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Getter
public class RequestTokenDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(RequestTokenDialog.class);

    private JBTextField tokenField;
    private String enteredToken;
    private final String dialogTitle = "Submit Token";

    public RequestTokenDialog() {
        super(true); // Dialog is modal
        setTitle(dialogTitle);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();

        panel.add(new JLabel("Your token:"));
        tokenField = new JBTextField();
        tokenField.setPreferredSize(new java.awt.Dimension(400, tokenField.getPreferredSize().height)); // Set width to 300
        panel.add(tokenField);
        return panel;
    }

    @Override
    protected void doOKAction() {
        enteredToken  = tokenField.getText();
        if (this.registerToken(enteredToken)) {
            saveTokenToStorage(enteredToken);
            super.doOKAction();
        }
    }

    // Save the token to PersistentStateComponent
    private void saveTokenToStorage(String token) {
        UserTokenStorage tokenStorage = ApplicationManager.getApplication().getService(UserTokenStorage.class);
        tokenStorage.setUserToken(token);
    }

    public boolean registerToken(String token) {
        try {
            // Create the payload (equivalent to the JavaScript payload)
            Map<String, String> payload = new HashMap<>();
            payload.put("token", token);
            payload.put("assignedTo", getMacAddress());

            // Convert the payload to a JSON string
            String jsonPayload = toJson(payload);

            // Create the HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ApiConfig.API_ENDPOINTS.REGISTER_USER_TOKEN))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            // Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status
            if (response.statusCode() == 201) {
                return true;
            } else {
                Messages.showMessageDialog(
                        "Invalid token. Please use another one or contact Sentra's Administrator for detail.",
                        dialogTitle,
                        Messages.getInformationIcon());
                System.out.println("Error: " + response.statusCode() + " - " + response.body());
                return false;
            }
        } catch (Exception e) {
            // Handle exceptions
            LOG.error("Failed to register token", e);
            return false;
        }
    }

    private String toJson(Map<String, String> map) {
        // Convert the map to a simple JSON string
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\",");
        }
        if (json.length() > 1) {
            json.deleteCharAt(json.length() - 1);  // Remove the last comma
        }
        json.append("}");
        return json.toString();
    }

    private String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to get Mac address", e);
        }
        return null;
    }

    public String getToken() {
        return tokenField.getText();
    }
}

