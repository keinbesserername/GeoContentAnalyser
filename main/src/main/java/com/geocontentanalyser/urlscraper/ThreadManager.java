package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadManager implements Runnable {
    // Pool of threads with a maximum of 5 threads
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    String baseURL;
    String fileName;
    Data data;

    public ThreadManager(String baseURL) {
        this.baseURL = baseURL;
        this.data = new Data(baseURL);
        // remove http:// or https:// from the baseURL
        this.fileName = baseURL.replace("www.", "").replace("https://", "")
                .replace("http://", "").replaceAll("[\\\\/:*?\"<>|]", "");
    }

    @Override
    public void run() {
        // remove the previous output file
        writeToFile(false, null);
        // start the thread
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor(baseURL, new Callback() {
            @Override
            public void onDataExtracted(Data newdata) {
                // merge the data
                // output the difference to a new Set
                LinkedHashSet<String> difference = data.mergeData(newdata);
                // write the difference to file
                writeToFile(true, difference);
            }
        });
        executor.execute(siteURLExtractor);
    }

    public void writeToFile(boolean append, LinkedHashSet<String> localresultURLs) {
        // write links to file
        try {
            FileWriter writer = new FileWriter("output/site/" + fileName + ".txt", append);
            if (localresultURLs != null) {
                for (String link : localresultURLs) {
                    writer.write(link + "\n");
                }
            }
            writer.close();
        } catch (Exception e) {
        }
    }
}
