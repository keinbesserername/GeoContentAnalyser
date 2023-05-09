package com.geocontentanalyser;

import java.io.IOException;
import java.util.List;

import com.geocontentanalyser.urlscraper.SiteURLExtractor;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

public class App {
    public static void main(String[] args) throws IOException {
        List<String> wikiURLlList = WikiScrapperMain.crawler();

        for (String URL : wikiURLlList) {
            SiteURLExtractor siteURLExtractor = new SiteURLExtractor(URL);
            Thread thread = new Thread(siteURLExtractor);
            thread.start();
        }
    }
}
