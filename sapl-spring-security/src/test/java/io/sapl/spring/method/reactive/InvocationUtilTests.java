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
package io.sapl.spring.method.reactive;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

class InvocationUtilTests {

	@Test
	void sneakyThrows() throws Throwable {
		var mock = mock(MethodInvocation.class);
		when(mock.proceed()).thenThrow(new IOException());
		assertThrows(IOException.class, () -> InvocationUtil.proceed(mock));
	}

}