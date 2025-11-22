package ast.arrays;

import java.io.*;

public class ArrayAccessNodes {
    
    /**
     * Node for accessing an array element (arr[index])
     */
    public static class ArrayAccessNode extends ast.nodes.ExpressionNodes.ExpressionNode {
        private String arrayName;
        private ast.nodes.ExpressionNodes.ExpressionNode indexExpr;
        
        public ArrayAccessNode(String arrayName, ast.nodes.ExpressionNodes.ExpressionNode indexExpr) {
            this.arrayName = arrayName;
            this.indexExpr = indexExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate array exists and is array type
            if (!scope.isDeclared(arrayName)) {
                addError("SEM021: Undefined array: '" + arrayName + "'");
                return;
            }
            
            String arrayType = scope.lookup(arrayName);
            if (!arrayType.startsWith("ARRAY:")) {
                addError("SEM021: Variable '" + arrayName + "' is not an array");
                return;
            }
            
            String elementType = arrayType.substring(6); // Remove "ARRAY:"
            
            // Check array is initialized
            if (!scope.isInitialized(arrayName)) {
                addError("SEM005: Array '" + arrayName + "' used before initialization");
            }
            
            // Validate index expression
            if (indexExpr != null) {
                indexExpr.validate(scope);
                propagateErrors(indexExpr);
                
                if (!indexExpr.getExpressionType().equals("NUMBER")) {
                    addError("SEM023: Array index must be numeric expression, got " + 
                            indexExpr.getExpressionType());
                }
            } else {
                addError("SEM023: Array access missing index expression");
            }
            
            // Set expression type to array's element type
            this.expressionType = elementType;
        }
        
        @Override
        public Object execute() {
            if (!ArrayCoreNodes.isArray(arrayName)) {
                throw new RuntimeException("Variable is not an array: " + arrayName);
            }
            
            ArrayCoreNodes.ArrayValue array = ArrayCoreNodes.getArray(arrayName);
            String arrayType = array.getType();
            
            // Calculate index
            int index = (int) Double.parseDouble(indexExpr.execute().toString());
            if (index < 0 || index >= array.length()) {
                throw new RuntimeException("Array index out of bounds: " + index);
            }
            
            String value = array.getElement(index);
            
            // Format value based on array type
            switch (arrayType) {
                case "SENTENCE":
                    return value.startsWith("\"") && value.endsWith("\"") ?
                        value.substring(1, value.length() - 1) : value;
                case "LETTER":
                    return value.startsWith("'") && value.endsWith("'") ?
                        value.substring(1, value.length() - 1) : value;
                case "LOGIC":
                    return value.toLowerCase();
                case "NUMBER":
                default:
                    return value;
            }
        }
    }
    
    /**
     * Node for assigning a value to an array element (arr[index] = value)
     */
    public static class ArrayElementAssignmentNode extends ast.nodes.CoreNodes.ASTNode {
        private String arrayName;
        private ast.nodes.ExpressionNodes.ExpressionNode indexExpr;
        private ast.nodes.ExpressionNodes.ExpressionNode valueExpr;
        
        public ArrayElementAssignmentNode(String arrayName, ast.nodes.ExpressionNodes.ExpressionNode indexExpr, 
                                         ast.nodes.ExpressionNodes.ExpressionNode valueExpr) {
            this.arrayName = arrayName;
            this.indexExpr = indexExpr;
            this.valueExpr = valueExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate array exists and is array type
            if (!scope.isDeclared(arrayName)) {
                addError("SEM021: Undefined array: '" + arrayName + "'");
                return;
            }
            
            String arrayType = scope.lookup(arrayName);
            if (!arrayType.startsWith("ARRAY:")) {
                addError("SEM021: Variable '" + arrayName + "' is not an array");
                return;
            }
            
            String elementType = arrayType.substring(6);
            
            // Check array is initialized
            if (!scope.isInitialized(arrayName)) {
                addError("SEM005: Array '" + arrayName + "' used before initialization");
            }
            
            // Validate index expression
            if (indexExpr != null) {
                indexExpr.validate(scope);
                propagateErrors(indexExpr);
                
                if (!indexExpr.getExpressionType().equals("NUMBER")) {
                    addError("SEM023: Array index must be numeric expression, got " + 
                            indexExpr.getExpressionType());
                }
            } else {
                addError("SEM023: Array element assignment missing index expression");
            }
            
            // Validate value expression
            if (valueExpr != null) {
                valueExpr.validate(scope);
                propagateErrors(valueExpr);
                
                // Check type compatibility
                if (!ast.nodes.CoreNodes.TypeChecker.areTypesCompatible(elementType, valueExpr.getExpressionType())) {
                    addError("SEM024: Cannot assign " + valueExpr.getExpressionType() + 
                            " to " + elementType + " array element");
                }
            } else {
                addError("SEM024: Array element assignment missing value expression");
            }
            
            // Mark array as modified (stays initialized)
        }
        
