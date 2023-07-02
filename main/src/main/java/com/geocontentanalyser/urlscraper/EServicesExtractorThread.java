package com.geocontentanalyser.urlscraper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class EServicesExtractorThread extends Thread {

    //
    // variables initialisation
    //
    
    private Document doc;
    // array of all available eservices, parsed from src\main\rec\e_services.csv
    private List<String> eServices;
    private String filename;
    private EServicesExtractor eServicesExtractor;
    private Boolean simplify;

    // parameters of an eservice

    private String title = "| Title       |  ";
    private String url   = "| URL         |  ";

    EServicesExtractorThread(Document doc, EServicesExtractor eServicesExtractor, String directory, String current_landkreis, Boolean simplify){
        this.doc = doc;
        this.eServicesExtractor = eServicesExtractor;
        this.eServices = this.eServicesExtractor.eServices;
        this.simplify = simplify;
        this.filename = directory + File.separator + current_landkreis.replaceAll("http(s)?://", "") + ".txt";
    }

    //
    // secondary functions, that are called from the scope of the main fuction start()
    //

    // used from start() to set up the baseline parameter values for an infobject

    private void configure(){
        this.title =      "| Title       |  ";                
        this.url =        "| URL         |  " +  this.doc.location();

        // this.biggest_length = this.url.length();

        // add url to the list to avoid dublicates, that can occur if the recursive depth is set as high
        this.eServicesExtractor.found_urls.add(this.doc.location());
    }

    // determines, if this page's title matches with an entry of this.eServices

    private String findTitle(){

        // search though header tags

        String[] header_tags = {"h1","h2","h3"};
        for(String tag : header_tags){
            Elements tags = this.doc.select(tag);
            if(!tags.isEmpty()){
                if(this.eServices.contains(tags.get(0).text())){
                    if(tags.get(0).text() != ""){
                        return tags.get(0).text() + "  <-----";
                    }                    
                }
                
            }
        }

        // search through doctitle

        if(this.eServices.contains(this.doc.title())){
            return doc.title() + "  <-----";
        }else{
            return "";
        }
    }

    //
    // the main function
    //

    @Override
    public void start(){
        this.configure();

        this.title += findTitle();

        // further, only if title matches with an eservice

        if(title != "| Title       |  "){        
        
            // save all findings

            try{
                this.eServicesExtractor.fileSemaphore.acquire();
            }catch(InterruptedException e){
            }
            

            try(FileWriter writer = new FileWriter(filename, true)){
                writer.write(this.title);
                writer.write("\n");
                writer.write(this.url);
                writer.write("\n\n");
                writer.close();
            }catch(IOException e){
            }

            this.eServicesExtractor.fileSemaphore.release();

        }
    }


}
