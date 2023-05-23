package com.geocontentanalyser.urlscraper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.regex.Matcher;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;;

public class InfobjectExtractor{
    Document doc;
    //infobject files' names are unique
    String time;
    //keeps track of number of found infobjects
    Integer infobject_counter = 0;
    //name of current landkreis
    String current_landkreis;

    //all possible kinds of infobjects
    String[] infobjects = {"museum", "kirche", "schule_", "schule.", "amt_", "amt.", "gymnasium.", "gimnasium_", "dezernat", "wesen", "bau.", "bau_", "zentrum", "einheit", "rat", "tag", "halle"};

    //keeping track of what's been found, not to allow dublicates
    ArrayList<String> found_addresses= new ArrayList<String>();
    ArrayList<String> found_emails = new ArrayList<String>();
    ArrayList<String> found_telephones = new ArrayList<String>();

    //parameters of an infobject                
    String title, url, address, email, telephone, open_times;
    //biggest length from all of the parameters (for closing bars in infobject.txt; for more info check out draw_right_bar() funciton)
    Integer biggest_length;

    //list of found containers, not to search for them multiple times
    Elements containers, text_tags;

    InfobjectExtractor(){
        this.time = Instant.now().toString().replace(":", "-").replace(".", "-").substring(0, 16);
    }        

