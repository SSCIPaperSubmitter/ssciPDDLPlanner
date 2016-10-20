package com.ericsson.research.pddlgenerator.representations;

import java.io.File;

/**
 * Created by eathkar on 2016-09-29.
 */
public class FileInput {
    private String transitionsFile;
    private String statesFile;
    private String trasnformationRulesFile;

    public FileInput(){}

    public FileInput(String tf, String sf, String tr){
        transitionsFile = tf;
        statesFile = sf;
        trasnformationRulesFile = tr;
    }

    public String getTransitionsFile(){return transitionsFile;}
    public String getStatesFile(){ return statesFile; }
    public String getTransformationRulesFile() {return trasnformationRulesFile;}
}
