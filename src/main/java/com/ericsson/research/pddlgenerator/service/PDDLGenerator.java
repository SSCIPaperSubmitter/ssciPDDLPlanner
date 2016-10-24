package com.ericsson.research.pddlgenerator.service;

import org.apache.jena.rdf.model.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by eathkar on 03/05/16.
 */
public class PDDLGenerator {


    public String[] GeneratePlan(String statesFile, String transitionsFile, String transformationRulesFile, boolean writeToFile){

        // For now we support PDDL file generation, lets start by parsing transformation rules file, continue with
        // transitions file and finally, end with states file

        // TODO: Processing of state file

        //String[] array = new String[2];

        ByteArrayInputStream tfInputStream =
                new ByteArrayInputStream(transformationRulesFile.getBytes(Charset.forName("UTF-8")));

        ByteArrayInputStream transitionsInputStream =
                new ByteArrayInputStream(transitionsFile.getBytes(Charset.forName("UTF-8")));

        ByteArrayInputStream statesInputStream =
                new ByteArrayInputStream(statesFile.getBytes(Charset.forName("UTF-8")));

        Model model_transitionsFile = ModelFactory.createDefaultModel();
        Model model_transformationsRuleFile = ModelFactory.createDefaultModel();
        Model model_statesFile = ModelFactory.createDefaultModel();

        model_transitionsFile.read(transitionsInputStream, null, "TURTLE");
        model_transformationsRuleFile.read(tfInputStream, null, "TURTLE");
        model_statesFile.read(statesInputStream, null, "TURTLE");

        StmtIterator transitions_file_statement_iterator = model_transitionsFile.listStatements();
        StmtIterator transformationsRule_file_statement_iterator = model_transformationsRuleFile.listStatements();
        StmtIterator states_file_statement_iterator = model_statesFile.listStatements();

        // Process Transformation Rules, States file, Transitions file
        Vector<TRassets> trModel = processTransformationRulesFile(transformationsRule_file_statement_iterator);
        PDDLAssets transitionsModel = processTransitionsFile(transitions_file_statement_iterator, trModel);
        PDDLAssets statesModel = processStatesFile(states_file_statement_iterator, trModel);

        /*

        for (int i = 0; i < statesModel.objects.size(); i++){
            System.out.println("Object "+statesModel.objects.elementAt(i).name);
            System.out.println("\tValue:"+statesModel.objects.elementAt(i).value);
            for (int k = 0; k < statesModel.objects.elementAt(i).parameters.size(); k++){
                System.out.println("\tparam["+k+"]:"+statesModel.objects.elementAt(i).parameters.elementAt(k));
            }
        }*/

        // Generate output
        return generateOutput(trModel, transitionsModel, statesModel);
    }

    private String[] generateOutput(Vector<TRassets> transformationRules, PDDLAssets transitionData, PDDLAssets stateData){
        String[] array = new String[2];

        if (!(TR_GetValueForID(transformationRules, TRConstants.hasLanguageRel)).isEmpty()) {
            String language = TR_GetValueForID(transformationRules, TRConstants.hasLanguageRel);
            if (language.compareToIgnoreCase("PDDL") == 0){

                // Read transformation rules, add basic metadata (domain and requirements)
                String domain = TR_GetValueForID(transformationRules, TRConstants.hasDomainRel);
                String requirements = TR_GetValueForID(transformationRules, TRConstants.hasRequirementsRel);
                String actionName = TR_GetValueForID(transformationRules, TRConstants.hasActionRel);
                String conditionName = TR_GetValueForID(transformationRules, TRConstants.hasConditionRel);
                String effectName = TR_GetValueForID(transformationRules, TRConstants.hasEffectRel);

                // Domain & Requirements (hardcoded)
                array[0] = domain + "\n\t" + requirements + "\n";
                // Types
                array[0] += TR_GetTypes(transformationRules);
                // Predicates and Functions
                array[0] += PDDL_GetPredicatesAndFunctions(transitionData, transformationRules);
                // Transitions (Actions in PDDL)
                array[0] += PDDL_GetActions(transitionData, actionName, conditionName, effectName);

                array[0] += "\n)";
            }
            else {
                // placeholder for more languages
            }
        }

        else {
            return null;
        }

        // System.out.println(array[0]); - For Verification Purposes, Printing of Domain File
        return array;
    }

