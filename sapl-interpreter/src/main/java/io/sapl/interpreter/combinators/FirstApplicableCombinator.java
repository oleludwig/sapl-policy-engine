package io.sapl.interpreter.combinators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.api.interpreter.SAPLInterpreter;
import io.sapl.api.pdp.Decision;
import io.sapl.api.pdp.Request;
import io.sapl.api.pdp.Response;
import io.sapl.grammar.sapl.Policy;
import io.sapl.interpreter.functions.FunctionContext;
import io.sapl.interpreter.pip.AttributeContext;
import reactor.core.publisher.Flux;

public class FirstApplicableCombinator implements PolicyCombinator {

	private SAPLInterpreter interpreter;

	public FirstApplicableCombinator(SAPLInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public Flux<Response> combinePolicies(List<Policy> policies, Request request, AttributeContext attributeCtx,
										  FunctionContext functionCtx, Map<String, JsonNode> systemVariables, Map<String, JsonNode> variables,
										  Map<String, String> imports) {
		List<Policy> matchingPolicies = new ArrayList<>();
		for (Policy policy : policies) {
			try {
				if (interpreter.matches(request, policy, functionCtx, systemVariables, variables, imports)) {
					matchingPolicies.add(policy);
				}
			} catch (PolicyEvaluationException e) {
				return Flux.just(Response.indeterminate());
			}
		}

        if (matchingPolicies.isEmpty()) {
            return Flux.just(Response.notApplicable());
        }

		final List<Flux<Response>> responseFluxes = new ArrayList<>(matchingPolicies.size());
		for (Policy policy : matchingPolicies) {
			responseFluxes.add(Flux.just(interpreter.evaluateRules(request, policy, attributeCtx, functionCtx,
					systemVariables, variables, imports)));
		}
		return Flux.combineLatest(responseFluxes, responses -> {
			for (Object response : responses) {
				final Response resp = (Response) response;
				if (resp.getDecision() != Decision.NOT_APPLICABLE) {
					return resp;
				}
			}
			return Response.notApplicable();
		}).distinctUntilChanged();
	}

}
