package com.geocontentanalyser;

import java.io.IOException;
import java.util.List;

import com.geocontentanalyser.urlscraper.SiteURLExtractor;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws IOException {
        List<String> wikiURLlList = WikiScrapperMain.crawler();

        //change the URL to the one you want to scrape, manually
        //depth must be 0
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor("https://www.saalekreis.de/");
        siteURLExtractor.extractURL("https://www.saalekreis.de/", 4);
    }
}