    // Returns predicates in PDDL from transitions file
    private String PDDL_GetPredicatesAndFunctions(PDDLAssets assets, Vector<TRassets> transformationRules){

        String parameterData = "";
        Vector<PDDLPredicate> tempArray = new Vector<PDDLPredicate>();

        for (int trIterator = 0; trIterator < assets.transitions.size(); trIterator++) {
            PDDLTransition currentTR = assets.transitions.elementAt(trIterator);

            Vector<PDDLPredicate> PDDLConditionExpression = currentTR.precondition.predicateSet.predicates;
            Vector<PDDLPredicate> PDDLEffectExpression = currentTR.action.predicateSet.predicates;

            for (int i = 0; i < PDDLConditionExpression.size(); i++) {
                PDDLPredicate currentPredicate = PDDLConditionExpression.elementAt(i);
                boolean hasPredicate = false;
                for (int tempArrayIterator = 0; tempArrayIterator < tempArray.size(); tempArrayIterator++) {
                    if (currentPredicate.realname.compareTo(tempArray.elementAt(tempArrayIterator).realname) == 0) {
                        hasPredicate = true;
                    }
                }
                if (!hasPredicate) {
                    tempArray.add(currentPredicate);
                }
            }

            for (int i = 0; i < PDDLEffectExpression.size(); i++) {
                PDDLPredicate currentPredicate = PDDLEffectExpression.elementAt(i);
                boolean hasPredicate = false;
                for (int tempArrayIterator = 0; tempArrayIterator < tempArray.size(); tempArrayIterator++) {
                    if (currentPredicate.realname.compareTo(tempArray.elementAt(tempArrayIterator).realname) == 0) {
                        hasPredicate = true;
                    }
                }
                if (!hasPredicate) {
                    tempArray.add(currentPredicate);
                }
            }
        }

        // Synthesis of Transition code
        if (tempArray.size() > 0){
            parameterData += "\n\t:predicates (";
            for (int i = 0; i < tempArray.size(); i++){
                // Exclusion of total-cost
                if(tempArray.elementAt(i).realname.compareToIgnoreCase("total-cost") != 0) {

                    // For non functions  (parameter) elements, start reporting
                    if (!tempArray.elementAt(i).isFunction) {
                        if (!tempArray.elementAt(i).realname.isEmpty()) {
                            parameterData += "\n\t\t(" + tempArray.elementAt(i).realname;
                            if (tempArray.elementAt(i).parameters.size() > 0) {
                                parameterData += " ";
                            }
                            for (int parameterIterator = 0; parameterIterator < tempArray.elementAt(i).parameters.size(); parameterIterator++) {
                                String parameterType = PDDL_GetParameterType(tempArray.elementAt(i), tempArray.elementAt(i).parameters.elementAt(parameterIterator));
                                parameterData += "?" + tempArray.elementAt(i).parameters.elementAt(parameterIterator) + " - " + parameterType;
                                if (parameterIterator < tempArray.elementAt(i).parameters.size() - 1) {
                                    parameterData += " ";
                                }
                            }
                            parameterData += ")";
                        }
                    }
                    else if (tempArray.elementAt(i).isFunction){ // add functions to transformation rules
                        TRassets functionAsset = new TRassets(false, tempArray.elementAt(i).name);
                        functionAsset.setName(tempArray.elementAt(i).realname);
                        functionAsset.functionParameterNames = tempArray.elementAt(i).parameters;
                        functionAsset.functionParameterTypes = tempArray.elementAt(i).parameterTypes;
                        transformationRules.add(functionAsset);
                    }


                }
            }
            parameterData += "\n\t)\n";
        }

        // Function Parsing and code synthesis, we need the Trasnformation Rules data vector for this ...
        boolean hasFunction = false;
        String functionData = "";
        for (int assetsIterator = 0; assetsIterator < transformationRules.size(); assetsIterator++){
            TRassets currentAsset = transformationRules.elementAt(assetsIterator);

            if (currentAsset.isFunction()){
                if (!hasFunction){
                    hasFunction = true;
                    functionData = "\n\t(:functions";
                }
                String functionName = currentAsset.getName();
                functionData += "\n\t\t(" + functionName;

                if (currentAsset.getFunctionParameterNames().size() > 0){

                    for (int functionParametersIterator = 0;
                         functionParametersIterator < currentAsset.getFunctionParameterNames().size();
                         functionParametersIterator++){

                        String currentParameter = currentAsset.getFunctionParameterNames().elementAt(functionParametersIterator);
                        String currentParameterIndex = currentParameter.substring(currentParameter.indexOf("_"), currentParameter.length());

                        for (int functionTypesIterator = 0;
                             functionTypesIterator < currentAsset.getFunctionParameterTypes().size();
                             functionTypesIterator++){

                            String currentType = currentAsset.getFunctionParameterTypes().elementAt(functionTypesIterator);
                            String currentTypeIndex = currentType.substring(currentType.indexOf("_"), currentType.length());

                            if ( currentTypeIndex.compareTo(currentParameterIndex) == 0){
                                functionData += " ?" +
                                        currentParameter.substring(0, currentParameter.indexOf("_"))
                                            + " - "
                                            + currentType.substring(0, currentType.indexOf("_"));
                            }
                        }
                    }
                }
                functionData += ")";
            }
        }
        if(hasFunction){
            functionData += "\n\t)";
        }

        return parameterData + functionData;
    }

