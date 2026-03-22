package com.example.fractal.custom;

import java.util.Locale;

public final class ComplexFormulaParser {

    private ComplexFormulaParser() {
    }

    public static CustomFormulaSettings compile(String input,
                                                CustomFormulaMode mode,
                                                double juliaReal,
                                                double juliaImaginary) {
        String formulaText = input == null ? "" : input.trim();
        String normalized = normalizeFormula(formulaText);
        if (normalized.isBlank()) {
            return new CustomFormulaSettings(formulaText, mode, juliaReal, juliaImaginary, null, "Formula cannot be empty.");
        }
        try {
            Parser parser = new Parser(normalized);
            Node root = parser.parseExpression();
            parser.ensureCompleted();
            return new CustomFormulaSettings(
                    formulaText,
                    mode,
                    juliaReal,
                    juliaImaginary,
                    new CompiledComplexExpression(root::evaluate, normalized),
                    null
            );
        } catch (IllegalArgumentException ex) {
            return new CustomFormulaSettings(formulaText, mode, juliaReal, juliaImaginary, null, ex.getMessage());
        }
    }

    private static String normalizeFormula(String formulaText) {
        int equalsIndex = formulaText.indexOf('=');
        if (equalsIndex >= 0) {
            return formulaText.substring(equalsIndex + 1).trim();
        }
        return formulaText;
    }

    private interface Node {
        Complex evaluate(Complex z, Complex c);
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source;
        }

        private Node parseExpression() {
            Node node = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    Node right = parseTerm();
                    Node left = node;
                    node = (z, c) -> left.evaluate(z, c).add(right.evaluate(z, c));
                } else if (match('-')) {
                    Node right = parseTerm();
                    Node left = node;
                    node = (z, c) -> left.evaluate(z, c).subtract(right.evaluate(z, c));
                } else {
                    return node;
                }
            }
        }

        private Node parseTerm() {
            Node node = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    Node right = parsePower();
                    Node left = node;
                    node = (z, c) -> left.evaluate(z, c).multiply(right.evaluate(z, c));
                } else if (match('/')) {
                    Node right = parsePower();
                    Node left = node;
                    node = (z, c) -> left.evaluate(z, c).divide(right.evaluate(z, c));
                } else {
                    return node;
                }
            }
        }

        private Node parsePower() {
            Node left = parseUnary();
            skipWhitespace();
            if (match('^')) {
                Node right = parsePower();
                return (z, c) -> left.evaluate(z, c).pow(right.evaluate(z, c));
            }
            return left;
        }

        private Node parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                Node node = parseUnary();
                return (z, c) -> node.evaluate(z, c).negate();
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                Node inner = parseExpression();
                expect(')');
                return inner;
            }
            if (isDigit(peek()) || peek() == '.') {
                return parseNumber();
            }
            if (isIdentifierStart(peek())) {
                return parseIdentifierOrFunction();
            }
            throw error("Unexpected token at position " + index + ".");
        }

        private Node parseNumber() {
            int start = index;
            while (isDigit(peek())) {
                index++;
            }
            if (peek() == '.') {
                index++;
                while (isDigit(peek())) {
                    index++;
                }
            }
            if (peek() == 'e' || peek() == 'E') {
                index++;
                if (peek() == '+' || peek() == '-') {
                    index++;
                }
                while (isDigit(peek())) {
                    index++;
                }
            }
            double value = Double.parseDouble(source.substring(start, index));
            return (z, c) -> new Complex(value, 0.0);
        }

        private Node parseIdentifierOrFunction() {
            int start = index;
            index++;
            while (isIdentifierPart(peek())) {
                index++;
            }
            String name = source.substring(start, index).toLowerCase(Locale.ROOT);
            skipWhitespace();
            if (match('(')) {
                Node argument = parseExpression();
                expect(')');
                return buildFunction(name, argument);
            }
            return buildVariable(name);
        }

        private Node buildFunction(String name, Node argument) {
            return switch (name) {
                case "sin" -> (z, c) -> argument.evaluate(z, c).sin();
                case "cos" -> (z, c) -> argument.evaluate(z, c).cos();
                case "tan" -> (z, c) -> argument.evaluate(z, c).tan();
                case "exp" -> (z, c) -> argument.evaluate(z, c).exp();
                case "log" -> (z, c) -> argument.evaluate(z, c).log();
                case "abs" -> (z, c) -> argument.evaluate(z, c).absValue();
                default -> throw error("Unsupported function: " + name);
            };
        }

        private Node buildVariable(String name) {
            return switch (name) {
                case "z" -> (z, c) -> z;
                case "c" -> (z, c) -> c;
                case "i" -> (z, c) -> Complex.I;
                case "pi" -> (z, c) -> new Complex(Math.PI, 0.0);
                case "e" -> (z, c) -> new Complex(Math.E, 0.0);
                default -> throw error("Unsupported symbol: " + name);
            };
        }

        private void ensureCompleted() {
            skipWhitespace();
            if (index != source.length()) {
                throw error("Unexpected trailing input at position " + index + ".");
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!match(expected)) {
                throw error("Expected '" + expected + "' at position " + index + ".");
            }
        }

        private boolean match(char expected) {
            if (peek() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return index < source.length() ? source.charAt(index) : '\0';
        }

        private void skipWhitespace() {
            while (Character.isWhitespace(peek())) {
                index++;
            }
        }

        private boolean isDigit(char ch) {
            return ch >= '0' && ch <= '9';
        }

        private boolean isIdentifierStart(char ch) {
            return Character.isLetter(ch);
        }

        private boolean isIdentifierPart(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_';
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message);
        }
    }
}
