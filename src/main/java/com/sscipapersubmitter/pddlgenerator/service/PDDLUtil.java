package com.sscipapersubmitter.pddlgenerator.service;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by eathkar on 18/08/16.
 */

// Some Utility Methods
public class PDDLUtil {
    public void addPredicateSetContent (PDDLAssets assets){
        // Adding predicates to predicate sets
        Iterator<PDDLSet> predicateSetIterator = assets.predicateSets.iterator();
        int predicateSetIteratorIndex = 0;
        while(predicateSetIterator.hasNext()){
            PDDLSet currentPredicateSet = predicateSetIterator.next();
            Iterator<PDDLPredicate> predicateIterator = currentPredicateSet.predicates.iterator();
            int currentPredicateIndex = 0;
            while (predicateIterator.hasNext()){
                // This is the predicate to be replaced in predicate Assets (index predicateSetIteratorIndex)
                PDDLPredicate currentPredicate = predicateIterator.next();
                // Lets find the index in the predicates array in assets ...
                int index = entityHasPredicate(currentPredicate.name, assets.predicates);
                // Now lets replace the index
                if (index >= 0){
                    PDDLPredicate predicateToReplaceWith = assets.predicates.elementAt(index);
                    assets.predicateSets.elementAt(predicateSetIteratorIndex).predicates.set(currentPredicateIndex, predicateToReplaceWith);
                }
                currentPredicateIndex++;
            }
            predicateSetIteratorIndex++;
        }

    }

    public void addActionAndPreconditionContent(PDDLAssets assets){
        // Lets add predicateSets to actions, preconditions, TODO for computations
        // First, actions
        Iterator<PDDLAction> actionIterator = assets.actions.iterator();
        int actionIteratorIndex = 0;
        while(actionIterator.hasNext()){
            PDDLAction currentAction = actionIterator.next();
            if(currentAction.predicateSet != null){
                // Lookup the index of predicateSet name in current action at actions.predicateSets
                int psIndex = entityHasPredicateSet(
                        currentAction.predicateSet.name,
                        assets.predicateSets);
                if (psIndex >= 0){
                    assets.actions.elementAt(actionIteratorIndex).predicateSet
                            = assets.predicateSets.elementAt(psIndex);
                }
            }
            actionIteratorIndex++;
        }
        Iterator<PDDLPrecondition> preconditionIterator = assets.preconditions.iterator();
        int preconditionIteratorIndex = 0;
        while(preconditionIterator.hasNext()){
            PDDLPrecondition currentPrecondition = preconditionIterator.next();
            if(currentPrecondition.predicateSet != null){
                // Lookup the index of predicateSet name in current action at actions.predicateSets
                int psIndex = entityHasPredicateSet(
                        currentPrecondition.predicateSet.name,
                        assets.predicateSets);
                if (psIndex >= 0){
                    assets.preconditions.elementAt(preconditionIteratorIndex).predicateSet
                            = assets.predicateSets.elementAt(psIndex);
                }
            }
            preconditionIteratorIndex++;
        }
    }

    public void addTransitionContent(PDDLAssets assets){

        // Lets add actions and preconditions to transitions, TODO for computations
        Iterator<PDDLTransition> transitionIterator = assets.transitions.iterator();
        int assetsTransitionIndex = 0;
        while(transitionIterator.hasNext()){
            PDDLTransition currentTransition = transitionIterator.next();
            if (currentTransition.precondition != null){
                // Lookup the index of precondition name in current transition is the same as in assets.preconditions
                int precIndex = entityHasPrecondition(
                        currentTransition.precondition.name,
                        assets.preconditions);
                if (precIndex >= 0){
                    assets.transitions.elementAt(assetsTransitionIndex).precondition
                            = assets.preconditions.elementAt(precIndex);
                }
            }
            if (currentTransition.action != null) {
                int actIndex = entityHasAction(
                        currentTransition.action.name,
                        assets.actions);
                if (actIndex >= 0){
                    assets.transitions.elementAt(assetsTransitionIndex).action
                            = assets.actions.elementAt(actIndex);
                }
            }
            if (currentTransition.computation != null){
                //TODO: Fix this!
            }
            assetsTransitionIndex++;
        }
    }

