package com.example.chatbot.aitools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorToolTest {

    private final CalculatorTool calculator = new CalculatorTool();

    @Test
    void evaluatesOperatorPrecedenceAndParentheses() {
        assertEquals(14.0, calculator.calculate("2 + 3 * 4"));
        assertEquals(20.0, calculator.calculate("(2 + 3) * 4"));
    }

    @Test
    void evaluatesUnaryAndRightAssociativeExponentiation() {
        assertEquals(-4.0, calculator.calculate("-2^2"));
        assertEquals(512.0, calculator.calculate("2^3^2"));
        assertEquals(0.25, calculator.calculate("2^-2"));
    }

    @Test
    void evaluatesFunctionsConstantsAndScientificNotation() {
        assertEquals(5.0, calculator.calculate("sqrt(9) + abs(-2)"));
        assertEquals(Math.PI, calculator.calculate("pi"));
        assertEquals(0.001, calculator.calculate("1e-3"));
        assertEquals(3.0, calculator.calculate("max(1, 3)"));
        assertEquals(8.0, calculator.calculate("pow(2, 3)"));
    }

    @Test
    void rejectsInvalidExpressionsAndNonFiniteResults() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(""));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate("1 + unknown"));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate("1 + 2 extra"));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate("sqrt(1, 2)"));
        assertThrows(ArithmeticException.class, () -> calculator.calculate("1 / 0"));
        assertThrows(ArithmeticException.class, () -> calculator.calculate("sqrt(-1)"));
    }
}
