package com.geocontentanalyser.wikiscraper;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

//Convert response to JSON, parse and filter the links
//Specifically for Landkreise, doesnt work with Kreisfreie Städte
public class WikiURLExtractor {
    private List<String> addresses = new ArrayList<String>();

    public void getName(JSONObject jsonObj) {
        // Convert String to JSON
        JSONArray jsonArray = jsonObj.getJSONObject("parse").getJSONArray("links");

        if (jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                String name = jsonArray.getJSONObject(i).getString("*");
                filter(name);
            }
        }
    }

    public void filter(String content) {
        // Doesnt contain Kreis or contain Liste
        if ((content.toLowerCase().contains("kreis") || content.toLowerCase().contains("region"))
                && !content.toLowerCase().contains("liste") && !content.toLowerCase().contains("deutsch")
                && !content.contentEquals("Kreisstadt") && !content.contentEquals("Landkreis")
                && !content.contentEquals("Kreisfreie Stadt")) {
            content = content.replace(" ", "_");
            // System.out.println(content);
            addresses.add(content);
        }

    }

    // return the list of names
    public List<String> getAddresses() {
        return addresses;
    }

    //reset the list
    public void reset() {
        addresses.clear();
    }
}