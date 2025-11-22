package ast.arrays;

import java.util.*;

public class ArrayDeclarationNodes {
    
    /**
     * Node for declaring and initializing arrays
     */
    public static class ArrayDeclarationNode extends ast.nodes.CoreNodes.ASTNode {
        private String type;
        private String name;
        private ast.nodes.ExpressionNodes.ExpressionNode value;
        
        public ArrayDeclarationNode(String type, String name, ast.nodes.ExpressionNodes.ExpressionNode value) {
            this.type = type.toUpperCase();
            this.name = name;
            this.value = value;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Normalize and validate array type
            String normalizedType = ast.nodes.CoreNodes.TypeChecker.normalizeType(type);
            if (!ast.nodes.CoreNodes.TypeChecker.isValidType(normalizedType)) {
                addError("SEM020: Invalid array type '" + type + "'");
                return;
            }
            
            // Check for duplicate declaration
            if (scope.isDeclaredInCurrentScope(name)) {
                addError("SEM020: Duplicate array declaration: '" + name + "'");
            }
            
            // Validate variable name pattern
            if (!isValidVariableName(name)) {
                addError("SEM020: Invalid array name '" + name + "'");
            }
            
            // Validate initializer expression
            if (value != null) {
                value.validate(scope);
                propagateErrors(value);
                
                // Check initializer is array type
                if (!value.getExpressionType().startsWith("ARRAY")) {
                    addError("SEM024: Array initializer must be array expression, got " + 
                            value.getExpressionType());
                } else {
                    // Check type compatibility
                    String elementType = value.getExpressionType().substring(6); // Remove "ARRAY:"
                    if (!elementType.equals(normalizedType)) {
                        addError("SEM024: Array element type mismatch: declared " + normalizedType + 
                                ", initializer " + elementType);
                    }
                }
            }
            
            // Register array in scope if valid
            if (!hasErrors()) {
                scope.declare(name, "ARRAY:" + normalizedType);
                if (value != null) {
                    scope.markInitialized(name);
                }
            }
        }
        
        @Override
        public Object execute() {
            // If the value is an ArrayLiteralNode, pass the type to it
            if (value instanceof ArrayLiteralNode) {
                ((ArrayLiteralNode) value).setType(type);
            }
            
            // Execute the value (static or dynamic array)
            ArrayCoreNodes.ArrayValue array = (ArrayCoreNodes.ArrayValue) value.execute();
            ArrayCoreNodes.putArray(name, array);
            ast.nodes.CoreNodes.GlobalContext.symbolTable.put(name, "ARRAY:" + type + ":" + name);
            return null;
        }
        
        /**
         * Check if variable name is valid
         */
        private boolean isValidVariableName(String name) {
            if (name == null || name.isEmpty()) return false;
            if (!Character.isLetter(name.charAt(0))) return false;
            for (char c : name.toCharArray()) {
                if (!Character.isLetterOrDigit(c) && c != '_') return false;
            }
            return true;
        }
    }
    
    /**
     * Node for array literals [1, 2, 3]
     */
    public static class ArrayLiteralNode extends ast.nodes.ExpressionNodes.ExpressionNode {
        private String type;
        private List<ast.nodes.ExpressionNodes.ExpressionNode> elements;
        
        public ArrayLiteralNode(List<ast.nodes.ExpressionNodes.ExpressionNode> elements) {
            this.elements = elements;
        }
        
        public void setType(String type) {
            this.type = type.toUpperCase();
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate all element expressions
            String inferredType = null;
            for (ast.nodes.ExpressionNodes.ExpressionNode element : elements) {
                element.validate(scope);
                propagateErrors(element);
                
                // Check element type consistency
                String elementType = element.getExpressionType();
                if (inferredType == null) {
                    inferredType = elementType;
                } else if (!elementType.equals(inferredType)) {
                    addError("SEM024: Inconsistent element types in array literal: " + 
                            inferredType + " and " + elementType);
                }
            }
            
            // Use parent type if set, otherwise use inferred type
            String finalType = (type != null) ? type : inferredType;
            if (finalType == null && !elements.isEmpty()) {
                finalType = "UNKNOWN";
            }
            
            // Set expression type
            this.expressionType = "ARRAY:" + finalType;
            
            // If parent type is set, validate compatibility
            if (type != null && inferredType != null && !inferredType.equals("UNKNOWN")) {
                if (!ast.nodes.CoreNodes.TypeChecker.areTypesCompatible(type, inferredType)) {
                    addError("SEM024: Array literal elements incompatible with declared type " + type);
                }
            }
        }
        
