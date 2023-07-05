package com.geocontentanalyser.infobject;

import java.io.File;

import java.util.ArrayList;

import java.util.concurrent.Semaphore;

import org.jsoup.nodes.*;

import com.geocontentanalyser.urlscraper.Data;

// extention of Thread is needed for usage of semaphores

public class InfobjectExtractor extends Thread{

    //
    // initialise variables
    //
     
    // where global variables go
    private Data data;
    // keeps track of number of found infobjects in general
    public Integer infobject_counter_global = 0;    
    // url of current landkreis
    public String current_landkreis;
    // name of the directory, where found data is stored
    private String directory;
    // current file, to which infobjects are stored (unique for each landkreis)
    public String filename;
    // "simplify" flag is used not to place the output in "capsules" anymore,
    // it is useful for parcing of infobject.txt files
    public Boolean simplify;

    // all possible kinds of infobjects
    // sg - Sachverständigengesellschaft, ag - Arbeitsgemeindschaft

    public String[] infobjects = {
                                  "museum", "kirche", "schule_", "schule.", "amt_", "amt.", "gymnasium.", "gimnasium_", "dezernat",
                                  "wesen", "bau.", "bau_", "zentrum", "einheit", "rat", "tag", "halle", "sg", "ag", "gemeindschaft",
                                  "gesellschaft", "organisation", "dom"
                                };

    // keeping track of what's been found, not to allow dublicates

    public ArrayList<String> found_urls = new ArrayList<String>();
    public ArrayList<String> found_addresses = new ArrayList<String>();
    public ArrayList<String> found_emails = new ArrayList<String>();
    public ArrayList<String> found_telephones = new ArrayList<String>();
    public ArrayList<String> found_coordinates = new ArrayList<String>();
    

    // gets populated with InfobjectExtractorThread classes with each new doc being parsed by SiteURLExtractor
    private ArrayList<InfobjectExtractrorThread> threads = new ArrayList<InfobjectExtractrorThread>();
    //
    public ArrayList<Semaphore> thread_semaphores = new ArrayList<Semaphore>();
    // used to write to an infobject.txt file consequtively, not concurrently, not to corrupt it
    public Semaphore fileSemaphore = new Semaphore(1, isAlive());

    public InfobjectExtractor(Data data, String sessionPfad, Boolean simplify){
        // create a folder for the duration of the whole search
        this.directory = sessionPfad;
        File dir = new File(this.directory);
        dir.mkdirs();

        this.data = data;      
        
        // if true, will not put infobjects into "capsules"
        this.simplify = simplify;
    } 
    
    // used from SiteURLExtractor each time a landkreis site is changed

    public void reconfigure(String baseURL){
        this.current_landkreis = baseURL;
        this.filename = this.directory + File.separator + this.current_landkreis.replaceAll("http(s)?://", "") + ".txt";  
    }

    // called each time from a thread when it 

    // root function of the class

    public void extract(Document doc, String baseURL){
        reconfigure(baseURL);
        this.data.setCount_InfoObjects(this.infobject_counter_global);

        Integer id = 0;
        if(this.threads.size() < 14){
            this.thread_semaphores.add(new Semaphore(1, isAlive()));
            this.threads.add(new InfobjectExtractrorThread(this, id, this.simplify));
            this.threads.get(this.threads.size() - 1).configure(doc, this.filename, this.current_landkreis);
            this.threads.get(this.threads.size() - 1).run();
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
