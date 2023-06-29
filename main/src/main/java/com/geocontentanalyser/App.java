package com.geocontentanalyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import com.geocontentanalyser.urlscraper.Callback;
import com.geocontentanalyser.urlscraper.Data;
import com.geocontentanalyser.urlscraper.ThreadManager;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

public class App {
    public static void main(String[] args) throws IOException {

        System.out.println(
                "Enter the path to the folder where you want to save the data\nThe folder will be created if it doesn't exist");
        Scanner scanner = new Scanner(System.in);

        String storePath = scanner.nextLine();
        scanner.close();

        // path validation
        try {
            if (storePath == null || storePath.isEmpty())
                throw new Exception();
            Path path = Paths.get(storePath);
            path.toAbsolutePath();
        } catch (Exception e) {
            storePath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator
            + "geocontentanalyser";
        }

        // create a folder that store the whole project
        File folder = new File(storePath);
        if(folder.mkdir())
            System.out.println("Folder created at " + storePath);
        else
            System.out.println("Folder already exists at " + storePath);

        // create a folder that store the data of this session
        String sessionPath =storePath + File.separator + Instant.now().toString().replace(":", "-").replace(".", "-").substring(0, 16);
        File file = new File(sessionPath);
        file.mkdir();

        // Allocate the thread pool
        // The number of threads is set to 15
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(15);
        Semaphore threadCreation = new Semaphore(15, true);

        // Acquire the list of URLs from Wikipedia
        List<String> wikiURLlList = WikiScrapperMain.crawler(sessionPath);
        // read each line from t.log and save to a list
        // make it quicker for testing
        /* 
        List<String> wikiURLlList = new ArrayList<String>();
        try {
            
            BufferedReader reader = new BufferedReader(new FileReader("output/2023-05-24T11-57.log"));
            String line = reader.readLine();
            while (line != null) {
                wikiURLlList.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        */
        // Start the threads
        for (String URL : wikiURLlList) {
            threadCreation.acquireUninterruptibly();
            ThreadManager threadManager = new ThreadManager(URL, sessionPath, new Callback() {
                @Override
                public void onDataExtracted(Data data) {
                    System.out.println("Data extracted" + data.getBaseURL());
                    threadCreation.release();
                }
            });
            executor.execute(threadManager);
        }
        executor.shutdown();
    }
}
