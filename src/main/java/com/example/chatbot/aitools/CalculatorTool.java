package com.example.chatbot.aitools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
 
import java.util.Locale;

@Component
public class CalculatorTool {

    private static final int MAX_EXPRESSION_LENGTH = 512;

    @Tool(description = """
            Evaluate a mathematical expression. Supports +, -, *, /, %, ^, parentheses,
            constants pi and e, and functions sqrt, abs, sin, cos, tan, asin, acos, atan,
            ln, log10, exp, floor, ceil, round, min, max, and pow. Use * for multiplication.
            Example: (12.5 + 3) * 2. Returns a finite numeric result.
            """)
    public double calculate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression must not be blank.");
        }
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException(
                    "Expression must be no longer than " + MAX_EXPRESSION_LENGTH + " characters.");
        }

        return new ExpressionParser(expression).parse();
    }

    private static final class ExpressionParser {

        private final String input;
        private int position;

        private ExpressionParser(String input) {
            this.input = input;
        }

        private double parse() {
            double result = parseExpression();
            skipWhitespace();
            if (position != input.length()) {
                throw error("Unexpected character '" + input.charAt(position) + "'.");
            }
            return requireFinite(result);
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                if (match('+')) {
                    value = requireFinite(value + parseTerm());
                } else if (match('-')) {
                    value = requireFinite(value - parseTerm());
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseUnary();
            while (true) {
                if (match('*')) {
                    value = requireFinite(value * parseUnary());
                } else if (match('/')) {
                    double divisor = parseUnary();
                    if (divisor == 0.0) {
                        throw new ArithmeticException("Division by zero.");
                    }
                    value = requireFinite(value / divisor);
                } else if (match('%')) {
                    double divisor = parseUnary();
                    if (divisor == 0.0) {
                        throw new ArithmeticException("Remainder by zero.");
                    }
                    value = requireFinite(value % divisor);
                } else {
                    return value;
                }
            }
        }

        private double parseUnary() {
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                return requireFinite(-parseUnary());
            }
            return parsePower();
        }

        private double parsePower() {
            double base = parsePrimary();
            if (match('^')) {
                return requireFinite(Math.pow(base, parseUnary()));
            }
            return base;
        }

        private double parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }
            if (position >= input.length()) {
                throw error("Expected a number, constant, function, or '('.");
            }

            char current = input.charAt(position);
            if (Character.isDigit(current) || current == '.') {
                return parseNumber();
            }
            if (Character.isLetter(current)) {
                return parseIdentifier();
            }
            throw error("Expected a number, constant, function, or '('.");
        }

        private double parseNumber() {
            int start = position;
            boolean hasDigits = false;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
                hasDigits = true;
            }
            if (position < input.length() && input.charAt(position) == '.') {
                position++;
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    position++;
                    hasDigits = true;
                }
            }
            if (!hasDigits) {
                throw error("Invalid number.");
            }
            if (position < input.length()
                    && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
                int exponentPosition = position++;
                if (position < input.length()
                        && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                    position++;
                }
                int exponentStart = position;
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    position++;
                }
                if (position == exponentStart) {
                    position = exponentPosition;
                    throw error("Invalid exponent.");
                }
            }

            try {
                return requireFinite(Double.parseDouble(input.substring(start, position)));
            } catch (NumberFormatException exception) {
                throw error("Invalid number.");
            }
        }

        private double parseIdentifier() {
            int start = position;
            while (position < input.length() && Character.isLetter(input.charAt(position))) {
                position++;
            }
            String name = input.substring(start, position).toLowerCase(Locale.ROOT);
            skipWhitespace();

            if (!match('(')) {
                return switch (name) {
                    case "pi" -> Math.PI;
                    case "e" -> Math.E;
                    default -> throw error("Unknown constant '" + name + "'.");
                };
            }

            double first = parseExpression();
            boolean hasSecondArgument = match(',');
            double second = hasSecondArgument ? parseExpression() : 0;
            expect(')');
            return applyFunction(name, first, second, hasSecondArgument);
        }

        private double applyFunction(String name, double first, double second, boolean hasSecondArgument) {
            double result = switch (name) {
                case "sqrt" -> unaryFunction(name, hasSecondArgument, Math::sqrt, first);
                case "abs" -> unaryFunction(name, hasSecondArgument, Math::abs, first);
                case "sin" -> unaryFunction(name, hasSecondArgument, Math::sin, first);
                case "cos" -> unaryFunction(name, hasSecondArgument, Math::cos, first);
                case "tan" -> unaryFunction(name, hasSecondArgument, Math::tan, first);
                case "asin" -> unaryFunction(name, hasSecondArgument, Math::asin, first);
                case "acos" -> unaryFunction(name, hasSecondArgument, Math::acos, first);
                case "atan" -> unaryFunction(name, hasSecondArgument, Math::atan, first);
                case "ln" -> unaryFunction(name, hasSecondArgument, Math::log, first);
                case "log10" -> unaryFunction(name, hasSecondArgument, Math::log10, first);
                case "exp" -> unaryFunction(name, hasSecondArgument, Math::exp, first);
                case "floor" -> unaryFunction(name, hasSecondArgument, Math::floor, first);
                case "ceil" -> unaryFunction(name, hasSecondArgument, Math::ceil, first);
                case "round" -> unaryFunction(name, hasSecondArgument, value -> (double) Math.round(value), first);
                case "min" -> binaryFunction(name, hasSecondArgument, Math::min, first, second);
                case "max" -> binaryFunction(name, hasSecondArgument, Math::max, first, second);
                case "pow" -> binaryFunction(name, hasSecondArgument, Math::pow, first, second);
                default -> throw error("Unknown function '" + name + "'.");
            };
            return requireFinite(result);
        }

        private double unaryFunction(
                String name, boolean hasSecondArgument, java.util.function.DoubleUnaryOperator function, double value) {
            if (hasSecondArgument) {
                throw error("Function '" + name + "' expects one argument.");
            }
            return function.applyAsDouble(value);
        }

        private double binaryFunction(
                String name,
                boolean hasSecondArgument,
                java.util.function.DoubleBinaryOperator function,
                double first,
                double second) {
            if (!hasSecondArgument) {
                throw error("Function '" + name + "' expects two arguments.");
            }
            return function.applyAsDouble(first, second);
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw error("Expected '" + expected + "'.");
            }
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private double requireFinite(double value) {
            if (!Double.isFinite(value)) {
                throw new ArithmeticException("Expression result is not a finite number.");
            }
            return value;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " (at position " + position + ")");
        }
    }
}
