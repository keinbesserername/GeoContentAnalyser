package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadManager implements Runnable {
    // Pool of threads with a maximum of 2 threads
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    String baseURL;
    String fileName;
    Data data;
    // semaphore to prevent dirty read/write, and limit the number of threads
    Semaphore writeProtection = new Semaphore(1);
    Semaphore threadLimitSemaphore = new Semaphore(5, true);
    // FIFO thread safe queue
    BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();
    Callback callback;
    String sessionPath;
    List<String> eServices;

    public ThreadManager(String baseURL, String sessionPath, List<String> eServices, Callback callback) {
        this.baseURL = baseURL;
        this.data = new Data(baseURL);
        // remove http:// or https:// from the baseURL
        this.sessionPath = sessionPath;
        this.fileName = sessionPath + "/URL/" + baseURL.replace("www.", "").replace("https://", "")
                .replace("http://", "").replaceAll("[\\\\/:*?\"<>|]", "");
        this.callback = callback;

        this.eServices = eServices;
    }

    @Override
    public void run() {
        // start the thread
        // prime the system with initial data
        // extractor got 1 argument for the first execution
        // 2 arguments for the next executions
        extractorCall(baseURL).run();
        while (!blockingQueue.isEmpty()) {
            try {
                String URL = blockingQueue.poll();
                // take thread count semaphore
                threadLimitSemaphore.acquireUninterruptibly();

                executor.execute(extractorCall(baseURL, URL));
                // limit to maximum of 2 requests per second.
                // most of the time, the execution time is less than 500ms
                // but to be safe, we will limit it to 2 requests per second
                Thread.sleep(500);
                // System.out.println("Count right now: " + countData[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Finished " + baseURL);
        executor.shutdown();
        callback.onDataExtracted(data);
    }

    // write the links to file, take in a boolean to determine whether to append or
    // not, and a set of links to write
    public void writeToFile(LinkedHashSet<String> URLs) {
        // write links to file
        try {
            FileWriter writer = new FileWriter(fileName + ".log", true);
            if (URLs != null) {
                for (String link : URLs) {
                    writer.write(link + "\n");
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // overloaded method for the first execution
    public SiteURLExtractor extractorCall(String baseURL) {
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor(baseURL, baseURL, sessionPath, eServices,
                new Callback() {
                    @Override
                    public void onDataExtracted(Data newdata) {
                        data.mergeData(newdata);
                        addToQueue(data.set);
                        writeToFile(data.set);
                    }

                    @Override
                    public void onRequestDone() {
                        // release thread count semaphore
                        threadLimitSemaphore.release();
                    }
                });
        return siteURLExtractor;
    }

    // overloaded method for the next execution
    public SiteURLExtractor extractorCall(String BaseURL, String URL) {
        // take thread count semaphore
        // start the thread
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor(baseURL, URL, sessionPath, eServices, new Callback() {
            @Override
            public void onDataExtracted(Data newdata) {
                // semaphore to prevent dirty read/write
                writeProtection.acquireUninterruptibly();
                // merge the data
                // output the difference to a new Set
                LinkedHashSet<String> difference = data.mergeData(newdata);
                // adding the difference to the set
                addToQueue(difference);
                // write the difference to file
                writeToFile(difference);
                //writeTemporaryData();
                //System.out.print(URL+"\n");
                writeProtection.release();       
            }
            @Override
            public void onRequestDone() {
                // release thread count semaphore
                threadLimitSemaphore.release();
            }
        });
        return siteURLExtractor;
    }

    public void writeTemporaryData() {
        FileWriter writer;
        try {
            writer = new FileWriter(fileName + ".tmp", true);
            writer.write(data.getCount_InfoObjects() + "-" + data.getCount_EServices() + "-" + data.getCount_Address()
                    + "-" + data.getCount_Coordinates() + "-" + data.getCount_EmbeddedMaps() + "-"
                    + data.getCount_ExternalMaps() + "-" + data.set.size() + "\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // add the links to the blocking queue
    public void addToQueue(LinkedHashSet<String> set) {
        for (String link : set) {
            if (link != null) {
                // push the link to the blocking queue
                blockingQueue.add(link);
            }
        }
    }
}