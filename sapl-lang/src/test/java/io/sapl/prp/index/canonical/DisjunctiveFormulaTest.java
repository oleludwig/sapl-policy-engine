package io.sapl.prp.index.canonical;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import lombok.val;

class DisjunctiveFormulaTest {

    @Test
    void should_throw_illegal_argument_exception_when_initialized_with_empty_collection() {
        assertThrows(IllegalArgumentException.class, () -> new DisjunctiveFormula(Collections.emptyList()));
    }

    @Test
    void testImmutable() {
        val immutableClause = new ConjunctiveClause(new Literal(new Bool(true)));
        val immutableFormula = new DisjunctiveFormula(immutableClause);

        assertThat(immutableClause.isImmutable(), is(true));
        assertThat(immutableFormula.size(), is(1));
        assertThat(immutableFormula.isImmutable(), is(true));
    }

    @Test
    void testEvaluate() {

        val trueClause = new ConjunctiveClause(new Literal(new Bool(true)));
        val falseClause = new ConjunctiveClause(new Literal(new Bool(false)));
        val f1 = new DisjunctiveFormula(trueClause, falseClause);
        val f2 = new DisjunctiveFormula(falseClause, trueClause);
        val f3 = new DisjunctiveFormula(falseClause);

        assertThat(f1.evaluate(), is(true));
        assertThat(f2.evaluate(), is(true));
        assertThat(f3.evaluate(), is(false));
    }

    @Test
    void testEquals() {
        val trueClause = new ConjunctiveClause(new Literal(new Bool(true)));
        val falseClause = new ConjunctiveClause(new Literal(new Bool(false)));
        val f1 = new DisjunctiveFormula(trueClause, falseClause);
        val f2 = new DisjunctiveFormula(falseClause, trueClause);
        val f3 = new DisjunctiveFormula(falseClause);
        val f4 = new DisjunctiveFormula(falseClause,falseClause);

        assertThat(f1.equals(f1), is(true));
        assertThat(f1.equals(f2), is(true));
        assertThat(f1.equals(f3), is(false));
        assertThat(f1.equals(f4), is(false));

        assertThat(f1.equals(null), is(false));
        assertThat(f1.equals(""), is(false));
    }
}