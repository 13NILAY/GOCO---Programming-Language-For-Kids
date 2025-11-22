package ast.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ConditionalNodes {
    
    // If statement node
    public static class IfNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode condition;
        private List<CoreNodes.ASTNode> statements;
        private List<ElseIfNode> elseIfNodes;
        private ElseNode elseNode;
        
        public IfNode(ExpressionNodes.ExpressionNode condition) {
            super();
            this.condition = condition;
            this.statements = new ArrayList<>();
            this.elseIfNodes = new ArrayList<>();
        }
        
        public void addStatement(CoreNodes.ASTNode statement) {
            statements.add(statement);
        }
        
        public void addElseIf(ElseIfNode elseIf) {
            elseIfNodes.add(elseIf);
        }
        
        public void setElse(ElseNode elseNode) {
            this.elseNode = elseNode;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate condition expression
            if (condition != null) {
                condition.validate(scope);
                propagateErrors(condition);
                
                // Check that condition is boolean type
                if (!condition.getExpressionType().equals(CoreNodes.TypeChecker.TYPE_LOGIC)) {
                    addError("SEM008: If condition must be boolean expression, got " + 
                            condition.getExpressionType());
                }
            } else {
                addError("SEM009: If statement missing condition");
            }
            
            // Create new scope for the if block
            CoreNodes.Scope ifScope = scope.enterScope("if");
            
            // Validate all statements inside if block
            for (CoreNodes.ASTNode statement : statements) {
                if (statement != null) {
                    statement.validate(ifScope);
                    propagateErrors(statement);
                }
            }
            
            // Validate all elseif nodes (they use parent scope for condition)
            for (ElseIfNode elseIfNode : elseIfNodes) {
                if (elseIfNode != null) {
                    elseIfNode.validate(scope);
                    propagateErrors(elseIfNode);
                }
            }
            
            // Validate else node if present
            if (elseNode != null) {
                elseNode.validate(scope);
                propagateErrors(elseNode);
            }
        }
        
        @Override
        public Object execute() {
            String conditionValue = condition.execute().toString();
            Stack<CoreNodes.ConditionState> conditionStack = CoreNodes.GlobalContext.getConditionStack();
            CoreNodes.ConditionState parentState = conditionStack.isEmpty() ? null : conditionStack.peek();
            CoreNodes.ConditionState state = new CoreNodes.ConditionState(Boolean.parseBoolean(conditionValue), parentState);
            conditionStack.push(state);
            
            if (CoreNodes.GlobalContext.shouldExecute()) {
                for (CoreNodes.ASTNode statement : statements) {
                    statement.execute();
                }
            }
            
            conditionStack.pop();
            
            // Execute else-if blocks if they exist
            for (ElseIfNode elseIfNode : elseIfNodes) {
                elseIfNode.executeWithPreviousState(state);
            }
            
            // Execute else block if it exists
            if (elseNode != null) {
                elseNode.executeWithPreviousState(state);
            }
            
            return null;
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getCondition() { return condition; }
        public List<CoreNodes.ASTNode> getStatements() { return new ArrayList<>(statements); }
        public List<ElseIfNode> getElseIfNodes() { return new ArrayList<>(elseIfNodes); }
        public ElseNode getElseNode() { return elseNode; }
    }
    
    // ElseIf node
    public static class ElseIfNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode condition;
        private List<CoreNodes.ASTNode> statements;
        
        public ElseIfNode(ExpressionNodes.ExpressionNode condition) {
            super();
            this.condition = condition;
            this.statements = new ArrayList<>();
        }
        
        public void addStatement(CoreNodes.ASTNode statement) {
            statements.add(statement);
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate condition expression
            if (condition != null) {
                condition.validate(scope);
                propagateErrors(condition);
                
                // Check that condition is boolean type
                if (!condition.getExpressionType().equals(CoreNodes.TypeChecker.TYPE_LOGIC)) {
                    addError("SEM008: ElseIf condition must be boolean expression, got " + 
                            condition.getExpressionType());
                }
            } else {
                addError("SEM009: ElseIf statement missing condition");
            }
            
            // Create new scope for the elseif block
            CoreNodes.Scope elseIfScope = scope.enterScope("elseif");
            
            // Validate all statements inside elseif block
            for (CoreNodes.ASTNode statement : statements) {
                if (statement != null) {
                    statement.validate(elseIfScope);
                    propagateErrors(statement);
                }
            }
        }
        
        @Override
        public Object execute() {
            // This shouldn't be called directly
            throw new UnsupportedOperationException("ElseIfNode should be executed via executeWithPreviousState");
        }
        
        public void executeWithPreviousState(CoreNodes.ConditionState prevState) {
            String conditionValue = condition.execute().toString();
            Stack<CoreNodes.ConditionState> conditionStack = CoreNodes.GlobalContext.getConditionStack();
            CoreNodes.ConditionState state = new CoreNodes.ConditionState(!prevState.anyTrue && Boolean.parseBoolean(conditionValue), prevState.parent);
            if (state.condition) prevState.anyTrue = true;
            conditionStack.push(state);
            
            if (CoreNodes.GlobalContext.shouldExecute()) {
                for (CoreNodes.ASTNode statement : statements) {
                    statement.execute();
                }
            }
            
            conditionStack.pop();
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getCondition() { return condition; }
        public List<CoreNodes.ASTNode> getStatements() { return new ArrayList<>(statements); }
    }
    
    // Else node
    public static class ElseNode extends CoreNodes.ASTNode {
        private List<CoreNodes.ASTNode> statements;
        
        public ElseNode() {
            super();
            this.statements = new ArrayList<>();
        }
        
        public void addStatement(CoreNodes.ASTNode statement) {
            statements.add(statement);
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Create new scope for the else block
            CoreNodes.Scope elseScope = scope.enterScope("else");
            
            // Validate all statements inside else block
            for (CoreNodes.ASTNode statement : statements) {
                if (statement != null) {
                    statement.validate(elseScope);
                    propagateErrors(statement);
                }
            }
        }
        
        @Override
        public Object execute() {
            // This shouldn't be called directly
            throw new UnsupportedOperationException("ElseNode should be executed via executeWithPreviousState");
        }
        
        public void executeWithPreviousState(CoreNodes.ConditionState prevState) {
            Stack<CoreNodes.ConditionState> conditionStack = CoreNodes.GlobalContext.getConditionStack();
            CoreNodes.ConditionState state = new CoreNodes.ConditionState(!prevState.anyTrue, prevState.parent);
            conditionStack.push(state);
            
            if (CoreNodes.GlobalContext.shouldExecute()) {
                for (CoreNodes.ASTNode statement : statements) {
                    statement.execute();
                }
            }
            
            conditionStack.pop();
        }
        
        // Getter for testing
        public List<CoreNodes.ASTNode> getStatements() { return new ArrayList<>(statements); }
    }
}
// 104