    private String PDDL_GetParameterType(PDDLPredicate predicate, String parameterName){
        String parameterType = "";

        // Get Parameter Index
        String parameterIndex = parameterName.substring(parameterName.indexOf("_"), parameterName.length());

        for (int i = 0; i < predicate.parameterTypes.size(); i++){
            if(predicate.parameterTypes.elementAt(i).endsWith(parameterIndex)){

                parameterType = predicate.parameterTypes.elementAt(i).substring(
                        0,
                        predicate.parameterTypes.elementAt(i).indexOf("_"));
                return parameterType;
            }
        }

        return parameterType;
    }

    private String PDDL_GetActions(PDDLAssets transitionData, String actionName, String conditionName, String effectName){
        String PDDLActions = "";


        Iterator<PDDLTransition> iter = transitionData.transitions.iterator();

        while (iter.hasNext()) {
            PDDLTransition currentTR = iter.next();
            if (currentTR.action != null) {
                if (currentTR.action.predicateSet != null) {

                    PDDLActions += "\n\t("+actionName +  " "  + PDDLConstants.removePrefixes(currentTR.name);

                    // TODO: Add Parameters
                    // TODO: Add Duration

                    // Condition Processing
                    Vector<PDDLPredicate> PDDLConditionExpression = currentTR.precondition.predicateSet.predicates;
                    PDDLActions += "\n\t\t" + conditionName + " (" + currentTR.precondition.predicateSetOperator.toLowerCase()+"\n";
                    PDDLActions += PDDL_GetActions_processPredicatesPrecondition(PDDLConditionExpression);
                    PDDLActions += "\t\t)\n";

                    // Effect processing
                    Vector<PDDLPredicate> PDDLEffectExpression = currentTR.action.predicateSet.predicates;
                    PDDLActions += "\n\t\t" + effectName + " (" + currentTR.action.predicateSetOperator.toLowerCase() + "\n";
                    PDDLActions += PDDL_GetActions_processPredicatesAction(PDDLEffectExpression);
                    PDDLActions += "\t\t)\n";

                    PDDLActions += "\t)\n";
                }
            }
        }

        return PDDLActions;
    }

