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

        Model model_transitionsFile = ModelFactory.createDefaultModel();
        Model model_transformationsRuleFile = ModelFactory.createDefaultModel();

        model_transitionsFile.read(transitionsInputStream, null, "TURTLE");
        model_transformationsRuleFile.read(tfInputStream, null, "TURTLE");

        StmtIterator transitions_file_statement_iterator = model_transitionsFile.listStatements();
        StmtIterator transformationsRule_file_statement_iterator = model_transformationsRuleFile.listStatements();

        // Process Transformation Rules, States file, Transitions file
        Vector<TRassets> trModel = processTransformationRulesFile(transformationsRule_file_statement_iterator);
        PDDLAssets transitionsModel = processDomainFile(transitions_file_statement_iterator, trModel);

        // Generate output
        return generateOutput(trModel, transitionsModel);
    }

    private String[] generateOutput(Vector<TRassets> transformationRules, PDDLAssets transitionData){
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
                //TODO: Change name of the below to GetPredicatesAndFunctions, do necessary changes in model
                array[0] += PDDL_GetPredicates(transitionData);
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

        System.out.println(array[0]);
        return array;
    }

    // Returns predicates in PDDL from transitions file
    private String PDDL_GetPredicates(PDDLAssets assets){

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
                // Exlusion of total-cost
                if(tempArray.elementAt(i).realname.compareToIgnoreCase("total-cost") != 0) {
                    if (!tempArray.elementAt(i).realname.isEmpty()) {
                        parameterData += "\n\t\t(" + tempArray.elementAt(i).realname;
                        //System.out.println (tempArray.elementAt(i).name+" parameter 0 -> "+tempArray.elementAt(i).parameters.elementAt(0));
                        if (tempArray.elementAt(i).parameters.size() > 0) {
                            parameterData += " ";
                        }
                        for (int parameterIterator = 0; parameterIterator < tempArray.elementAt(i).parameters.size(); parameterIterator++) {
                            parameterData += "?" + tempArray.elementAt(i).parameters.elementAt(parameterIterator);
                            if (parameterIterator < tempArray.elementAt(i).parameters.size() - 1) {
                                parameterData += " ";
                            }
                        }
                        parameterData += ")";
                    }
                }
            }
            parameterData += "\n\t)\n";
        }

        return parameterData;
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

    // TODO: Process parameters for each action here - do we need an extra entity and subentities type (coordinate, ...) and then a
    // relationship in every parameter hasParameterType ?
    private String PDDL_GetActions_processParameters (PDDLAction currentAction) {
        String parameterData = "";




        return parameterData;
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

    private Vector<TRassets> processTransformationRulesFile(StmtIterator iter){

        Vector<TRassets> trassets = new Vector<TRassets>();
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();

        while (iter.hasNext()){
            Statement stmt = iter.next();

            Resource s = stmt.getSubject();
            Resource p = stmt.getPredicate();
            RDFNode o = stmt.getObject();

        //    System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));
        }

        // Parse <name, value> tuples, add them to a raw vector

        Iterator<PDDLRaw> rawDataIterator = rawData.iterator();
        while(rawDataIterator.hasNext()){
            PDDLRaw currentDatum = rawDataIterator.next();
            if (currentDatum.predicate.endsWith(TRConstants.hasNameRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true, false);
            else if (currentDatum.predicate.endsWith(TRConstants.hasValueRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, false, false);
            else if (currentDatum.predicate.endsWith(TRConstants.isSubclassOf)){
                if (currentDatum.object.endsWith("PredicateTypes"))
                    safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true, true);
            }
        }

        /*for (int a = 0; a < trassets.size(); a++){
            System.out.println(trassets.elementAt(a).getID() + " NAME " + trassets.elementAt(a).getName() + " VAL "+
                trassets.elementAt(a).getValue());
        }*/

        // Return Raw Vector
        return trassets;
    }

    // Safe insert of Transformation Rules (avoids duplicate entries)
    private void safeInsertTransformationRuleData(String subject, String data, Vector<TRassets> trRules, boolean nameData, boolean isPredicateType){

        if (!isPredicateType) {
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
        else {
            trRules.add(new TRassets(true, subject));
        }

        return;
    }

    private PDDLAssets processDomainFile(StmtIterator iter, Vector<TRassets> transformationRules) {

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