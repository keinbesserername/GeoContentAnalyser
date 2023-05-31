package com.geocontentanalyser.urlscraper;

import java.io.IOException;
import java.util.LinkedHashSet;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SiteURLExtractor implements Runnable {

    String baseURL;
    String URL;
    Data data = new Data(baseURL);

    private Callback callback;

    public SiteURLExtractor(String baseURL, String URL, Callback callback) {
        this.baseURL = baseURL;
        this.callback = callback;
        this.URL = URL;
    }

    public Data extractURL(String URL) throws IOException {

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
                Thread.sleep(100);
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
        // filter the document
        filter(doc);
        // output to Data
        data.setSet(deduplicate(links));
        // return data
        return data;
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

            if (strippedLink.contains(baseURL) && (!strippedLink.contains("javascript")) && isLink(strippedLink)
                    && (strippedLink.matches(htmlPattern) || strippedLink.matches(noExtPattern))) {

                set.add(linkString);
            }
        }
        return set;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public void run() {
        try {
            extractURL(URL);
            callback.onDataExtracted(data);
        } catch (Exception e) {
            e.printStackTrace();
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

    public Boolean isLink(String inputString) {
        /* Try creating a valid URL */
        try {
            new java.net.URL(inputString).toURI();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

}
