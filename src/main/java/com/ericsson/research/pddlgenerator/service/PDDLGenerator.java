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

    // Returns predicates in PDDL from transitions file
    private String PDDL_GetPredicates(PDDLAssets transitions){

        String predicates = "";

        Iterator<PDDLPredicate> iter = transitions.predicates.iterator();

        if(transitions.predicates.size() > 0){
            predicates = "\t(:predicates\n";
        }

        while (iter.hasNext()){
            PDDLPredicate currentPredicate = iter.next();
            predicates += "\t\t(" +PDDLConstants.removePrefixes(currentPredicate.name)+"\n";


        }

        if(transitions.predicates.size() > 0){
            predicates += "\t)\n";
        }

        return predicates;
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
                    PDDLActions += processPredicatesPrecondition(PDDLConditionExpression);
                    PDDLActions += "\t\t)\n";

                    // Effect processing
                    Vector<PDDLPredicate> PDDLEffectExpression = currentTR.action.predicateSet.predicates;
                    PDDLActions += "\n\t\t" + effectName + " (" + currentTR.action.predicateSetOperator.toLowerCase() + "\n";
                    PDDLActions += processPredicatesAction(PDDLEffectExpression);
                    PDDLActions += "\t\t)\n";

                    PDDLActions += "\t)\n";
                }
            }
        }

        return PDDLActions;
    }

    private String processPredicatesAction(Vector<PDDLPredicate> predicates){
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
                        data += ", ";
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

    private String processPredicatesPrecondition(Vector<PDDLPredicate> predicates){
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
                        data += ", ";
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

    private String[] generateOutput(Vector<TRassets> transformationRules, PDDLAssets transitionData){
        String[] array = new String[2];

        if (!(getTRValueForID(transformationRules, TRConstants.hasLanguageRel)).isEmpty()) {
            String language = getTRValueForID(transformationRules, TRConstants.hasLanguageRel);
            if (language.compareToIgnoreCase("PDDL") == 0){

                // Read transformation rules, add basic metadata (domain and requirements)
                String domain = getTRValueForID(transformationRules, TRConstants.hasDomainRel);
                String requirements = getTRValueForID(transformationRules, TRConstants.hasRequirementsRel);
                String actionName = getTRValueForID(transformationRules, TRConstants.hasActionRel);
                String conditionName = getTRValueForID(transformationRules, TRConstants.hasConditionRel);
                String effectName = getTRValueForID(transformationRules, TRConstants.hasEffectRel);
                System.out.println("Language Detected: PDDL (domain "+domain+")");
                array[0] = domain + "\n\t" + requirements + "\n";

                //TODO: Predicates and Actions
                // Predicates
                // array[0] += PDDL_GetPredicates(transitionData);

                // Transitions (Actions in PDDL)
                array[0] += PDDL_GetActions(transitionData, actionName, conditionName, effectName);


                array[0] += "\n)";
            }
            else {
                // TODO: Add support for more languages here
            }
        }

        else {
            return null;
        }

        System.out.println(array[0]);
        return array;
    }

    private String getTRValueForID(Vector<TRassets> trRules, String ID){

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

            //System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));
        }

        // Parse <name, value> tuples, add them to a raw vector

        Iterator<PDDLRaw> rawDataIterator = rawData.iterator();
        while(rawDataIterator.hasNext()){
            PDDLRaw currentDatum = rawDataIterator.next();
            if (currentDatum.predicate.endsWith(TRConstants.hasNameRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, true);
            else if (currentDatum.predicate.endsWith(TRConstants.hasValueRel))
                safeInsertTransformationRuleData(currentDatum.subject, currentDatum.object, trassets, false);
        }

        /*for (int a = 0; a < trassets.size(); a++){
            System.out.println(trassets.elementAt(a).getID() + " NAME " + trassets.elementAt(a).getName() + " VAL "+
                trassets.elementAt(a).getValue());
        }*/

        // Return Raw Vector
        return trassets;
    }

    // Safe insert of Transformation Rules (avoids duplicate entries)
    private void safeInsertTransformationRuleData(String subject, String data, Vector<TRassets> trRules, boolean nameData){

        for (int i=0; i < trRules.size(); i++){
            if (subject.compareTo(trRules.elementAt(i).getID()) == 0){
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