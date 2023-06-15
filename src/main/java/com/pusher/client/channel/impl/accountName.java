import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AmplitudeServiceSender {
    public static void sendAccountName(String accountName) {
        // Create the JSON payload with the account name
        String payload = "{\"event_type\": \"AccountNameUpdate\", \"user_id\": \"YOUR_USER_ID\", \"event_properties\": {\"AccountName\": \"" + accountName + "\"}}";

        try {
            // Set up the connection to the Amplitude API
            URL url = new URL("https://api.amplitude.com/2/httpapi");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the payload to the Amplitude API
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.getBytes());
            outputStream.flush();
            outputStream.close();

            // Check the response code to ensure the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Account name sent successfully to Amplitude.");
            } else {
                System.out.println("Failed to send account name. Response code: " + responseCode);
            }

            // Close the connection
            connection.disconnect();
        } catch (Exception e) {
            System.out.println("An error occurred while sending account name: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String accountName = "Example Account";
        sendAccountName(accountName);
    }
}