    public void printTransitionModel(PDDLAssets assets){
        Iterator<PDDLTransition> iter2 = assets.transitions.iterator();
        while (iter2.hasNext()){
            PDDLTransition currentTR = iter2.next();
            System.out.println("Transition: "+currentTR.name);
            if (currentTR.computation != null) { // some may not have
                System.out.println("\tComputation " + currentTR.computation.name);
                // TODO: Print rest of stuff !
            }
            if (currentTR.action != null){
                if(currentTR.action.predicateSet != null){
                    System.out.println("\tAction "+currentTR.action.name +
                            "\n\t\t" + "Predicate Set: "+currentTR.action.predicateSet.name +
                            "\n\t\t" + "Predicate Set Operator: "+currentTR.action.predicateSetOperator);
                    if (currentTR.action.predicateSet.predicates != null){
                        Iterator<PDDLPredicate> predicates = currentTR.action.predicateSet.predicates.iterator();
                        while (predicates.hasNext()){
                            PDDLPredicate currentPredicate = predicates.next();
                            System.out.println("\t\t\t" + "Predicate: "+currentPredicate.name);
                            for (int i = 0; i < currentPredicate.parameters.size(); i++){
                                System.out.println("\tParameter:"+currentPredicate.parameters.elementAt(i));
                            }
                            if (!currentPredicate.encapsulatingMethod.isEmpty()){
                                System.out.println("\tMethod:"+currentPredicate.encapsulatingMethod);
                            }
                        }
                    }
                }
            }
            if (currentTR.precondition != null) {
                if (currentTR.precondition.predicateSet != null) {
                    System.out.println("\tPrecondition " + currentTR.precondition.name +
                            "\n\t\t" + "Predicate Set: " + currentTR.precondition.predicateSet.name +
                            "\n\t\t" + "Predicate Set Operator: " + currentTR.precondition.predicateSetOperator);
                }
                if (currentTR.precondition.predicateSet.predicates != null){
                    Iterator<PDDLPredicate> predicates = currentTR.precondition.predicateSet.predicates.iterator();
                    while (predicates.hasNext()){
                        PDDLPredicate currentPredicate = predicates.next();
                        System.out.println("\t\t\t" + "Predicate: "+currentPredicate.name);
                        for (int i = 0; i < currentPredicate.parameters.size(); i++){
                            System.out.println("\t\t\t\tParameter:"+currentPredicate.parameters.elementAt(i));
                        }
                        if (!currentPredicate.encapsulatingMethod.isEmpty()){
                            System.out.println("\t\t\t\tMethod:"+currentPredicate.encapsulatingMethod);
                        }
                    }
                }
            }
        }
    }

    public SubjectType getSubjectType(
            String subject,
            PDDLAssets assets
    ){


        Iterator<PDDLPrecondition> preconditionIterator = assets.preconditions.iterator();
        Iterator<PDDLAction> actionIterator = assets.actions.iterator();
        Iterator<PDDLTransition> transitionIterator = assets.transitions.iterator();
        Iterator<PDDLSet> predicateSetIterator = assets.predicateSets.iterator();
        Iterator<PDDLPredicate> predicateIterator = assets.predicates.iterator();

        int counter = 0;
        while(actionIterator.hasNext()){
            if (subject.compareTo(actionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.ACTION, counter);
            }
            counter++;
        }
        counter = 0;
        while(transitionIterator.hasNext()){
            if (subject.compareTo(transitionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.TRANSITION, counter);
            }
            counter++;
        }
        counter = 0;
        while(preconditionIterator.hasNext()){
            if (subject.compareTo(preconditionIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.PRECONDITION, counter);
            }
            counter++;
        }
        counter = 0;
        while(predicateSetIterator.hasNext()){
            if (subject.compareTo(predicateSetIterator.next().name) == 0){
                return new SubjectType(PDDLConstants.objectType.PREDICATE_SET, counter);
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
        //Its ok to suppress the below warning, some things in the model are never used by this generator
        //System.out.println("Warning: could not determine subject type for "+subject);

        return null;
    }

    private int entityHasPredicate(String entityName, Vector<PDDLPredicate> predicates){
        for (int i = 0; i < predicates.size(); i++){
            if (predicates.elementAt(i).name.compareTo(entityName) == 0){
                return i;
            }
        }
        return -1;
    }

    private int entityHasPredicateSet(String entityName, Vector<PDDLSet> predicateSets){
        for (int i = 0; i < predicateSets.size(); i++){
            if (predicateSets.elementAt(i).name.compareTo(entityName) == 0){
                return i;
            }
        }
        return -1;
    }

    private int entityHasPrecondition(String entityName, Vector<PDDLPrecondition> preconditions){
        for (int i = 0; i < preconditions.size(); i++){
            if (preconditions.elementAt(i).name.compareTo(entityName) == 0){
                return i;
            }
        }
        return -1;
    }

    private int entityHasAction(String entityName, Vector<PDDLAction> actions){
        for (int i = 0; i < actions.size(); i++){
            if (actions.elementAt(i).name.compareTo(entityName) == 0){
                return i;
            }
        }
        return -1;
    }
}
