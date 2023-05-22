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

public class SiteURLExtractor implements Runnable {
    LinkedHashSet<String> resultURLs = new LinkedHashSet<String>();
    String baseURL;
    String fileName;

    public SiteURLExtractor(String baseURL) {
        this.baseURL = baseURL;
        this.fileName = baseURL.replaceAll("[\\\\/:*?\"<>|]", "");
    }

    public void extractURL(String URL, int recursiveDepth) throws IOException {
        recursiveDepth = 0;

        // parse content into HTML
        Document doc = Jsoup.connect(URL).get();
        // get all links
        Elements links = doc.select("a[href]");
        // deduplicate links
        ArrayList<String> localresultURLs = deduplicate(links);
        // add links to resultURLs
        resultURLs.addAll(localresultURLs);

        // write links to file
        try {
            FileWriter writer = new FileWriter("output/site/" + fileName + ".txt", true);
            for (String link : localresultURLs) {
                writer.write(link + "\n");
            }
            writer.close();
        } catch (IOException e) {
        }

        // filter the document
        filter(doc);

        // if recursive depth is less than 4, call recursive function
        if (recursiveDepth <= 4) {
            // call recursive function to extract links from each link
            for (String link : localresultURLs) {
                // delay 1 second
                try {
                    Thread.sleep(250);
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

    @Override
    public void run() {
        // remove the previous output file
        try {
            FileWriter writer = new FileWriter("output/site/" + fileName + ".txt", false);
            writer.close();
        } catch (IOException e) {
        }

        try {
            extractURL(baseURL, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void filter(Document doc) {
        // Insert whatever filter you want here
        // This function presents a HTML file formatted as a JSoup document
        // You can use the JSoup API to filter the document
        // Just call it here

    }

}
