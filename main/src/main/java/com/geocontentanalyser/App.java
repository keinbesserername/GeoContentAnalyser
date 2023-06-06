package com.geocontentanalyser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

import com.geocontentanalyser.urlscraper.Callback;
import com.geocontentanalyser.urlscraper.Data;
import com.geocontentanalyser.urlscraper.ThreadManager;
import com.geocontentanalyser.wikiscraper.WikiScrapperMain;

public class App {
    public static void main(String[] args) throws IOException {

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        Semaphore threadCreation = new Semaphore(10, true);

        List<String> wikiURLlList = WikiScrapperMain.crawler();
        /*
         * //read each line from t.log and save to a list
         * List<String> wikiURLlList = new ArrayList<String>();
         * BufferedReader reader = new BufferedReader(new
         * FileReader("output/2023-05-24T11-57.log"));
         * String line = reader.readLine();
         * while (line != null) {
         * wikiURLlList.add(line);
         * line = reader.readLine();
         * }
         * reader.close();
         */
        for (String URL : wikiURLlList) {
            threadCreation.acquireUninterruptibly();
            ThreadManager threadManager = new ThreadManager(URL, new Callback() {
                @Override
                public void onDataExtracted(Data data) {
                    // TODO Auto-generated method stub
                    System.out.println("Data extracted" + data.getBaseURL());
                    threadCreation.release();
                }
            });
            executor.execute(threadManager);
        }
        executor.shutdown();
    }
}
