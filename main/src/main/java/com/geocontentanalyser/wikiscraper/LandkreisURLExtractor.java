package com.geocontentanalyser.wikiscraper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LandkreisURLExtractor {
    String url;

    public String URLextractor(String content) {
        // search for the first occurence of the string, then skip the web address
        // max-width: 10em; overflow: hidden; word-wrap: break-word;\"><a
        // rel=\"nofollow\" class=\"external text\" href=\"http://www.kreis-borken.de"
        int endIndex = content.indexOf("max-width: 10em; overflow: hidden; word-wrap: break-word") + 109;

        // regex to extract the web address between the quotes
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(content.substring(endIndex - 1));
        matcher.find();
        String temp = matcher.group(1);
        url = temp.substring(0, temp.length() - 1);
        return url;
    }
}