package com.geocontentanalyser.urlscraper;

import java.util.LinkedHashSet;

public class Data {
    // This class stores the counts of the different types of data
    String baseURL;
    int count_InfoObjects;
    int count_EServices;
    int count_Address;
    int count_Coordinates;
    int count_EmbeddedMaps;
    int count_ExternalMaps;
    LinkedHashSet<String> set;
    
    public Data(String baseURL) {
        this.baseURL = baseURL;
        this.count_InfoObjects = 0;
        this.count_EServices = 0;
        this.count_Address = 0;
        this.count_Coordinates = 0;
        this.count_EmbeddedMaps = 0;
        this.count_ExternalMaps = 0;
        this.set = new LinkedHashSet<String>();
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public int getCount_InfoObjects() {
        return count_InfoObjects;
    }

    public void setCount_InfoObjects(int count_InfoObjects) {
        this.count_InfoObjects = count_InfoObjects;
    }

    public int getCount_EServices() {
        return count_EServices;
    }

    public void setCount_EServices(int count_EServices){
        this.count_EServices = count_EServices;
    }

    public int getCount_Address() {
        return count_Address;
    }

    public void setCount_Address(int count_Address) {
        this.count_Address = count_Address;
    }

    public int getCount_Coordinates() {
        return count_Coordinates;
    }

    public void setCount_Coordinates(int count_Coordinates) {
        this.count_Coordinates = count_Coordinates;
    }

    public int getCount_EmbeddedMaps() {
        return count_EmbeddedMaps;
    }

    public void setCount_EmbeddedMaps(int count_EmbeddedMaps) {
        this.count_EmbeddedMaps = count_EmbeddedMaps;
    }

    public int getCount_ExternalMaps() {
        return count_ExternalMaps;
    }

    public void setCount_ExternalMaps(int count_ExternalMaps) {
        this.count_ExternalMaps = count_ExternalMaps;
    }

    public LinkedHashSet<String> getSet() {
        return set;
    }

    public void setSet(LinkedHashSet<String> set) {
        this.set = set;
    }

    public LinkedHashSet<String> mergeData(Data newdata)
    {
        this.count_InfoObjects += newdata.getCount_InfoObjects();
        this.count_Address += newdata.getCount_Address();
        this.count_Coordinates += newdata.getCount_Coordinates();
        this.count_EmbeddedMaps += newdata.getCount_EmbeddedMaps();
        this.count_ExternalMaps += newdata.getCount_ExternalMaps();
        //make a copy of the new set and remove all elements of existing set from the copy
        LinkedHashSet<String> difference = new LinkedHashSet<String>(newdata.getSet());
        difference.removeAll(this.set);
        this.set.addAll(newdata.set);
        return difference;
    }
}
