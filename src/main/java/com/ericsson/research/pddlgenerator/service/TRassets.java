package com.ericsson.research.pddlgenerator.service;

import org.springframework.web.servlet.view.velocity.VelocityView;

import java.util.Vector;

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
        else {  // Liberal assumption: Function type
            isfunction = true;
            ID = predicateType;
            istype = false;
            functionParameterNames = new Vector<String>();
            functionParameterTypes = new Vector<String>();
        }
    }

    public TRassets(String IDarg){
        ID = IDarg;
        istype = false;
        isfunction = false;
    }

    private boolean isfunction;
    private boolean istype;
    private String type;
    private String ID;
    private String name;
    private String value;
    public Vector<String> functionParameterNames;
    public Vector<String> functionParameterTypes;

    public boolean isFunction(){ return isfunction; }

    public Vector<String> getFunctionParameterNames(){ return functionParameterNames; }

    public Vector<String> getFunctionParameterTypes(){ return functionParameterTypes; }

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

