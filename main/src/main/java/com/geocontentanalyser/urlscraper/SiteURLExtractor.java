package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class SiteURLExtractor implements Runnable {
    LinkedHashSet<String> resultURLs = new LinkedHashSet<String>();
    String baseURL;
    String fileName;

    public SiteURLExtractor(String baseURL) {
        this.baseURL = baseURL;
        // remove http:// or https:// from the baseURL
        this.fileName = baseURL.replace("www.", "").replace("https://", "")
                .replace("http://", "").replaceAll("[\\\\/:*?\"<>|]", "");
    }

    public void extractURL(String URL, int recursiveDepth) throws IOException {
        recursiveDepth = 0;

        Document doc = null;
        
        Connection connection = Jsoup.connect(URL).followRedirects(true)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
        .timeout(5000);
        
        // parse content into HTML document
        doc = connection.get();
        // get all links
        Elements links = doc.select("a[href]");
        // deduplicate links
        ArrayList<String> localresultURLs = deduplicate(links);
        // add links to resultURLs
        resultURLs.addAll(localresultURLs);

        // write links to file
        writeToFile(true, localresultURLs);

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
            // check if contain javascript, #, mailto, or not html, not baseURL
            // have any extension, or already exist in resultURLs
            String linkString = trackerStripper(link.attr("abs:href"));
            if (linkString.contains("javascript") || linkString.contains("#")
                    || linkString.contains("mailto") || !linkString.contains("html")
                    || !linkString.contains(baseURL) || resultURLs.contains(linkString)) {
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
        writeToFile(false, null);
        try {
            extractURL(baseURL, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(boolean append, ArrayList<String> localresultURLs) {
        // write links to file
        try {
            FileWriter writer = new FileWriter("output/site/" + fileName + ".txt", append);
            if (localresultURLs != null) {
                for (String link : localresultURLs) {
                    writer.write(link + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
        }
    }

    public String trackerStripper(String link) {
        // strip everything after the ? in the link
        String[] linkParts = link.split("\\?");
        link = linkParts[0];
        return link;
    }

    public void filter(Document doc) {
        // Insert whatever filter you want here
        // This function presents a HTML file formatted as a JSoup document
        // You can use the JSoup API to filter the document
        // Just call it here

    }

}
