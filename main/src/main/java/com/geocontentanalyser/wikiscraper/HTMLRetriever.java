package com.geocontentanalyser.wikiscraper;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.json.JSONObject;

public class HTMLRetriever {
    public JSONObject getHTML(String shortURL) {
        JSONObject jsonObj = null;
        try {
            // set up an URI, as URL(String) is deprecated.
            // URLEncoder.encode() is used to encode the URL, as it may contain special characters.
            URI uri_temp = new URI("https://de.wikipedia.org/w/api.php?action=parse&page=" + shortURL + "&format=json");
            String temp = uri_temp.toASCIIString();
            URI uri = new URI(temp);
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // send GET request.
            connection.setRequestMethod("GET");
            
            // the response is in JSON format, storing it as a String.
            String line = "";
            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
            }
            // Convert JSON formatted String to JSON
            jsonObj = new JSONObject(content.toString());
            // close the HTTP connection
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObj;
    }
}
