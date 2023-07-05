package com.geocontentanalyser.urlscraper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.jsoup.nodes.Document;

// extention of Thread is needed for the usage of semaphore

public class EServicesExtractor extends Thread{

    //
    // initialise variables
    //

    // where glbal variables go
    private Data data;
    // html doc
    private Document doc;
    // keeps track of number of found infobjects in general
    public Integer eservice_counter_global = 0;    
    // 
    public ArrayList<String> found_urls = new ArrayList<String>();
    // url of current landkreis
    public String current_landkreis;
    // where found data is stored
    private String directory;
    // current file, to which infobjects are stored (unique for each landkreis)
    public String filename;
    // "simplify" flag is used not to place the output in "capsules" anymore,
    // it is useful for parcing of infobject.txt files (yet to be implemented)
    public Boolean simplify;

    // all possible names of services according to FIM katalogue in form of the official CSV table
    private String all_eservices_path = "src" + File.separator + "main" + File.separator + "rec" + File.separator + "all_eservices.csv";
    // parse it into a list
    public List<String> eServices = this.parse_eservices();

    // gets populated with InfobjectExtractorThread classes with each new doc being parsed by SiteURLExtractor
    private ArrayList<EServicesExtractorThread> threads = new ArrayList<EServicesExtractorThread>();
    // used to write to an infobject.txt file consequtively, not concurrently, not to corrupt it
    public Semaphore fileSemaphore = new Semaphore(1, isAlive());
    //
    public ArrayList<Semaphore> thread_semaphores = new ArrayList<Semaphore>();

    EServicesExtractor(Data data, String sessionPfad, Boolean simplify){

        // create a folder for the duration of the whole search
        this.data = data;
        this.directory = sessionPfad;
        File dir = new File(this.directory);
        dir.mkdirs();


        // root url is used to separate infobjects by files, depending on their landkreis attachment
        // reconfigure(baseURL); 

        this.simplify = simplify;
    }

    // used from the constructor each time a landkreis site is changed

    public void reconfigure(String baseURL){
        this.current_landkreis = baseURL;
        this.filename = this.directory + File.separator + this.current_landkreis.replaceAll("http(s)?://", "") + ".txt";  
    }

    // parse all possible names of eservices from the official .csv file

    private List<String> parse_eservices(){
        ArrayList<String> eServices = new ArrayList<String>();
        
        // src\main\rec\all_eservices.csv
        try(BufferedReader br = new BufferedReader(new FileReader(all_eservices_path)))  
        {
            String line;
            while((line = br.readLine()) != null) {
                String[] values = line.split(";");
                if(values.length >= 6){
                    if(values[5].replaceAll("[0-9]", "").length() >= 5){
                        eServices.add(values[5]);
                    }                    
                }                
            }
        }catch(FileNotFoundException e){
            System.out.println("Error reading from file");
        }catch(IOException e){
            System.out.println("Other exception");
        }
        return eServices;
    }

    //
    // main function
    //

    public void extract(Document doc, String baseURL){
        this.data.setCount_EServices(this.eservice_counter_global);
        this.reconfigure(baseURL);

        Integer id = 0;
        if(this.threads.size() < 14){
            this.thread_semaphores.add(new Semaphore(1, isAlive()));
            this.threads.add(new EServicesExtractorThread(this, id, this.directory, this.simplify));
            this.threads.get(this.threads.size() - 1).configure(doc, this.filename, this.current_landkreis);
            this.threads.get(this.threads.size() - 1).start();
            id++;
        }else{
            for(Semaphore semaphore : this.thread_semaphores){
                if(semaphore.tryAcquire()){
                    semaphore.release();
                    this.threads.get(thread_semaphores.indexOf(semaphore)).configure(doc, this.filename, this.current_landkreis);
                this.threads.get(thread_semaphores.indexOf(semaphore)).extract();   
                    break;      
                }
            }
        }      

    }

    
}
