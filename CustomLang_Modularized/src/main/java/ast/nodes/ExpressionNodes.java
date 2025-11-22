package ast.nodes;

public class ExpressionNodes {

    // Base expression node with type tracking
    public static abstract class ExpressionNode extends CoreNodes.ASTNode {
        protected String expressionType;

        public ExpressionNode() {
            super();
            this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
        }

        @Override
        public abstract Object execute();

        /**
         * Get the inferred type of this expression
         */
        public String getExpressionType() {
            return expressionType;
        }

        /**
         * Set the expression type (for type inference)
         */
        protected void setExpressionType(String type) {
            this.expressionType = type;
        }
    }

    // Literal expression node
    public static class LiteralNode extends ExpressionNode {
        private String value;

        public LiteralNode(String value) {
            super();
            this.value = value;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Infer type from literal value
            this.expressionType = CoreNodes.TypeChecker.inferTypeFromLiteral(value);

            if (this.expressionType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                addError("SEM006: Invalid literal format: '" + value + "'");
                return;
            }

            // Validate literal format
            if (value != null) {
                // Check for mismatched quotes
                if (value.startsWith("'") && !value.endsWith("'")) {
                    addError("SEM006: Mismatched single quotes in literal: '" + value + "'");
                } else if (value.startsWith("\"") && !value.endsWith("\"")) {
                    addError("SEM006: Mismatched double quotes in literal: '" + value + "'");
                }

                // Validate character literals (should be single character)
                if (value.startsWith("'") && value.endsWith("'") && value.length() > 3) {
                    addError("SEM006: Character literal can only contain one character: '" + value + "'");
                }

                // Validate empty character literal
                if (value.equals("''")) {
                    addError("SEM006: Empty character literal is not allowed");
                }
            }
        }

        @Override
        public Object execute() {
            return CoreNodes.GlobalContext.formatLiteral(value);
        }

        // Getter for testing
        public String getValue() {
            return value;
        }
    }

    // Variable reference node
    public static class VariableNode extends ExpressionNode {
        private String name;

