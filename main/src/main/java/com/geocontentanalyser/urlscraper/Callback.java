package com.geocontentanalyser.urlscraper;

public interface Callback {
    void onDataExtracted(Data data);
    void onRequestDone();
}
