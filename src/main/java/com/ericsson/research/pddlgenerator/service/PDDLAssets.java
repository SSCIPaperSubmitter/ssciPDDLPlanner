package com.ericsson.research.pddlgenerator.service;

import java.util.Vector;

/**
 * Created by eathkar on 03/05/16.
 */

/*
 * Raw Assets as for First-Time Parsing
 */
public class PDDLAssets {

    public Vector<PDDLAction> actions;
    public Vector<PDDLPrecondition> preconditions;
    public Vector<PDDLTransition> transitions;
    public Vector<PDDLPredicateSet> predicateSets;
    public Vector<PDDLPredicate> predicates;

    public PDDLAssets(){
        actions = new Vector<PDDLAction>();
        preconditions = new Vector<PDDLPrecondition>();
        transitions = new Vector<PDDLTransition>();
        predicateSets = new Vector<PDDLPredicateSet>();
        predicates = new Vector<PDDLPredicate>();
    }
}

class PDDLConstants {

    // These paramaters could be configured
    public static String prefix = "";
    public static String modelName = "";
    public static String domainFileName = "domain-file";
    public static String problemFileName = "problem-file";
    public static String extensionName = ".pddl";

    // These should not be changed unless ontologies are changed !!
    private String predicate_has_parameter = "hasParameter";
    private String predicate_has_timing = "hasTiming";
    private String predicate_has_value = "hasValue";
    private String predicate_has_operator = "hasOperator";
    private String predicate_has_name = "hasName";
    private String predicate_is_durative_action = "isDurativeAction";
    private String predicate_hasConditionExpressionPart = "hasConditionExpressionPart";
    private String predicate_hasConditionOperator = "hasConditionOperator";
    private String predicate_hasNegation = "hasNegation";
    private String predicate_subclassOf = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    private String predicate_hasCondition = "hasCondition";
    private String predicate_hasDuration = "hasDuration";
    private String predicate_hasEffect = "hasEffect";
    private String predicate_hasEffectOperator = "hasEffectOperator";
    private String predicate_type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private String predicate_hasPredicateList = "hasPredicateList";
    private String predicate_hasPredicate = "hasPredicate";
    private String predicate_hasActionOperator = "hasActionOperator";
    private String predicate_hasPreconditionExpressionPart = "hasPreconditionExpressionPart";
    private String predicate_hasPrecondition = "hasPrecondition";
    private String predicate_hasAction = "hasAction";
    private String predicate_hasComputation = "hasComputation";
    private String predicate_hasPreconditionOperator = "hasPreconditionOperator";
    private String predicate_hasPredicateSet = "hasPredicateSet";
    private String predicate_hasPredicateSetOperator = "hasPredicateSetOperator";
    private String predicate_hasPredicateRealName = "hasPredicateRealName";
    private String predicate_hasPredicateOperatorAmount = "hasPredicateOperatorAmount";
    private String predicate_hasPredicateOperator = "hasPredicateOperator";
    private String predicate_hasPredicateTiming = "hasPredicateTiming";

    private String object_action = "Middle#Action";
    private String object_conditionalExpression = "Middle#ConditionalExpression";
    private String object_Duration = "Middle#Duration";
    private String object_Condition = "Middle#Condition";
    private String object_Effect = "Middle#Effect";
    private String object_Predicate = "Middle#Predicate";
    private String object_Function = "Middle#Function";
    private String object_Parameters = "Middle#Parameters";
    private String object_PredicateList = "Middle#PredicateList";
    private String object_Transition = "Middle#Transition";
    private String object_Precondition = "Middle#Precondition";
    private String object_PredicateSet = "Middle#PredicateSet";
    private String object_Remove = "Middle#Remove";
    private String object_Add = "Middle#Add";
    //private String object_PredicateSetOperator = "Middle#Predicat"

    // NON-Modeled - "hardcoded" features
    private String domain = "transportPlanner";
    private String []requirements = {"STRIPS", "TYPING", "FLUENTS", "DURATIVE-ACTIONS"};

    public enum predicateType{
        HAS_PARAMETER, HAS_TIMING, HAS_VALUE, HAS_OPERATOR, HAS_NAME, IS_DURATIVE_ACTION, HAS_CONDITION_EXPRESSION_PART,
        HAS_CONDITION_OPERATOR, SUBCLASS_OF, HAS_NEGATION, HAS_CONDITION, HAS_DURATION, HAS_EFFECT, HAS_EFFECT_OPERATOR,
        TYPE, HAS_PREDICATE_LIST, HAS_PREDICATE, HAS_ACTION_OPERATOR, HAS_PRECONDITION_EXPRESSION_PART, HAS_PRECONDITION,
        HAS_COMPUTATION, HAS_ACTION, HAS_PRECONDITION_OPERATOR, HAS_PREDICATE_SET, HAS_PREDICATE_SET_OPERATOR,
        HAS_PREDICATE_REAL_NAME, HAS_PREDICATE_OPERATOR, HAS_PREDICATE_OPERATOR_AMOUNT, HAS_PREDICATE_TIMING
    }

