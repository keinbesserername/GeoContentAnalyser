package com.geocontentanalyser.urlscraper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class HTMLRetriever {
    String contentString = "";

    public String getHTML(String shortURL) {

        try {
            // set up an URI, as URL(String) is deprecated.
            // URLEncoder.encode() is used to encode the URL, as it may contain special
            // characters.
            URI uri_temp = new URI(shortURL);
            String temp = uri_temp.toASCIIString();
            URI uri = new URI(temp);
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // send GET request.
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode + "\nAt:" + shortURL);
            }
            // the response is in JSON format, storing it as a String.
            String line = "";
            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
            }
            contentString = content.toString();
            // close the HTTP connection
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentString;
    }
}
