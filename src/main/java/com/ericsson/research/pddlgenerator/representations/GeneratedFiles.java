package com.ericsson.research.pddlgenerator.representations;

/**
 * Created by eathkar on 2016-10-14.
 */
public class GeneratedFiles {

    private String problemFile;
    private String domainFile;


    public GeneratedFiles(String pf, String df){
        problemFile = pf;
        domainFile = df;
    }


    public String getProblemFile(){ return problemFile; }
    public String getDomainFile(){return domainFile;}
}