        public VariableNode(String name) {
            super();
            this.name = name;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check variable exists
            if (!scope.isDeclared(name)) {
                addError("SEM002: Undefined variable: '" + name + "'");
                this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                return;
            }

            // Get variable type
            this.expressionType = scope.lookup(name);

            // Check variable is initialized
            if (!scope.isInitialized(name)) {
                addError("SEM005: Variable '" + name + "' used before initialization");
            }
        }

        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(name)) {
                throw new RuntimeException("Undefined variable: " + name);
            }
            return CoreNodes.GlobalContext.symbolTable.get(name);
        }

        // Getter for testing
        public String getName() {
            return name;
        }
    }

    public static class BinaryOperationNode extends ExpressionNode {
        private String operator;
        private ExpressionNode left;
        private ExpressionNode right;

        public BinaryOperationNode(String operator, ExpressionNode left, ExpressionNode right) {
            super();
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Validate left operand
            if (left != null) {
                left.validate(scope);
                propagateErrors(left);
            } else {
                addError("SEM007: Binary operation missing left operand");
            }

            // Validate right operand
            if (right != null) {
                right.validate(scope);
                propagateErrors(right);
            } else {
                addError("SEM007: Binary operation missing right operand");
            }

            // Proceed with type checking if both operands are valid
            if (left != null && right != null && !hasErrors()) {
                String leftType = left.getExpressionType();
                String rightType = right.getExpressionType();

                // Skip type checking if either type is unknown (already has errors)
                if (!leftType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN) &&
                        !rightType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                    validateBinaryOperation(leftType, rightType);
                }
            }
        }

        /**
         * Validate type compatibility for binary operations
         */
        private void validateBinaryOperation(String leftType, String rightType) {
            switch (operator) {
                case "+":
                case "-":
                case "*":
                case "/":
                case "%":
                    // Arithmetic operators require numeric operands
                    if (!leftType.equals(CoreNodes.TypeChecker.TYPE_NUMBER) ||
                            !rightType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                        addError("SEM003: Arithmetic operator '" + operator +
                                "' requires numeric operands, got " + leftType + " and " + rightType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
                    break;

                case "&&":
                case "||":
                    // Logical operators require boolean operands
                    if (!leftType.equals(CoreNodes.TypeChecker.TYPE_LOGIC) ||
                            !rightType.equals(CoreNodes.TypeChecker.TYPE_LOGIC)) {
                        addError("SEM003: Logical operator '" + operator +
                                "' requires boolean operands, got " + leftType + " and " + rightType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_LOGIC;
                    break;

                case ">":
                case "<":
                case ">=":
                case "<=":
                    // Relational operators require numeric operands
                    if (!leftType.equals(CoreNodes.TypeChecker.TYPE_NUMBER) ||
                            !rightType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                        addError("SEM003: Relational operator '" + operator +
                                "' requires numeric operands, got " + leftType + " and " + rightType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_LOGIC;
                    break;

                case "==":
                case "!=":
                    // Equality operators require compatible types
                    if (!CoreNodes.TypeChecker.areTypesCompatible(leftType, rightType) &&
                            !CoreNodes.TypeChecker.areTypesCompatible(rightType, leftType)) {
                        addError("SEM004: Equality operator '" + operator +
                                "' requires compatible types, got " + leftType + " and " + rightType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_LOGIC;
                    break;

                default:
                    addError("SEM007: Unknown binary operator: '" + operator + "'");
                    this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                    break;
            }
        }

        @Override
        public Object execute() {
            // Always execute both sides to ensure proper evaluation
            Object leftResult = left.execute();
            Object rightResult = right.execute();
            String leftValue = leftResult != null ? leftResult.toString() : "null";
            String rightValue = rightResult != null ? rightResult.toString() : "null";

            // Handle arithmetic operations
            if (operator.equals("+") || operator.equals("-") ||
                    operator.equals("*") || operator.equals("/") || operator.equals("%")) {
                double val1 = Double.parseDouble(leftValue);
                double val2 = Double.parseDouble(rightValue);
                switch (operator) {
                    case "+":
                        return String.valueOf(val1 + val2);
                    case "-":
                        return String.valueOf(val1 - val2);
                    case "*":
                        return String.valueOf(val1 * val2);
                    case "/":
                        if (val2 == 0)
                            throw new RuntimeException("Division by zero");
                        return String.valueOf(val1 / val2);
                    case "%":
                        if (val2 == 0)
                            throw new RuntimeException("Modulus by zero");
                        return String.valueOf(val1 % val2);
                    default:
                        break;
                }
            }

            // Handle comparison operations
            if (operator.equals(">") || operator.equals("<") ||
                    operator.equals(">=") || operator.equals("<=") ||
                    operator.equals("==") || operator.equals("!=")) {

                // Check if both are quoted strings or characters
                if ((leftValue.startsWith("\"") && leftValue.endsWith("\"") &&
                        rightValue.startsWith("\"") && rightValue.endsWith("\"")) ||
                        (leftValue.startsWith("'") && leftValue.endsWith("'") &&
                                rightValue.startsWith("'") && rightValue.endsWith("'"))) {

                    // Remove quotes for comparison
                    String l = leftValue.substring(1, leftValue.length() - 1);
                    String r = rightValue.substring(1, rightValue.length() - 1);
                    switch (operator) {
                        case "==":
                            return String.valueOf(l.equals(r));
                        case "!=":
                            return String.valueOf(!l.equals(r));
                        default:
                            throw new RuntimeException("Invalid operator for string/char comparison");
                    }
                }

                // Try numeric comparison
                try {
                    double val1 = Double.parseDouble(leftValue);
                    double val2 = Double.parseDouble(rightValue);
                    switch (operator) {
                        case ">":
                            return String.valueOf(val1 > val2);
                        case "<":
                            return String.valueOf(val1 < val2);
                        case ">=":
                            return String.valueOf(val1 >= val2);
                        case "<=":
                            return String.valueOf(val1 <= val2);
                        case "==":
                            return String.valueOf(Math.abs(val1 - val2) < 0.000001);
                        case "!=":
                            return String.valueOf(Math.abs(val1 - val2) >= 0.000001);
                        default:
                            break;
                    }
                } catch (NumberFormatException e) {
                    // If not numbers and not strings/chars, treat as boolean comparison
                    if (leftValue.equals("true") || leftValue.equals("false") ||
                            rightValue.equals("true") || rightValue.equals("false")) {
                        boolean val1 = Boolean.parseBoolean(leftValue);
                        boolean val2 = Boolean.parseBoolean(rightValue);
                        switch (operator) {
                            case "==":
                                return String.valueOf(val1 == val2);
                            case "!=":
                                return String.valueOf(val1 != val2);
                            default:
                                throw new RuntimeException("Invalid operator for boolean values");
                        }
                    }
                }
            }

            // Handle logical operations
            if (operator.equals("&&") || operator.equals("||")) {
                boolean val1 = Boolean.parseBoolean(leftValue);
                boolean val2 = Boolean.parseBoolean(rightValue);
                switch (operator) {
                    case "&&":
                        return String.valueOf(val1 && val2);
                    case "||":
                        return String.valueOf(val1 || val2);
                    default:
                        break;
                }
            }

            // If we get here, operator is not supported
            throw new RuntimeException("Unsupported operator: " + operator);
        }

        // Getters for testing
        public String getOperator() {
            return operator;
        }

        public ExpressionNode getLeft() {
            return left;
        }

        public ExpressionNode getRight() {
            return right;
        }
    }

    public static class UnaryOperationNode extends ExpressionNode {
        private String operator;
        private ExpressionNode operand;

        public UnaryOperationNode(String operator, ExpressionNode operand) {
            super();
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Validate operand
            if (operand != null) {
                operand.validate(scope);
                propagateErrors(operand);
            } else {
                addError("SEM007: Unary operation missing operand");
                return;
            }

            // Proceed with type checking if operand is valid
            if (!hasErrors()) {
                String operandType = operand.getExpressionType();

                // Skip type checking if operand type is unknown (already has errors)
                if (!operandType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                    validateUnaryOperation(operandType);
                }
            }
        }

        /**
         * Validate type compatibility for unary operations
         */
        private void validateUnaryOperation(String operandType) {
            switch (operator) {
                case "!":
                    // Logical NOT requires boolean operand
                    if (!operandType.equals(CoreNodes.TypeChecker.TYPE_LOGIC)) {
                        addError("SEM003: Logical NOT operator '!' requires boolean operand, got " + operandType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_LOGIC;
                    break;

                case "-":
                case "+":
                    // Unary plus/minus requires numeric operand
                    if (!operandType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                        addError("SEM003: Unary operator '" + operator +
                                "' requires numeric operand, got " + operandType);
                    }
                    this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
                    break;

                default:
                    addError("SEM007: Unknown unary operator: '" + operator + "'");
                    this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                    break;
            }
        }

        @Override
        public Object execute() {
            // Always execute operand to ensure proper evaluation
            Object result = operand.execute();
            String value = result != null ? result.toString() : "null";

            if (operator.equals("!")) {
                return String.valueOf(!Boolean.parseBoolean(value));
            } else if (operator.equals("-")) {
                try {
                    double val = Double.parseDouble(value);
                    return String.valueOf(-val);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Unary minus requires a numeric operand");
                }
            } else if (operator.equals("+")) {
                try {
                    double val = Double.parseDouble(value);
                    return String.valueOf(val);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Unary plus requires a numeric operand");
                }
            }

            // If we get here, operator is not supported
            throw new RuntimeException("Unsupported unary operator: " + operator);
        }

        // Getters for testing
        public String getOperator() {
            return operator;
        }

        public ExpressionNode getOperand() {
            return operand;
        }
    }

    public static class PreIncrementNode extends ExpressionNode {
        private String variableName;

        public PreIncrementNode(String variableName) {
            super();
            this.variableName = variableName;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
                this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                return;
            }

            // Check if variable is initialized
            if (!scope.isInitialized(variableName)) {
                addError("SEM005: Variable '" + variableName + "' used before initialization");
            }

            // Check if variable is numeric type
            String varType = scope.lookup(variableName);
            if (!varType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                addError("SEM003: Cannot increment non-numeric variable '" + variableName +
                        "' of type " + varType);
            }

            // Pre-increment returns NUMBER
            this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
        }

        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(variableName)) {
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            Object value = CoreNodes.GlobalContext.symbolTable.get(variableName);
            double numValue;
            try {
                numValue = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot increment non-numeric value: " + value);
            }

            // Increment first, then return
            numValue++;
            String result = String.valueOf(numValue);
            CoreNodes.GlobalContext.symbolTable.put(variableName, result);
            return result;
        }

        // Getter for testing
        public String getVariableName() {
            return variableName;
        }
    }

    public static class PostIncrementNode extends ExpressionNode {
        private String variableName;

        public PostIncrementNode(String variableName) {
            super();
            this.variableName = variableName;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
                this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                return;
            }

            // Check if variable is initialized
            if (!scope.isInitialized(variableName)) {
                addError("SEM005: Variable '" + variableName + "' used before initialization");
            }

            // Check if variable is numeric type
            String varType = scope.lookup(variableName);
            if (!varType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                addError("SEM003: Cannot increment non-numeric variable '" + variableName +
                        "' of type " + varType);
            }

            // Post-increment returns NUMBER
            this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
        }

        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(variableName)) {
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            Object value = CoreNodes.GlobalContext.symbolTable.get(variableName);
            double numValue;
            try {
                numValue = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot increment non-numeric value: " + value);
            }

            // Save the original value to return
            String originalValue = String.valueOf(numValue);
            // Increment and update the symbol table
            numValue++;
            CoreNodes.GlobalContext.symbolTable.put(variableName, String.valueOf(numValue));
            // Return the original value
            return originalValue;
        }

        // Getter for testing
        public String getVariableName() {
            return variableName;
        }
    }

    public static class PreDecrementNode extends ExpressionNode {
        private String variableName;

        public PreDecrementNode(String variableName) {
            super();
            this.variableName = variableName;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
                this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                return;
            }

            // Check if variable is initialized
            if (!scope.isInitialized(variableName)) {
                addError("SEM005: Variable '" + variableName + "' used before initialization");
            }

            // Check if variable is numeric type
            String varType = scope.lookup(variableName);
            if (!varType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                addError("SEM003: Cannot decrement non-numeric variable '" + variableName +
                        "' of type " + varType);
            }

            // Pre-decrement returns NUMBER
            this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
        }

        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(variableName)) {
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            Object value = CoreNodes.GlobalContext.symbolTable.get(variableName);
            double numValue;
            try {
                numValue = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot decrement non-numeric value: " + value);
            }

            // Decrement first, then return
            numValue--;
            String result = String.valueOf(numValue);
            CoreNodes.GlobalContext.symbolTable.put(variableName, result);
            return result;
        }

        // Getter for testing
        public String getVariableName() {
            return variableName;
        }
    }

    public static class PostDecrementNode extends ExpressionNode {
        private String variableName;

        public PostDecrementNode(String variableName) {
            super();
            this.variableName = variableName;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
                this.expressionType = CoreNodes.TypeChecker.TYPE_UNKNOWN;
                return;
            }

            // Check if variable is initialized
            if (!scope.isInitialized(variableName)) {
                addError("SEM005: Variable '" + variableName + "' used before initialization");
            }

            // Check if variable is numeric type
            String varType = scope.lookup(variableName);
            if (!varType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                addError("SEM003: Cannot decrement non-numeric variable '" + variableName +
                        "' of type " + varType);
            }

            // Post-decrement returns NUMBER
            this.expressionType = CoreNodes.TypeChecker.TYPE_NUMBER;
        }

        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(variableName)) {
                throw new RuntimeException("Undefined variable: " + variableName);
            }

            Object value = CoreNodes.GlobalContext.symbolTable.get(variableName);
            double numValue;
            try {
                numValue = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot decrement non-numeric value: " + value);
            }

            // Save the original value to return
            String originalValue = String.valueOf(numValue);
            // Decrement and update the symbol table
            numValue--;
            CoreNodes.GlobalContext.symbolTable.put(variableName, String.valueOf(numValue));
            // Return the original value
            return originalValue;
        }

        // Getter for testing
        public String getVariableName() {
            return variableName;
        }
    }
}

// 394