    private void configure(){
        this.title =      "|Title       |  -----> ";                
        this.url =        "|URL         | " +  this.doc.location();
        this.address =    "|Address     | ";
        this.email =      "|Email       | ";
        this.telephone =  "|Telephone   | ";
        this.open_times = "|Open times  | ";

        this.biggest_length = this.url.length();

        this.containers = this.doc.select("span,td,div");
        this.text_tags = this.doc.select("p,span,text");
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

    private String findAddress(Document doc){
        String addressContainer = "", index = "", street = "";

        Pattern pattern = Pattern.compile("[0-9]{5} [A-Z][a-z]*");
        Matcher matcher = pattern.matcher(this.doc.text());
        
        ArrayList<String> indexes = new ArrayList<String>();
        ArrayList<String> streets = new ArrayList<String>();

        //check the presence of <address> tag
        Elements elements = this.doc.select("address");
        if(!elements.isEmpty()){
            pattern = Pattern.compile("[A-z|-]{5,}\s[1-9]+\s+[0-9]{5}(\s|)[A-Z][a-z]*");
            matcher = pattern.matcher(elements.first().text());
            if (matcher.find()){
                //check, if this address was encountered prior
                if(!this.found_addresses.contains(matcher.group(0))){
                    return matcher.group(0);
                }else{
                    streets.add(matcher.group(0));
                }                
            }
        }

        //if not <address>, search elsewhere
        elements = this.containers;
        for(Element element : elements){
            while(matcher.find()){
                indexes.add(matcher.group());
            }
        
            //trying to get the whole address from parrent element
            if(!indexes.isEmpty()){
                //trying to find email in the block of address
                this.email += findEmail(addressContainer);
                    
                //trying to find phone number in the block of address           
                this.telephone +=findTelephone(addressContainer);

                //searching for the direct reference
                addressContainer = element.parent().text();
                pattern = Pattern.compile("(((A|a)nschrift)|(Addresse))(.|\n)+");
                matcher = pattern.matcher(addressContainer);
                if(matcher.find()){
                    street = matcher.group(0);                    
                }else{
                    //if unsuccessful, search for pattern of street-naming
                    pattern = Pattern.compile("((\\S+((S|s)tr|weg|platz))(.|) [1-9][0-9]*)");
                    matcher = pattern.matcher(addressContainer);
                    if(matcher.find()){
                        street = matcher.group()  + " " + index;
                    }
                }

                //check, if the found by the pattern address is unique
                if(found_addresses.contains(street)){
                    streets.add(street);
                    street = "";
                }

                //in case, unique address was found
                if(street != ""){
                    //remove newline characters and redundant whitespaces
                    street = elements.first().text().replace("\n| {2,}", ", ");
                    //remove unnecessary information
                    street = street.replace("Anschrift|Adresse|Address", "");
                    return street;  
                //in case, all found streets are not unique   
                }else{
                    street = streets.get(0) + " (not unique)";
                    return street;
                }
                                              
            }else{
                return "not found";
            }           
        }
        return "not found";
    }

    private String findEmail(String text){
        //find a pattern with @ or [at] in the middle and dot at the end
        Pattern pattern = Pattern.compile("[\n\r\s]\\S+\s{0,1}(@|\\[at\\])\s{0,1}\\S+\\.\\S{1,6}");
        Matcher matcher = pattern.matcher(text);
        if(matcher.find()){
            if(!this.found_emails.contains(matcher.group(0))){
                this.found_emails.add(matcher.group(0));
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
            //searching through all containers on page
            Pattern pattern = Pattern.compile("(Ö|ö)ffnungszeiten");
            Matcher matcher = pattern.matcher(element.text());
            if(matcher.find()){
                //if found, see if the whole open time is in container
                pattern = Pattern.compile("Öffnungszeiten(\s|\n)*((Mo|Di|Mi|Do|Fr|Sa|So).{10,45}Uhr(\n|\s))+", Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(element.text());
                if(matcher.find()){
                    return matcher.group(0).replace("(Ö|ö)ffnungszeiten(\s|)", "");
                }else{
                    //if not in the container itself, search through parent element
                    matcher = pattern.matcher(element.parent().text());
                    if(matcher.find()){
                        return matcher.group(0).replace("(Ö|ö)ffnungszeiten(\s|)", "");
                    }else{
                        matcher = pattern.matcher(element.parent().parent().text());
                        if(matcher.find()){
                            return matcher.group(0).replace("(Ö|ö)ffnungszeiten(\s|)", "");
                        }
                        /*
                        else{
                            //if no success, just drop everything in the parent element
                            return element.parent().text().replace("(Ö|ö)ffnungszeiten( |)", "");

                            //if no success, just drop everything in the element
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
            //trying to find telephone by label

            telephone_refined = matcher.group(0).replaceAll("[A-z]|\\.| {2,}|:", "");
            if(!this.found_telephones.contains(telephone_refined)){
                this.found_telephones.add(telephone_refined);
                return telephone_refined;
            }else{
                return "not found";
            }            
        }else{
            //trying to find unlabeled telephone 
            
            pattern = Pattern.compile("[0-9][0-9\\-\\/\s]{5,15}[0-9]");
            matcher = pattern.matcher(text);
            if(matcher.find()){
                if(!this.found_telephones.contains(matcher.group(0))){
                    this.found_telephones.add(matcher.group(0));
                    return matcher.group(0);
                }else{
                    return "not found";
                } 
            }else{
                return "not found.";
            }            
        }
    }

    //functions for formalisation of output into infobject.txt

    //draw a bar, that has a length, no less than of the longest textual representation of an infobjects field (most likely, URL)
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

    //change a string, so that vertical bars of each "capsule" in infobject.txt files is properly closed at the right side
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


    //root function of the class
    public void extract(Document doc){
        this.doc = doc;        
        for(String item : this.infobjects){
            if(this.doc.location().contains(item)){
                //return parameters to default
                this.configure();                

                //regex search for address
                this.address += this.findAddress(doc);
                
                //it is an infobject, only if it has physical address, otherwise, it's a missmatch
                if(address != "|Address     |"){

                    //searching tile
                    this.title += this.findTitle(doc);

                    //find open times, if they haven't yet been found
                    this.open_times += this.findOpenTimes();
                    
                    //searching for email, if it hadn't been yet found
                    if(this.email == "|Email       | "){
                        this.email += this.findEmail(doc.text());
                    } 

                    //searching for phone number, if it hadn't been yet found
                    if(telephone == "|Telephone   | "){
                        this.telephone += this.findTelephone(doc.text());
                    } 
                    
                    //incrementing the infobject counter
                    infobject_counter++;
                    try{
                        RandomAccessFile file = new RandomAccessFile("output/" + this.time + "-infobject.txt", "rw");
                        byte[] bytes = ("\nInfobjects Found: " + infobject_counter + "\n\n\n").getBytes();
                        file.write(bytes, 0, bytes.length);
                        file.close();
                    }
                    catch(IOException e){
                    }

                    //writing the found in the file
                    try {
                        FileWriter writer = new FileWriter("output/" + this.time + "-infobject.txt", true);
                        //separator
                        writer.write("/");
                        draw_vertical_bar(writer);
                        writer.write("\\ \n");
                        //header
                        writer.write(draw_right_bar(title));
                        //url
                        writer.write(draw_right_bar(url));
                        //physical address
                        writer.write(draw_right_bar(address));
                        //open times
                        writer.write(draw_right_bar(open_times));
                        //email
                        writer.write(draw_right_bar(email));
                        //phone number
                        writer.write(draw_right_bar(telephone));
                        //bottom separator
                        writer.write("\\");
                        draw_vertical_bar(writer);
                        writer.write("/ \n\n\n");
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }  
    }
   
}
