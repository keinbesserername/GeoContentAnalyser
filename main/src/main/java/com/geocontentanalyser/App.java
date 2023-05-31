package com.geocontentanalyser;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.geocontentanalyser.urlscraper.ThreadManager;

public class App {
    public static void main(String[] args) throws IOException {
        //List<String> wikiURLlList = WikiScrapperMain.crawler();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        
        /* 
        //read each line from t.log and save to a list
        List<String> wikiURLlList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new FileReader("output/2023-05-24T11-57.log"));
        String line = reader.readLine();
        while (line != null) {
            wikiURLlList.add(line);
            line = reader.readLine();
        }
        reader.close();

        for (String URL : wikiURLlList) {
            
            ThreadManager threadManager = new ThreadManager(URL);
            executor.execute(threadManager);
        }

        executor.shutdown();
        */
        ThreadManager threadManager = new ThreadManager("https://www.saalekreis.de/");
        executor.execute(threadManager);
    }
}
