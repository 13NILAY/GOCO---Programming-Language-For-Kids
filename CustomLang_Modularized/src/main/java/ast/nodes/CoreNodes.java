package ast.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class CoreNodes {

    public static class GlobalContext {
        public static Map<String, Object> currentContext = new HashMap<>();

        // Main symbol table shared across all nodes
        public static HashMap<String, String> symbolTable = new HashMap<>();

        // Stack for tracking condition states in if-elseif-else blocks
        private static Stack<ConditionState> conditionStack = new Stack<>();

        // Helper method to get variable value with proper formatting
        public static String getVariableValue(String varName) {
            if (!symbolTable.containsKey(varName)) {
                throw new RuntimeException("Undefined variable: " + varName);
            }

            String value = symbolTable.get(varName);

            // Try to parse as number if it looks like one
            try {
                if (value.startsWith("ARRAY:")) {
                    return ast.arrays.ArrayCoreNodes.formatArrayForDisplay(varName);
                }

                if (value == null)
                    return "undefined";
                if (value.matches("-?\\d+(\\.\\d+)?")) {
                    return Double.toString(Double.parseDouble(value));
                } else if (value.equals("true") || value.equals("false")) {
                    return value; // Print boolean values directly
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    return value.substring(1, value.length() - 1); // Remove single quotes for letters
                } else {
                    return value.replaceAll("^\"|\"$", ""); // Remove double quotes for sentences
                }
            } catch (NumberFormatException e) {
                return value.replaceAll("^\"|\"$", ""); // Remove quotes
            }
        }

        // Helper method to format literals for display
        public static String formatLiteral(String literal) {
            if (literal.startsWith("\"") && literal.endsWith("\"")) {
                return literal.substring(1, literal.length() - 1);
            } else if (literal.startsWith("'") && literal.endsWith("'")) {
                return literal.substring(1, literal.length() - 1);
            }
            return literal;
        }

        // Utility method to check if current block should be executed
        public static boolean shouldExecute() {
            return conditionStack.isEmpty() || conditionStack.peek().isExecutable();
        }

        public static Stack<ConditionState> getConditionStack() {
            return conditionStack;
        }
    }

    // Class for managing condition states in if-elseif-else blocks
    public static class ConditionState {
        boolean condition;
        boolean anyTrue; // Tracks if any condition in the chain was true
        ConditionState parent;

        public ConditionState(boolean condition, ConditionState parent) {
            this.condition = condition;
            this.anyTrue = condition;
            this.parent = parent;
        }

        public boolean isExecutable() {
            return condition && (parent == null || parent.isExecutable());
        }
    }

    // Scope Management Class for semantic validation
    public static class Scope {
        private Map<String, String> symbols; // variable name -> type
        private Map<String, Boolean> initialized; // track variable initialization
        private Scope parent;
        private String scopeType; // "global", "function", "loop", "conditional", "switch"

        public Scope() {
            this(null, "global");
        }

        public Scope(Scope parent, String scopeType) {
            this.symbols = new HashMap<>();
            this.initialized = new HashMap<>();
            this.parent = parent;
            this.scopeType = scopeType;
        }

        /**
         * Declare a new variable in current scope
         * @param name Variable name
         * @param type Variable type (NUMBER, LETTER, SENTENCE, LOGIC)
         * @return true if successful, false if already declared
         */
        public boolean declare(String name, String type) {
            if (symbols.containsKey(name)) {
                return false; // Already declared in this scope
            }
            symbols.put(name, type);
            initialized.put(name, false);
            return true;
        }

        /**
         * Declare and initialize a variable
         */
        public boolean declareAndInitialize(String name, String type) {
            if (symbols.containsKey(name)) {
                return false;
            }
            symbols.put(name, type);
            initialized.put(name, true);
            return true;
        }

        /**
         * Mark a variable as initialized
         */
        public void markInitialized(String name) {
            if (symbols.containsKey(name)) {
                initialized.put(name, true);
            } else if (parent != null) {
                parent.markInitialized(name);
            }
        }

        /**
         * Check if variable is initialized
         */
        public boolean isInitialized(String name) {
            if (symbols.containsKey(name)) {
                return initialized.get(name);
            } else if (parent != null) {
                return parent.isInitialized(name);
            }
            return false;
        }

        /**
         * Lookup variable type in current and parent scopes
         * @param name Variable name
         * @return Variable type or null if not found
         */
        public String lookup(String name) {
            if (symbols.containsKey(name)) {
                return symbols.get(name);
            } else if (parent != null) {
                return parent.lookup(name);
            }
            return null; // Variable not found
        }

        /**
         * Check if variable exists in any scope
         */
        public boolean isDeclared(String name) {
            return lookup(name) != null;
        }

        /**
         * Check if variable is declared in current scope only
         */
        public boolean isDeclaredInCurrentScope(String name) {
            return symbols.containsKey(name);
        }

        /**
         * Create a child scope
         */
        public Scope enterScope(String scopeType) {
            return new Scope(this, scopeType);
        }

        /**
         * Return to parent scope
         */
        public Scope exitScope() {
            return parent;
        }

        /**
         * Get scope type
         */
        public String getScopeType() {
            return scopeType;
        }

        /**
         * Check if we're inside a loop (for break/continue validation)
         */
        public boolean isInLoop() {
            if (scopeType.equals("loop")) {
                return true;
            } else if (parent != null) {
                return parent.isInLoop();
            }
            return false;
        }

        /**
         * Check if we're inside a switch (for case/default validation)
         */
        public boolean isInSwitch() {
            if (scopeType.equals("switch")) {
                return true;
            } else if (parent != null) {
                return parent.isInSwitch();
            }
            return false;
        }

        /**
         * Get all declared variables in current scope
         */
        public Map<String, String> getSymbols() {
            return new HashMap<>(symbols);
        }
    }

    // Base ASTNode class with error handling
    public static abstract class ASTNode {
        protected List<String> errors;
        protected boolean hasError;
        protected int lineNumber;

        public ASTNode() {
            this.errors = new ArrayList<>();
            this.hasError = false;
            this.lineNumber = -1; // Default, can be set by parser
        }

        /**
         * Execute the node (runtime behavior)
         */
        public abstract Object execute();

        /**
         * Validate the node (semantic analysis)
         * @param scope Current scope for validation
         */
        public abstract void validate(Scope scope);

        /**
         * Add an error message to this node
         * @param message Error description
         */
        protected void addError(String message) {
            hasError = true;
            String errorMsg = lineNumber > 0 
                ? "Line " + lineNumber + ": " + message 
                : message;
            errors.add(errorMsg);
        }

        /**
         * Check if this node has errors
         */
        public boolean hasErrors() {
            return hasError;
        }

        /**
         * Get all errors from this node
         */
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        /**
         * Clear all errors
         */
        protected void clearErrors() {
            errors.clear();
            hasError = false;
        }

        /**
         * Set line number for error reporting
         */
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        /**
         * Get line number
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Propagate errors from child node
         */
        protected void propagateErrors(ASTNode child) {
            if (child.hasErrors()) {
                hasError = true;
                errors.addAll(child.getErrors());
            }
        }
    }

    // Add tracking mechanism for active nodes to the ProgramNode class
    public static class ProgramNode extends ASTNode {
        private List<ASTNode> statements;
        public static List<ASTNode> activeNodes = new ArrayList<>();

        public ProgramNode() {
            this.statements = new ArrayList<>();
        }

        public void addStatement(ASTNode statement) {
            statements.add(statement);
        }

        @Override
        public void validate(Scope scope) {
            clearErrors();
            
            // Create global scope if not provided
            if (scope == null) {
                scope = new Scope();
            }

            // Validate all statements in the program
            for (ASTNode statement : statements) {
                statement.validate(scope);
                propagateErrors(statement);
            }

            // Report all collected errors with better formatting
            if (hasErrors()) {
                System.err.println("=== Semantic Validation Errors ===");
                for (String error : getErrors()) {
                    System.err.println(error);
                }
                System.err.println("Total errors: " + getErrors().size());
                System.err.println("================================");
            }
        }

        @Override
        public Object execute() {
            for (ASTNode statement : statements) {
                // Track active nodes for break/continue
                activeNodes.add(statement);
                try {
                    statement.execute();
                } catch (LoopNodes.BreakException e) {
                    // Break was called but no loop handler found
                    // This is an error condition
                    throw new RuntimeException("Break statement outside of loop");
                } catch (LoopNodes.ContinueException e) {
                    // Continue was called but no loop handler found
                    // This is an error condition
                    throw new RuntimeException("Continue statement outside of loop");
                } finally {
                    activeNodes.remove(statement);
                }
            }
            return null;
        }

        /**
         * Validate and execute if no errors
         */
        public void validateAndExecute() {
            Scope globalScope = new Scope();
            validate(globalScope);
            
            if (!hasErrors()) {
                execute();
            } else {
                System.err.println("Execution aborted due to validation errors.");
            }
        }

        /**
         * Get all statements for testing
         */
        public List<ASTNode> getStatements() {
            return new ArrayList<>(statements);
        }
    }

    // Statement node with terminator (dot)
    public static class StatementNode extends ASTNode {
        private ASTNode statement;

        public StatementNode(ASTNode statement) {
            this.statement = statement;
        }

        @Override
        public void validate(Scope scope) {
            clearErrors();
            
            if (statement != null) {
                statement.validate(scope);
                propagateErrors(statement);
            } else {
                addError("Statement node contains null statement");
            }
        }

        @Override
        public Object execute() {
            statement.execute();
            return null;
        }

        /**
         * Get the wrapped statement
         */
        public ASTNode getStatement() {
            return statement;
        }
    }

    /**
     * Helper class for type checking
     */
    public static class TypeChecker {
        
        /**
         * Valid language types
         */
        public static final String TYPE_NUMBER = "NUMBER";
        public static final String TYPE_LETTER = "LETTER";
        public static final String TYPE_SENTENCE = "SENTENCE";
        public static final String TYPE_LOGIC = "LOGIC";
        public static final String TYPE_ARRAY = "ARRAY";
        public static final String TYPE_UNKNOWN = "UNKNOWN";

        /**
         * Normalize type name to uppercase
         */
        public static String normalizeType(String type) {
            if (type == null) return TYPE_UNKNOWN;
            return type.toUpperCase();
        }

        /**
         * Check if type is valid
         */
        public static boolean isValidType(String type) {
            String normalized = normalizeType(type);
            return normalized.equals(TYPE_NUMBER) || 
                   normalized.equals(TYPE_LETTER) || 
                   normalized.equals(TYPE_SENTENCE) || 
                   normalized.equals(TYPE_LOGIC);
        }

        /**
         * Check if types are compatible for assignment
         */
        public static boolean areTypesCompatible(String targetType, String sourceType) {
            String target = normalizeType(targetType);
            String source = normalizeType(sourceType);
            
            // Same types are always compatible
            if (target.equals(source)) {
                return true;
            }
            
            // NUMBER can accept LETTER (implicit conversion)
            if (target.equals(TYPE_NUMBER) && source.equals(TYPE_LETTER)) {
                return true;
            }
            
            // SENTENCE can accept LETTER (implicit conversion)
            if (target.equals(TYPE_SENTENCE) && source.equals(TYPE_LETTER)) {
                return true;
            }
            
            return false;
        }

        /**
         * Infer type from literal value
         */
        public static String inferTypeFromLiteral(String literal) {
            if (literal == null) return TYPE_UNKNOWN;
            
            // Remove quotes for analysis
            String value = literal.trim();
            
            // Boolean literals
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return TYPE_LOGIC;
            }
            
            // Number literals
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                return TYPE_NUMBER;
            }
            
            // Character literals (single quotes)
            if (value.startsWith("'") && value.endsWith("'")) {
                return TYPE_LETTER;
            }
            
            // String literals (double quotes)
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return TYPE_SENTENCE;
            }
            
            return TYPE_UNKNOWN;
        }
    }
}
// 386