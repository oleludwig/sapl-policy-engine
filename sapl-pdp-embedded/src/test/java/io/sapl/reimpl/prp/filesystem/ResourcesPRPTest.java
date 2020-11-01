package io.sapl.reimpl.prp.filesystem;

import java.util.HashMap;
import java.util.logging.Level;

import org.junit.Test;

import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.interpreter.DefaultSAPLInterpreter;
import io.sapl.interpreter.functions.AnnotationFunctionContext;
import io.sapl.prp.resources.ResourcesPrpUpdateEventSource;
import io.sapl.reimpl.prp.GenericInMemoryIndexedPolicyRetrievalPoint;
import io.sapl.reimpl.prp.index.naive.NaiveImmutableParsedDocumentIndex;
import reactor.core.publisher.SignalType;

public class ResourcesPRPTest {
	@Test
	public void doTest() {
		var interpreter = new DefaultSAPLInterpreter();
		var source = new ResourcesPrpUpdateEventSource("/policies", interpreter);
		var prp = new GenericInMemoryIndexedPolicyRetrievalPoint(new NaiveImmutableParsedDocumentIndex(), source);
		var authzSubscription = AuthorizationSubscription.of("Willi", "write", "icecream");
		prp.retrievePolicies(authzSubscription, new AnnotationFunctionContext(), new HashMap<>())
				.log(null, Level.INFO, SignalType.ON_NEXT).blockFirst();
		prp.dispose();
	}
}