package com.geocontentanalyser.wikiscraper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.json.JSONObject;

public class WikiScrapperMain {
    
		
        //Temporary file writing. Will be replaced in later interations
        public static void printToFile(String name, String url, String fileName) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("output/" + fileName + ".log", true));
    
                writer.write("|" + name + "|" + url + "|");
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void crawler() 
        {
            WikiURLExtractor wikiExtractor = new WikiURLExtractor();
            LandkreisURLExtractor landkreisExtractor = new LandkreisURLExtractor();
            HTMLRetriever retriever = new HTMLRetriever();
            wikiExtractor.getName(retriever.getHTML("Liste_der_Landkreise_in_Deutschland"));
		    List<String> wikiURLlList = wikiExtractor.getAddresses();
            String fileName = Instant.now().toString().replace(":", "-").replace(".", "-").substring(0, 16);
        

            for (String url : wikiURLlList) {
                // If there is a redirection, the JSON array will be empty
                // in that case, get the new URL and try again
                JSONObject jsonObj = retriever.getHTML(url);
                if (jsonObj.getJSONObject("parse").getJSONArray("categories").length() == 0) {
                    WikiURLExtractor newRetriever = new WikiURLExtractor();
                    newRetriever.getName(jsonObj);
                    jsonObj = retriever.getHTML(newRetriever.getAddresses().get(0));
                    newRetriever.reset();
                }
                printToFile(url, landkreisExtractor.URLextractor(jsonObj.toString()), fileName);
            }
            
        }
}
