package com.geocontentanalyser.urlscraper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.geocontentanalyser.eService.EServicesExtractor;
import com.geocontentanalyser.infobject.InfobjectExtractor;

public class SiteURLExtractor implements Runnable {
    LinkedHashSet<String> resultURLs = new LinkedHashSet<String>();
    String time;
    InfobjectExtractor infobjectExtractor;
    EServicesExtractor eServicesExtractor;
    String sessionPath;

    String baseURL;
    String URL;
    Data data = new Data(baseURL);
    String noProtocolBaseURL;
    List<String> eServices;

    private Callback callback;

    public SiteURLExtractor(String baseURL, String URL, String sessionPath,
            List<String> eServices, Callback callback) {

        this.baseURL = baseURL;
        this.callback = callback;
        this.URL = URL;
        this.sessionPath = sessionPath;
        this.noProtocolBaseURL = baseURL.replace("https://", "").replace("http://", "").replace("www.", "");
        this.time = Instant.now().toString().replace(":", "-").replace(".", "-").substring(0, 16);
        this.eServices = eServices;
        this.infobjectExtractor = new InfobjectExtractor(this.data, this.sessionPath, false);
        this.eServicesExtractor = new EServicesExtractor(this.data, this.sessionPath, false, eServices);
    }

    public Data extractURL(String URL) throws IOException, InterruptedException {

        Document doc = new Document("");

        Connection connection = Jsoup.connect(URL).followRedirects(true)
                .userAgent(
                        "PostmanRuntime/7.26.5");

        // this is very slow. The bottleneck lays here

        try {
            Response response = connection.method(Method.GET).execute();
            doc = response.parse();
        } catch (UnsupportedMimeTypeException m) {
            // if the mime type is not supported, return empty data
            System.out.println("Unsupported mime type");
            return data;
        } catch (SocketTimeoutException e) {
            // if the socket times out, retry
            System.out.println("Socket timed out");
            retry(connection);
        } catch (HttpStatusException e) {
            // if the socket times out, retry
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                return data;
            }
            System.out.println("HTTP Status Exception");
            retry(connection);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // logic to handle internal redirection

        try {
            Elements metaTags = doc.select("meta[http-equiv=Refresh]");
            if (metaTags.size() > 0) {
                // Get the content attribute value
                String content = metaTags.attr("content");
                // Extract the URL from the content attribute value
                // System.out.println(content);
                String redirectUrl = content.substring(content.indexOf("http"));
                // Navigate to the redirect URL
                doc = Jsoup.connect(redirectUrl).get();
            }
        } catch (Exception e) {
            System.out.println("Internal redirection failed");
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
            String linkString = link.attr("abs:href");
            String strippedLink = trackerStripper(linkString);

            if (strippedLink.contains(noProtocolBaseURL) && (!strippedLink.contains("javascript"))
                    && isLink(strippedLink)
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
            e.getMessage();
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
        // System.out.println(doc.toString());
        // System.out.println(StringUtils.countMatches(doc.toString(), "<map"));

        try {
            interactiveMapCount(doc);
            externalMapCount(doc);
            infobjectExtractor.extract(doc, baseURL);
            eServicesExtractor.extract(doc, baseURL);
        } catch (Exception e) {
        }
    }

    public void interactiveMapCount(Document doc) {
        Elements iframes = doc.select("iframe");
        // Iterate over each iframe element
        for (Element iframe : iframes) {
            String src = iframe.attr("src");
            if (src.contains("google.com/maps") || src.contains("bing.com/maps")) {
                data.count_EmbeddedMaps++;
            }
        }
    }// TODO: test this properly

    public void externalMapCount(Document doc) {
        // Get all links
        Elements links = doc.select("a[href]");
        // filter for google maps or bing maps
        for (Element link_Element : links) {
            // convert to absolute link
            String link = link_Element.attr("abs:href");
            if (link.contains("google.com/maps") || link.contains("bing.com/maps")) {
                data.count_ExternalMaps++;
            }
        }
    }

    public Boolean isLink(String inputString) {
        /* Try creating a valid URL */
        try {
            new java.net.URL(inputString).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Document retry(Connection connection) {
        int retry = 0;
        Document doc = new Document("");
        // retry 3 times if socket times out
        while (retry < 3) {
            // System.out.println("Retry " + retry + " " + URL);
            try {
                // exponential backoff
                Thread.sleep((long) (100 * Math.pow(3, retry + 1)));
                Response response = connection.method(Method.GET).execute();
                doc = response.parse();
                // System.out.println("success " + URL);
                break;
            } catch (Exception e) {
                retry++;
                // System.out.println("failed");
            }
        }
        return doc;
    }
}
