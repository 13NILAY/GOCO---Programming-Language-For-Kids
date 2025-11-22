package ast.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LoopNodes {
    
    // Loop statement node (for while and for loops)
    public static class LoopNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode condition;
        private CoreNodes.ASTNode initialization;
        private CoreNodes.ASTNode update;
        private List<CoreNodes.ASTNode> statements;
        private boolean isBreakTriggered = false;
        private boolean isContinueTriggered = false;
        
        // Original constructor for while loop
        public LoopNode(ExpressionNodes.ExpressionNode condition) {
            super();
            this.condition = condition;
            this.initialization = null;
            this.update = null;
            this.statements = new ArrayList<>();
        }
        
        // New constructor for for loop
        public LoopNode(ExpressionNodes.ExpressionNode condition, CoreNodes.ASTNode initialization, CoreNodes.ASTNode update) {
            super();
            this.condition = condition;
            this.initialization = initialization;
            this.update = update;
            this.statements = new ArrayList<>();
        }
        
        public void addStatement(CoreNodes.ASTNode statement) {
            statements.add(statement);
        }
        
        public void setBreakTriggered(boolean triggered) {
            isBreakTriggered = triggered;
        }
        
        public void setContinueTriggered(boolean triggered) {
            isContinueTriggered = triggered;
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Validate initialization for for-loop (uses parent scope)
            if (initialization != null) {
                initialization.validate(scope);
                propagateErrors(initialization);
            }
            
            // Validate condition expression
            if (condition != null) {
                condition.validate(scope);
                propagateErrors(condition);
                
                // Check that condition is boolean type
                if (!condition.getExpressionType().equals(CoreNodes.TypeChecker.TYPE_LOGIC)) {
                    addError("SEM010: Loop condition must be boolean expression, got " + 
                            condition.getExpressionType());
                }
            } else {
                addError("SEM011: Loop statement missing condition");
            }
            
            // Validate update for for-loop (uses parent scope)
            if (update != null) {
                update.validate(scope);
                propagateErrors(update);
            }
            
            // Create new scope for the loop body
            CoreNodes.Scope loopScope = scope.enterScope("loop");
            
            // Validate all statements inside loop body
            for (CoreNodes.ASTNode statement : statements) {
                if (statement != null) {
                    statement.validate(loopScope);
                    propagateErrors(statement);
                }
            }
        }
        
        @Override
        public Object execute() {
            // Create a snapshot of the current condition state
            Stack<CoreNodes.ConditionState> conditionStack = CoreNodes.GlobalContext.getConditionStack();
            CoreNodes.ConditionState parentState = conditionStack.isEmpty() ? null : conditionStack.peek();
            
            // Execute initialization for for loop
            if (initialization != null && CoreNodes.GlobalContext.shouldExecute()) {
                initialization.execute();
            }
            
            // Reset break and continue flags
            isBreakTriggered = false;
            isContinueTriggered = false;
            
            while (Boolean.parseBoolean(condition.execute().toString())) {
                // Reset condition state for this iteration
                if (!conditionStack.isEmpty() && conditionStack.peek() != parentState) {
                    conditionStack.pop();
                }
                
                // Reset continue flag at the start of each iteration
                isContinueTriggered = false;
                
                // Check if we're in an executable branch
                if (parentState == null || parentState.isExecutable()) {
                    // Execute each statement in the loop body
                    for (CoreNodes.ASTNode statement : statements) {
                        statement.execute();
                        
                        // If break is triggered, exit the loop
                        if (isBreakTriggered) {
                            break;
                        }
                        
                        // If continue is triggered, skip remaining statements
                        if (isContinueTriggered) {
                            break;
                        }
                    }
                    
                    // If break is triggered, exit the loop
                    if (isBreakTriggered) {
                        break;
                    }
                    
                    // Execute the update statement for for loop
                    if (update != null && CoreNodes.GlobalContext.shouldExecute() && !isBreakTriggered) {
                        update.execute();
                    }
                } else {
                    // If parent condition is false, break out
                    break;
                }
                
                // Safety check to prevent infinite loops
                if (!CoreNodes.GlobalContext.shouldExecute()) {
                    break;
                }
            }
            
            // Restore condition state
            if (!conditionStack.isEmpty() && conditionStack.peek() != parentState) {
                conditionStack.pop();
            }
            
            // Reset flags after loop execution
            isBreakTriggered = false;
            isContinueTriggered = false;
            return null;
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getCondition() { return condition; }
        public CoreNodes.ASTNode getInitialization() { return initialization; }
        public CoreNodes.ASTNode getUpdate() { return update; }
        public List<CoreNodes.ASTNode> getStatements() { return new ArrayList<>(statements); }
        public boolean isForLoop() { return initialization != null || update != null; }
    }
    
    // Do-While loop statement node
    public static class DoWhileNode extends CoreNodes.ASTNode {
        private ExpressionNodes.ExpressionNode condition;
        private List<CoreNodes.ASTNode> statements;
        private boolean isBreakTriggered = false;
        private boolean isContinueTriggered = false;
        
        public DoWhileNode() {
            super();
            this.statements = new ArrayList<>();
        }
        
        public void setCondition(ExpressionNodes.ExpressionNode condition) {
            this.condition = condition;
        }
        
        public void addStatement(CoreNodes.ASTNode statement) {
            statements.add(statement);
        }
        
        public void setBreakTriggered(boolean triggered) {
            isBreakTriggered = triggered;
        }
        
        public void setContinueTriggered(boolean triggered) {
            isContinueTriggered = triggered;
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
                    addError("SEM010: Do-while condition must be boolean expression, got " + 
                            condition.getExpressionType());
                }
            } else {
                addError("SEM011: Do-while statement missing condition");
            }
            
            // Create new scope for the do-while body
            CoreNodes.Scope doWhileScope = scope.enterScope("loop");
            
            // Validate all statements inside do-while body
            for (CoreNodes.ASTNode statement : statements) {
                if (statement != null) {
                    statement.validate(doWhileScope);
                    propagateErrors(statement);
                }
            }
        }
        
        @Override
        public Object execute() {
            // Create a snapshot of the current condition state
            Stack<CoreNodes.ConditionState> conditionStack = CoreNodes.GlobalContext.getConditionStack();
            CoreNodes.ConditionState parentState = conditionStack.isEmpty() ? null : conditionStack.peek();
            
            // Reset break and continue flags
            isBreakTriggered = false;
            isContinueTriggered = false;
            
            do {
                // Reset condition state for this iteration
                if (!conditionStack.isEmpty() && conditionStack.peek() != parentState) {
                    conditionStack.pop();
                }
                
                // Reset continue flag at the start of each iteration
                isContinueTriggered = false;
                
                // Check if we're in an executable branch
                if (parentState == null || parentState.isExecutable()) {
                    // Execute each statement in the loop body
                    for (CoreNodes.ASTNode statement : statements) {
                        statement.execute();
                        
                        // If break is triggered, exit the loop
                        if (isBreakTriggered) {
                            break;
                        }
                        
                        // If continue is triggered, skip remaining statements
                        if (isContinueTriggered) {
                            break;
                        }
                    }
                    
                    // If break is triggered, exit the loop
                    if (isBreakTriggered) {
                        break;
                    }
                } else {
                    // If parent condition is false, break out
                    break;
                }
                
                // Safety check to prevent infinite loops
                if (!CoreNodes.GlobalContext.shouldExecute()) {
                    break;
                }
            } while (Boolean.parseBoolean(condition.execute().toString()));
            
            // Restore condition state
            if (!conditionStack.isEmpty() && conditionStack.peek() != parentState) {
                conditionStack.pop();
            }
            
            // Reset flags after loop execution
            isBreakTriggered = false;
            isContinueTriggered = false;
            return null;
        }
        
        // Getters for testing
        public ExpressionNodes.ExpressionNode getCondition() { return condition; }
        public List<CoreNodes.ASTNode> getStatements() { return new ArrayList<>(statements); }
    }
    
    // Break statement node
    public static class BreakNode extends CoreNodes.ASTNode {
        
        public BreakNode() {
            super();
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Check if we're inside a loop context
            if (!scope.isInLoop()) {
                addError("SEM012: Break statement must be inside a loop");
            }
        }
        
        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                // Find the innermost loop and trigger a break
                findInnerLoop(true);
            }
            return null;
        }
        
        private void findInnerLoop(boolean breakFlag) {
            // Search up the call stack to find the innermost loop
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if ((element.getClassName().endsWith("LoopNode") && element.getMethodName().equals("execute"))
                    || (element.getClassName().endsWith("DoWhileNode") && element.getMethodName().equals("execute"))) {
                    
                    // Use reflection to get the executing loop instance
                    try {
                        for (StackTraceElement frame : stackTrace) {
                            if (frame.getMethodName().equals("execute")) {
                                // This is a heuristic approach
                                for (Thread thread : Thread.getAllStackTraces().keySet()) {
                                    if (thread == Thread.currentThread()) {
                                        for (CoreNodes.ASTNode node : CoreNodes.ProgramNode.activeNodes) {
                                            if (node instanceof LoopNode) {
                                                LoopNode loop = (LoopNode) node;
                                                loop.setBreakTriggered(breakFlag);
                                                return;
                                            } else if (node instanceof DoWhileNode) {
                                                DoWhileNode loop = (DoWhileNode) node;
                                                loop.setBreakTriggered(breakFlag);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // If reflection fails, use a simpler approach
                        throw new BreakException();
                    }
                }
            }
            // If no loop found, throw an exception
            throw new RuntimeException("break statement outside of loop");
        }
    }
    
    // Continue statement node
    public static class ContinueNode extends CoreNodes.ASTNode {
        
        public ContinueNode() {
            super();
        }
        
        @Override
        public void validate(CoreNodes.Scope scope) {
            clearErrors();
            
            // Check if we're inside a loop context
            if (!scope.isInLoop()) {
                addError("SEM012: Continue statement must be inside a loop");
            }
        }
        
        @Override
        public Object execute() {
            if (CoreNodes.GlobalContext.shouldExecute()) {
                // Find the innermost loop and trigger a continue
                findInnerLoop(true);
            }
            return null;
        }
        
        private void findInnerLoop(boolean continueFlag) {
            // Search up the call stack to find the innermost loop
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if ((element.getClassName().endsWith("LoopNode") && element.getMethodName().equals("execute"))
                    || (element.getClassName().endsWith("DoWhileNode") && element.getMethodName().equals("execute"))) {
                    
                    // Use reflection to get the executing loop instance
                    try {
                        for (StackTraceElement frame : stackTrace) {
                            if (frame.getMethodName().equals("execute")) {
                                // This is a heuristic approach
                                for (Thread thread : Thread.getAllStackTraces().keySet()) {
                                    if (thread == Thread.currentThread()) {
                                        for (CoreNodes.ASTNode node : CoreNodes.ProgramNode.activeNodes) {
                                            if (node instanceof LoopNode) {
                                                LoopNode loop = (LoopNode) node;
                                                loop.setContinueTriggered(continueFlag);
                                                return;
                                            } else if (node instanceof DoWhileNode) {
                                                DoWhileNode loop = (DoWhileNode) node;
                                                loop.setContinueTriggered(continueFlag);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // If reflection fails, use a simpler approach
                        throw new ContinueException();
                    }
                }
            }
            // If no loop found, throw an exception
            throw new RuntimeException("continue statement outside of loop");
        }
    }
    
    // Exception classes to handle break and continue when reflection fails
    public static class BreakException extends RuntimeException {
        public BreakException() {
            super("Break executed");
        }
    }
    
    public static class ContinueException extends RuntimeException {
        public ContinueException() {
            super("Continue executed");
        }
    }
}

// 118