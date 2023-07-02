package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class InfobjectExtractrorThread extends Thread {
    // the html document being parsed
    private Document doc;
    // main class is passed to an instance of this thread class in order to change the former's attributes, that are global for all infobjects
    private InfobjectExtractor infobjectExtractor;
    // where is being written (full path from the project's root)
    private String filename;
    //
    private String current_landkreis;
    // determines, if infobjects should be dumped into "capsules" in infobject.txt files (to be implemented)
    private Boolean simplify;


    // keeps track of number of found infobjects on a given site
    private Integer infobject_counter = 0;
    // parameters of an infobject                
    private String title, url, address, email, telephone, open_times, map;
    // biggest length from all of the parameters (for closing bars in infobject.txt; for more info check out draw_right_bar() funciton)
    private Integer biggest_length;

    // list of found containers, not to search for them multiple times
    private Elements containers, text_tags;

    InfobjectExtractrorThread(Document doc, InfobjectExtractor infobjectExtractor, String filename, String current_landkreis, Boolean simplify){
        this.doc = doc;
        this.infobjectExtractor = infobjectExtractor;
        this.filename = filename;
        this.current_landkreis = current_landkreis;
        this.simplify = simplify;
    }

    // used from start() to set up the baseline parameter values for an infobject
    private void configure(){
        this.title =      "| Title       |  -----> ";                
        this.url =        "| URL         | " +  this.doc.location();
        this.address =    "| Address     | ";
        this.email =      "| Email       | ";
        this.telephone =  "| Telephone   | ";
        this.open_times = "| Open times  | ";
        this.map =        "| Map         | ";

        this.biggest_length = this.url.length();

        this.containers = this.doc.select("span,td,div");
        this.text_tags = this.doc.select("p,span,text");

        // add url to the list to avoid dublicates, that can occur if the recursive depth is set as high
        this.infobjectExtractor.found_urls.add(this.doc.location());
    }    

    private String findMap(Document doc){
        Elements iframes = doc.select("iframe");
        for(Element iframe : iframes){
            for(Attribute attr : iframe.attributes()){
                if (attr.getValue().contains("google.com/maps")){ //  maybe, just replace with "map" of "maps"
                    this.infobjectExtractor.map_counter += 1;
                    return attr.getValue();
                }
            }
        }
        return "not found";
    }

    private String findAddress(Document doc){
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
                this.email += findEmail(addressContainer);
                    
                // trying to find phone number in the block of address           
                this.telephone +=findTelephone(addressContainer);

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
                                street = street.replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(?-i)", "");
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

                    street = street.replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(?-i)", "");
                    return street;  
                // in case, all found streets are not unique   
                }else{
                    street = streets.get(0).replace("(?i)(Anschrift|Adr(esse)?|Addr(ess)?)(?-i)", "") + " (not unique)";
                    return street;
                }
                                              
            }else{
                return "not found";
            }           
        }
        return "not found";
    }

    private String findTitle(Document doc){
        String[] header_tags = {"h1","h2","h3"};
        for(String tag : header_tags){
            if(!doc.select(tag).isEmpty()){
            return doc.select(tag).first().text() + "  <-----";
            }
        }
        return doc.title() + "  <-----";
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
    private void draw_vertical_bar(FileWriter writer){
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
    
    @Override
    public void run(){
         
        for(String item : this.infobjectExtractor.infobjects){                  
            if(this.doc.location().contains(item) && !this.infobjectExtractor.found_urls.contains(doc.location())){
                // return parameters to default
                this.configure();
                
                // search for a map (even if it's not an infobject page)
                this.map += this.findMap(doc);

                // regex search for address
                this.address += this.findAddress(doc);
                
                // it is an infobject, only if it has physical address, otherwise, it's a missmatch
                if(address != "| Address     | "){

                    // searching tile
                    this.title += this.findTitle(doc);

                    // find open times, if they haven't yet been found
                    this.open_times += this.findOpenTimes();
                    
                    // searching for email, if it hadn't been yet found
                    if(this.email == "| Email       | "){
                        this.email += this.findEmail(doc.text());
                    } 

                    // searching for phone number, if it hadn't been yet found
                    if(telephone == "| Telephone   | "){
                        this.telephone += this.findTelephone(doc.text());
                    } 
                    
                    // wait for semaphore's availability
                    try{
                        this.infobjectExtractor.fileSemaphore.acquire();
                    }
                    catch(InterruptedException e){
                    }

                    // incrementing the infobject counter
                    this.infobjectExtractor.infobject_counter_global++;
                    try{
                        RandomAccessFile file = new RandomAccessFile(this.filename, "rw");

                        // found infobjects and maps
                        byte[] bytes = 
                        (this.current_landkreis + 
                        "\nInfobjects Found: " + this.infobjectExtractor.infobject_counter_global + 
                        "\nMaps found: " + this.infobjectExtractor.map_counter + "\n\n\n").getBytes();

                        file.write(bytes, 0, bytes.length);

                        file.close();
                    }
                    catch(IOException e){
                    }

                    // writing the found in the file
                    try {
                        FileWriter writer = new FileWriter(this.filename, true);
                        // separator
                        writer.write("/");
                        this.draw_vertical_bar(writer);
                        writer.write("\\ \n");
                        // header
                        writer.write(this.draw_right_bar(this.title));
                        // url
                        writer.write(this.draw_right_bar(this.url));
                        // physical address
                        writer.write(this.draw_right_bar(this.address));
                        // open times
                        writer.write(this.draw_right_bar(this.open_times));
                        // email
                        writer.write(this.draw_right_bar(this.email));
                        // phone number
                        writer.write(this.draw_right_bar(this.telephone));
                        // map url
                        writer.write(this.draw_right_bar(this.map));
                        // bottom separator
                        writer.write("\\");
                        this.draw_vertical_bar(writer);
                        writer.write("/ \n\n\n");
                        writer.close();
                    } catch (IOException e) {
                    }

                    this.infobjectExtractor.fileSemaphore.release();
                }
            }
        }  
    }

    // in case, a destructor is need in the future instances of the project
    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        super.finalize();
    }
    
}
