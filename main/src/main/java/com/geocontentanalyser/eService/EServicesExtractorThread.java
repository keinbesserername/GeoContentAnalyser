package com.geocontentanalyser.eService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.geocontentanalyser.urlscraper.Data;

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
    private Boolean found = false;
    public Integer id;
    public Data data;
    // parameters of an eservice

    private String title = this.default_title;
    private String url = this.default_url;
    private String default_title, default_url;
    private String current_landkreis;

    public EServicesExtractorThread(EServicesExtractor eServicesExtractor, Integer id, String directory,
            Boolean simplify, List<String> eServices, Data data) {
        this.id = id;
        this.eServicesExtractor = eServicesExtractor;
        this.simplify = simplify;
        this.eServices = eServices;
        this.data = data;

        if (this.simplify) {
            this.default_title = "Title       :";
            this.default_url = "URL         :";
        } else {
            this.default_title = "| Title       |  -----> ";
            this.default_url = "| URL         | ";
        }
    }

    //
    // secondary functions, that are called from the scope of the main fuction
    // start()
    //

    // used from start() to set up the baseline parameter values for an infobject

    public void configure(Document doc, String filename, String current_landkreis) {
        this.doc = doc;
        this.filename = filename;
        this.current_landkreis = current_landkreis;

        this.title = this.default_title;
        this.url = this.default_url + this.doc.location();

        // add url to the list to avoid dublicates, that can occur if the recursive
        // depth is set as high
        this.eServicesExtractor.found_urls.add(this.doc.location());
    }

    // determines, if this page's title matches with an entry of this.eServices

    private String findTitle() {

        String[] header_tags = { "h1", "h2", "h3" };
        for (String tag : header_tags) {
            Elements tags = this.doc.select(tag);
            if (!tags.isEmpty()) {

                // search through header tags

                for (String item : this.eServices) {
                    if (tags.get(0).text().contains(item)) {
                        if (tags.get(0).text() != "") {
                            this.found = true;
                            return tags.get(0).text() + "  <-----";
                        }
                    }
                    if (this.doc.title().contains(item)) {
                        this.found = true;
                        return doc.title() + "  <-----";
                    }
                }
            }
        }
        return "";
    }

    // functions for formalisation of output into infobject.txt

    // draw a bar, that has a length, no less than of the longest textual
    // representation of an infobjects field (most likely, URL)
    private void draw_horizontal_bar(FileWriter writer) {
        Integer i = 0;
        try {
            while (i < 120) {
                writer.write("-");
                i++;
            }
        } catch (IOException e) {
        }
    }

    // change a string, so that vertical bars of each "capsule" in infobject.txt
    // files is properly closed at the right side
    private String draw_right_bar(String string) {
        Integer difference = 120 - string.length();
        if (difference > 0) {
            for (Integer i = 0; i <= difference; i++) {
                string = string + " ";
            }
        }
        string = string + "|\n";
        return string;
    }

    //
    // the main function
    //

    public void extract() {

        this.title += findTitle();

        // further, only if title matches with an eservice

        if (this.found) {
            this.eServicesExtractor.eservice_counter_global++;

            // save all findings

            try {
                this.eServicesExtractor.fileSemaphore.acquire();
            } catch (InterruptedException e) {
            }

            // output global metrics
            try {
                RandomAccessFile file = new RandomAccessFile(this.filename, "rw");

                // found infobjects and maps
                byte[] bytes = (this.current_landkreis +
                        "\nE-Services Found: " + this.eServicesExtractor.eservice_counter_global + "\n\n\n").getBytes();
                data.setCount_EServices(this.eServicesExtractor.eservice_counter_global);
                file.write(bytes, 0, bytes.length);

                file.close();
            } catch (IOException e) {
            }

            try (FileWriter writer = new FileWriter(filename, true)) {

                // separator
                writer.write("/");
                this.draw_horizontal_bar(writer);
                writer.write("\\\n");

                writer.write(this.draw_right_bar(this.title));
                writer.write(this.draw_right_bar(this.url));

                // bottom separator
                writer.write("\\");
                this.draw_horizontal_bar(writer);
                writer.write("/ \n\n\n");
                writer.close();
            } catch (IOException e) {
            }

            this.found = false;
            this.eServicesExtractor.fileSemaphore.release();

        }
    }

    // thread function

    @Override
    public void start() {
        this.extract();
    }

}
