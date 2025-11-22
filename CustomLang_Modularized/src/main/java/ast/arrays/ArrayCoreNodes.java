package ast.arrays;

import java.util.*;

public class ArrayCoreNodes {
    
    /**
     * Class representing an array in our language
     */
    public static class ArrayValue {
        private List<String> elements;
        private String type; // NUMBER, LETTER, SENTENCE, LOGIC
        
        public ArrayValue(String type) {
            this.type = type;
            this.elements = new ArrayList<>();
        }
        
        public ArrayValue(String type, List<String> elements) {
            this.type = type;
            this.elements = new ArrayList<>(elements);
        }
        
        public void addElement(String value) {
            elements.add(value);
        }
        
        public String getElement(int index) {
            if (index < 0 || index >= elements.size()) {
                throw new RuntimeException("Array index out of bounds: " + index);
            }
            return elements.get(index);
        }
        
        public void setElement(int index, String value) {
            if (index < 0 || index >= elements.size()) {
                throw new RuntimeException("Array index out of bounds: " + index);
            }
            elements.set(index, value);
        }
        
        public String pop() {
            if (elements.isEmpty()) {
                throw new RuntimeException("Cannot pop from an empty array");
            }
            return elements.remove(elements.size() - 1);
        }
        
        public void push(String value) {
            elements.add(value);
        }
        
        public int length() {
            return elements.size();
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < elements.size(); i++) {
                String element = elements.get(i);
                switch (type) {
                    case "SENTENCE":
                        if (!element.startsWith("\"") || !element.endsWith("\"")) {
                            element = "\"" + element + "\"";
                        }
                        break;
                    case "LETTER":
                        if (!element.startsWith("'") || !element.endsWith("'")) {
                            element = "'" + element + "'";
                        }
                        break;
                    case "LOGIC":
                        element = element.toLowerCase();
                        break;
                }
                sb.append(element);
                if (i < elements.size() - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        
        public void validateArrayElement(String value) {
            switch (this.type) {
                case "NUMBER":
                    if (!isNumeric(value))
                        throw new RuntimeException("Non-numeric value in NUMBER array: " + value);
                    break;
                case "SENTENCE":
                    // Allow any string
                    break;
                case "LETTER":
                    if (value.length() != 1 && !(value.startsWith("'") && value.endsWith("'") && value.length() == 3))
                        throw new RuntimeException("Invalid LETTER value: " + value);
                    break;
                case "LOGIC":
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                        throw new RuntimeException("Invalid LOGIC value: " + value);
                    break;
            }
        }
    }
    
    /**
     * Static map to store arrays separately from regular variables
     */
    private static HashMap<String, ArrayValue> arrayTable = new HashMap<>();
    
    /**
     * Utility method to check if a variable is an array
     */
    public static boolean isArray(String varName) {
        return arrayTable.containsKey(varName);
    }
    
    /**
     * Get array from table
     */
    public static ArrayValue getArray(String varName) {
        return arrayTable.get(varName);
    }
    
    /**
     * Put array in table
     */
    public static void putArray(String varName, ArrayValue array) {
        arrayTable.put(varName, array);
    }
    
    /**
     * Utility method to check if a string is numeric
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // Remove quotes if present
        if ((str.startsWith("\"") && str.endsWith("\"")) ||
            (str.startsWith("'") && str.endsWith("'"))) {
            str = str.substring(1, str.length() - 1);
        }
        
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Utility method to validate if a value is appropriate for the array type
     */
    public static boolean isValidForType(String type, String value) {
        switch (type) {
            case "NUMBER":
                return isNumeric(value);
            case "LOGIC":
                return value.equals("true") || value.equals("false");
            case "LETTER":
                return value.length() == 1 || (value.startsWith("'") && value.endsWith("'") && value.length() == 3);
            case "SENTENCE":
                return true; // All values are valid for SENTENCE
            default:
                return true; // Unknown types are assumed valid
        }
    }
    
    /**
     * Helper method for displaying array elements
     */
    public static String getArrayElementForDisplay(String arrayName, int index) {
        if (!isArray(arrayName)) {
            throw new RuntimeException("Variable is not an array: " + arrayName);
        }
        
        ArrayValue array = arrayTable.get(arrayName);
        String elementValue = array.getElement(index);
        String arrayType = array.getType();
        
        // Format the element based on array type
        switch (arrayType) {
            case "NUMBER":
                return elementValue;
            case "SENTENCE":
                if (elementValue.startsWith("\"") && elementValue.endsWith("\"")) {
                    return elementValue.substring(1, elementValue.length() - 1);
                }
                return elementValue;
            case "LETTER":
                if (elementValue.startsWith("'") && elementValue.endsWith("'")) {
                    return elementValue.substring(1, elementValue.length() - 1);
                }
                return elementValue;
            case "LOGIC":
                return elementValue.toLowerCase();
            default:
                return elementValue;
        }
    }
    
    /**
     * Format array for display
     */
    public static String formatArrayForDisplay(String arrayName) {
        if (!isArray(arrayName)) {
            throw new RuntimeException("Variable is not an array: " + arrayName);
        }
        
        ArrayValue array = arrayTable.get(arrayName);
        return array.toString();
    }
}