    public enum objectType{
        ACTION, CONDITIONAL_EXPRESSION, DURATION, CONDITION, EFFECT, PREDICATE, FUNCTION, PARAMETER, PREDICATE_LIST,
        TRANSITION, PRECONDITION, PREDICATE_SET, PREDICATE_SET_OPERATOR, REMOVE, ADD
    }

    public PDDLConstants(String arg_prefix, String arg_modelName){ // dont necessarily have to input these
        prefix = arg_prefix;
        modelName = arg_modelName;
    }

    public static String removePrefixes(String input){
        if (prefix.isEmpty() && modelName.isEmpty()){
            int hashIndex = input.indexOf("#");
            return input.substring(hashIndex + 1, input.length());
        }
        else
            return input.substring(prefix.length() + modelName.length() + 1, input.length());
    }

    public String[] getRequirements() {return requirements;}

    public String getDomain(){
        return domain;
    }

    public String getPredicateDefinition(predicateType predicate){
        switch(predicate){
            case HAS_PARAMETER:
                return prefix+modelName+"#"+predicate_has_parameter;
            case HAS_TIMING:
                return prefix+modelName+"#"+predicate_has_timing;
            case HAS_OPERATOR:
                return prefix+modelName+"#"+predicate_has_operator;
            case HAS_NAME:
                return prefix+modelName+"#"+predicate_has_name;
            case IS_DURATIVE_ACTION:
                return prefix+modelName+"#"+predicate_is_durative_action;
            case HAS_VALUE:
                return prefix+modelName+"#"+predicate_has_value;
            case HAS_CONDITION_EXPRESSION_PART:
                return prefix+modelName+"#"+predicate_hasConditionExpressionPart;
            case HAS_CONDITION_OPERATOR:
                return prefix+modelName+"#"+predicate_hasConditionOperator;
            case SUBCLASS_OF:
                return predicate_subclassOf;
            case HAS_NEGATION:
                return prefix+modelName+"#"+predicate_hasNegation;
            case HAS_CONDITION:
                return prefix+modelName+"#"+predicate_hasCondition;
            case HAS_EFFECT:
                return prefix+modelName+"#"+predicate_hasEffect;
            case HAS_DURATION:
                return prefix+modelName+"#"+predicate_hasDuration;
            case HAS_EFFECT_OPERATOR:
                return prefix+modelName+"#"+predicate_hasEffectOperator;
            case TYPE:
                return predicate_type;
            case HAS_PREDICATE_LIST:
                return predicate_hasPredicateList;
            case HAS_PREDICATE:
                return predicate_hasPredicate;
            case HAS_ACTION_OPERATOR:
                return predicate_hasActionOperator;
            case HAS_PRECONDITION_EXPRESSION_PART:
                return predicate_hasPreconditionExpressionPart;
            case HAS_ACTION:
                return predicate_hasAction;
            case HAS_COMPUTATION:
                return predicate_hasComputation;
            case HAS_PRECONDITION:
                return predicate_hasPrecondition;
            case HAS_PRECONDITION_OPERATOR:
                return predicate_hasPreconditionOperator;
            case HAS_PREDICATE_SET:
                return predicate_hasPredicateSet;
            case HAS_PREDICATE_SET_OPERATOR:
                return predicate_hasPredicateSetOperator;
            case HAS_PREDICATE_REAL_NAME:
                return predicate_hasPredicateRealName;
            case HAS_PREDICATE_OPERATOR:
                return predicate_hasPredicateOperator;
            case HAS_PREDICATE_OPERATOR_AMOUNT:
                return predicate_hasPredicateOperatorAmount;
            case HAS_PREDICATE_TIMING:
                return predicate_hasPredicateTiming;
        }
        return null;
    }

    public String getObjectDefinition(objectType object){
        switch (object){
            case ACTION:
                return prefix+object_action;
            case CONDITION:
                return prefix+object_Condition;
            case EFFECT:
                return prefix+object_Effect;
            case PREDICATE:
                return prefix+object_Predicate;
            case DURATION:
                return prefix+object_Duration;
            case CONDITIONAL_EXPRESSION:
                return prefix+object_conditionalExpression;
            case FUNCTION:
                return prefix+object_Function;
            case PARAMETER:
                return prefix+object_Parameters;
            case PREDICATE_LIST:
                return prefix+object_PredicateList;
            case TRANSITION:
                return prefix+object_Transition;
            case PRECONDITION:
                return prefix+object_Precondition;
            case PREDICATE_SET:
                return prefix+object_PredicateSet;
            case REMOVE:
                return prefix+object_Remove;
            case ADD:
                return prefix+object_Add;
        }
        return null;
    }
}

class PDDLRaw {
    public String subject;
    public String predicate;
    public String object;

    public PDDLRaw(String subj, String pred, String obj){
        subject = subj;
        predicate = pred;
        object = obj;
    }
}

