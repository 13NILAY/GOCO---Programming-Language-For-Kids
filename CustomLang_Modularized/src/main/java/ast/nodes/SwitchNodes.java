package ast.nodes;

import java.util.List;
import java.util.ArrayList;

public class SwitchNodes {
    
    public static class SwitchNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode switchExpr;
        private List<CaseNode> caseNodes;
        private DefaultNode defaultNode;
        
        public SwitchNode(ExpressionNodes.ExpressionNode switchExpr, List<CaseNode> caseNodes, DefaultNode defaultNode) {
            super();
            this.switchExpr = switchExpr;
            this.caseNodes = caseNodes;
            this.defaultNode = defaultNode;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate switch expression
            if (switchExpr != null) {
                switchExpr.validate(scope);
                propagateErrors(switchExpr);
                
                // Get switch expression type for case validation
                String switchExpressionType = switchExpr.getExpressionType();
                
                // Validate that we have at least one case or default
                if ((caseNodes == null || caseNodes.isEmpty()) && defaultNode == null) {
                    addError("SEM013: Switch statement must have at least one case or default block");
                }
                
                // Create scope for switch block
                CoreNodes.Scope switchScope = scope.enterScope("switch");
                
                // Validate all case nodes with switch expression type
                if (caseNodes != null) {
                    for (CaseNode caseNode : caseNodes) {
                        if (caseNode != null) {
                            caseNode.validateWithSwitchType(switchScope, switchExpressionType);
                            propagateErrors(caseNode);
                        }
                    }
                }
                
                // Validate default node if present
                if (defaultNode != null) {
                    defaultNode.validate(switchScope);
                    propagateErrors(defaultNode);
                }
            } else {
                addError("SEM013: Switch statement missing expression");
            }
        }
        
        @Override
        public Object execute() {
            Object switchValue = switchExpr.execute();
            boolean matched = false;
            for (CaseNode cn : caseNodes) {
                if (cn.matches(switchValue)) {
                    cn.execute();
                    matched = true;
                    break; // executes only first matching case
                }
            }
            if (!matched && defaultNode != null) {
                defaultNode.execute();
            }
            return null;
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getSwitchExpr() { return switchExpr; }
        public List<CaseNode> getCaseNodes() { return caseNodes != null ? new ArrayList<>(caseNodes) : new ArrayList<>(); }
        public DefaultNode getDefaultNode() { return defaultNode; }
    }
    
    public static class CaseNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode caseExpr;
        private List<CoreNodes.ASTNode> statements;
        
        public CaseNode(ExpressionNodes.ExpressionNode caseExpr, List<CoreNodes.ASTNode> statements) {
            super();
            this.caseExpr = caseExpr;
            this.statements = statements;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            // This method should not be called directly - use validateWithSwitchType
            clearErrors();
            addError("SEM015: Case node validation requires switch expression type");
        }
        
        /**
         * Validate case node with switch expression type for type consistency checking
         */
        public void validateWithSwitchType(CoreNodes.Scope scope, String switchExpressionType) {
            clearErrors();
            
            // Validate case expression
            if (caseExpr != null) {
                caseExpr.validate(scope);
                propagateErrors(caseExpr);
                
                // Check case expression type matches switch expression type
                String caseExpressionType = caseExpr.getExpressionType();
                
                // Skip type checking if either type is unknown (already has errors)
                if (!caseExpressionType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN) &&
                    !switchExpressionType.equals(CoreNodes.TypeChecker.TYPE_UNKNOWN)) {
                    
                    // Check for exact type match
                    if (!caseExpressionType.equals(switchExpressionType)) {
                        // Check if types are compatible (allow some implicit conversions)
                        if (!CoreNodes.TypeChecker.areTypesCompatible(switchExpressionType, caseExpressionType)) {
                            addError("SEM014: Case expression type " + caseExpressionType + 
                                    " does not match switch expression type " + switchExpressionType);
                        }
                    }
                }
            } else {
                addError("SEM015: Case statement missing expression");
            }
            
            // Create scope for case block
            CoreNodes.Scope caseScope = scope.enterScope("case");
            
            // Validate all statements inside case block
            if (statements != null) {
                for (CoreNodes.ASTNode statement : statements) {
                    if (statement != null) {
                        statement.validate(caseScope);
                        propagateErrors(statement);
                    }
                }
            }
        }
        
        public boolean matches(Object switchValue) {
            Object caseValue = caseExpr.execute();
            
            // Handle LETTER (character comparison)
            if (switchValue instanceof Character && caseValue instanceof Character) {
                return ((Character) switchValue).charValue() == ((Character) caseValue).charValue();
            }
            
            // Handle SENTENCE (string, case-insensitive)
            if (switchValue instanceof String && caseValue instanceof String) {
                return ((String) switchValue).equalsIgnoreCase((String) caseValue);
            }
            
            // Handle NUMBER types (int, double, long, etc.)
            if (switchValue instanceof Number && caseValue instanceof Number) {
                return ((Number) switchValue).doubleValue() == ((Number) caseValue).doubleValue();
            }
            
            // Handle LOGIC (boolean)
            if (switchValue instanceof Boolean && caseValue instanceof Boolean) {
                return ((Boolean) switchValue).booleanValue() == ((Boolean) caseValue).booleanValue();
            }
            
            // Fallback generic equality
            return switchValue.equals(caseValue);
        }
        
        @Override
        public Object execute() {
            for (CoreNodes.ASTNode stmt : statements) {
                stmt.execute();
            }
            return null;
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getCaseExpr() { return caseExpr; }
        public List<CoreNodes.ASTNode> getStatements() { return statements != null ? new ArrayList<>(statements) : new ArrayList<>(); }
    }
    
    public static class DefaultNode extends CoreNodes.ASTNode {
        private List<CoreNodes.ASTNode> statements;
        
        public DefaultNode(List<CoreNodes.ASTNode> statements) {
            super();
            this.statements = statements;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Create scope for default block
            CoreNodes.Scope defaultScope = scope.enterScope("default");
            
            // Validate all statements inside default block
            if (statements != null) {
                for (CoreNodes.ASTNode statement : statements) {
                    if (statement != null) {
                        statement.validate(defaultScope);
                        propagateErrors(statement);
                    }
                }
            }
        }
        
        @Override
        public Object execute() {
            for (CoreNodes.ASTNode stmt : statements) {
                stmt.execute();
            }
            return null;
        }
        
        // Getter for testing
        public List<CoreNodes.ASTNode> getStatements() { return statements != null ? new ArrayList<>(statements) : new ArrayList<>(); }
    }
}

//128