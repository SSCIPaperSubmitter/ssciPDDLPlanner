package asepaper.pddlgenerator;

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by eathkar on 03/05/16.
 */
public class PDDLGenerator {

    public String[] GeneratePlan(String domainFile, String problemFile, boolean writeToFile){
        String[] array = new String[2];

        FileManager.get().addLocatorClassLoader(PDDLGenerator.class.getClassLoader());

        Model model_domainFile = FileManager.get().loadModel(domainFile, null, "TURTLE");
        Model model_problemFile = FileManager.get().loadModel(problemFile, null, "TURTLE");

        StmtIterator model_file_statement_iterator = model_domainFile.listStatements();
        StmtIterator problem_file_statement_iterator = model_problemFile.listStatements();

        PDDLModel domainModel = new PDDLModel();

        domainModel = processDomainFile(model_file_statement_iterator);
        String problemFileContent = processProblemFile(problem_file_statement_iterator, domainModel);

        if (writeToFile){
            File file = new File (PDDLConstants.domainFileName);
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(domainModel.dataToReturn);
                bw.close();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

            File file2 = new File (PDDLConstants.problemFileName);
            try {
                FileWriter fw = new FileWriter(file2.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(problemFileContent);
                bw.close();
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
        array[0] = domainModel.dataToReturn;
        array[1] = problemFileContent;
        return array;
    }

    private boolean containsType(String []typeArray, String typeToSearch){
        boolean containsType = false;
        //System.out.println("TYPE ARRAY:"+typeArray.length);
        for (int i = 0; i < typeArray.length; i++){
            //System.out.println("accessing typeArray["+i+"]:"+typeArray[i]+" with length: "+typeArray.length);
            if (typeArray[i].compareTo(typeToSearch) == 0){
                return true;
            }
        }

        return containsType;
    }

    private String processProblemFile(StmtIterator iter, PDDLModel model) {
        String data = "";
        PDDLConstants constants = new PDDLConstants("", "");
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();
        Vector<PDDLObject> pddlObjects = new Vector<PDDLObject>();

        System.out.println("Processing problem file ...");

        // Note: Since predicates come at random, we will first save an unprocessed (raw) list of triplets, and
        // subsequently process them

        while (iter.hasNext()) {
            Statement stmt = iter.next();

            Resource s = stmt.getSubject();
            Resource p = stmt.getPredicate();
            RDFNode o = stmt.getObject();

            //System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
            rawData.add(new PDDLRaw(o.toString(), p.toString(), s.toString()));

            //System.out.println();
        }


        // Lets first generate some constant names - header of our file
        String problemData = "(define (problem " +
                constants.getDomain() +
                "-01)\n\t(:domain " +
                constants.getDomain() +
                ")";


        // The data structure which will store the processed data

        PDDLAssets assets = new PDDLAssets();

        // Now lets do the processing
        // First Pass: Scan and store primal primitives

        Iterator<PDDLRaw> iterator = rawData.iterator();
        Vector<PDDLObjectCollection> objectStore = new Vector<PDDLObjectCollection>();
        Vector<PDDLAction> actions = new Vector<PDDLAction>();
        Vector<PDDLPredicateList> predicate_lists = new Vector<PDDLPredicateList>();
        Vector<PDDLPredicate> predicates = new Vector<PDDLPredicate>();

        while (iterator.hasNext()) {
            PDDLRaw currentTriplet = iterator.next();
            //System.out.println(currentTriplet.object + " " + currentTriplet.predicate + " " + currentTriplet.subject);
            if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE))
                    && containsType(model.types, constants.removePrefixes(currentTriplet.subject))){
                String objectName = constants.removePrefixes(currentTriplet.object);
                String objectType = constants.removePrefixes(currentTriplet.subject);
                addPDDLObject(objectStore, new PDDLObject(objectName, objectType));
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE)) &&
                    currentTriplet.subject.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.ACTION))){
                /*System.out.println("action exists: "+constants.removePrefixes(currentTriplet.object)+ " "+
                        actionExists(actions, constants.removePrefixes(currentTriplet.object)));*/
                if(!actionExists(actions, constants.removePrefixes(currentTriplet.object))) {
                    actions.add(new PDDLAction(constants.removePrefixes(currentTriplet.object)));
                    //System.out.println("Add action at type "+constants.removePrefixes(currentTriplet.object));
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE_LIST))){
                String predicateList = constants.removePrefixes(currentTriplet.subject);
                String action = constants.removePrefixes(currentTriplet.object);
                Iterator<PDDLAction> actionIterator = actions.iterator();
                boolean action_added = false;

                while (actionIterator.hasNext()){
                    PDDLAction currentAction = actionIterator.next();
                    if (currentAction.name.compareTo(constants.removePrefixes(action)) == 0){
                        // We need the name of the predicate list in order to match it to a PDDLPredicateList later on
                        currentAction.predicateListName = predicateList;
                        action_added = true;
                        break;
                    }
                }
                // In case we read a predicatelist prior to an action, then we add the action and the predicate list
                if(!action_added){
                    actions.add(new PDDLAction(constants.removePrefixes(action)));
                    actions.lastElement().predicateListName = predicateList;
                    //System.out.println("Add action at hasPredicateList "+constants.removePrefixes(action));
                }
            }
            else if(currentTriplet.predicate.endsWith(
                    constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PREDICATE))){

              //  if (currentTriplet.subject.contains("C09_C10")) {
                //    System.out.println("Subjet " + currentTriplet.subject + " object " + currentTriplet.object + " predicate " + currentTriplet.predicate);
               // }
                int index = predicateListIndex(predicate_lists, constants.removePrefixes(currentTriplet.object));
                if (index < 0){
                    predicate_lists.add(new PDDLPredicateList(constants.removePrefixes(currentTriplet.object)));
                    predicate_lists.lastElement().list.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.subject)));
                } else {
                    predicate_lists.elementAt(index).list.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.subject)));
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE)) &&
                    currentTriplet.subject.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PREDICATE_LIST))){
                if (predicateListIndex(predicate_lists, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicate_lists.add(new PDDLPredicateList(constants.removePrefixes(currentTriplet.object)));
                }
            }
            else if(currentTriplet.predicate.endsWith(
                constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                int index = predicateIndex(predicates, constants.removePrefixes(currentTriplet.object));
                if (index < 0){
                    //System.out.println("adding "+constants.removePrefixes(currentTriplet.object));
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                    predicates.lastElement().parameters.add(constants.removePrefixes(currentTriplet.subject));
                } else {
                    predicates.elementAt(index).parameters.add(constants.removePrefixes(currentTriplet.subject));
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE)) &&
                    currentTriplet.subject.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PREDICATE))){
                if (predicateIndex(predicates, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE))
                    && containsType(model.functionTypes, constants.removePrefixes(currentTriplet.subject))){
                if (predicateIndex(predicates, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                    predicates.lastElement().type = constants.removePrefixes(currentTriplet.subject);
                }
                else {
                    predicates.elementAt(predicateIndex(predicates,
                            constants.removePrefixes(currentTriplet.object))).type =
                            constants.removePrefixes(currentTriplet.subject);
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.TYPE))
                    && containsType(model.predicateTypes, constants.removePrefixes(currentTriplet.subject))){
                if (predicateIndex(predicates, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                    predicates.lastElement().type = constants.removePrefixes(currentTriplet.subject);
                    predicates.lastElement().isPred = true;
                }
                else {
                    predicates.elementAt(predicateIndex(predicates,
                            constants.removePrefixes(currentTriplet.object))).type =
                            constants.removePrefixes(currentTriplet.subject);
                    predicates.elementAt(predicateIndex(predicates,
                            constants.removePrefixes(currentTriplet.object))).isPred = true;
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_OPERATOR))){
                if(predicateIndex(predicates, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                    predicates.lastElement().hasOperator = currentTriplet.subject;
                }
                else
                    predicates.elementAt(predicateIndex(predicates, constants.removePrefixes(currentTriplet.object))).
                            hasOperator = currentTriplet.subject;
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_VALUE))){
                if(predicateIndex(predicates, constants.removePrefixes(currentTriplet.object)) < 0){
                    predicates.add(new PDDLPredicate(constants.removePrefixes(currentTriplet.object)));
                    if (currentTriplet.subject.indexOf("^") > 0)
                        predicates.lastElement().hasValue = currentTriplet.subject.substring(0,
                            currentTriplet.subject.indexOf("^"));
                    else
                        predicates.lastElement().hasValue = currentTriplet.subject;
                }
                else {
                    if (currentTriplet.subject.indexOf("^") > 0)
                        predicates.elementAt(predicateIndex(predicates, constants.
                                removePrefixes(currentTriplet.object))).hasValue = currentTriplet.subject.substring(0,
                                currentTriplet.subject.indexOf("^"));
                    else
                        predicates.elementAt(predicateIndex(predicates, constants.
                                removePrefixes(currentTriplet.object))).hasValue = currentTriplet.subject;
                }
            }
            else if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_ACTION_OPERATOR))){
                Iterator<PDDLAction> actionIterator = actions.iterator();
                String action = constants.removePrefixes(currentTriplet.object);
                String operator = currentTriplet.subject;

                boolean action_added = false;

                while (actionIterator.hasNext()){
                    PDDLAction currentAction = actionIterator.next();
                    if (currentAction.name.compareTo(action) == 0){
                        currentAction.assignOperator(operator);
                        action_added = true;
                        break;
                    }
                }

                if(!action_added){
                    actions.add(new PDDLAction(action));
                    actions.lastElement().assignOperator(operator);
                    //System.out.println("Add action at hasActionOperator "+action);
                }
            }

        }

        // Verify predicates were stored at this point
        /*for (int la = 0; la < predicates.size(); la++){
            System.out.println("PREDICATE: "+predicates.elementAt(la).name);
        }*/

        // some "stitching" required after parsing:
        // (a) stitch predicates to pred. lists and
        // (b) pred. lists to actions

        for (int i = 0; i < predicate_lists.size(); i++){
            PDDLPredicateList currentList = predicate_lists.elementAt(i);
            for (int k = 0; k < currentList.list.size(); k++){
                PDDLPredicate currentPredicate = currentList.list.elementAt(k);
                for (int j = 0; j < predicates.size(); j++){
                    //System.out.println("Current predicate: "+currentPredicate.name + " and predicate list "+predicates.elementAt(j).name);
                    if (currentPredicate.name.compareTo(predicates.elementAt(j).name) == 0){
                        currentList.list.set(k, predicates.elementAt(j));
                    }
                }
            }
        }

        for (int i = 0; i < actions.size(); i++){
            PDDLAction currentAction = actions.elementAt(i);
            for (int k = 0; k < predicate_lists.size(); k++){
                if (currentAction.predicateListName.compareTo(predicate_lists.elementAt(k).name) == 0){
                    currentAction.predicateList = predicate_lists.elementAt(k);
                }
            }
        }

        // object generation
        String objectData = "\n\t(:objects";
        for (int i = 0; i < objectStore.size(); i++){
            PDDLObjectCollection currentCollection = objectStore.elementAt(i);
            for (int k = 0; k < currentCollection.getObjects().size(); k++){
                if (k == 0){
                    objectData += "\n\t\t";
                }
                PDDLObject currentObject = currentCollection.getObjects().elementAt(k);
                if (k < (currentCollection.getObjects().size() - 1))
                    objectData += " " + currentObject.name;
                else
                    objectData += " " + currentObject.name + " - " + currentObject.type;
            }

        }
        objectData += "\n\t)";

        // state generation

        String stateData = "";
        for (int i = 0; i < actions.size(); i++){
            PDDLAction currentAction = actions.elementAt(i);
            stateData += "\n\t(:"+currentAction.name;
            if (currentAction.hasOperator()){
                stateData += " ("+currentAction.getOperator();
            }
            for (int k = 0; k < currentAction.predicateList.list.size(); k++){
                PDDLPredicate currentPredicate = currentAction.predicateList.list.elementAt(k);
                // System.out.println("CURRENT PREDICATE: "+currentPredicate.name);
                stateData += "\n\t\t";
                if (!currentPredicate.hasOperator.isEmpty())
                    stateData += "("+currentPredicate.hasOperator+" ";
                stateData += "(";
                stateData += currentPredicate.type;
                if (currentPredicate.parameters.size() > 0){
                    stateData += " ";
                }

                for (int j = currentPredicate.parameters.size() - 1; j >= 0; j--){
                    stateData += currentPredicate.parameters.elementAt(j);
                    if(j > 0)
                        stateData += " ";
                    else
                        stateData += ")";

                }
                // This should take care of total-cost
                if (!currentPredicate.hasOperator.isEmpty()
                        && currentPredicate.parameters.size() == 0){
                    stateData += ")";
                }
                if(!currentPredicate.hasValue.isEmpty()){
                    stateData += " " + currentPredicate.hasValue;
                }
                if(!currentPredicate.hasOperator.isEmpty()){
                    stateData += ")";
                }

            }
            if (currentAction.hasOperator()){
                stateData += "\n\t\t)";
            }
            stateData += "\n\t)";
        }

        stateData += "\n)";

        data = problemData + objectData + stateData;

        return data;
    }

    private int predicateIndex (Vector<PDDLPredicate> predicateStore, String predicate){
        int index = 0;
        Iterator<PDDLPredicate> predicateIterator = predicateStore.iterator();
        while(predicateIterator.hasNext()){
            PDDLPredicate pred = predicateIterator.next();
            if (pred.name.compareTo(predicate) == 0){
                return index;
            }
            index++;
        }
        return -1;
    }
    private int predicateListIndex (Vector<PDDLPredicateList> predicateListStore, String predicateList){
        int index = 0;
        Iterator<PDDLPredicateList> predicateListIterator = predicateListStore.iterator();
        while(predicateListIterator.hasNext()){
            PDDLPredicateList list = predicateListIterator.next();
            if (list.name.compareTo(predicateList) == 0){
                return index;
            }
            index++;
        }
        return -1;
    }

    private boolean actionExists (Vector <PDDLAction> actionStore, String actionName){

        Iterator<PDDLAction> actionIterator = actionStore.iterator();
        while (actionIterator.hasNext()){
            String an = actionIterator.next().name;

            if(an.compareTo(actionName) == 0){
                return true;
            }
        }

        return false;
    }

    private void addPDDLObject(Vector<PDDLObjectCollection> dataStore, PDDLObject objectToAdd){
        if (dataStore.size() == 0){
            dataStore.add(new PDDLObjectCollection(objectToAdd.type));
            dataStore.lastElement().insertIntoCollection(objectToAdd);
        } else{
            int listIndex = 0;
            boolean hasList = false;
            Iterator<PDDLObjectCollection> iter = dataStore.iterator();
            while (iter.hasNext()) {
                PDDLObjectCollection currentCollection = iter.next();
                if (currentCollection.datatype.compareTo(objectToAdd.type) == 0) {
                    hasList = true;
                    break;
                }
                listIndex++;
            }

            if (hasList){
                dataStore.elementAt(listIndex).insertIntoCollection(objectToAdd);
            }
            else {
                dataStore.add(new PDDLObjectCollection(objectToAdd.type));
                dataStore.lastElement().insertIntoCollection(objectToAdd);
            }
        }
    }

    private PDDLModel processDomainFile(StmtIterator iter){

        PDDLModel modelToReturn = new PDDLModel();

        String domainData = "";
        String requirementData = "";
        String typeData = "";
        String predicateData = "";
        String functionData = "";
        String actionData = "";
        String data = "";

        PDDLConstants constants = new PDDLConstants("", "");
        Vector<PDDLRaw> rawData = new Vector<PDDLRaw>();

        System.out.println("Processing domain file ...");

        // Note: Since predicates come at random, we will first save an unprocessed (raw) list of triplets, and
        // subsequently process them

            while(iter.hasNext()){
                Statement stmt = iter.next();

                Resource s = stmt.getSubject();
                Resource p = stmt.getPredicate();
                RDFNode o = stmt.getObject();

                //System.out.println("Adding "+s.toString()+ " "+p.toString()+" "+o.toString());
                rawData.add(new PDDLRaw(s.toString(), p.toString(), o.toString()));

                //System.out.println();
            }

            // The data structure which will store the processed data

            PDDLAssets assets = new PDDLAssets();

            // Now lets do the processing
            // First Pass: Scan and store primal primitives

            Iterator<PDDLRaw> iterator = rawData.iterator();
            while (iterator.hasNext()){


                PDDLRaw currentTriplet = iterator.next();

                if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.ACTION))){
                    assets.actions.add(new PDDLAction(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PREDICATE))){
                    assets.predicates.add(new PDDLPredicate(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.FUNCTION))){
                    assets.functions.add(new PDDLFunction(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.CONDITION))){
                    assets.conditions.add(new PDDLCondition(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.EFFECT))){
                    assets.effects.add(new PDDLEffect(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.CONDITIONAL_EXPRESSION))){
                    assets.conditionalExpressions.add(new PDDLConditionalExpression(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.DURATION))){
                    assets.durations.add(new PDDLDuration(currentTriplet.subject));
                }
                else if (currentTriplet.predicate.contains(constants.getPredicateDefinition(PDDLConstants.predicateType.SUBCLASS_OF))
                        && currentTriplet.object.endsWith(constants.getObjectDefinition(PDDLConstants.objectType.PARAMETER))){
                    assets.parameters.add(new PDDLParameter(currentTriplet.subject));
                }

            }

            // Second Pass: For second-level primitives, populate with content
            iterator = rawData.iterator();
            while (iterator.hasNext()){
                PDDLRaw currentTriplet = iterator.next();
                SubjectType stype = getSubjectType(currentTriplet.subject, assets);

                // Note, there may be some triplets with unrecognized types, thats ok, we can ignore them
                if (stype != null){
                   //System.out.println("Got: "+stype.subjectType + " "+stype.vectorPosition);
                   switch (stype.subjectType){
                       case FUNCTION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.functions.elementAt(stype.vectorPosition).parameters.add(currentTriplet.object);
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_OPERATOR))){
                               assets.functions.elementAt(stype.vectorPosition).operator = currentTriplet.object;
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_VALUE))){
                               assets.functions.elementAt(stype.vectorPosition).value = currentTriplet.object;
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_NAME))){
                               assets.functions.elementAt(stype.vectorPosition).realname = currentTriplet.object;
                           }
                           break;
                       case PREDICATE:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.predicates.elementAt(stype.vectorPosition).parameters.add(currentTriplet.object);
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_NAME))){
                               assets.predicates.elementAt(stype.vectorPosition).realname = currentTriplet.object;
                           }
                           break;
                       case DURATION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.durations.elementAt(stype.vectorPosition).parameters.add(currentTriplet.object);
                           }
                           break;
                       case CONDITIONAL_EXPRESSION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.conditionalExpressions.elementAt(stype.vectorPosition).parameters.add(currentTriplet.object);
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_TIMING))){
                               assets.conditionalExpressions.elementAt(stype.vectorPosition).timing = currentTriplet.object;
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_NEGATION))){

                               if (currentTriplet.object.contains("true")) {
                                   assets.conditionalExpressions.elementAt(stype.vectorPosition).hasNegation = true;
                               }
                               else assets.conditionalExpressions.elementAt(stype.vectorPosition).hasNegation = false;
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_OPERATOR))){
                               assets.conditionalExpressions.elementAt(stype.vectorPosition).operator = currentTriplet.object;
                           }
                           break;
                       case CONDITION: // Requires meta-linking
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_CONDITION_EXPRESSION_PART))){
                               assets.conditions.elementAt(stype.vectorPosition).conditional_expressions.add(new PDDLConditionalExpression(currentTriplet.object));
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_CONDITION_OPERATOR))){
                               assets.conditions.elementAt(stype.vectorPosition).conditionalOperator = currentTriplet.object;
                           }
                           break;
                       case EFFECT:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_CONDITION_EXPRESSION_PART))){
                               assets.effects.elementAt(stype.vectorPosition).conditional_expressions.add(new PDDLConditionalExpression(currentTriplet.object));
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_EFFECT_OPERATOR))){
                               assets.effects.elementAt(stype.vectorPosition).conditionalOperator = currentTriplet.object;
                           }
                           break;
                       case ACTION:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_EFFECT))){
                               assets.actions.elementAt(stype.vectorPosition).effect.add(new PDDLEffect(currentTriplet.object));
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_CONDITION))){
                               assets.actions.elementAt(stype.vectorPosition).condition.add(new PDDLCondition(currentTriplet.object));
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.IS_DURATIVE_ACTION))){
                              if (currentTriplet.object.contains("true")) assets.actions.elementAt(stype.vectorPosition).isDurativeAction = true;
                              else assets.actions.elementAt(stype.vectorPosition).isDurativeAction = false;
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_DURATION))){
                               assets.actions.elementAt(stype.vectorPosition).duration.add(new PDDLDuration(currentTriplet.object));
                           }
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_PARAMETER))){
                               assets.actions.elementAt(stype.vectorPosition).parameters.add(new PDDLParameter(currentTriplet.object));
                           }
                           break;
                       case PARAMETER:
                           if (currentTriplet.predicate.endsWith(constants.getPredicateDefinition(PDDLConstants.predicateType.HAS_NAME))){
                               assets.parameters.elementAt(stype.vectorPosition).realName = currentTriplet.object;
                           }
                           break;
                   }

                }
            }

            // Compilation of domain file

            // a. Predicates
            Iterator<PDDLPredicate> predicateIterator = assets.predicates.iterator();

            predicateData = "\t(:predicates\n";

            modelToReturn.predicateTypes = new String[assets.predicates.size()];
            int index = 0;

            // This list is used for elimination of duplicate predicates
            Vector<String> duplicatePredicates = new Vector<String>();

            while(predicateIterator.hasNext()){
                PDDLPredicate currentPredicate = predicateIterator.next();

                boolean duplicateExists = false;

                if (!currentPredicate.realname.isEmpty()){
                    if (isDuplicatePredicate(duplicatePredicates, currentPredicate.realname)){
                        duplicateExists = true;
                    }
                }
                else {
                    if (isDuplicatePredicate(duplicatePredicates, constants.removePrefixes(currentPredicate.name))){
                        duplicateExists = true;
                    }
                }


                if (!currentPredicate.realname.isEmpty()) {
                    if (!duplicateExists) predicateData += "\t\t(" + currentPredicate.realname;
                    modelToReturn.predicateTypes[index] = currentPredicate.realname;
                    duplicatePredicates.add(currentPredicate.realname);
                }
                else{
                    if (!duplicateExists) predicateData += "\t\t(" + constants.removePrefixes(currentPredicate.name);
                    modelToReturn.predicateTypes[index] = constants.removePrefixes(currentPredicate.name);
                    duplicatePredicates.add(constants.removePrefixes(currentPredicate.name));
                }

                if (currentPredicate.parameters.size() > 0){
                    if (!duplicateExists) predicateData += " ";
                }

                Iterator<String> currentPredicateParameterIterator = currentPredicate.parameters.iterator();
                int counter = 0;

                // ------
                /*if (!currentPredicate.realname.isEmpty()) {
                    System.out.println("Predicate: " + constants.removePrefixes(currentPredicate.realname));
                }
                else {
                    System.out.println("Predicate: " + currentPredicate.name);
                }*/

                // ------

                while (currentPredicateParameterIterator.hasNext()){
                    String currentParameter = constants.removePrefixes(currentPredicateParameterIterator.next());
                    //System.out.println("\tParameter: "+currentParameter);
                    if (!duplicateExists) predicateData += "?"+(currentParameter.substring(0, 1)).toLowerCase()+counter+" - "+
                            lookupParameterName(currentParameter, assets.parameters, constants);
                    if (currentPredicateParameterIterator.hasNext())
                        if (!duplicateExists) predicateData += " ";
                }

                if (!duplicateExists) predicateData += ")\n";
                index++;
            }
            predicateData += "\t)\n";

            // b. Functions
            Vector<String> declaredFunctions = new Vector<String>();
            Iterator<PDDLFunction> functionIterator = assets.functions.iterator();

            functionData = "\t(:functions\n";
            modelToReturn.functionTypes = new String[assets.functions.size()];
            index = 0;
            while(functionIterator.hasNext()) {
                PDDLFunction currentFunction = functionIterator.next();
                String functionName = "";

                if (!currentFunction.realname.isEmpty()){
                    functionName = currentFunction.realname;
                    modelToReturn.functionTypes[index] = currentFunction.realname;
                }
                else {
                    functionName = constants.removePrefixes(currentFunction.name);
                    modelToReturn.functionTypes[index] = constants.removePrefixes(currentFunction.name);
                }

                if (!isFunctionDeclared(declaredFunctions, functionName)) {

                    functionData += "\t\t(" + functionName;

                    if (currentFunction.parameters.size() > 0) {
                        functionData += " ";
                    }

                    Iterator<String> currentFunctionParameterIterator = currentFunction.parameters.iterator();
                    int counter = 0;
                    while (currentFunctionParameterIterator.hasNext()) {
                        String currentParameter = constants.removePrefixes(currentFunctionParameterIterator.next());

                        functionData += "?" + (currentParameter.substring(0, 1)).toLowerCase() + counter + " - " +
                                lookupParameterName(currentParameter, assets.parameters, constants);
                        if (currentFunctionParameterIterator.hasNext())
                            functionData += " ";
                    }

                    functionData += ")\n";

                    declaredFunctions.add(functionName);
                }
                index++;
            }
            functionData += "\t)\n";
            Vector<PDDLInstance> instanceBank = new Vector<PDDLInstance>();
            // c. Actions
            Iterator<PDDLAction> actionIterator = assets.actions.iterator();

            while(actionIterator.hasNext()){
                PDDLAction currentAction = actionIterator.next();

                actionData += "\t(:";

                if (currentAction.isDurativeAction){
                    actionData += "durative-";
                }

                actionData += "action "+constants.removePrefixes(currentAction.name)+"\n";
                actionData += "\t\t:parameters (";

                // Clear instance bank to prepare for storing instances of the current action ...
                instanceBank.clear();

                for (int i = 0; i < currentAction.parameters.size(); i++){
                    PDDLParameter param = currentAction.parameters.elementAt(i);
                    String currentParameter = constants.removePrefixes(param.name);
                    actionData += "?" + (currentParameter.substring(0, 1)).toLowerCase() + i + " - " +
                            lookupParameterName(currentParameter, assets.parameters, constants);
                    if (i < currentAction.parameters.size() - 1)
                        actionData += " ";
                    instanceBank.add(new PDDLInstance("?" + (currentParameter.substring(0, 1)).toLowerCase() + i,
                            lookupParameterName(currentParameter, assets.parameters, constants)));
                }

                actionData += ")\n\t\t:duration (= ?duration ";

                // there is only 1 duration, perhaps we can reconsider vector in next release of generator
                for (int i = 0; i < currentAction.duration.size(); i++){
                    PDDLDuration duration = currentAction.duration.elementAt(i);
                    actionData += lookupDurationParameter(duration.name, assets.durations, assets.functions, constants,
                            assets.parameters, instanceBank);
                }

                actionData += ")\n\t\t:condition (";

                if (currentAction.condition.elementAt(0).conditionalOperator.equalsIgnoreCase("AND")){
                    actionData += "and";
                }
                else
                    actionData += currentAction.condition.elementAt(0).conditionalOperator;


                for (int i = 0; i < currentAction.condition.size(); i++){
                    PDDLCondition currentCondition = currentAction.condition.elementAt(i);
                    String conditionName = currentCondition.name;

                    for (int k = 0; k < assets.conditions.size(); k++){
                        if (conditionName.compareTo(assets.conditions.elementAt(k).name) == 0){
                            // First lets get the conditional operator ....
                            String conditionalOperator = assets.conditions.elementAt(k).conditionalOperator;
                            if (conditionalOperator.equalsIgnoreCase("AND")) actionData += "and\n";
                            else if (conditionalOperator.equalsIgnoreCase("OR")) actionData += "or\n";
                            else actionData += conditionalOperator+"\n";
                            // And now the conditions one by one
                            actionData += getConditionalExpressions(
                                    assets.conditions.elementAt(k).conditional_expressions,
                                    assets.conditionalExpressions,
                                    assets.predicates,
                                    assets.functions,
                                    assets.parameters,
                                    instanceBank,
                                    constants);
                        }
                    }

                }

                actionData += "\t\t:effect (";
                PDDLEffect currentEffect = new PDDLEffect("");
                for (int i = 0; i < assets.effects.size(); i++){
                    if(assets.effects.elementAt(i).name.compareTo(currentAction.effect.elementAt(0).name)==0){
                        currentEffect = assets.effects.elementAt(i);
                       // System.out.println(currentEffect.conditionalOperator);

                        String conditionalOperator;
                        if(currentEffect.conditionalOperator.indexOf("^") > 0)
                            conditionalOperator = currentEffect.conditionalOperator.substring(0,
                                currentEffect.conditionalOperator.indexOf("^"));
                        else
                            conditionalOperator = currentEffect.conditionalOperator;
                        if (conditionalOperator.equalsIgnoreCase("AND")){
                            actionData += "and";
                        }
                        else
                            actionData += conditionalOperator;
                        actionData += "\n";

                        actionData += getConditionalExpressions(
                                currentEffect.conditional_expressions,
                                assets.conditionalExpressions,
                                assets.predicates,
                                assets.functions,
                                assets.parameters,
                                instanceBank,
                                constants);
                    }
                }
                if (currentEffect.name.isEmpty()){
                    System.out.println("Warning: no effect for action "+currentAction+". " +
                            "Generated PDDL may have errors");
                }

            actionData += "\t)\n\n";

            }

            domainData = "(define (domain "+constants.getDomain()+")\n";

            requirementData = "\n\t(:requirements ";
            for (int i = 0; i < constants.getRequirements().length; i++){
                requirementData += ":"+constants.getRequirements()[i];
                if (i != (constants.getRequirements().length - 1)){
                    requirementData += " ";
                }
            }
            requirementData += ")\n\n";

            typeData = "\t(:types ";

            Vector<String> types = new Vector<String>();
            for (int k = 0; k < assets.parameters.size(); k++){
                String typename;
                if (!assets.parameters.elementAt(k).realName.isEmpty()){
                    typename = constants.removePrefixes(assets.parameters.elementAt(k).realName);
                }
                else
                    typename = constants.removePrefixes(assets.parameters.elementAt(k).name);

                Iterator<String> stringIterator = types.iterator();
                boolean hasType = false;
                while (stringIterator.hasNext()){
                    if (stringIterator.next().compareTo(typename) == 0){
                        hasType = true;
                        break;
                    }
                }
                if (!hasType){
                    types.add(typename);
                }
            }
        modelToReturn.types = new String[types.size()];
        Iterator<String> stringIterator = types.iterator();
        int counter = 0;
            while (stringIterator.hasNext()){
                String currentType = stringIterator.next();
                typeData += currentType;
                modelToReturn.types[counter] = currentType;
                if (stringIterator.hasNext())
                    typeData += " ";
                counter++;
            }
            typeData += ")\n\n";


        data = domainData + requirementData + typeData + predicateData + functionData + actionData + ")";
        modelToReturn.dataToReturn = data;

        return modelToReturn;
    }

    private boolean isDuplicatePredicate(Vector<String> predlist, String pred){
        Iterator<String> iter = predlist.iterator();
        while (iter.hasNext()){
            String currentPred = iter.next();
            if (currentPred.compareTo(pred) == 0){
                return true;
            }
        }
        return false;
    }

    private String getConditionalExpressions (Vector<PDDLConditionalExpression> expressionsToGet,
                                              Vector<PDDLConditionalExpression> expressionRepo,
                                              Vector<PDDLPredicate> predicateRepo,
                                              Vector<PDDLFunction> functionRepo,
                                              Vector<PDDLParameter> parameters,
                                              Vector<PDDLInstance> instanceBank,
                                              PDDLConstants constants){
        String data = "";

        for (int k = 0; k < expressionsToGet.size(); k++){
            //System.out.println(expressionsToGet.elementAt(k).name);
            PDDLConditionalExpression currentExpressionInRepo = expressionsToGet.elementAt(k);

            for (int i = 0; i < expressionRepo.size(); i++){

                if (currentExpressionInRepo.name.compareTo(expressionRepo.elementAt(i).name) == 0) {
                    data += "\t\t\t(" + expressionRepo.elementAt(i).timing + " (";
                    boolean hasNegation = false;
                    boolean hasFunctionArg = false;
                    if (expressionRepo.elementAt(i).hasNegation) {
                        data += "not (";
                        hasNegation = true;
                    }
                    /*if (!expressionRepo.elementAt(i).operator.isEmpty()){
                        data += expressionRepo.elementAt(i).operator + " (";
                        hasOperator = true;
                    }*/
                    Iterator<String> parameterList = expressionRepo.elementAt(i).parameters.iterator();

                    // ------ Parameter search -------

                    while (parameterList.hasNext()){
                        String currentParameter = parameterList.next();
                        Iterator<PDDLPredicate> pddlParameterIterator = predicateRepo.iterator();

                        // some conditions include predicates, search predicates first
                        boolean searchSucceeded = false;
                        while(pddlParameterIterator.hasNext()){
                            PDDLPredicate predicateInRepo = pddlParameterIterator.next();
                            if (currentParameter.compareTo(predicateInRepo.name) == 0) {

                                String name = "";
                                if (!predicateInRepo.realname.isEmpty()) {
                                    name = constants.removePrefixes(predicateInRepo.realname);
                                    data += name + " ";
                                } else {
                                    name = constants.removePrefixes(predicateInRepo.name);
                                    data +=  name + " ";
                                }
                                // Parameter resolution from instance bank
                                for (int a = 0; a < predicateInRepo.parameters.size(); a++) {
                                    String currentPredicate = constants.removePrefixes(predicateInRepo.parameters.elementAt(a));
                                    String instancePrefixName = "?" + (currentPredicate.substring(0, 1)).toLowerCase();
                                    for (int d = 0; d < instanceBank.size(); d++){
                                        PDDLInstance currentInstance = instanceBank.elementAt(d);
                                        if (currentInstance.instanceName.startsWith(instancePrefixName)){
                                            instancePrefixName = currentInstance.instanceName;
                                            break;
                                        }
                                    }
                                    data += instancePrefixName;

                                    if (a < predicateInRepo.parameters.size() -1)
                                        data += " ";
                                }
                                data += ")";
                                searchSucceeded = true;
                            }
                        }
                        // Lets have a look at functions as well in case search did not succeed in predicates ...
                        if (!searchSucceeded){
                            Iterator<PDDLFunction> functionIterator = functionRepo.iterator();
                            while (functionIterator.hasNext()){
                                PDDLFunction currentFunction = functionIterator.next();

                                boolean functionHasOperator = false;
                                boolean functionIsTotalCost = false;

                                if(currentParameter.compareTo(currentFunction.name) == 0) {
                                    //System.out.println(currentFunction.name);
                                    if (!currentFunction.realname.isEmpty()) {
                                        if (currentFunction.operator.isEmpty()) {
                                            data += constants.removePrefixes(currentFunction.realname);
                                            data += " ";
                                        } else {
                                            data += currentFunction.operator + " (";
                                            data += constants.removePrefixes(currentFunction.realname);
                                            functionHasOperator = true;
                                        }
                                    } else {
                                        if (currentFunction.operator.isEmpty()) {
                                            data += constants.removePrefixes(currentFunction.name);
                                            data += " ";
                                        } else {
                                            data += currentFunction.operator + " (";
                                            data += constants.removePrefixes(currentFunction.name);
                                            functionHasOperator = true;
                                        }
                                    }

                                    if (functionHasOperator && currentFunction.parameters.size() > 0) data += " ";

                                    // Parameter resolution from instance bank
                                    for (int a = 0; a < currentFunction.parameters.size(); a++) {
                                        String currentPredicate = constants.removePrefixes(currentFunction.parameters.elementAt(a));
                                        String instancePrefixName = "?" + (currentPredicate.substring(0, 1)).toLowerCase();
                                        for (int d = 0; d < instanceBank.size(); d++) {
                                            PDDLInstance currentInstance = instanceBank.elementAt(d);
                                            if (currentInstance.instanceName.startsWith(instancePrefixName)) {
                                                instancePrefixName = currentInstance.instanceName;
                                                break;
                                            }
                                        }
                                        data += instancePrefixName;
                                        if (a < currentFunction.parameters.size() - 1)
                                            data += " ";
                                    }
                                    data += ")";

                                    // for functions, some special handling is required in case there is value

                                    if (!currentFunction.value.isEmpty() && currentFunction.value.indexOf("^") > 0)
                                        data += " " + currentFunction.value.substring(0,
                                                currentFunction.value.indexOf("^"));
                                    /*else {
                                        data += " " + currentFunction.value;
                                    }*/
                                    if (functionHasOperator && !currentFunction.value.isEmpty())
                                        data += ")";
                                    else if (functionHasOperator && currentFunction.value.isEmpty()){
                                        hasFunctionArg = true;
                                        data += " (";
                                    }
                                }
                            }
                        }
                    }
                    // ---- end parameter search ------

                    if(hasNegation) data += ")";
                    if(hasFunctionArg) data += ")";
                    data += ")";

                }
            }


            data += "\n";
        }
        data += "\t\t)\n";

        return data;
    }

    private String lookupDurationParameter (String durationName, Vector<PDDLDuration> duration,
                                            Vector<PDDLFunction> functions, PDDLConstants constants,
                                            Vector<PDDLParameter> parameters, Vector<PDDLInstance> instanceBank){

        Iterator<PDDLDuration> durationIterator = duration.iterator();
        while (durationIterator.hasNext()){
            PDDLDuration currentDuration = durationIterator.next();
            if (currentDuration.name.compareTo(durationName) == 0) {
                if (currentDuration.parameters.elementAt(0).contains("XMLSchema#integer") ||
                        currentDuration.parameters.elementAt(0).contains("XMLSchema#nonNegativeInteger") ||
                        currentDuration.parameters.elementAt(0).contains("XMLSchema#positiveInteger")) {
                    return currentDuration.parameters.elementAt(0).substring(0,
                            currentDuration.parameters.elementAt(0).indexOf("^"));
                } else {
                    String functionName = currentDuration.parameters.elementAt(0);
                    for (int i = 0; i < functions.size(); i++) {
                        PDDLFunction currentFunction = functions.elementAt(i);
                        if (currentFunction.name.compareTo(functionName) == 0){
                            Iterator<String> currentFunctionParameterIterator = currentFunction.parameters.iterator();
                            int counter = 0;
                            String functionData = "("+constants.removePrefixes(functionName)+" ";
                            while (currentFunctionParameterIterator.hasNext()) {

                                String currentParameter = currentFunctionParameterIterator.next();
                                if (!(currentParameter.compareTo("total-cost") == 0)) {
                                    currentParameter = constants.removePrefixes(
                                            currentParameter);

                                    // look into instance bank to find the name
                                    String instancePrefixName = "?" + (currentParameter.substring(0, 1)).toLowerCase();
                                    for (int k = 0; k < instanceBank.size(); k++) {
                                        PDDLInstance currentInstance = instanceBank.elementAt(k);
                                        if (currentInstance.instanceName.contains(instancePrefixName) &&
                                                currentInstance.instanceType.compareTo(
                                                        lookupParameterName(currentParameter, parameters, constants)) == 0) {
                                            // found instance in instance bank
                                            functionData += currentInstance.instanceName;
                                            break;
                                        }
                                    }
                                }

                                if (currentFunctionParameterIterator.hasNext())
                                    functionData += " ";
                            }
                            functionData += ")";
                            return functionData;
                        }
                    }
                }
            }
        }

        return "";
    }

    private boolean isFunctionDeclared (Vector<String> declaredFunctions, String fname){
        Iterator<String> declaredFunctionsIterator = declaredFunctions.iterator();
        while(declaredFunctionsIterator.hasNext()){
            String currentFunctionName = declaredFunctionsIterator.next();
            if (currentFunctionName.compareTo(fname) == 0){
                return true;
            }
        }
        return false;
    }

    private String lookupParameterName(String name, Vector<PDDLParameter> pddlParameters, PDDLConstants util){

        Iterator<PDDLParameter> pddlParameterIterator = pddlParameters.iterator();
        while(pddlParameterIterator.hasNext()){
            PDDLParameter currentParameter = pddlParameterIterator.next();

            if (util.removePrefixes(currentParameter.name).compareTo(name) == 0){
                if (!currentParameter.realName.isEmpty()){
                    return currentParameter.realName;
                }
                else
                    return util.removePrefixes(currentParameter.name);
            }

        }
        return "";
    }

    private SubjectType getSubjectType(
            String subject,
            PDDLAssets assets
            ){

        Iterator<PDDLDuration> durationIterator = assets.durations.iterator();
        Iterator<PDDLPredicate> predicateIterator = assets.predicates.iterator();
        Iterator<PDDLConditionalExpression> conditionalExpressionIterator = assets.conditionalExpressions.iterator();
        Iterator<PDDLEffect> effectIterator = assets.effects.iterator();
        Iterator<PDDLCondition> conditionIterator = assets.conditions.iterator();
        Iterator<PDDLAction> actionIterator = assets.actions.iterator();
        Iterator<PDDLFunction> functionIterator = assets.functions.iterator();
        Iterator<PDDLParameter> parameterIterator = assets.parameters.iterator();

        int counter = 0;
        while(durationIterator.hasNext()){
            if (subject.compareTo(durationIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.DURATION, counter);
            }
            counter++;
        }
        counter = 0;
        while(actionIterator.hasNext()){
            if (subject.compareTo(actionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.ACTION, counter);
            }
            counter++;
        }
        counter = 0;
        while(conditionIterator.hasNext()){
            if (subject.compareTo(conditionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.CONDITION, counter);
            }
            counter++;
        }
        counter = 0;
        while(effectIterator.hasNext()){
            if (subject.compareTo(effectIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.EFFECT, counter);
            }
            counter++;
        }
        counter = 0;
        while(functionIterator.hasNext()){
            if (subject.compareTo(functionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.FUNCTION, counter);
            }
            counter++;
        }
        counter = 0;
        while(conditionalExpressionIterator.hasNext()){
            if (subject.compareTo(conditionalExpressionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.CONDITIONAL_EXPRESSION, counter);
            }
            counter++;
        }
        counter = 0;
        while(predicateIterator.hasNext()){
            if (subject.compareTo(predicateIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.PREDICATE, counter);
            }
            counter++;
        }
        counter = 0;
        while(parameterIterator.hasNext()){
            if (subject.compareTo(parameterIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.PARAMETER, counter);
            }
            counter++;
        }
        //Its ok to suppress the below warning, some things in the model are never used by this generator
        //System.out.println("Warning: could not determine subject type for "+subject);

        return null;
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