/*
class PDDLFunction {
    public String name;
    public Vector<String> parameters;
    public String operator;
    public String value;
    public String realname; //where applicable

    public PDDLFunction(String functionName){
        name = functionName;
        parameters = new Vector<String>();
        operator = "";
        value = "";
        realname = "";
    }
}
*/
class PDDLPredicate {

    public String name;
    public Vector<String> parameters;
    public String encapsulatingMethod;
    public String realname;
    public String operatorAmount;
    public String operator;
    public String hasTiming;

    public PDDLPredicate(String predicateName){
        name = predicateName;
        parameters = new Vector<String>();
        encapsulatingMethod = "";
        realname = "";
        operatorAmount = "";
        operator = "";
        hasTiming = "";
    }

    public PDDLPredicate(String predicateName, String method){
        name = predicateName;
        parameters = new Vector<String>();
        encapsulatingMethod = method;
        realname = "";
        operatorAmount = "";
        operator = "";
        hasTiming = "";
    }

    public void addParameter(String parameter){
        parameters.add(parameter);
    }
}

class PDDLPredicateSet{
    public String name;

    public Vector<PDDLPredicate> predicates;

    public PDDLPredicateSet(String setName){
        name = setName;
        predicates = new Vector<PDDLPredicate>();
    }
}

/*
// This class is exclusively used by the problem file
class PDDLPredicateList{
    String name;
    Vector<PDDLPredicate> list;

    public PDDLPredicateList(String listName){
        name = listName;
        list = new Vector<PDDLPredicate>();
    }
}
*/
class PDDLModel {
    public String[] types;
    public String[] predicateTypes;
    public String[] functionTypes;
    public String dataToReturn;
}
/*
class PDDLObjectCollection {
    public Vector<PDDLObject> data;
    public String datatype;
    public PDDLObjectCollection(String dtype){
        data = new Vector<PDDLObject>();
        datatype = dtype;
    }
    public void insertIntoCollection(PDDLObject obj){
        data.add(obj);
    }
    public Vector<PDDLObject> getObjects(){
        return data;
    }
}

class PDDLObject {
    public String name;
    public String type;

    public PDDLObject (String arg_name, String arg_type){
        name = arg_name;
        type = arg_type;
    }
}*/

class PDDLTransition {
    private String operator = ""; // Used in problem file

    public PDDLTransition(String primitiveName){
        name = primitiveName;
        isDurativeAction = false; // assumed to be false a priori

        predicateListName = "";
    }

    // --- used in problem file
    public void assignOperator(String op){
        operator = op;
    }

    public boolean hasOperator(){
        if (!operator.isEmpty()){
            return true;
        }
        return false;
    }

    public String getOperator(){
        return operator;
    }
    // --- end pf use

    public boolean sanityCheck(){
        if (computation == null || action == null || precondition == null){
            return false;
        }
        else return true;
    }

    public String name;

    // These are used by the model file
    public boolean isDurativeAction;

    public PDDLComputation computation;
    public PDDLAction action;
    public PDDLPrecondition precondition;
    public String predicateListName;
}

class PDDLComputation{
    public String name;

    public PDDLComputation(String computationName){
        name = computationName;
    }
}

class PDDLPrecondition{
    public String name;
    public String predicateSetOperator;
    public PDDLPredicateSet predicateSet;
    public PDDLPrecondition(String preconditionName){
        name = preconditionName;
    }
}

class PDDLAction {
    public String name;
    public PDDLPredicateSet predicateSet;
    public String predicateSetOperator;
    public PDDLAction(String primitiveName){
        name = primitiveName;
    }
}

class PDDLCondition {
    public String name;
   // public Vector<PDDLConditionalExpression> conditional_expressions;
    public String conditionalOperator;

    public PDDLCondition(String predicateName){
        name = predicateName;
       // conditional_expressions = new Vector<PDDLConditionalExpression>();
        conditionalOperator = "";
    }
}
/*
class PDDLConditionalExpression {
    public String name;
    public String timing;
    public Vector<String> parameters;
    public boolean hasNegation;
    public String operator;

    public PDDLConditionalExpression(String predicateName){
        parameters = new Vector<String>();
        name = predicateName;
        hasNegation = false;
        timing = "";
        operator = "";
    }
}

class PDDLEffect {
    public String name;
    public Vector<PDDLConditionalExpression> conditional_expressions;
    public String conditionalOperator;

    public PDDLEffect(String predicateName){
        name = predicateName;
        conditional_expressions = new Vector<PDDLConditionalExpression>();
        conditionalOperator = "";
    }
}
class PDDLParameter {
    public String name;
    public String realName;

    public PDDLParameter(String predicateName){
        realName = "";
        name = predicateName;
    }
}

class PDDLDuration {
    public String name;
    public Vector<String> parameters;

    public PDDLDuration(String predicateName){
        parameters = new Vector<String>();
        name = predicateName;
    }
}

class PDDLInstance{
    public String instanceName;
    public String instanceType;

    public PDDLInstance(String name, String type){
        instanceName = name;
        instanceType = type;
    }
}*/