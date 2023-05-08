package com.geocontentanalyser;

import java.io.IOException;

import com.geocontentanalyser.urlscraper.SiteURLExtractor;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException
    {   
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor();
        siteURLExtractor.setBaseURL("https://www.saalekreis.de");
        siteURLExtractor.extractURL("https://www.saalekreis.de", 0);
        //WikiScrapperMain.crawler();
    }
}
