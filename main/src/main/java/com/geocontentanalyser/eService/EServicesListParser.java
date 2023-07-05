package com.geocontentanalyser.eService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EServicesListParser {

    // all possible names of services according to FIM katalogue in form of the official CSV table
    private String all_eservices_path = "main/src" + File.separator + "main" + File.separator + "rec" + File.separator + "all_eservices.csv";
    // parse it into a list
    public List<String> eServices = this.parse_eservices();

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
        System.out.println("Parsed " + eServices.size() + " eServices");
        return eServices;
    }
}
