/*
 * Copyright (C) 2017-2023 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.grammar.sapl.impl;

import io.sapl.api.interpreter.Val;
import io.sapl.grammar.sapl.FilterStatement;
import io.sapl.grammar.sapl.WildcardStep;
import io.sapl.grammar.sapl.impl.util.FilterAlgorithmUtil;
import io.sapl.grammar.sapl.impl.util.StepAlgorithmUtil;
import lombok.NonNull;
import reactor.core.publisher.Flux;

/**
 * Implements the application of a wildcard step to a previous value, e.g
 * 'value.*'.
 * <p>
 * Grammar: Step: '.' ({WildcardStep} '*') ;
 */
public class WildcardStepImplCustom extends WildcardStepImpl {

    @Override
    public Flux<Val> apply(@NonNull Val parentValue) {
        return StepAlgorithmUtil.apply(parentValue, WildcardStepImplCustom::wildcard, "*", WildcardStep.class);
    }

    @Override
    public Flux<Val> applyFilterStatement(@NonNull Val unfilteredValue, int stepId,
            @NonNull FilterStatement statement) {
        return FilterAlgorithmUtil.applyFilter(unfilteredValue, stepId, WildcardStepImplCustom::wildcard, statement,
                WildcardStep.class);
    }

    public static Flux<Val> wildcard() {
        return Flux.just(Val.TRUE.withTrace(WildcardStep.class));
    }

}
