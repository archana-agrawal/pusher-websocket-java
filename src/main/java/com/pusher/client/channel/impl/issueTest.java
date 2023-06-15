import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AmplitudeServiceSender {
    public static void sendDateOfBirth(LocalDate dateOfBirth) {
        // Format the date in the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDateOfBirth = dateOfBirth.format(formatter);

        // Create the JSON payload with the date of birth
        String payload = "{\"event_type\": \"DateOfBirthUpdate\", \"user_id\": \"YOUR_USER_ID\", \"event_properties\": {\"DateOfBirth\": \"" + formattedDateOfBirth + "\"}}";

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
                System.out.println("Date of birth sent successfully to Amplitude.");
            } else {
                System.out.println("Failed to send date of birth. Response code: " + responseCode);
            }

            // Close the connection
            connection.disconnect();
        } catch (Exception e) {
            System.out.println("An error occurred while sending date of birth: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        LocalDate dateOfBirth = LocalDate.of(1990, 10, 15);
        sendDateOfBirth(dateOfBirth);
    }
}
