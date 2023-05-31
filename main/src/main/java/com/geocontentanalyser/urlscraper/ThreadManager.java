package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.util.LinkedHashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadManager implements Runnable {
    // Pool of threads with a maximum of 5 threads
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    String baseURL;
    String fileName;
    Data data;
    Semaphore writeProtection = new Semaphore(1);
    Semaphore threadLimitSemaphore = new Semaphore(5, true);
    // FIFO thread safe queue
    BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();

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
        // prime the system with initial data
        // it doesnt matter if the boolean is true or false
        // it is just to differentiate between the two overloaded methods
        extractorCall(baseURL).run();

        // Utilizing the blocking queue, as iterator can throw
        // ConcurrentModificationException
        while (!blockingQueue.isEmpty()) {
            try {
                String URL = blockingQueue.poll();
                // take thread count semaphore
                threadLimitSemaphore.acquireUninterruptibly();
                executor.execute(extractorCall(baseURL,URL));
                // limit to maximum of 10 requests per second. Realistically,
                // it is unlikely that thread would finish its execution within 100ms
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        // executor.shutdown();
    }

    // write the links to file, take in a boolean to determine whether to append or
    // not, and a set of links to write
    public void writeToFile(boolean append, LinkedHashSet<String> URLs) {
        // write links to file
        try {
            FileWriter writer = new FileWriter("output/site/" + fileName + ".txt", append);
            if (URLs != null) {
                for (String link : URLs) {
                    writer.write(link + "\n");
                }
            }
            writer.close();
        } catch (Exception e) {
        }
    }

    // overloaded method for the first execution
    public SiteURLExtractor extractorCall(String baseURL) {
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor(baseURL, baseURL, new Callback() {
            @Override
            public void onDataExtracted(Data newdata) {
                data.mergeData(newdata);
                addToQueue(data.set);
                writeToFile(true, data.set);
            }
        });
        return siteURLExtractor;
    }

    // overloaded method for the next execution
    public SiteURLExtractor extractorCall(String BaseURL, String URL) {
        // take thread count semaphore
        // start the thread
        SiteURLExtractor siteURLExtractor = new SiteURLExtractor(baseURL, URL, new Callback() {
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
                writeToFile(true, difference);
                writeProtection.release();
                // release thread count semaphore
                threadLimitSemaphore.release();
            }
        });
        return siteURLExtractor;
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