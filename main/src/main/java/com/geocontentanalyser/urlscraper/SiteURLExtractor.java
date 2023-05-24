package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

        Document doc = new Document("");

        Connection connection = Jsoup.connect(URL).followRedirects(true)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");

        // this is very slow. The bottleneck lays here
        int retry = 0;
        try {
            Response response = connection.method(Method.GET).execute();
            // retry 3 times if status code is not 200
            while ((response.statusCode() != 200 && retry < 3)
                    || (response.statusCode() > 400 && response.statusCode() < 500)) {
                System.out.println("Retry " + retry + " " + URL);
                response = connection.method(Method.GET).execute();
                retry++;
            }
            // parse content into HTML document
            doc = response.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // doc = connection.get();
        // get all links
        Elements links = doc.select("a[href]");
        // deduplicate links
        Set<String> localresultURLs = deduplicate(links);
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
                // delay 0.1 second
                try {
                    // due to how slow the program run, the delay is much greater than 0.1 second
                    // anyway. This is here to prevent runaways. 10 requests per second is very slow
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                extractURL(link, recursiveDepth + 1);
            }
        }

    }

    public LinkedHashSet<String> deduplicate(Elements links) {
        // Regex to filter html
        String htmlPattern = ".*\\.(html|htm|php|asp|aspx)$";
        // Regex to filter links without extension
        String noExtPattern = ".*/([^.]+)$";
        // LinkedHashSet to deduplicate links
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        for (Element link : links) {
            String linkString = trackerStripper(link.attr("abs:href"));
            String strippedLink = trackerStripper(linkString);

            if (strippedLink.contains(baseURL) && !resultURLs.contains(linkString)
                    && (strippedLink.matches(htmlPattern) || strippedLink.matches(noExtPattern))) {
                // duplicate links are not added
                set.add(linkString);
            }
        }
        // conversion to arraylist
        // ArrayList<String> list = new ArrayList<String>(set);
        return set;
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

    public void writeToFile(boolean append, Set<String> localresultURLs) {
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
        // strip everything after the # in the link
        String[] linkParts = link.split("#");
        String output = linkParts[0];
        // strip everything after the ? in the link
        linkParts = output.split("\\?");
        output = linkParts[0];
        return output;
    }

    public void filter(Document doc) {
        // Insert whatever filter you want here
        // This function presents a HTML file formatted as a JSoup document
        // You can use the JSoup API to filter the document
        // Just call it here
    }

}
