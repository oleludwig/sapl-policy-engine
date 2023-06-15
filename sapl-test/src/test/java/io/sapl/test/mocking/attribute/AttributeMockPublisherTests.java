/*
 * Copyright © 2017-2022 Dominic Heutelbeck (dominic@heutelbeck.com)
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
package io.sapl.test.mocking.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.sapl.api.interpreter.Val;
import reactor.test.StepVerifier;

class AttributeMockPublisherTests {

	@Test
	void test() {
		var mock = new AttributeMockPublisher("foo.bar");

		StepVerifier.create(mock.evaluate("test.attribute", null, null, null)).then(() -> mock.mockEmit(Val.of(1)))
				.expectNext(Val.of(1)).thenCancel().verify();

		mock.assertVerifications();
	}

	@Test
	void test_errorMessage() {
		var mock = new AttributeMockPublisher("test.test");
		assertThat(mock.getErrorMessageForCurrentMode()).isNotEmpty();
	}

}