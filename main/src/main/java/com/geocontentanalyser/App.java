package com.geocontentanalyser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

import com.geocontentanalyser.eService.EServicesListParser;
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
        if (folder.mkdir())
            System.out.println("Folder created at " + storePath);
        else
            System.out.println("Folder already exists at " + storePath);

        // create a folder that store the data of this session
        String sessionPath = storePath + File.separator
                + Instant.now().toString().replace(":", "-").replace(".", "-").substring(0, 16);
        File file = new File(sessionPath);
        file.mkdir();

        // create a folder that store the URLs
        File newfile = new File(sessionPath + File.separator + "URL");
        newfile.mkdir();

        EServicesListParser eServicesListParser = new EServicesListParser();
        List<String> eServices = eServicesListParser.eServices;

        // Allocate the thread pool
        // The number of threads is set to 30
        int threadCount = 15;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        Semaphore threadCreation = new Semaphore(threadCount, true);

        // Acquire the list of URLs from Wikipedia
        // List<String> wikiURLlList = WikiScrapperMain.crawler(sessionPath);
        // read each line from t.log and save to a list
        List<String> wikiURLlList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader("site.log"));
        String line = reader.readLine();
        while (line != null) {
            wikiURLlList.add(line);
            line = reader.readLine();
        }
        reader.close();
        // create a list to store the data objects
        List<Data> dataList = new ArrayList<>();

        // Start the threads
        for (String URL : wikiURLlList) {
            threadCreation.acquireUninterruptibly();

            ThreadManager threadManager = new ThreadManager(URL, sessionPath, eServices, new Callback() {
                @Override
                public void onDataExtracted(Data data) {

                    dataList.add(data);
                    threadCreation.release();
                }
            });
            executor.execute(threadManager);
            //threadManager.run();

        }
        executor.shutdown();

        // wait for all threads to finish

        // At this point, all the data count should be in the list<Data> dataList
        // put whatever statistic function here

        // print statistics to file
        // printStatistics(dataList, sessionPath);
    }

    public static void printStatistics(List<Data> dataList, String sessionPath) {
        try {

            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(sessionPath + File.separator + "statistic.log", false));
            writer.write(
                    "|Landkreis URL|Address|Coordinates|Embedded Maps|External Maps|Info Objects|E-Services|\n| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |\n");
            for (Data data : dataList) {
                writer.append("|" + data.getBaseURL() + "|" + data.getCount_Address() + "|"
                        + data.getCount_Coordinates() + "|" + data.getCount_EmbeddedMaps() + "|"
                        + data.getCount_ExternalMaps() + "|" + data.getCount_InfoObjects() + "|"
                        + data.getCount_EServices() + "|\n");
            }
            writer.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

}
