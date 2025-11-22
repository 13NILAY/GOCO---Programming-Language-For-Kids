

# 🟦 GoCo — Programming Language for Kids (10–14)

GoCo is a **beginner-friendly programming language** designed to make coding **easy, visual, and intuitive** for kids aged 10–14.
It features a clean syntax, simple commands, and powerful programming constructs — all crafted to help young learners understand programming concepts quickly and confidently.

---

## 📘 What is GoCo?

GoCo is a **custom-built interpreted programming language** developed using **Java + JavaCC**, featuring:

* A full **lexer → parser → AST → execution** pipeline
* Kid-friendly syntax with mandatory `.` terminators
* Case-insensitive commands
* Real-time execution inside a **custom Electron-based IDE**

---

## ✨ Key Features

### 🧮 **Beginner-Friendly Data Types**

* `NUMBER` – integers & decimals
* `SENTENCE` – strings
* `LETTER` – single characters
* `LOGIC` – true/false values

---

### 🖥️ **Input & Output**

* `DISPLAY()` → prints output
* `DISPLAYNL()` → prints with newline
* `INPUT()` → accepts typed input

---

### 🔁 **Looping Constructs**

* `loop(condition)` → like `while`
* `do { ... } loop(condition).` → like `do-while`
* `loop(INIT TILL CONDITION, UPDATE)` → custom `for` loop syntax

---

### 🔍 **Conditional Logic**

* `if { }`
* `elseif { }`
* `else { }`

---

### 📦 **Arrays with Built-In Functions**

* `PUSH(arr, val)`
* `POP(arr)`
* `GET(arr, index)`
* `SET(arr, index, value)`
* `LENGTH(arr)`

Supports:

* dynamic arrays (`NEW TYPE[size]`)
* literals (`[10, 20, 30]`)

---

### 🔀 **Switch-Case Support**

Works with:

* NUMBER
* SENTENCE
* LETTER
* LOGIC

---

## 📂 Project Structure

```
GOCO/
└── CustomLang/
    ├── src/
    │   └── main/
    │       └── java/
    │           └── parser/
    │               ├── MyLanguageParser.jj      <-- JavaCC grammar file
    │               ├── ASTNodes.java            <-- All AST node classes
    │               ├── ASTNodesArray.java       <-- Array-related AST logic
    │               ├── ASTNodesNormalError.java <-- Error handling
    │               ├── run.bat                  <-- Script to run code
    │               └── *.java                   <-- Auto-generated + manual files
```

---

## 📸 Screenshots (Add Yours Here)

> 🖼️ **Add your language screenshots below this line.**
> Replace the placeholders with actual image paths from repository uploads.

<img width="1304" height="650" alt="image" src="https://github.com/user-attachments/assets/c4a6ae05-d9e6-4e98-b2ba-83398055f4a7" />
<img width="1305" height="656" alt="image" src="https://github.com/user-attachments/assets/6ffa54e2-54dc-4898-92f4-9861dfca49cd" />



## 🚀 How to Run GoCo Locally

### **Prerequisites**

* Java Installed
* VS Code Installed
* JavaCC Installed

---

### **Steps**

1️⃣ **Open the GoCo project in VS Code**

```
File → Open Folder → Select CustomLang/
```

2️⃣ **Navigate to parser folder**

```
cd CustomLang/src/main/java/parser
```

3️⃣ **Generate parser code**

```
javacc MyLanguageParser.jj
```

4️⃣ **Compile the Java files**

```
javac *.java
```

5️⃣ **Run a GoCo program**

```
./run.bat test.goco
```

> Make sure your `.goco` file is inside the `parser` folder before running.

---

## 🧠 Example GoCo Program

```
NUMBER x = 10.
NUMBER y = 5.

IF(x > y){
    DISPLAY("X is greater").
}else{
    DISPLAY("Y is greater").
}
```

---

## 🧩 About the Project

GoCo was built to **reduce the learning curve** for kids starting programming.
It provides a clean and structured environment where beginners learn:

* Step-by-step logic
* Syntax discipline
* Problem-solving
* Computational thinking

Its IDE and language together form a complete, fun learning ecosystem.

---

## 👥 Contributors

| Name               | Role                                                  |
| ------------------ | ----------------------------------------------------- |
| **Nilay Rathod**   | Language Design, Lexer, Parser, AST, Execution Engine |
| **Mitansh Kanani** | Language + IDE Integration                            |
| **Milan Haria**    | ElectronJS-based IDE Development                      |

---

## 📜 License & Copyright

This project is officially **registered and copyrighted** under the Government of India.

---