    private String PDDL_GetActions_processPredicatesAction(Vector<PDDLPredicate> predicates){
        String data = "";
        boolean hasAddOperator;
        boolean hasRemoveOperator;
        boolean hasGenericOperator;

        for (int i = 0; i<predicates.size(); i++){
            PDDLPredicate currentPredicate = predicates.elementAt(i);
     //       if (currentPredicate.parameters.size() > 0){
                hasAddOperator = false;
                hasRemoveOperator = false;
                hasGenericOperator = false;
                String timing = "at start";

                if (!currentPredicate.hasTiming.isEmpty()){
                    timing = currentPredicate.hasTiming;
                }

                if (currentPredicate.operator.compareToIgnoreCase("remove") == 0){
                    hasAddOperator = false;
                    hasRemoveOperator = true;
                }
                else if (currentPredicate.operator.compareToIgnoreCase("add") == 0){
                    hasAddOperator = true;
                    hasRemoveOperator = false;
                }else{
                    if (!currentPredicate.operator.isEmpty()){
                        hasGenericOperator = true;
                    }
                }

                if (hasAddOperator){
                    data += "\t\t\t("+timing+" (" + currentPredicate.realname+" ";
                }
                else if (hasRemoveOperator){
                    data += "\t\t\t("+timing+" (not (" + currentPredicate.realname+" ";
                }
                else if (hasGenericOperator){
                    if (currentPredicate.parameters.size()  == 0)
                        data += "\t\t\t("+timing+" (" + currentPredicate.operator + " (" + currentPredicate.realname+") ";
                    else
                        data += "\t\t\t("+timing+" (" + currentPredicate.operator + " (" + currentPredicate.realname+" ";
                }
                else
                    data += "\t\t\t("+timing+" (" + currentPredicate.realname+" ";

                String []parameterData = new String[currentPredicate.parameters.size()];
                for (int k = 0; k < currentPredicate.parameters.size(); k++){
                    String pd = currentPredicate.parameters.elementAt(k);
                    int position = Integer.parseInt(pd.substring((pd.indexOf("_") + 1), pd.length()));
                    String variableName = pd.substring(0, pd.indexOf("_"));
                    parameterData[position] = "?" + variableName;
                }

                for (int k = 0; k < parameterData.length; k++){
                    data += parameterData[k];
                    if (k < (currentPredicate.parameters.size() - 1)){
                        data += " ";
                    }
                }

                if (!currentPredicate.operatorAmount.isEmpty()){
                    String operatorAmount = currentPredicate.operatorAmount;
                    if (operatorAmount.contains("^")){
                        operatorAmount = operatorAmount.substring(0, operatorAmount.indexOf("^"));
                    }
                    if (!hasGenericOperator)
                        data += ") " + operatorAmount + "))\n";
                    else
                        if (currentPredicate.parameters.size() == 0)
                            data += "(" + operatorAmount + "))\n";
                        else
                            data += ") " + operatorAmount + "))\n";
                }
                else {
                    if (hasRemoveOperator)
                        data += ")))\n";
                    else
                        data += "))\n";
                }
   /*         }
            else
                data += "\t\t\t( " + currentPredicate.realname + "() )\n";*/
        }
        return data;
    }

    private String PDDL_GetActions_processPredicatesPrecondition(Vector<PDDLPredicate> predicates){
        String data = "";
        boolean operatorFlag = false;
        for (int i = 0; i<predicates.size(); i++){
            PDDLPredicate currentPredicate = predicates.elementAt(i);
            if (currentPredicate.parameters.size() > 0){

                if (!currentPredicate.operator.isEmpty()){
                    operatorFlag = true;
                    data += "\t\t\t(at start (" + currentPredicate.operator + " (" + currentPredicate.realname+" ";
                }
                else
                    data += "\t\t\t(at start (" + currentPredicate.realname+" ";

                String []parameterData = new String[currentPredicate.parameters.size()];
                for (int k = 0; k < currentPredicate.parameters.size(); k++){
                    String pd = currentPredicate.parameters.elementAt(k);
                    int position = Integer.parseInt(pd.substring((pd.indexOf("_") + 1), pd.length()));
                    String variableName = pd.substring(0, pd.indexOf("_"));
                    parameterData[position] = "?" + variableName;
                }

                for (int k = 0; k < parameterData.length; k++){
                    data += parameterData[k];
                    if (k < (currentPredicate.parameters.size() - 1)){
                        data += " ";
                    }
                }

                String operatorAmount = currentPredicate.operatorAmount;

                if (operatorAmount.contains("^")){
                    operatorAmount = operatorAmount.substring(0, operatorAmount.indexOf("^"));
                }

                if (operatorFlag)
                    data += ") " + operatorAmount + "))\n";
                else
                    data += "))\n";
                operatorFlag = false;
            }
            else
                data += "\t\t\t( " + currentPredicate.realname + "() )\n";
        }
        return data;
    }

    private String TR_GetTypes(Vector<TRassets> input){
        String data = "";
        boolean hasData = false;

        for (int i = 0; i < input.size(); i++){
            TRassets currentAsset = input.elementAt(i);
            if (currentAsset.isType()){
                if (data.isEmpty()){
                    data += "\n\t(:types";
                    hasData = true;
                }
                String type = PDDLConstants.removePrefixes(currentAsset.getType());
                data += " " + type;
                //data += " " + currentAsset.getType();
            }
        }

        if(hasData){
            data += ")\n";
        }

        return data;
    }

