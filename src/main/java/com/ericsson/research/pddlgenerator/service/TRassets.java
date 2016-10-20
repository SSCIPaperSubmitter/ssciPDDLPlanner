package com.ericsson.research.pddlgenerator.service;

/**
 * Created by eathkar on 2016-10-14.
 */
public class TRassets {

    private String ID;
    private String name;
    private String value;


    public TRassets(String IDarg){
        ID = IDarg;
    }

    public void setName(String argN){
        name = argN;
    }

    public void setValue(String valN){
        value = valN;
    }

    public String getID(){
        return ID;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
