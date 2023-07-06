package com.geocontentanalyser.infobject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.geocontentanalyser.urlscraper.Data;

public class InfobjectExtractorThread extends Thread {
    // unique identifier
    public Integer id;
    // main class is passed to an instance of this thread class in order to change the former's attributes, that are global for all infobjects
    private InfobjectExtractor infobjectExtractor;
    // determines, if infobjects should be dumped into "capsules" in infobject.txt files (to be implemented)
    private Boolean simplify;

    // the html document being parsed
    private Document doc;
    // where is being written (full path from the project's root)
    private String filename;
    //
    private String current_landkreis;

    public Data data;
    
    // parameters of infobjects 
    private HashMap<String,String> infobject_features = new HashMap<>();

    // parameters of an infobject by default               
    private String default_title, default_url, default_address, default_email, default_telephone, default_open_times, default_coordinates;
    // biggest length from all of the parameters (for closing bars in infobject.txt; for more info check out draw_right_bar() funciton)
    private Integer biggest_length;

    // list of found containers, not to search for them multiple times
    private Elements containers, text_tags;

    InfobjectExtractorThread(InfobjectExtractor infobjectExtractor, Integer id, Boolean simplify, Data data){
        this.infobjectExtractor = infobjectExtractor;
        this.id = id;
        this.simplify = simplify;
        this.data = data;
        // implement or drop simplified output
        if(this.simplify){
            this.default_title =       "Title       :";                
            this.default_url =         "URL         :";
            this.default_address =     "Address     :";
            this.default_email =       "Email       :";
            this.default_telephone =   "Telephone   :";
            this.default_open_times =  "Open times  :";
            this.default_coordinates = "Coordinates :";
        }else{
            this.default_title =       "| Title       |  -----> ";                
            this.default_url =         "| URL         | "        ;
            this.default_address =     "| Address     | "        ;
            this.default_email =       "| Email       | "        ;
            this.default_telephone =   "| Telephone   | "        ;
            this.default_open_times =  "| Open times  | "        ;
            this.default_coordinates = "| Coordinates | "        ;
        }
    }



    // used from start() to set up the baseline parameter values for an infobject
    public void configure(Document doc, String filename, String current_landkreis){
        this.doc = doc;
        this.filename = filename;
        this.current_landkreis = current_landkreis;

        infobject_features.put("title", this.default_title);
        infobject_features.put("title", this.default_title);                
        infobject_features.put("url", this.default_url +  this.doc.location());
        infobject_features.put("address", this.default_address);
        infobject_features.put("email", this.default_email);
        infobject_features.put("telephone", this.default_telephone);
        infobject_features.put("open_times", this.default_open_times);
        infobject_features.put("coordinates", this.default_coordinates);

        this.biggest_length = this.infobject_features.get("url").length();

        this.containers = this.doc.select("span,td,div");
        this.text_tags = this.doc.select("p,span,text");
    }    

    private String findCoordinates(){
        // search for degrees, minutes, & seconds
        Pattern pattern = Pattern.compile("(\\d\\d)\u00B0(\\d\\d)'(\\d\\d)\"[N,n,S,s,W,w,E,e][,|.|\\s]*(\\d\\d)\u00B0(\\d\\d)'(\\d\\d)\"[N,n,S,s,W,w,E,e]");
        Matcher matcher = pattern.matcher(this.doc.text());

        if(matcher.find()){
            this.infobjectExtractor.found_coordinates.add(matcher.group(0));
            return matcher.group(0);
        }else{
            // search for decimal degrees 
            pattern = Pattern.compile("(-?)(\\d){1,2}[,|.](\\d){5,16}[,|.|\\s]*(-?)(\\d){1,3}[,|.](\\d){5,16}");
            matcher = pattern.matcher(this.doc.text());
            if(matcher.find()){
                this.infobjectExtractor.found_coordinates.add(matcher.group(0));
                return matcher.group(0);
            }else{
                return "not found";
            }
        }
    }