    private String TR_GetValueForID(Vector<TRassets> trRules, String ID){

        for (int i=0; i<trRules.size(); i++){
            //System.out.println("ID Scan: "+trRules.elementAt(i).getID()+" ID "+ID);
            if (trRules.elementAt(i).getID().endsWith(ID)){
                return trRules.elementAt(i).getValue();
            }
        }

        return "";
    }

    private int TR_CheckForFunction(String subjectName, Vector<TRassets> rawData){
        Iterator<TRassets> rawIterator = rawData.iterator();
        int i = 0;
        while(rawIterator.hasNext()){
            TRassets current = rawIterator.next();
            if( (current.getID().compareTo(subjectName)) == 0){
                if (current.isFunction()) {
                    //System.out.println(subjectName+"---"+i);
                    return i;
                }
            }
            i++;
        }

        return -1;
    }

    private Vector<TRassets> processTransformationRulesFile(StmtIterator iter){

        Vector<TRassets> trassets = new Vector<TRassets>();
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();

        while (iter.hasNext()){
            Statement stmt = iter.next();

            Resource s = stmt.getSubject();
            Resource p = stmt.getPredicate();
            RDFNode o = stmt.getObject();

           //System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));
        }

        // Parse <name, value> tuples, add them to a raw vector

        Iterator<PDDLRaw> rawDataIterator = rawData.iterator();
        while(rawDataIterator.hasNext()){
            PDDLRaw currentDatum = rawDataIterator.next();
            if (currentDatum.predicate.endsWith(TRConstants.hasNameRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true, false, false);
            else if (currentDatum.predicate.endsWith(TRConstants.hasValueRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, false, false, false);
            else if (currentDatum.predicate.endsWith(TRConstants.isSubclassOf)){
                if (currentDatum.object.endsWith(TRConstants.PREDICATETYPES))
                    safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true, true, false);
                else if (currentDatum.object.endsWith(TRConstants.FUNCTIONS))
                    safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true, true, true);
            }
        }

        // Need a second pass for functions, they have parameters
        rawDataIterator = rawData.iterator();
        PDDLConstants constants = new PDDLConstants("", "");
        while (rawDataIterator.hasNext()){
            PDDLRaw currentDatum = rawDataIterator.next();
            int index = TR_CheckForFunction(currentDatum.subject, trassets);
            if (index >= 0){
                //System.out.println("object: "+currentDatum.object + " predicate "+currentDatum.predicate +  " subject:"+ currentDatum.subject );
                if(currentDatum.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_REAL_NAME))){
                    trassets.elementAt(index).setName(currentDatum.object);
                }
                else if(currentDatum.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                    trassets.elementAt(index).functionParameterNames.add(currentDatum.object);
                }
                else if(currentDatum.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.IS_OF_TYPE))){
                    trassets.elementAt(index).functionParameterTypes.add(currentDatum.object);
                }
            }
        }

        // Return Raw Vector
        return trassets;
    }


    private void safeInsertTransformationRuleData(String subject, String data, Vector<TRassets> trRules,
                                                  boolean nameData, boolean isPredicateType, boolean isFunctionType){
        // Safe insert of Transformation Rules (avoids duplicate entries)
        if (!isPredicateType && !isFunctionType) {
            for (int i = 0; i < trRules.size(); i++) {
                if (subject.compareTo(trRules.elementAt(i).getID()) == 0) {
                    if (nameData)
                        trRules.elementAt(i).setName(data);
                    else
                        trRules.elementAt(i).setValue(data);
                    return;
                }
            }

            trRules.add(new TRassets(subject));
            if (nameData)
                trRules.lastElement().setName(data);
            else
                trRules.lastElement().setValue(data);
        }
        else if (isPredicateType && !isFunctionType){
            trRules.add(new TRassets(true, subject));
        }
        else if (isFunctionType){
            trRules.add(new TRassets(false, subject));
        }

        return;
    }

    private PDDLAssets processTransitionsFile(StmtIterator iter, Vector<TRassets> transformationRules) {

        //PDDLModel modelToReturn = new PDDLModel();
        PDDLUtil utilityMethods = new PDDLUtil();

        PDDLConstants constants = new PDDLConstants("", "");
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();

         while (iter.hasNext()) {
            Statement stmt = iter.next();

            Resource s = stmt.getSubject();
            Resource p = stmt.getPredicate();
            RDFNode o = stmt.getObject();

       //     System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));
        }

        // The data structure which will store the processed data

        PDDLAssets assets = new PDDLAssets();

        // Now lets do the processing
        // First Pass: Scan and store primitives by name

        Iterator<PDDLRaw> iterator = rawData.iterator();
        while (iterator.hasNext()) {


            PDDLRaw currentTriplet = iterator.next();

                if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                    && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.TRANSITION))) {
                    assets.transitions.add(new PDDLTransition(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PREDICATE_SET))) {
                    assets.predicateSets.add(new PDDLPredicateSet(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.ACTION))) {
                    assets.actions.add (new PDDLAction(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PRECONDITION))) {
                    assets.preconditions.add (new PDDLPrecondition(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PREDICATE))) {
                    assets.predicates.add (new PDDLPredicate(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.ADD))) {
                    assets.predicates.add (new PDDLPredicate(currentTriplet.subject, "ADD"));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.REMOVE))) {
                    assets.predicates.add (new PDDLPredicate(currentTriplet.subject, "REMOVE"));
                }

        }

        // Second Pass: Scan and store lower-level entities
            iterator = rawData.iterator();
            while (iterator.hasNext()){
                PDDLRaw currentTriplet = iterator.next();
                SubjectType stype = utilityMethods.getSubjectType(currentTriplet.subject, assets);

                // Note, there may be some triplets with unrecognized types, thats ok, we can ignore them
                if (stype != null){
                   //System.out.println("Got: "+stype.subjectType + " "+stype.vectorPosition);
                   switch (stype.subjectType) {
                       case TRANSITION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_ACTION))){
                               assets.transitions.elementAt(stype.vectorPosition).action = new PDDLAction(currentTriplet.object);
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_COMPUTATION))){
                               assets.transitions.elementAt(stype.vectorPosition).computation = new PDDLComputation(currentTriplet.object);
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PRECONDITION))){
                               assets.transitions.elementAt(stype.vectorPosition).precondition = new PDDLPrecondition(currentTriplet.object);
                           }
                           break;
                       case ACTION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_SET))){
                               assets.actions.elementAt(stype.vectorPosition).predicateSet = new PDDLPredicateSet(currentTriplet.object);
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_SET_OPERATOR))){
                               assets.actions.elementAt(stype.vectorPosition).predicateSetOperator = currentTriplet.object;
                               break;
                           }
                       case PRECONDITION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_SET))){
                               assets.preconditions.elementAt(stype.vectorPosition).predicateSet = new PDDLPredicateSet(currentTriplet.object);
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_SET_OPERATOR))){
                               assets.preconditions.elementAt(stype.vectorPosition).predicateSetOperator = currentTriplet.object;
                               break;
                           }
                       case PREDICATE_SET:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE))){
                               assets.predicateSets.elementAt(stype.vectorPosition).predicates.add(new PDDLPredicate(currentTriplet.object));
                               break;
                           }
                       case PREDICATE:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.predicates.elementAt(stype.vectorPosition).addParameter(currentTriplet.object);
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_REAL_NAME))){
                               assets.predicates.elementAt(stype.vectorPosition).realname = currentTriplet.object;
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_OPERATOR))){
                               assets.predicates.elementAt(stype.vectorPosition).operator = currentTriplet.object;
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_OPERATOR_AMOUNT))){
                               assets.predicates.elementAt(stype.vectorPosition).operatorAmount = currentTriplet.object;
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_TIMING))){
                               assets.predicates.elementAt(stype.vectorPosition).hasTiming = currentTriplet.object;
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.IS_OF_TYPE))){
                               assets.predicates.elementAt(stype.vectorPosition).addParameterType(currentTriplet.object);
                               break;
                           }
                           else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.IS_FUNCTION))){
                               assets.predicates.elementAt(stype.vectorPosition).isFunction = true;
                               break;
                           }
                   }

                }
            }

            // --- Begin Parsing of Transitions Model

            // Add content to PDDLAssets data structure
            utilityMethods.addPredicateSetContent(assets);
            utilityMethods.addActionAndPreconditionContent(assets);
            utilityMethods.addTransitionContent(assets);

            // Used for verification, can be commented out
            // utilityMethods.printTransitionModel(assets);
            // -- Parsing of Transitions Model is Complete

        return assets;
    }

    private PDDLAssets processStatesFile(StmtIterator iter, Vector<TRassets> transformationRules){
        PDDLAssets assets = new PDDLAssets();
        //PDDLModel modelToReturn = new PDDLModel();
        PDDLUtil utilityMethods = new PDDLUtil();

        PDDLConstants constants = new PDDLConstants("", "");
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();

        while (iter.hasNext()) {
            Statement stmt = iter.next();

            Resource s = stmt.getSubject();
            Resource p = stmt.getPredicate();
            RDFNode o = stmt.getObject();

            //System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));
        }

        Iterator<PDDLRaw> iterator = rawData.iterator();
        while (iterator.hasNext()) {

            PDDLRaw currentTriplet = iterator.next();

            if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE))
                    && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.NAMED_INDIVIDUAL))) {
                boolean found = false;
                for (int i = 0; i < assets.objects.size(); i++){
                    if (assets.objects.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        found = true;
                    }
                }
                if (!found){
                    if (currentTriplet.subject.contains("_")) {
                        String type = currentTriplet.subject.substring(
                                currentTriplet.subject.indexOf("#") + 1,
                                currentTriplet.subject.indexOf("_")
                        );
                        assets.objects.add(new PDDLObject(currentTriplet.subject, type));
                    }
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))) {
                boolean found = false;
                for (int i = 0; i < assets.objects.size(); i++){
                    if (assets.objects.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        assets.objects.elementAt(i).parameters.add(currentTriplet.object);
                        found = true;
                    }
                }
                if (!found){
                    String type = "";
                    if (currentTriplet.subject.contains("_")) {
                        type = currentTriplet.subject.substring(
                                currentTriplet.subject.indexOf("#") + 1,
                                currentTriplet.subject.indexOf("_")
                        );
                    }
                    assets.objects.add(new PDDLObject(currentTriplet.subject, type));
                    assets.objects.lastElement().parameters.add(currentTriplet.object);
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_VALUE))){
                boolean found = false;
                for (int i = 0; i < assets.objects.size(); i++){
                    if (assets.objects.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        assets.objects.elementAt(i).value = currentTriplet.object;
                        found = true;
                    }
                }
                if (!found){
                    String type = "";
                    if (currentTriplet.subject.contains("_")) {
                        type = currentTriplet.subject.substring(
                                currentTriplet.subject.indexOf("#") + 1,
                                currentTriplet.subject.indexOf("_")
                        );
                    }
                    assets.objects.add(new PDDLObject(currentTriplet.subject, type));
                    assets.objects.lastElement().value = currentTriplet.object;
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE))
                    && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.ACTION))) {
                boolean found = false;
                for (int i = 0; i < assets.actions.size(); i++){
                    if (assets.actions.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        found = true;
                    }
                }
                if (!found) assets.actions.add(new PDDLAction(currentTriplet.subject));
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_LIST))) {
                boolean found = false;
                for (int i = 0; i < assets.actions.size(); i++){
                    if (assets.actions.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        assets.actions.elementAt(i).predicateSetName = currentTriplet.object;
                        found = true;
                    }
                }
                if (!found){
                    assets.actions.add(new PDDLAction(currentTriplet.subject));
                    assets.actions.lastElement().predicateSetName = currentTriplet.object;
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE))){
                boolean found = false;
                for (int i = 0; i < assets.predicateSets.size(); i++){
                    if (assets.predicateSets.elementAt(i).name.compareTo(currentTriplet.subject) == 0){
                        found = true;
                        assets.predicateSets.elementAt(i).predicates.add(new PDDLPredicate(currentTriplet.object));
                    }
                }
                if (!found){
                    assets.predicateSets.add(new PDDLPredicateSet(currentTriplet.subject));
                    assets.predicateSets.lastElement().predicates.add(new PDDLPredicate(currentTriplet.object));
                }
            }
        }

        // Second parse, connect predicate sets to from actions to named individuals

        return assets;
    }
}

// Utility data structure
class SubjectType{
    public PDDLConstants.objectType subjectType;
    public int vectorPosition;

    public SubjectType(PDDLConstants.objectType type, int vectorPos){
        subjectType = type;
        vectorPosition = vectorPos;
    }
}