        @Override
        public Object execute() {
            if (ast.nodes.CoreNodes.GlobalContext.shouldExecute()) {
                if (!ArrayCoreNodes.isArray(arrayName)) {
                    throw new RuntimeException("Variable is not an array: " + arrayName);
                }
                
                ArrayCoreNodes.ArrayValue array = ArrayCoreNodes.getArray(arrayName);
                
                // Calculate the index
                int index;
                try {
                    Object indexObj = indexExpr.execute();
                    if (indexObj instanceof Number) {
                        index = ((Number) indexObj).intValue();
                    } else {
                        String indexStr = indexObj.toString().trim();
                        if (indexStr.startsWith("\"") && indexStr.endsWith("\"")) {
                            indexStr = indexStr.substring(1, indexStr.length() - 1);
                        }
                        double doubleIndex = Double.parseDouble(indexStr);
                        index = (int) doubleIndex;
                        if (doubleIndex != index) {
                            throw new RuntimeException("Array index must be an integer: " + doubleIndex);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Array index must be a number: " + indexExpr.execute().toString());
                }
                
                // Get the value to assign
                Object value = valueExpr.execute();
                String valueStr = value.toString();
                
                // Validate the value for the array type
                if (array.getType().equals("NUMBER") && !ArrayCoreNodes.isNumeric(valueStr)) {
                    throw new RuntimeException("Cannot assign non-numeric value to NUMBER array: " + valueStr);
                }
                
                // Set the element at the specified index
                array.setElement(index, valueStr);
            }
            return null;
        }
    }
    
    /**
     * Node for compound assignment to an array element (arr[index] += value)
     */
    public static class ArrayElementCompoundAssignmentNode extends ast.nodes.CoreNodes.ASTNode {
        private String arrayName;
        private ast.nodes.ExpressionNodes.ExpressionNode indexExpr;
        private String operator;
        private ast.nodes.ExpressionNodes.ExpressionNode valueExpr;
        
        public ArrayElementCompoundAssignmentNode(String arrayName, ast.nodes.ExpressionNodes.ExpressionNode indexExpr,
                                                 String operator, ast.nodes.ExpressionNodes.ExpressionNode valueExpr) {
            this.arrayName = arrayName;
            this.indexExpr = indexExpr;
            this.operator = operator;
            this.valueExpr = valueExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate array exists and is array type
            if (!scope.isDeclared(arrayName)) {
                addError("SEM021: Undefined array: '" + arrayName + "'");
                return;
            }
            
            String arrayType = scope.lookup(arrayName);
            if (!arrayType.startsWith("ARRAY:")) {
                addError("SEM021: Variable '" + arrayName + "' is not an array");
                return;
            }
            
            String elementType = arrayType.substring(6);
            
            // Check array is initialized
            if (!scope.isInitialized(arrayName)) {
                addError("SEM005: Array '" + arrayName + "' used before initialization");
            }
            
            // Validate index expression
            if (indexExpr != null) {
                indexExpr.validate(scope);
                propagateErrors(indexExpr);
                
                if (!indexExpr.getExpressionType().equals("NUMBER")) {
                    addError("SEM023: Array index must be numeric expression, got " + 
                            indexExpr.getExpressionType());
                }
            } else {
                addError("SEM023: Array compound assignment missing index expression");
            }
            
            // Validate value expression
            if (valueExpr != null) {
                valueExpr.validate(scope);
                propagateErrors(valueExpr);
                
                // Validate operation compatibility
                validateCompoundOperation(elementType, valueExpr.getExpressionType());
            } else {
                addError("SEM024: Array compound assignment missing value expression");
            }
        }
        
        /**
         * Validate compound assignment operation compatibility
         */
        private void validateCompoundOperation(String elementType, String valueType) {
            if (operator.equals("+=")) {
                // += can work for NUMBER and SENTENCE arrays
                if (elementType.equals("SENTENCE")) {
                    // SENTENCE arrays can concatenate with any type
                    return; // All types are convertible to string
                } else if (elementType.equals("NUMBER")) {
                    // NUMBER arrays require numeric operands
                    if (!valueType.equals("NUMBER")) {
                        addError("SEM024: Cannot use += with " + valueType + 
                                " on NUMBER array element");
                    }
                } else {
                    // LETTER and LOGIC arrays don't support +=
                    addError("SEM024: Compound assignment '" + operator + 
                            "' not supported for " + elementType + " arrays");
                }
            } else {
                // -=, *=, /=, %= only work for NUMBER arrays
                if (!elementType.equals("NUMBER")) {
                    addError("SEM024: Arithmetic compound assignment '" + operator + 
                            "' only supported for NUMBER arrays");
                } else if (!valueType.equals("NUMBER")) {
                    addError("SEM024: Cannot use " + operator + " with " + valueType + 
                            " on NUMBER array element");
                }
            }
        }
        
        @Override
        public Object execute() {
            if (ast.nodes.CoreNodes.GlobalContext.shouldExecute()) {
                if (!ArrayCoreNodes.isArray(arrayName)) {
                    throw new RuntimeException("Variable is not an array: " + arrayName);
                }
                
                ArrayCoreNodes.ArrayValue array = ArrayCoreNodes.getArray(arrayName);
                
                // Calculate the index
                int index;
                try {
                    Object indexObj = indexExpr.execute();
                    if (indexObj instanceof Number) {
                        index = ((Number) indexObj).intValue();
                    } else {
                        String indexStr = indexObj.toString().trim();
                        if (indexStr.startsWith("\"") && indexStr.endsWith("\"")) {
                            indexStr = indexStr.substring(1, indexStr.length() - 1);
                        }
                        double doubleIndex = Double.parseDouble(indexStr);
                        index = (int) doubleIndex;
                        if (doubleIndex != index) {
                            throw new RuntimeException("Array index must be an integer: " + doubleIndex);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Array index must be a number: " + indexExpr.execute().toString());
                }
                
                // Get the current value
                String currentValue = array.getElement(index).toString();
                String newValueStr = valueExpr.execute().toString();
                
                // Perform the compound operation
                String result;
                if (operator.equals("+=")) {
                    if (array.getType().toUpperCase().startsWith("SENTENCE")) {
                        result = currentValue + newValueStr;
                    } else {
                        double val1 = Double.parseDouble(currentValue);
                        double val2 = Double.parseDouble(newValueStr);
                        result = String.valueOf(val1 + val2);
                    }
                } else {
                    double val1 = Double.parseDouble(currentValue);
                    double val2 = Double.parseDouble(newValueStr);
                    switch (operator) {
                        case "-=": result = String.valueOf(val1 - val2); break;
                        case "*=": result = String.valueOf(val1 * val2); break;
                        case "/=":
                            if (val2 == 0) throw new RuntimeException("Division by zero");
                            result = String.valueOf(val1 / val2);
                            break;
                        case "%=":
                            if (val2 == 0) throw new RuntimeException("Modulus by zero");
                            result = String.valueOf(val1 % val2);
                            break;
                        default: throw new RuntimeException("Unknown operator: " + operator);
                    }
                }
                
                array.setElement(index, result);
            }
            return null;
        }
    }
    
    /**
     * Node for handling input to array elements
     */
    public static class ArrayElementInputNode extends ast.nodes.CoreNodes.ASTNode {
        private String arrayName;
        private ast.nodes.ExpressionNodes.ExpressionNode indexExpr;
        
        public ArrayElementInputNode(String arrayName, ast.nodes.ExpressionNodes.ExpressionNode indexExpr) {
            this.arrayName = arrayName;
            this.indexExpr = indexExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate array exists and is array type
            if (!scope.isDeclared(arrayName)) {
                addError("SEM021: Undefined array: '" + arrayName + "'");
                return;
            }
            
            String arrayType = scope.lookup(arrayName);
            if (!arrayType.startsWith("ARRAY:")) {
                addError("SEM021: Variable '" + arrayName + "' is not an array");
                return;
            }
            
            // Check array is initialized
            if (!scope.isInitialized(arrayName)) {
                addError("SEM005: Array '" + arrayName + "' used before initialization");
            }
            
            // Validate index expression
            if (indexExpr != null) {
                indexExpr.validate(scope);
                propagateErrors(indexExpr);
                
                if (!indexExpr.getExpressionType().equals("NUMBER")) {
                    addError("SEM023: Array index must be numeric expression, got " + 
                            indexExpr.getExpressionType());
                }
            } else {
                addError("SEM023: Array element input missing index expression");
            }
            
            // Note: Input type validation happens at runtime
            // Mark array as modified (stays initialized)
        }
        
        @Override
        public Object execute() {
            if (ast.nodes.CoreNodes.GlobalContext.shouldExecute()) {
                if (!ArrayCoreNodes.isArray(arrayName)) {
                    throw new RuntimeException("Variable is not an array: " + arrayName);
                }
                
                ArrayCoreNodes.ArrayValue array = ArrayCoreNodes.getArray(arrayName);
                int index = (int) Double.parseDouble(indexExpr.execute().toString());
                if (index < 0 || index >= array.length()) {
                    throw new RuntimeException("Array index out of bounds: " + index);
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    System.out.print("Enter value for " + arrayName + "[" + index + "]: ");
                    String userInput = reader.readLine();
                    
                    if (array.getType().equals("LETTER")) {
                        if (userInput.length() != 1) {
                            throw new RuntimeException("LETTER input must be a single character.");
                        }
                        array.setElement(index, "'" + userInput + "'");
                    } else if (array.getType().equals("NUMBER")) {
                        try {
                            Double.parseDouble(userInput);
                            array.setElement(index, userInput);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Numeric input required");
                        }
                    } else {
                        array.setElement(index, userInput);
                    }
                } catch (IOException e) {
                    System.err.println("Error: Failed to read input.");
                }
            }
            return null;
        }
    }
}
// 211