    private String findAddress(){
        String addressContainer = "", index = "", street = "";

        Pattern pattern = Pattern.compile("[0-9]{5} [A-Z][a-z]*");
        Matcher matcher = pattern.matcher(this.doc.text());
        
        ArrayList<String> indexes = new ArrayList<String>();
        ArrayList<String> streets = new ArrayList<String>();

        // check the presence of <address> tag
        Elements elements = this.doc.select("address");
        // just carve out everything and leave the word "address" out
        if(!elements.isEmpty()){
            return elements.get(0).text().replace("(A|a)ddress(\s|\n|)","").replace("(A|a)nschrift(\s|\n|)","");
        }

        // if not <address>, search elsewhere
        elements = this.containers;
        for(Element element : elements){
            while(matcher.find()){
                indexes.add(matcher.group());
            }
        
            // trying to get the whole address from parrent element
            if(!indexes.isEmpty()){
                // trying to find email in the block of address
                this.infobject_features.put("email", this.default_email + findEmail(addressContainer));
                    
                // trying to find phone number in the block of address           
                this.infobject_features.put("telephone", this.default_telephone + findTelephone(addressContainer));

                // searching for the direct reference
                addressContainer = element.parent().text();
                pattern = Pattern.compile("((anschrift)|(addresse))(.|\n)+", Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(addressContainer);
                if(matcher.find()){
                    street = matcher.group(0);                    
                }else{
                    // if unsuccessful, search for pattern of street-naming
                    pattern = Pattern.compile("((\\S+((S|s)tr|weg|platz))(.|) [1-9][0-9]*)");
                    matcher = pattern.matcher(addressContainer);
                    if(matcher.find()){
                        street = matcher.group()  + " " + index;
                    }else{
                        // if pattern of street-naming has not been found, search for more general pattern
                        pattern = Pattern.compile("[A-z|-]{5,}\s[1-9]+\s+[0-9]{5}(\s|)[A-Z][a-z]*");
                        matcher = pattern.matcher(elements.first().text());
                        if (matcher.find()){
                            // check, if this address was encountered prior
                            if(!this.infobjectExtractor.found_addresses.contains(matcher.group(0))){
                                // remove unnecessary information
                                street = street.replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(\s*)(?-i)", "");
                                return street;  
                            }else{
                                streets.add(matcher.group(0));
                            }                
                        }
                    }
                }

                // check, if the found by the pattern address is unique
                if(this.infobjectExtractor.found_addresses.contains(street)){
                    streets.add(street);
                    street = "";
                }

                // in case, unique address was found
                if(street != ""){
                    // remove newline characters and redundant whitespaces
                    street = elements.first().text().replace("\n| {2,}", ", ");
                    
                    // remove unnecessary information
                    street = street.replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(\s*)(?-i)", "");

                    // add the found address to the list 
                    this.infobjectExtractor.found_addresses.add(street);

                    return street;  
                // in case, all found streets are not unique   
                }else{
                    street = streets.get(0).replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(\s*)(?-i)", "") + " (not unique)";
                    return street;
                }
                                              
            }else{
                return "not found";
            }           
        }
        return "not found";
    }

    private String findTitle(){
        String[] header_tags = {"h1","h2","h3"};
        for(String tag : header_tags){
            if(!this.doc.select(tag).isEmpty()){
                if(this.simplify){
                    return this.doc.select(tag).first().text();
                }else{
                    return this.doc.select(tag).first().text() + "  <-----";
                }
            
            }
        }
        if(this.simplify){
            return this.doc.title();
        }else{
            return this.doc.title() + "  <-----";
        }
        
    }

    private String findEmail(String text){
        // find a pattern with @ or [at] in the middle and dot at the end
        Pattern pattern = Pattern.compile("[\n\r\s]\\S+\s{0,1}(@|\\[at\\])\s{0,1}\\S+\\.\\S{1,6}");
        Matcher matcher = pattern.matcher(text);
        if(matcher.find()){
            if(!this.infobjectExtractor.found_emails.contains(matcher.group(0))){
                this.infobjectExtractor.found_emails.add(matcher.group(0));
                return matcher.group(0).replace("[at]","@").replace(" ", "");
            }else{
                return "not found";
            }            
        }else{
            return "not found";
        }
    }

    private String findOpenTimes(){
        for(Element element : this.text_tags){
            // searching through all containers on page
            Pattern pattern = Pattern.compile("[(Ö|ö)ffnungszeiten|Sprechzeiten]");
            Matcher matcher = pattern.matcher(element.text());
            if(matcher.find()){
                // if found, see if the whole open time is in container
                pattern = Pattern.compile("[(\u00D6|\u00F6)ffnungszeiten|Sprechzeiten](\s|\n)*((Mo|Di|Mi|Do|Fr|Sa|So).{10,45}Uhr(\n|\s))+", Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(element.text());
                if(matcher.find()){
                    return matcher.group(0).replace("[(\u00D6|\u00F6)ffnungszeiten|Sprechzeiten](\s|)", "");
                }else{
                    // if not in the container itself, search through parent element
                    matcher = pattern.matcher(element.parent().text());
                    if(matcher.find()){
                        return matcher.group(0).replace("[(Ö|ö)ffnungszeiten|Sprechzeiten](\s|)", "");
                    }else{
                        matcher = pattern.matcher(element.parent().parent().text());
                        if(matcher.find()){
                            return matcher.group(0).replace("[(Ö|ö)ffnungszeiten|Sprechzeiten](\s|)", "");
                        }
                        /*
                        else{
                            // if no success, just drop everything in the parent element
                            return element.parent().text().replace("(Ö|ö)ffnungszeiten( |)", "");

                            // if no success, just drop everything in the element
                            return element.text().replace("(Ö|ö)ffnungszeiten( |)", "");
                        }
                        */
                    }
                }
            }
        }
        return "not found";
    }

    private String findTelephone(String text){
        Pattern pattern = Pattern.compile("(Tel(\\.|e(f|ph)on(e|)|))(:|)(\s|\n)*[0-9][0-9\\-\\/\s]{5,15}[0-9]");
        Matcher matcher = pattern.matcher(text);
        String telephone_refined = "";
        if(matcher.find()){
            // trying to find telephone by label

            telephone_refined = matcher.group(0).replaceAll("[A-z]|\\.| {2,}|:", "");
            if(!this.infobjectExtractor.found_telephones.contains(telephone_refined)){
                this.infobjectExtractor.found_telephones.add(telephone_refined);
                return telephone_refined.replace(" ", "").replace("-", "").replace("/", "");
            }else{
                // trying to find unlabeled telephone 
                
                pattern = Pattern.compile("[0-9][0-9\\-\\/\s]{5,15}[0-9]");
                matcher = pattern.matcher(text);
                if(matcher.find()){
                    if(!this.infobjectExtractor.found_telephones.contains(matcher.group(0))){
                        this.infobjectExtractor.found_telephones.add(matcher.group(0));
                        return matcher.group(0);
                    }else{
                        return "not found";
                    } 
                }else{
                    return "not found";
                }            
            }
        }    
        return "not found";
    }

    // functions for formalisation of output into infobject.txt

    // draw a bar, that has a length, no less than of the longest textual representation of an infobjects field (most likely, URL)
    private void draw_horizontal_bar(FileWriter writer){
        Integer i = 0;
        try{
            while(i < this.biggest_length + 5){
                writer.write("-");
                i++;
            }        
        }catch(IOException e){
        }
    }

    // change a string, so that vertical bars of each "capsule" in infobject.txt files is properly closed at the right side
    private String draw_right_bar(String string){
        Integer difference = this.biggest_length - string.length() + 5;
        if(difference > 0){
            for(Integer i = 0; i <= difference; i++){
                string = string + " ";
            }
        }
        string = string + "|\n";
        return string;
    }

    // if a page looks like an infobject page, we try to parse it 

    private void parse(){
        // search for coordinate pairs (even if it's not an infobject page)
        this.infobject_features.put("coordinates", this.default_coordinates + findCoordinates());

        // regex search for address
        this.infobject_features.put("address", this.default_address + findAddress());
        
        // it is an infobject, only if it has physical address, otherwise, it's a missmatch
        if(this.infobject_features.get("address") != this.default_address){

            // searching tile
            this.infobject_features.put("title", this.default_title + findTitle());

            // find open times, if they haven't yet been found
            this.infobject_features.put("open_times", this.default_open_times + findOpenTimes());
            
            // searching for email, if it hadn't been yet found
            if(this.infobject_features.get("email") == this.default_email){
                this.infobject_features.put("email", this.default_email + findEmail(this.doc.text()));
            } 

            // searching for phone number, if it hadn't been yet found
            if(this.infobject_features.get("telephone") == this.default_telephone){
                this.infobject_features.put("telephone", this.default_telephone + findTelephone(this.doc.text()));
            } 
            
            // wait for semaphore's availability
            try{
                this.infobjectExtractor.fileSemaphore.acquire();
            }
            catch(InterruptedException e){
            }

            // incrementing the infobject counter
            this.infobjectExtractor.infobject_counter_global++;
        }
    }    
    
    // write all findings to the file
    private void save_results(){

        // add url to the list to avoid dublicates, that can occur if the recursive depth is set as high
        this.infobjectExtractor.found_urls.add(this.doc.location());
        
        // output global metrics
        try{
            RandomAccessFile file = new RandomAccessFile(this.filename, "rw");

            // found infobjects and maps
            byte[] bytes = 
            (this.current_landkreis + 
            "\nInfobjects Found: " + this.infobjectExtractor.infobject_counter_global + 
            "\nUnique Addresses Found: " + this.infobjectExtractor.found_addresses.size() +
            "\nCoordinate Pairs Found: " + this.infobjectExtractor.found_coordinates.size() + "\n\n\n").getBytes();
            data.setCount_InfoObjects(this.infobjectExtractor.infobject_counter_global);
            data.setCount_Address(this.infobjectExtractor.found_addresses.size());
            data.setCount_Coordinates(this.infobjectExtractor.found_coordinates.size());
            file.write(bytes, 0, bytes.length);

            file.close();
        }
        catch(IOException e){
        }

        // writing the found in the file
        try {
            FileWriter writer = new FileWriter(this.filename, true);

            if(this.simplify){
                for(HashMap.Entry<String, String> feature : this.infobject_features.entrySet()){
                    writer.write(feature.getValue());
                }
                writer.write("\n\n\n");
                writer.close();                
            }else{
                // separator
                writer.write("/");
                this.draw_horizontal_bar(writer);
                writer.write("\\\n");

                // found features
                for(HashMap.Entry<String, String> feature : this.infobject_features.entrySet()){
                    writer.write(this.draw_right_bar(feature.getValue()));
                }

                // bottom separator
                writer.write("\\");
                this.draw_horizontal_bar(writer);
                writer.write("/ \n\n\n");
                writer.close();                
            }
            
        } catch (IOException e) {
        }

        this.infobjectExtractor.fileSemaphore.release();
    }
    
    // root function

    public void extract(){
        
        // get semaphore
        try{
            this.infobjectExtractor.thread_semaphores.get(id).acquire();
        }catch(InterruptedException e){
        }

        // figure out, if it is an infobject page 
        for(String item : this.infobjectExtractor.infobjects){                  
            if(this.doc.location().contains(item)){
                if(!this.infobjectExtractor.found_urls.contains(doc.location())){
                    // main process of the class
                    this.parse();
                    this.save_results();     
                    break;  
                }
            }
        }

        //release semaphore
        this.infobjectExtractor.thread_semaphores.get(id).release();

    }

    // thread function
    @Override
    public void run(){
        
        this.extract();
        
    }
    
}
