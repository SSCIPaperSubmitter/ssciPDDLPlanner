package com.ericsson.research.pddlgenerator.service;

/**
 * Created by eathkar on 2016-10-14.
 */
public class TRassets {

    public TRassets(boolean isPredicateType, String predicateType){
        if(isPredicateType){
            type = predicateType;
            istype = isPredicateType;
            ID = predicateType;
        }
    }

    public TRassets(String IDarg){
        ID = IDarg;
    }

    private boolean istype;
    private String type;
    private String ID;
    private String name;
    private String value;




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

    public String getType(){ return type; }

    public boolean isType(){ return istype; }
}
