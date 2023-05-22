package com.geocontentanalyser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.geocontentanalyser.urlscraper.SiteURLExtractor;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

public class App {
    public static void main(String[] args) throws IOException {
        //List<String> wikiURLlList = WikiScrapperMain.crawler();

        /* 
        //read each line from t.log and save to a list
        List<String> wikiURLlList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader("output/2023-05-11T07-35.log"));
        String line = reader.readLine();
        while (line != null) {
            wikiURLlList.add(line);
            line = reader.readLine();
        }
        reader.close();
        
        for (String URL : wikiURLlList) {
            SiteURLExtractor siteURLExtractor = new SiteURLExtractor(URL);
            Thread thread = new Thread(siteURLExtractor);
            thread.start();
        }
        */
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor("https://www.kreis-mettmann.de/");
        Thread thread = new Thread(siteURLExtractor);
        thread.start();
    }
}
