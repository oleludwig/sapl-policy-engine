/*
 * SAPLTest generated by Xtext
 */
package io.sapl.test.grammar.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.sapl.test.grammar.SAPLTestRuntimeModule;
import io.sapl.test.grammar.SAPLTestStandaloneSetup;
import io.sapl.test.grammar.ide.SAPLTestIdeModule;
import org.eclipse.xtext.util.Modules2;

/**
 * Initialization support for running Xtext languages in web applications.
 */
public class SAPLTestWebSetup extends SAPLTestStandaloneSetup {

    @Override
    public Injector createInjector() {
        return Guice.createInjector(
                Modules2.mixin(new SAPLTestRuntimeModule(), new SAPLTestIdeModule(), new SAPLTestWebModule()));
    }

}
