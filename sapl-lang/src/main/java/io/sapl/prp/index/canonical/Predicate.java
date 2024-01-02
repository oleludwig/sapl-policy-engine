// Generated by delombok at Tue Jan 02 01:24:37 CET 2024
/*
 * Copyright (C) 2017-2023 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * SPDX-License-Identifier: Apache-2.0
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
package io.sapl.prp.index.canonical;

import com.google.common.base.Preconditions;
import io.sapl.api.interpreter.Val;
import reactor.core.publisher.Mono;

public class Predicate {
    private final Bool    bool;
    private final Bitmask conjunctions           = new Bitmask();
    private final Bitmask falseForTruePredicate  = new Bitmask();
    private final Bitmask falseForFalsePredicate = new Bitmask();

    public Predicate(final Bool bool) {
        this.bool = Preconditions.checkNotNull(bool);
    }

    public Mono<Val> evaluate() {
        return getBool().evaluateExpression();
    }

    @java.lang.SuppressWarnings("all")
    // @lombok.Generated
    public Bool getBool() {
        return this.bool;
    }

    @java.lang.SuppressWarnings("all")
    // @lombok.Generated
    public Bitmask getConjunctions() {
        return this.conjunctions;
    }

    @java.lang.SuppressWarnings("all")
    // @lombok.Generated
    public Bitmask getFalseForTruePredicate() {
        return this.falseForTruePredicate;
    }

    @java.lang.SuppressWarnings("all")
    // @lombok.Generated
    public Bitmask getFalseForFalsePredicate() {
        return this.falseForFalsePredicate;
    }
}
