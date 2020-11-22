/**
 * Copyright © 2020 Dominic Heutelbeck (dominic@heutelbeck.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.reimpl.prp.index.canonical;

import io.sapl.api.interpreter.Val;
import io.sapl.api.prp.PolicyRetrievalResult;
import io.sapl.grammar.sapl.SAPL;
import io.sapl.interpreter.EvaluationContext;
import io.sapl.prp.inmemory.indexed.Bitmask;
import io.sapl.prp.inmemory.indexed.DisjunctiveFormula;
import io.sapl.prp.inmemory.indexed.improved.CTuple;
import io.sapl.prp.inmemory.indexed.improved.Predicate;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class CanonicalIndexAlgorithm {


    public Mono<PolicyRetrievalResult> match(EvaluationContext subscriptionScopedEvaluationContext,
                                             CanonicalIndexDataContainer dataContainer) {

        Mono<CanonicalIndexMatchingContext> contextMono = Mono
                .just(new CanonicalIndexMatchingContext(dataContainer, subscriptionScopedEvaluationContext));

        for (Predicate predicate : dataContainer.getPredicateOrder()) {
            contextMono = contextMono.flatMap(matchingCtx -> accumulate(matchingCtx, predicate, dataContainer));
        }

        return contextMono.map(matchingCtx -> {
            var matching = matchingCtx.getMatchingCandidatesMask();
            var formulas = fetchFormulas(matching, dataContainer.getRelatedFormulas());
            var policies = fetchPolicies(formulas, dataContainer.getFormulaToDocuments());
            return new PolicyRetrievalResult(policies, matchingCtx.isErrorsInTargets());
        }).onErrorReturn(new PolicyRetrievalResult(Collections.emptyList(), true));
    }

    private static Mono<CanonicalIndexMatchingContext> accumulate(CanonicalIndexMatchingContext matchingCtx,
                                                                  Predicate predicate, CanonicalIndexDataContainer dataContainer) {
        if (!isReferenced(predicate, matchingCtx.getClauseCandidatesMask()))
            return Mono.just(matchingCtx);

        return predicate.evaluate(matchingCtx.getSubscriptionScopedEvaluationContext())
                .map(handleEvaluationResult(matchingCtx, predicate, dataContainer));
    }

    private static Function<Val, CanonicalIndexMatchingContext> handleEvaluationResult(
            CanonicalIndexMatchingContext matchingCtx, Predicate predicate, CanonicalIndexDataContainer dataContainer) {
        return evaluationResult -> {
            if (evaluationResult.isError()) {
                matchingCtx.setErrorsInTargets(true);
                removeCandidatesRelatedToPredicate(predicate, matchingCtx.getClauseCandidatesMask());
            } else {
                updateMatchingContext(predicate, evaluationResult.getBoolean(), matchingCtx, dataContainer);
            }
            return matchingCtx;
        };
    }

    Bitmask orBitMask(@NonNull Bitmask b1, @NonNull Bitmask b2) {
        Bitmask result = new Bitmask(b1);
        result.or(b2);
        return result;
    }

    private void updateMatchingContext(Predicate predicate, Boolean evaluationResult,
                                       CanonicalIndexMatchingContext matchingCtx, CanonicalIndexDataContainer dataContainer) {
        Bitmask candidates = matchingCtx.getClauseCandidatesMask();

        Bitmask satisfiedCandidates = findSatisfiableCandidates(candidates, predicate, evaluationResult,
                matchingCtx.getTrueLiteralsOfConjunction(), dataContainer.getNumberOfLiteralsInConjunction());
        //add satisfied candidates to mask of matching candidates
        matchingCtx.getMatchingCandidatesMask().or(satisfiedCandidates);

        Bitmask unsatisfiedCandidates = findUnsatisfiableCandidates(candidates, predicate, evaluationResult);

        Bitmask orphanedCandidates = findOrphanedCandidates(candidates, satisfiedCandidates,
                matchingCtx.getEliminatedFormulasWithConjunction(),
                dataContainer.getConjunctionsInFormulasReferencingConjunction(),
                dataContainer.getNumberOfFormulasWithConjunction());

        reduceCandidates(candidates, unsatisfiedCandidates, satisfiedCandidates, orphanedCandidates);
    }

    void removeCandidatesRelatedToPredicate(final Predicate predicate, Bitmask candidates) {
        // Bitmask affectedCandidates = findRelatedCandidates(predicate);
        Bitmask affectedCandidates = predicate.getConjunctions();
        candidates.andNot(affectedCandidates);
    }

    Bitmask findOrphanedCandidates(final Bitmask candidates, final Bitmask satisfiableCandidates,
                                   int[] eliminatedFormulasWithConjunction,
                                   Map<Integer, Set<CTuple>> conjunctionsInFormulasReferencingConjunction,
                                   int[] numberOfFormulasWithConjunction) {
        Bitmask result = new Bitmask();

        satisfiableCandidates.forEachSetBit(index -> {
            Set<CTuple> cTuples = conjunctionsInFormulasReferencingConjunction.get(index);
            for (CTuple cTuple : cTuples) {
                if (!candidates.isSet(cTuple.getCI()))
                    continue;
                eliminatedFormulasWithConjunction[cTuple.getCI()] += cTuple.getN();

                // if all formular of conjunction have been eliminated
                if (eliminatedFormulasWithConjunction[cTuple.getCI()] == numberOfFormulasWithConjunction[cTuple
                        .getCI()]) {
                    result.set(cTuple.getCI());
                }
            }
        });

        return result;
    }

    void reduceCandidates(final Bitmask candidates, final Bitmask unsatisfiedCandidates,
                          final Bitmask satisfiedCandidates, final Bitmask orphanedCandidates) {
        candidates.andNot(unsatisfiedCandidates);
        candidates.andNot(satisfiedCandidates);
        candidates.andNot(orphanedCandidates);
    }

    Set<DisjunctiveFormula> fetchFormulas(final Bitmask satisfiableCandidates,
                                          List<Set<DisjunctiveFormula>> relatedFormulas) {
        final Set<DisjunctiveFormula> result = new HashSet<>();
        satisfiableCandidates.forEachSetBit(index -> result.addAll(relatedFormulas.get(index)));
        return result;
    }

    Bitmask findSatisfiableCandidates(final Bitmask candidates, final Predicate predicate,
                                      final boolean evaluationResult, final int[] trueLiteralsOfConjunction,
                                      int[] numberOfLiteralsInConjunction) {
        Bitmask result = new Bitmask();
        // calling method with negated evaluation result will return satisfied clauses
        Bitmask satisfiableCandidates = findUnsatisfiableCandidates(candidates, predicate, !evaluationResult);

        satisfiableCandidates.forEachSetBit(index -> {
            // increment number of true literals
            trueLiteralsOfConjunction[index] += 1;
            // if all literals in conjunction are true, add conjunction to result
            if (trueLiteralsOfConjunction[index] == numberOfLiteralsInConjunction[index])
                result.set(index);
        });

        return result;
    }

    boolean isReferenced(final Predicate predicate, final Bitmask candidates) {
        return predicate.getConjunctions().intersects(candidates);
    }

    private Set<SAPL> fetchPolicies(final Set<DisjunctiveFormula> formulas,
                                    Map<DisjunctiveFormula, Set<SAPL>> formulaToDocuments) {
        return formulas.parallelStream().map(formulaToDocuments::get).flatMap(Collection::parallelStream)
                .collect(Collectors.toSet());
    }

    Bitmask findUnsatisfiableCandidates(final Bitmask candidates, final Predicate predicate,
                                        final boolean predicateEvaluationResult) {
        Bitmask result = new Bitmask(candidates);
        if (predicateEvaluationResult) {
            result.and(predicate.getFalseForTruePredicate());
        } else {
            result.and(predicate.getFalseForFalsePredicate());
        }
        return result;
    }

}