        @Override
        public Object execute() {
            ArrayCoreNodes.ArrayValue array = new ArrayCoreNodes.ArrayValue(type);
            for (ast.nodes.ExpressionNodes.ExpressionNode elementExpr : elements) {
                Object rawValue = elementExpr.execute();
                String valueStr = rawValue.toString();
                
                // Validate and add elements
                array.validateArrayElement(valueStr);
                array.addElement(valueStr);
            }
            return array;
        }
    }
    
    /**
     * Node for dynamic array creation with size
     */
    public static class DynamicArrayNode extends ast.nodes.ExpressionNodes.ExpressionNode {
        private String type;
        private ast.nodes.ExpressionNodes.ExpressionNode sizeExpr;
        
        public DynamicArrayNode(String type, ast.nodes.ExpressionNodes.ExpressionNode sizeExpr) {
            this.type = type;
            this.sizeExpr = sizeExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate array type
            String normalizedType = ast.nodes.CoreNodes.TypeChecker.normalizeType(type);
            if (!ast.nodes.CoreNodes.TypeChecker.isValidType(normalizedType)) {
                addError("SEM020: Invalid array type '" + type + "'");
                return;
            }
            
            // Validate size expression
            if (sizeExpr != null) {
                sizeExpr.validate(scope);
                propagateErrors(sizeExpr);
                
                // Check size is numeric
                if (!sizeExpr.getExpressionType().equals("NUMBER")) {
                    addError("SEM023: Array size must be numeric expression, got " + 
                            sizeExpr.getExpressionType());
                }
            } else {
                addError("SEM023: Dynamic array missing size expression");
            }
            
            // Set expression type
            this.expressionType = "ARRAY:" + normalizedType;
        }
        
        @Override
        public Object execute() {
            int size = Integer.parseInt(sizeExpr.execute().toString());
            ArrayCoreNodes.ArrayValue array = new ArrayCoreNodes.ArrayValue(type);
            
            // Initialize with default values
            for (int i = 0; i < size; i++) {
                switch (type) {
                    case "NUMBER": array.addElement("0"); break;
                    case "LETTER": array.addElement("' '"); break;
                    case "SENTENCE": array.addElement("\"\""); break;
                    case "LOGIC": array.addElement("false"); break;
                }
            }
            return array;
        }
    }
    
    /**
     * Node for array assignment (arr = [1,2,3])
     */
    public static class ArrayAssignmentNode extends ast.nodes.CoreNodes.ASTNode {
        private String arrayName;
        private ast.nodes.ExpressionNodes.ExpressionNode valueExpr;
        
        public ArrayAssignmentNode(String arrayName, ast.nodes.ExpressionNodes.ExpressionNode valueExpr) {
            this.arrayName = arrayName;
            this.valueExpr = valueExpr;
        }
        
        @Override
        public void validate(ast.nodes.CoreNodes.Scope scope) {
            clearErrors();
            
            // Check target array exists
            if (!scope.isDeclared(arrayName)) {
                addError("SEM021: Undefined array: '" + arrayName + "'");
                return;
            }
            
            // Check target is array type
            String targetType = scope.lookup(arrayName);
            if (!targetType.startsWith("ARRAY:")) {
                addError("SEM021: Variable '" + arrayName + "' is not an array");
                return;
            }
            
            String targetElementType = targetType.substring(6); // Remove "ARRAY:"
            
            // Validate source expression
            if (valueExpr != null) {
                valueExpr.validate(scope);
                propagateErrors(valueExpr);
                
                // Check source is array type
                if (!valueExpr.getExpressionType().startsWith("ARRAY")) {
                    addError("SEM024: Cannot assign non-array value to array variable");
                } else {
                    // Check type compatibility
                    String sourceElementType = valueExpr.getExpressionType().substring(6);
                    if (!sourceElementType.equals(targetElementType)) {
                        addError("SEM024: Array type mismatch: target " + targetElementType + 
                                ", source " + sourceElementType);
                    }
                }
            } else {
                addError("SEM024: Array assignment missing value expression");
            }
            
            // Mark array as initialized
            if (!hasErrors()) {
                scope.markInitialized(arrayName);
            }
        }
        
        @Override
        public Object execute() {
            Object value = valueExpr.execute();
            if (value instanceof ArrayCoreNodes.ArrayValue) {
                ArrayCoreNodes.ArrayValue array = (ArrayCoreNodes.ArrayValue) value;
                ArrayCoreNodes.putArray(arrayName, array);
                ast.nodes.CoreNodes.GlobalContext.symbolTable.put(arrayName, "ARRAY:" + array.getType() + ":" + arrayName);
            } else {
                throw new RuntimeException("Cannot assign non-array value to array variable: " + arrayName);
            }
            return null;
        }
    }
}
// 174