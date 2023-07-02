package com.geocontentanalyser.urlscraper;

import java.io.File;

import java.util.ArrayList;

import java.util.concurrent.Semaphore;

import org.jsoup.nodes.*;

// extention of Thread is needed for usage of semaphores

public class InfobjectExtractor extends Thread{

    //
    // initialise variables
    //
     
    private Document doc;
    // infobject files' names are unique
    private String time;
    // keeps track of number of found infobjects in general
    public Integer infobject_counter_global = 0;    
    // keeps track of found maps (even if they're outside of infobjects)
    public Integer map_counter = 0;
    // url of current landkreis
    public String current_landkreis;
    // name of the directory, where found data is stored
    private String directory;
    // current file, to which infobjects are stored (unique for each landkreis)
    public String filename;
    // "simplify" flag is used not to place the output in "capsules" anymore, it is useful for parcing of infobject.txt files (yet to be implemented)
    public Boolean simplify;

    // all possible kinds of infobjects
    // sg - Sachverständigengesellschaft, ag - Arbeitsgemeindschaft

    public String[] infobjects = {"museum", "kirche", "schule_", "schule.", "amt_", "amt.", "gymnasium.", "gimnasium_", "dezernat", "wesen", "bau.", "bau_",
                                  "zentrum", "einheit", "rat", "tag", "halle", "sg", "ag", "gemeindschaft", "gesellschaft", "organisation", };

    // keeping track of what's been found, not to allow dublicates

    public ArrayList<String> found_urls = new ArrayList<String>();
    public ArrayList<String> found_addresses = new ArrayList<String>();
    public ArrayList<String> found_emails = new ArrayList<String>();
    public ArrayList<String> found_telephones = new ArrayList<String>();

    // gets populated with InfobjectExtractorThread classes with each new doc being parsed by SiteURLExtractor
    private ArrayList<Thread> threads = new ArrayList<Thread>();
    // used to write to an infobject.txt file consequtively, not concurrently, not to corrupt it
    public Semaphore fileSemaphore = new Semaphore(1, isAlive());

    InfobjectExtractor(String baseURL, String time, Boolean simplify){
        // create a folder for the duration of the whole search
        this.time = time;
        this.directory = "output" + File.separator + this.time + File.separator + "infobjects" + File.separator;
        File dir = new File(this.directory);
        dir.mkdirs();

        // root url is used to separate infobjects by files, depending on their landkreis attachment
        reconfigure(baseURL);      
        
        // if true, will not put infobjects into "capsules"
        this.simplify = simplify;
    } 
    
    // used from SiteURLExtractor each time a landkreis site is changed

    public void reconfigure(String baseURL){
        this.current_landkreis = baseURL;
        this.filename = this.directory + File.separator + this.current_landkreis.replaceAll("http(s)?://", "") + ".txt";  
    }

    // root function of the class

    public void extract(Document doc){
        this.doc = doc;
        this.threads.add(new InfobjectExtractrorThread(this.doc, this, this.filename, this.current_landkreis, this.simplify));
        this.threads.get(this.threads.size() - 1).start();
    }

    
   
}
