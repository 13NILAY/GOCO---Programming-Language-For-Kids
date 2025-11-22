package ast.nodes;

public class DeclarationNodes {

    // Variable declaration node
    public static class VarDeclarationNode extends CoreNodes.ASTNode {
        private String type;
        private String name;
        private ExpressionNodes.ExpressionNode value;

        public VarDeclarationNode(String type, String name, ExpressionNodes.ExpressionNode value) {
            super();
            this.type = type;
            this.name = name;
            this.value = value;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // DEBUG: Add this to track validation
            System.err.println("DEBUG: Validating declaration for variable: " + name);

            // Normalize type for consistent checking
            String normalizedType = CoreNodes.TypeChecker.normalizeType(type);

            // Check if type is valid
            if (!CoreNodes.TypeChecker.isValidType(normalizedType)) {
                addError("SEM001: Invalid type '" + type + "' in variable declaration");
                return; // Cannot proceed with invalid type
            }

            // Check for valid variable name pattern
            if (!isValidVariableName(name)) {
                addError("SEM001: Invalid variable name '" + name
                        + "' (must start with letter, contain only letters, digits, and underscores)");
            }

            // FIXED: Check for duplicate declaration in ANY scope (not just current scope)
            if (scope.isDeclared(name)) {
                addError("SEM001: Duplicate variable declaration: '" + name + "'");
                System.err.println("DEBUG: Duplicate declaration detected for " + name);
            }

            // Validate initial value if provided
            if (value != null) {
                value.validate(scope);
                propagateErrors(value);

                // Check type compatibility between declared type and initial value
                if (!hasErrors()) {
                    String valueType = inferExpressionType(value, scope);
                    if (!valueType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                        if (!CoreNodes.TypeChecker.areTypesCompatible(normalizedType, valueType)) {
                            addError("SEM004: Type mismatch in variable declaration: cannot assign " +
                                    valueType + " to " + normalizedType + " variable '" + name + "'");
                        }
                    }
                }
            }

            // Declare variable in scope if no errors
            if (!hasErrors()) {
                if (value != null) {
                    scope.declareAndInitialize(name, normalizedType);
                    System.err.println("DEBUG: Successfully declared and initialized: " + name);
                } else {
                    scope.declare(name, normalizedType);
                    System.err.println("DEBUG: Successfully declared: " + name);
                }
            } else {
                System.err.println("DEBUG: Declaration failed for " + name + " due to errors");
            }
        }

        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                String valueStr = (value != null) ? value.execute().toString() : getDefaultValue();
                CoreNodes.GlobalContext.symbolTable.put(name, valueStr);
                System.err.println("DEBUG: Executed declaration - " + name + " = " + valueStr);
            }
            return null;
        }

        private String getDefaultValue() {
            if (type.toUpperCase().startsWith("NUMBER")) {
                return "0";
            } else if (type.toUpperCase().startsWith("LETTER")) {
                return "' '";
            } else if (type.toUpperCase().startsWith("LOGIC")) {
                return "false";
            } else {
                return "\"\"";
            }
        }

        /**
         * Check if variable name is valid (starts with letter, contains only
         * alphanumeric and underscore)
         */
        private boolean isValidVariableName(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            // Must start with letter
            if (!Character.isLetter(name.charAt(0))) {
                return false;
            }
            // Rest must be letters, digits, or underscores
            for (int i = 1; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return false;
                }
            }
            return true;
        }

        /**
         * Infer type from expression
         */
        private String inferExpressionType(ExpressionNodes.ExpressionNode expr, CoreNodes.Scope scope) {
            if (expr instanceof ExpressionNodes.LiteralNode) {
                ExpressionNodes.LiteralNode literal = (ExpressionNodes.LiteralNode) expr;
                return CoreNodes.TypeChecker.inferTypeFromLiteral(literal.getValue());
            } else if (expr instanceof ExpressionNodes.VariableNode) {
                ExpressionNodes.VariableNode var = (ExpressionNodes.VariableNode) expr;
                String varType = scope.lookup(var.getName());
                return varType != null ? varType : CoreNodes.TypeChecker.TYPE_UNKNOWN;
            } else if (expr instanceof ExpressionNodes.BinaryOperationNode) {
                // Binary operations typically return NUMBER for arithmetic, LOGIC for
                // comparisons
                ExpressionNodes.BinaryOperationNode binOp = (ExpressionNodes.BinaryOperationNode) expr;
                String op = binOp.getOperator();
                if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                    return CoreNodes.TypeChecker.TYPE_NUMBER;
                } else if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">") ||
                        op.equals("<=") || op.equals(">=") || op.equals("&&") || op.equals("||")) {
                    return CoreNodes.TypeChecker.TYPE_LOGIC;
                }
            }
            return CoreNodes.TypeChecker.TYPE_UNKNOWN;
        }

        // Getters for testing
        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public ExpressionNodes.ExpressionNode getValue() {
            return value;
        }
    }

    // Assignment node
    public static class AssignmentNode extends CoreNodes.ASTNode {
        private String name;
        private String operator;
        private ExpressionNodes.ExpressionNode value;

        public AssignmentNode(String name, String operator, ExpressionNodes.ExpressionNode value) {
            super();
            this.name = name;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // DEBUG
            System.err.println("DEBUG: Validating assignment for variable: " + name);

            // Check if variable exists in scope
            if (!scope.isDeclared(name)) {
                addError("SEM002: Undefined variable: '" + name + "'");
                System.err.println("DEBUG: Undefined variable: " + name);
                return; // Cannot proceed without variable
            }

            // Get variable type
            String varType = scope.lookup(name);

            // Validate the value expression
            if (value != null) {
                value.validate(scope);
                propagateErrors(value);

                // Check type compatibility for assignments
                if (!hasErrors()) {
                    String valueType = inferExpressionType(value, scope);

                    if (operator.equals("=")) {
                        // Simple assignment - check type compatibility
                        if (!valueType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                            if (!CoreNodes.TypeChecker.areTypesCompatible(varType, valueType)) {
                                addError("SEM004: Type mismatch: cannot assign " + valueType +
                                        " to " + varType + " variable '" + name + "'");
                            }
                        }
                    } else {
                        // Compound assignment (+=, -=, *=, /=, %=)
                        // Variable must be numeric
                        if (!varType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                            addError("SEM003: Cannot use compound assignment operator '" + operator +
                                    "' on non-numeric variable '" + name + "' of type " + varType);
                        }
                        // Value must be numeric
                        if (!valueType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN) &&
                                !valueType.equals(CoreNodes.TypeChecker.TYPE_NUMBER)) {
                            addError("SEM004: Cannot use " + valueType +
                                    " value with compound assignment operator '" + operator + "'");
                        }
                    }
                }
            } else {
                addError("SEM004: Assignment requires a value");
            }

            // Mark variable as initialized after assignment
            if (!hasErrors()) {
                scope.markInitialized(name);
                System.err.println("DEBUG: Assignment validation passed for: " + name);
            } else {
                System.err.println("DEBUG: Assignment validation failed for: " + name);
            }
        }

        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                if (!CoreNodes.GlobalContext.symbolTable.containsKey(name)) {
                    throw new RuntimeException("Undefined variable: " + name);
                }

                String valueStr = value.execute().toString();
                if (operator.equals("=")) {
                    // Simple assignment
                    // Check if the value is a CHAR_LITERAL or STRING_LITERAL and store it as is
                    if (valueStr.startsWith("'") && valueStr.endsWith("'") ||
                            valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                        CoreNodes.GlobalContext.symbolTable.put(name, valueStr);
                    } else {
                        // Otherwise, parse it as a number or boolean
                        try {
                            Double.parseDouble(valueStr);
                            CoreNodes.GlobalContext.symbolTable.put(name, valueStr);
                        } catch (NumberFormatException e) {
                            CoreNodes.GlobalContext.symbolTable.put(name,
                                    Boolean.toString(Boolean.parseBoolean(valueStr)));
                        }
                    }
                } else {
                    // Compound assignment (+=, -=, *=, /=, %=)
                    String currentValue = CoreNodes.GlobalContext.symbolTable.get(name);
                    double val1 = Double.parseDouble(currentValue);
                    double val2 = Double.parseDouble(valueStr);
                    double result;
                    switch (operator) {
                        case "+=":
                            result = val1 + val2;
                            break;
                        case "-=":
                            result = val1 - val2;
                            break;
                        case "*=":
                            result = val1 * val2;
                            break;
                        case "/=":
                            if (val2 == 0)
                                throw new RuntimeException("Division by zero");
                            result = val1 / val2;
                            break;
                        case "%=":
                            if (val2 == 0)
                                throw new RuntimeException("Modulus by zero");
                            result = val1 % val2;
                            break;
                        default:
                            throw new RuntimeException("Unknown operator: " + operator);
                    }
                    CoreNodes.GlobalContext.symbolTable.put(name, String.valueOf(result));
                }
                System.err.println("DEBUG: Executed assignment - " + name + " " + operator + " " + valueStr);
            }
            return null;
        }

        /**
         * Infer type from expression
         */
        private String inferExpressionType(ExpressionNodes.ExpressionNode expr, CoreNodes.Scope scope) {
            if (expr instanceof ExpressionNodes.LiteralNode) {
                ExpressionNodes.LiteralNode literal = (ExpressionNodes.LiteralNode) expr;
                return CoreNodes.TypeChecker.inferTypeFromLiteral(literal.getValue());
            } else if (expr instanceof ExpressionNodes.VariableNode) {
                ExpressionNodes.VariableNode var = (ExpressionNodes.VariableNode) expr;
                String varType = scope.lookup(var.getName());
                return varType != null ? varType : CoreNodes.TypeChecker.TYPE_UNKNOWN;
            } else if (expr instanceof ExpressionNodes.BinaryOperationNode) {
                ExpressionNodes.BinaryOperationNode binOp = (ExpressionNodes.BinaryOperationNode) expr;
                String op = binOp.getOperator();
                if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                    return CoreNodes.TypeChecker.TYPE_NUMBER;
                } else if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">") ||
                        op.equals("<=") || op.equals(">=") || op.equals("&&") || op.equals("||")) {
                    return CoreNodes.TypeChecker.TYPE_LOGIC;
                }
            }
            return CoreNodes.TypeChecker.TYPE_UNKNOWN;
        }

        // Getters for testing
        public String getName() {
            return name;
        }

        public String getOperator() {
            return operator;
        }

        public ExpressionNodes.ExpressionNode getValue() {
            return value;
        }
    }

    public static class IncrementStatementNode extends CoreNodes.ASTNode {
        private String variableName;
        private boolean isPreIncrement;

        public IncrementStatementNode(String variableName, boolean isPreIncrement) {
            super();
            this.variableName = variableName;
            this.isPreIncrement = isPreIncrement;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
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
        }

        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
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

                // Increment and update the symbol table
                numValue++;
                CoreNodes.GlobalContext.symbolTable.put(variableName, String.valueOf(numValue));
            }
            return null;
        }

        // Getters for testing
        public String getVariableName() {
            return variableName;
        }

        public boolean isPreIncrement() {
            return isPreIncrement;
        }
    }

    public static class DecrementStatementNode extends CoreNodes.ASTNode {
        private String variableName;
        private boolean isPreDecrement;

        public DecrementStatementNode(String variableName, boolean isPreDecrement) {
            super();
            this.variableName = variableName;
            this.isPreDecrement = isPreDecrement;
        }

        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();

            // Check if variable exists in scope
            if (!scope.isDeclared(variableName)) {
                addError("SEM002: Undefined variable: '" + variableName + "'");
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
        }

        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
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

                // Decrement and update the symbol table
                numValue--;
                CoreNodes.GlobalContext.symbolTable.put(variableName, String.valueOf(numValue));
            }
            return null;
        }

        // Getters for testing
        public String getVariableName() {
            return variableName;
        }

        public boolean isPreDecrement() {
            return isPreDecrement;
        }
    }
}
// 294