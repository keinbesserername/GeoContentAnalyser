package com.geocontentanalyser.wikiscraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class WikiScrapperMain {

    public static List<String> crawler(String sessionPath) {
        WikiURLExtractor wikiExtractor = new WikiURLExtractor();
        LandkreisURLExtractor landkreisExtractor = new LandkreisURLExtractor();
        HTMLRetriever retriever = new HTMLRetriever();
        wikiExtractor.getName(retriever.getHTML("Liste_der_Landkreise_in_Deutschland"));
        List<String> wikiURLlList = wikiExtractor.getAddresses();
        List<String> siteURLList = new ArrayList<String>();
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(sessionPath + File.separator + "LandkreisURL.log", false));
            writer.write("|Landkreis Name|Landkreis URL|\n| ------------- | ------------- |\n");

            for (String name : wikiURLlList) {
                // If there is a redirection, the JSON array will be empty
                // in that case, get the new URL and try again
                JSONObject jsonObj = retriever.getHTML(name);
                if (jsonObj.getJSONObject("parse").getJSONArray("categories").length() == 0) {
                    WikiURLExtractor newRetriever = new WikiURLExtractor();
                    newRetriever.getName(jsonObj);
                    jsonObj = retriever.getHTML(newRetriever.getAddresses().get(0));
                    newRetriever.reset();
                }
                writer.write("|" + name + "|" + landkreisExtractor.URLextractor(jsonObj.toString()) + "|" + "\n");
                siteURLList.add(landkreisExtractor.URLextractor(jsonObj.toString()));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return siteURLList;
    }
}