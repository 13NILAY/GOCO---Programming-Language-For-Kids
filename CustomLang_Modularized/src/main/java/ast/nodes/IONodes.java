package ast.nodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class IONodes {
    
    // Base Display node
    public static abstract class BaseDisplayNode extends CoreNodes.ASTNode {
        protected ExpressionNodes.ExpressionNode expression;
        protected String idName;
        protected String stringLiteral;
        protected boolean isVar;
        protected Object value;
        protected ExpressionNodes.ExpressionNode expr;
        
        public BaseDisplayNode() {
            super();
        }
        
        /**
         * Base validation method for all display nodes
         */
        protected void validateBaseDisplay(CoreNodes.Scope scope) {
            // Validate expression if present
            if (expression != null) {
                expression.validate(scope);
                propagateErrors(expression);
            }
            
            // Validate variable reference if present
            if (idName != null) {
                if (!scope.isDeclared(idName)) {
                    addError("SEM016: Undefined variable in display: '" + idName + "'");
                } else if (!scope.isInitialized(idName)) {
                    addError("SEM005: Variable '" + idName + "' used before initialization");
                }
            }
            
            // Validate string literal format if present
            if (stringLiteral != null) {
                validateStringLiteral(stringLiteral);
            }
            
            // Validate expr if present
            if (expr != null) {
                expr.validate(scope);
                propagateErrors(expr);
            }
        }
        
        /**
         * Validate string literal format
         */
        protected void validateStringLiteral(String literal) {
            if (literal == null) return;
            
            if (literal.startsWith("'") && !literal.endsWith("'")) {
                addError("SEM006: Mismatched single quotes in display literal: '" + literal + "'");
            } else if (literal.startsWith("\"") && !literal.endsWith("\"")) {
                addError("SEM006: Mismatched double quotes in display literal: '" + literal + "'");
            }
            
            // Validate character literal length
            if (literal.startsWith("'") && literal.endsWith("'") && literal.length() != 3) {
                addError("SEM006: Character literal must contain exactly one character: '" + literal + "'");
            }
        }
        
        // Method to get formatted output
        protected String getFormattedOutput() {
            if (expression != null) {
                // Expression result
                Object result = expression.execute();
                
                // Handle array functions that return values
                if (result instanceof String && result.toString().startsWith("ARRAY:")) {
                    // Handle reference to array in symbol table
                    String[] parts = result.toString().split(":");
                    String arrayName = parts[2];
                    return ast.arrays.ArrayCoreNodes.formatArrayForDisplay(arrayName);
                }
                
                // Make sure we handle null values gracefully
                return result != null ? result.toString() : "(empty)";
            } else if (idName != null) {
                // Variable value
                if (!CoreNodes.GlobalContext.symbolTable.containsKey(idName)) {
                    throw new RuntimeException("Undefined variable: " + idName);
                }
                
                // Check if it's an array
                if (ast.arrays.ArrayCoreNodes.isArray(idName)) {
                    return ast.arrays.ArrayCoreNodes.formatArrayForDisplay(idName);
                }
                
                String value = CoreNodes.GlobalContext.symbolTable.get(idName);
                return formatValue(value);
            } else if (stringLiteral != null) {
                // String literal
                return stringLiteral.substring(1, stringLiteral.length() - 1);
            }
            return "";
        }
        
        // Helper method to format values based on their type
        protected String formatValue(String value) {
            if (value == null) {
                return "(empty)";
            }
            
            // Try to parse as number if it looks like one
            try {
                if (value.matches("-?\\d+(\\.\\d+)?")) {
                    return Double.toString(Double.parseDouble(value));
                } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    return value.toLowerCase(); // Print boolean values directly
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    return value.substring(1, value.length() - 1); // Remove single quotes for letters
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1); // Remove double quotes for sentences
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        
        // Get variable value with proper formatting
        protected String getVariableValue(String varName) {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(varName)) {
                throw new RuntimeException("Undefined variable: " + varName);
            }
            
            String value = CoreNodes.GlobalContext.symbolTable.get(varName);
            return formatValue(value);
        }
        
        // Format literal values
        protected String formatLiteral(String literal) {
            if (literal.startsWith("\"") && literal.endsWith("\"")) {
                return literal.substring(1, literal.length() - 1);
            } else if (literal.startsWith("'") && literal.endsWith("'")) {
                return literal.substring(1, literal.length() - 1);
            }
            return literal;
        }
        
        // Helper method to display results with proper formatting
        protected void displayResult(Object result, boolean withNewline) {
            if (result == null) {
                if (withNewline) System.out.println("(empty)");
                else System.out.print("(empty)");
                return;
            }
            
            // Handle array-specific formatting
            if (result instanceof ast.arrays.ArrayCoreNodes.ArrayValue) {
                String output = result.toString();
                if (withNewline) System.out.println(output);
                else System.out.print(output);
            } else if (result instanceof String) {
                String strResult = (String) result;
                
                // Handle array references
                if (ast.arrays.ArrayCoreNodes.isArray(strResult)) {
                    String output = ast.arrays.ArrayCoreNodes.formatArrayForDisplay(strResult);
                    if (withNewline) System.out.println(output);
                    else System.out.print(output);
                    return;
                }
                
                // Format string based on its content
                if (strResult.startsWith("\"") && strResult.endsWith("\"")) {
                    strResult = strResult.substring(1, strResult.length() - 1);
                } else if (strResult.startsWith("'") && strResult.endsWith("'")) {
                    strResult = strResult.substring(1, strResult.length() - 1);
                }
                
                if (withNewline) System.out.println(strResult);
                else System.out.print(strResult);
            } else {
                if (withNewline) System.out.println(result);
                else System.out.print(result);
            }
        }
    }
    
    // Display statement node (without newline)
    public static class DisplayNode extends BaseDisplayNode {
        // Constructor for expression output
        public DisplayNode(ExpressionNodes.ExpressionNode expression) {
            super();
            this.expression = expression;
            this.isVar = false;
        }
        
        // Constructor for variable output
        public DisplayNode(String idName) {
            super();
            this.idName = idName;
            this.isVar = true;
            this.value = idName;
        }
        
        // Constructor for string literal output
        public DisplayNode(String stringLiteral, boolean isLiteral) {
            super();
            this.stringLiteral = stringLiteral;
            this.isVar = false;
            this.value = stringLiteral;
        }
        
        // Constructor for array element access or array function
        public DisplayNode(Object value, boolean isVar, ExpressionNodes.ExpressionNode expr) {
            super();
            this.value = value;
            this.isVar = isVar;
            this.expr = expr;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            validateBaseDisplay(scope);
            
            // Check that display has at least some content
            if (expression == null && idName == null && stringLiteral == null && value == null && expr == null) {
                addError("SEM017: Display statement has no content to display");
            }
        }
        
        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                if (expression != null) {
                    // Handle expression-based display
                    Object result = expression.execute();
                    displayResult(result, false);
                } else if (isVar) {
                    if (value instanceof String && ast.arrays.ArrayCoreNodes.isArray((String) value)) {
                        // Display entire array
                        System.out.print(ast.arrays.ArrayCoreNodes.formatArrayForDisplay((String) value));
                    } else if (value instanceof ast.arrays.ArrayAccessNodes.ArrayAccessNode) {
                        // Display array element
                        Object result = ((ast.arrays.ArrayAccessNodes.ArrayAccessNode) value).execute();
                        displayResult(result, false);
                    } else if (value instanceof String) {
                        // Regular variable
                        System.out.print(getVariableValue((String) value));
                    } else {
                        System.out.print(value != null ? value : "(empty)");
                    }
                } else {
                    // Expression or literal
                    if (expr != null) {
                        Object result = expr.execute();
                        displayResult(result, false);
                    } else if (value instanceof String) {
                        System.out.print(formatLiteral((String) value));
                    } else {
                        System.out.print(value != null ? value : "(empty)");
                    }
                }
            }
            return null;
        }
    }
    
    // Display statement node with newline
    public static class DisplayNLNode extends DisplayNode {
        // Constructor for expression output
        public DisplayNLNode(ExpressionNodes.ExpressionNode expression) {
            super(expression);
        }
        
        // Constructor for variable output
        public DisplayNLNode(String idName) {
            super(idName);
        }
        
        // Constructor for string literal output
        public DisplayNLNode(String stringLiteral, boolean isLiteral) {
            super(stringLiteral, isLiteral);
        }
        
        // Constructor for array element access
        public DisplayNLNode(Object value, boolean isVar, ExpressionNodes.ExpressionNode expr) {
            super(value, isVar, expr);
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            validateBaseDisplay(scope);
            
            // Check that display has at least some content
            if (expression == null && idName == null && stringLiteral == null && value == null && expr == null) {
                addError("SEM017: Display statement has no content to display");
            }
        }
        
        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                if (expression != null) {
                    // Handle expression-based display
                    Object result = expression.execute();
                    displayResult(result, true);
                } else if (isVar) {
                    if (value instanceof String && ast.arrays.ArrayCoreNodes.isArray((String) value)) {
                        // Display entire array
                        System.out.println(ast.arrays.ArrayCoreNodes.formatArrayForDisplay((String) value));
                    } else if (value instanceof ast.arrays.ArrayAccessNodes.ArrayAccessNode) {
                        // Display array element
                        Object result = ((ast.arrays.ArrayAccessNodes.ArrayAccessNode) value).execute();
                        displayResult(result, true);
                    } else if (value instanceof String) {
                        // Regular variable
                        System.out.println(getVariableValue((String) value));
                    } else {
                        System.out.println(value != null ? value : "(empty)");
                    }
                } else {
                    // Expression or literal
                    if (expr != null) {
                        Object result = expr.execute();
                        displayResult(result, true);
                    } else if (value instanceof String) {
                        System.out.println(formatLiteral((String) value));
                    } else {
                        System.out.println(value != null ? value : "(empty)");
                    }
                }
            }
            return null;
        }
    }
    
    // Multi display node (fixed)
    public static class MultiDisplayNode extends CoreNodes.ASTNode {
        private List<Object> items;
        
        public MultiDisplayNode(List<Object> items) {
            super();
            this.items = new ArrayList<>(items);
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            if (items == null || items.isEmpty()) {
                addError("SEM017: Multi-display statement has no items to display");
                return;
            }
            
            for (Object item : items) {
                if (item instanceof ExpressionNodes.ExpressionNode) {
                    // Validate expression
                    ExpressionNodes.ExpressionNode expr = (ExpressionNodes.ExpressionNode) item;
                    expr.validate(scope);
                    propagateErrors(expr);
                } else if (item instanceof String) {
                    String str = (String) item;
                    
                    // Check if it's a quoted literal or a variable reference
                    if (isQuotedLiteral(str)) {
                        // Validate literal format
                        validateStringLiteral(str);
                    } else {
                        // It's a variable reference - validate variable exists
                        if (!scope.isDeclared(str)) {
                            addError("SEM016: Undefined variable in multi-display: '" + str + "'");
                        } else if (!scope.isInitialized(str)) {
                            addError("SEM005: Variable '" + str + "' used before initialization");
                        }
                    }
                }
            }
        }
        
        /**
         * Check if string is a quoted literal
         */
        private boolean isQuotedLiteral(String str) {
            return (str.startsWith("\"") && str.endsWith("\"")) || 
                   (str.startsWith("'") && str.endsWith("'"));
        }
        
        /**
         * Validate string literal format
         */
        private void validateStringLiteral(String literal) {
            if (literal == null) return;
            
            if (literal.startsWith("'") && !literal.endsWith("'")) {
                addError("SEM006: Mismatched single quotes in display literal: '" + literal + "'");
            } else if (literal.startsWith("\"") && !literal.endsWith("\"")) {
                addError("SEM006: Mismatched double quotes in display literal: '" + literal + "'");
            }
            
            // Validate character literal length
            if (literal.startsWith("'") && literal.endsWith("'") && literal.length() != 3) {
                addError("SEM006: Character literal must contain exactly one character: '" + literal + "'");
            }
        }
        
        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.shouldExecute()) {
                return null;
            }
            
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                if (item instanceof ExpressionNodes.ExpressionNode) {
                    Object value = ((ExpressionNodes.ExpressionNode) item).execute();
                    output.append(value);
                } else if (item instanceof String) {
                    String str = (String) item;
                    
                    // If it's a string literal, remove the quotes
                    // Check if it's an array variable
                    if (ast.arrays.ArrayCoreNodes.isArray(str)) {
                        output.append(ast.arrays.ArrayCoreNodes.formatArrayForDisplay(str));
                    } else if (CoreNodes.GlobalContext.symbolTable.containsKey(str)) {
                        // It's a variable
                        String value = CoreNodes.GlobalContext.symbolTable.get(str);
                        
                        // Check if it's an array reference in the symbol table
                        if (value != null && value.startsWith("ARRAY:")) {
                            output.append(ast.arrays.ArrayCoreNodes.formatArrayForDisplay(value.substring(6)));
                        } else {
                            output.append(value != null ? CoreNodes.GlobalContext.getVariableValue(str) : "null");
                        }
                    } else if (str.startsWith("\"") && str.endsWith("\"")) {
                        output.append(str.substring(1, str.length() - 1));
                    } else if (str.startsWith("'") && str.endsWith("'")) {
                        output.append(str.substring(1, str.length() - 1));
                    } else {
                        output.append(str);
                    }
                }
                
                // Add space between items (but not after the last item)
                if (i < items.size() - 1) {
                    output.append(" ");
                }
            }
            System.out.print(output.toString());
            return null;
        }
        
        // Getter for testing
        public List<Object> getItems() { return new ArrayList<>(items); }
    }
    
    // Multi display with newline node (fixed)
    public static class MultiDisplayNLNode extends CoreNodes.ASTNode {
        private List<Object> items;
        
        public MultiDisplayNLNode(List<Object> items) {
            super();
            this.items = new ArrayList<>(items);
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            if (items == null || items.isEmpty()) {
                addError("SEM017: Multi-display statement has no items to display");
                return;
            }
            
            for (Object item : items) {
                if (item instanceof ExpressionNodes.ExpressionNode) {
                    // Validate expression
                    ExpressionNodes.ExpressionNode expr = (ExpressionNodes.ExpressionNode) item;
                    expr.validate(scope);
                    propagateErrors(expr);
                } else if (item instanceof String) {
                    String str = (String) item;
                    
                    // Check if it's a quoted literal or a variable reference
                    if (isQuotedLiteral(str)) {
                        // Validate literal format
                        validateStringLiteral(str);
                    } else {
                        // It's a variable reference - validate variable exists
                        if (!scope.isDeclared(str)) {
                            addError("SEM016: Undefined variable in multi-display: '" + str + "'");
                        } else if (!scope.isInitialized(str)) {
                            addError("SEM005: Variable '" + str + "' used before initialization");
                        }
                    }
                }
            }
        }
        
        /**
         * Check if string is a quoted literal
         */
        private boolean isQuotedLiteral(String str) {
            return (str.startsWith("\"") && str.endsWith("\"")) || 
                   (str.startsWith("'") && str.endsWith("'"));
        }
        
        /**
         * Validate string literal format
         */
        private void validateStringLiteral(String literal) {
            if (literal == null) return;
            
            if (literal.startsWith("'") && !literal.endsWith("'")) {
                addError("SEM006: Mismatched single quotes in display literal: '" + literal + "'");
            } else if (literal.startsWith("\"") && !literal.endsWith("\"")) {
                addError("SEM006: Mismatched double quotes in display literal: '" + literal + "'");
            }
            
            // Validate character literal length
            if (literal.startsWith("'") && literal.endsWith("'") && literal.length() != 3) {
                addError("SEM006: Character literal must contain exactly one character: '" + literal + "'");
            }
        }
        
        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.shouldExecute()) {
                return null;
            }
            
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                if (item instanceof ExpressionNodes.ExpressionNode) {
                    Object value = ((ExpressionNodes.ExpressionNode) item).execute();
                    output.append(value);
                } else if (item instanceof String) {
                    String str = (String) item;
                    
                    // If it's a string literal, remove the quotes
                    if (ast.arrays.ArrayCoreNodes.isArray(str)) {
                        output.append(ast.arrays.ArrayCoreNodes.formatArrayForDisplay(str));
                    } else if (CoreNodes.GlobalContext.symbolTable.containsKey(str)) {
                        // It's a variable
                        String value = CoreNodes.GlobalContext.symbolTable.get(str);
                        
                        // Check if it's an array reference in the symbol table
                        if (value != null && value.startsWith("ARRAY:")) {
                            output.append(ast.arrays.ArrayCoreNodes.formatArrayForDisplay(value.substring(6)));
                        } else {
                            output.append(value != null ? CoreNodes.GlobalContext.getVariableValue(str) : "null");
                        }
                    } else if (str.startsWith("\"") && str.endsWith("\"")) {
                        output.append(str.substring(1, str.length() - 1));
                    } else if (str.startsWith("'") && str.endsWith("'")) {
                        output.append(str.substring(1, str.length() - 1));
                    } else {
                        output.append(str);
                    }
                }
                
                // Add space between items (but not after the last item)
                if (i < items.size() - 1) {
                    output.append(" ");
                }
            }
            System.out.println(output.toString());
            return null;
        }
        
        // Getter for testing
        public List<Object> getItems() { return new ArrayList<>(items); }
    }
    
    // Input statement node
    public static class InputNode extends CoreNodes.ASTNode {
        private String varName;
        
        public InputNode(String varName) {
            super();
            this.varName = varName;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Check variable exists
            if (!scope.isDeclared(varName)) {
                addError("SEM016: Undefined variable in input: '" + varName + "'");
                return;
            }
            
            // Get variable type for input validation
            String varType = scope.lookup(varName);
            
            // Note: Type-specific input validation happens at runtime
            // LETTER type can only accept single character input
            // NUMBER type requires numeric input
            // SENTENCE accepts any string
            // LOGIC accepts boolean values
            
            // Mark variable as will-be-initialized (since input will assign to it)
            scope.markInitialized(varName);
        }
        
        @Override
        public Object execute() {
            if (!CoreNodes.GlobalContext.symbolTable.containsKey(varName)) {
                throw new RuntimeException("Undefined variable: " + varName);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                System.out.print("Enter value for " + varName + ": ");
                String userInput = reader.readLine();
                
                String currentValue = CoreNodes.GlobalContext.symbolTable.get(varName);
                boolean isNumber = currentValue.matches("-?\\d+(\\.\\d+)?");
                boolean isLetter = currentValue.startsWith("'") && currentValue.endsWith("'");
                
                if (isNumber) {
                    try {
                        Double.parseDouble(userInput);
                        CoreNodes.GlobalContext.symbolTable.put(varName, userInput);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Numeric input required for NUMBER variable '" + varName + "'.");
                    }
                } else if (isLetter) {
                    if (userInput.length() != 1) {
                        System.err.println("LETTER input must be a single character.");
                    } else {
                        CoreNodes.GlobalContext.symbolTable.put(varName, "'" + userInput + "'");
                    }
                } else {
                    CoreNodes.GlobalContext.symbolTable.put(varName, "\"" + userInput + "\"");
                }
            } catch (IOException e) {
                System.err.println("Error: Failed to read input.");
            }
            return null;
        }
        
        // Getter for testing
        public String getVarName() { return varName; }
    }
}

// 236