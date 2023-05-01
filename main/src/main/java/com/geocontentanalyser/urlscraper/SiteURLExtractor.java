package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class SiteURLExtractor {
    LinkedHashSet<String> resultURLs = new LinkedHashSet<String>();
    String baseURL;

    public void extractURL(String URL, int recursiveDepth) {
        recursiveDepth = 0;
        HTMLRetriever htmlRetriever = new HTMLRetriever();
        String content = htmlRetriever.getHTML(URL);

        // parse content into HTML
        Document doc = Jsoup.parse(content, URL);
        // get all links
        Elements links = doc.select("a[href]");
        // deduplicate links
        ArrayList<String> localresultURLs = deduplicate(links);
        // add links to resultURLs
        resultURLs.addAll(localresultURLs);

        // write links to file
        try {
            FileWriter writer = new FileWriter("output/links.txt", true);
            for (String link : localresultURLs) {
                writer.write(link + "\n");
            }
            writer.close();
        } catch (IOException e) {
        }

        // if recursive depth is less than 4, call recursive function
        if (recursiveDepth <= 4) {
            // call recursive function to extract links from each link
            for (String link : localresultURLs) {
                // delay 1 second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                extractURL(link, recursiveDepth + 1);
            }
        }

    }

    public ArrayList<String> deduplicate(Elements links) {
        Iterator<Element> it = links.iterator();

        while (it.hasNext()) {
            Element link = it.next();
            // check if contain javascript, #, mailto, or not html, not baseURL, or already
            // exist in resultURLs
            if (link.attr("href").contains("javascript") || link.attr("href").contains("#")
                    || link.attr("href").contains("mailto") || !link.attr("href").contains("html")
                    || !link.attr("abs:href").contains(baseURL) || resultURLs.contains(link.attr("abs:href"))) {
                it.remove();
            }
        }

        // conversion to set to remove duplicates
        Set<String> set = new LinkedHashSet<String>();
        for (Element link : links) {
            set.add(link.attr("abs:href"));
        }

        // conversion to arraylist
        ArrayList<String> list = new ArrayList<String>(set);
        return list;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

}
