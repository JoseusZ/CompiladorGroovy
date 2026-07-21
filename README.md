# CompiladorGroovy — AST-based Tree-Walking Interpreter for a Groovy Subset

An educational implementation of a **compiler front-end** (lexer + recursive-descent parser) that produces an **Abstract Syntax Tree**, paired with a **tree-walking interpreter** that executes the AST directly via `NodoAST.ejecutar(env)`. No bytecode, no virtual machine — **the AST is both the compiled representation and the runtime**.

Architecturally this is the same model used by teaching compilers like *Crafting a Compiler* and modern JS transpilers in dev mode: source → tokens → AST → walk the tree.

> **Status:** Educational / WIP. The frontend (lexer + parser + AST) is the most complete part. The interpreter executes a subset of statements; advanced constructs (`if`, `while`, `for`, `return`, classes, methods, arrays) are currently parsed but not yet executed.

---

## ✨ Features

| Phase | Highlights |
|---|---|
| **Lexical analysis** | Regex-based tokenizer, line/block comments, strings (single/double/triple quoted), numbers (int / decimal / hex / scientific), reserved-word recognition, detailed error reporting. |
| **Parsing** | Recursive-descent parser following a documented BNF grammar, lookahead support, full operator precedence, AST construction for every recognized construct. |
| **Interpretation** | Post-order AST walker, environment-based symbol table, dynamic arithmetic (Groovy duck-typing), `print` / `println` evaluation. |

---

## 📂 Project Structure

```
CompiladorGroovy-main/
├── input/
│   └── datos_entrada.txt        # Source program to compile
├── output/
│   └── tokens_verticales.txt    # Lexer output (consumed by the parser)
├── scr/
│   ├── Main.groovy              # Entry point — orchestrates the 3 phases
│   ├── AnalyzerLex.groovy       # Phase 1 · Lexer
│   ├── AnalyzerParser.groovy    # Phase 2 · Parser + AST node definitions
│   ├── AST.groovy               # Core AST node classes
│   ├── Environment.groovy       # Symbol table / variable memory
│   └── gramatica.txt            # BNF grammar specification
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

- **Groovy** ≥ 3.x installed and on `PATH` (`groovy --version`).

### Run

```bash
# from the project root
groovy scr/Main.groovy
```

The compiler reads `input/datos_entrada.txt` and runs the three phases sequentially, printing diagnostics to stdout.

### ⚠️ Path Configuration

`Main.groovy` currently hardcodes the path:

```groovy
String rutaBase = 'C:/Users/PC23-LC1/Documents/Compilador'
```

Update **line 9** of [`scr/Main.groovy`](scr/Main.groovy) to point to your local project root, otherwise the lexer will fail to locate the input file.

```groovy
String rutaBase = '<your-absolute-path>/CompiladorGroovy-main'
```

---

## 🔄 How It Works

```text
input/datos_entrada.txt
        │
        ▼
┌────────────────────────────┐
│ Phase 1 · LEXICAL ANALYSIS │   AnalyzerLex.groovy
│  Regex tokenizer           │   • strips comments & strings
│  Reserved-word table       │   • numbers / identifiers / operators
│  → output/tokens_…txt      │
└────────────────────────────┘
        │
        ▼
┌────────────────────────────┐
│ Phase 2 · PARSING + AST    │   AnalyzerParser.groovy
│  Recursive-descent parser  │   • lookahead, full precedence
│  Builds NodoAST tree       │   • validates syntax
└────────────────────────────┘
        │
        ▼
┌────────────────────────────┐
│ Phase 3 · INTERPRETATION   │   NodoAST.ejecutar(env)
│  Post-order AST walker     │   • Environment = variable memory
│  Console output            │   • executes validated programs
└────────────────────────────┘
```

---

## 🔍 Phase Details

### Phase 1 — Lexical Analysis

Implemented in [`scr/AnalyzerLex.groovy`](scr/AnalyzerLex.groovy). The lexer iterates over the input file line-by-line and applies a single combined regex that captures tokens in priority order:

1. Multi-line strings (`"""..."""` / `'''...'''`)
2. Single-line strings (`"..."` / `'...'`)
3. Invalid identifiers (e.g. `123abc`)
4. Numbers — hex, decimal with scientific notation, plain integers
5. Identifiers
6/7. Composite operators (`<=>`, `==~`, `+=`, `<<`, `?:`, `->`, …)
8. Simple operators (`+ - * / = < >` …)
9. Delimiters `() , ; : [ ] { }`

It also:

- Strips **line comments** (`//`) and **block comments** (`/* … */` with cross-line state).
- Skips a leading shebang (`#!`) on line 1.
- Recognizes Groovy **reserved words** and prefixes them with `PR_` (e.g. `int → PR_INT`).
- Emits each token as `Linea N | <TIPO, valor>` for downstream parsing.

### Phase 2 — Parsing & AST Construction

Implemented in [`scr/AnalyzerParser.groovy`](scr/AnalyzerParser.groovy). A **recursive-descent parser** with one-token lookahead consumes the textual token file produced by Phase 1 and constructs an **Abstract Syntax Tree**.

Grammar follows the BNF in [`scr/gramatica.txt`](scr/gramatica.txt). The expression hierarchy enforces correct precedence:

```
OR  →  AND  →  Equality  →  Relational  →  Additive  →  Multiplicative  →  Factor
```

Supported statements:

- `if / else`
- `while (cond) { … }`
- `for (init; cond; step) { … }`
- Variable declaration with explicit type (`int`, `def`, `boolean`, …)
- Assignment (`=`, `+=`, `-=`)
- Array access (`id[exp]`)
- Array literal (`[exp, exp, …]`)
- `return`, `print`, `println`
- Method calls and class declarations *(parsed-only for now)*
- Nested blocks `{ … }`

Errors are reported with line numbers and abort the parse via a thrown `Exception`.

### Phase 3 — Interpretation

The AST walker calls `NodoAST.ejecutar(env)` on the root node. The [`Environment`](scr/Environment.groovy) class provides a simple symbol table backed by a `Map<String, Object>`.

The interpreter currently **fully executes**:

- Variable declaration & assignment
- Arithmetic expressions (`+ - * /`)
- String concatenation
- Boolean / numeric / string literals
- `print` / `println`

---

## 📐 Grammar (BNF)

The full grammar is documented in [`scr/gramatica.txt`](scr/gramatica.txt). Excerpt:

```bnf
<programa>           ::= <lista_declaraciones> eof
<sentencia>         ::= <if_stmt> | <while_stmt> | <for_stmt>
                      | <variable_decl> | <arreglo_decl> | <asignacion_stmt>
                      | <retorno_stmt> | <impresion_stmt>
                      | <llamada_metodo> | <bloque>

<expresion>         ::= <exp_logica_or>
<exp_logica_or>     ::= <exp_logica_and> ("||" <exp_logica_and>)*
<exp_logica_and>    ::= <exp_igualdad>    ("&&" <exp_igualdad>)*
<exp_igualdad>      ::= <exp_relacional>  (("==" | "!=") <exp_relacional>)*
<exp_relacional>    ::= <exp_aritmetica>  (("<" | ">" | "<=" | ">=") <exp_aritmetica>)*
<exp_aritmetica>    ::= <termino>         (("+" | "-") <termino>)*
<termino>           ::= <factor>          (("*" | "/" | "%") <factor>)*
<factor>            ::= "(" <expresion> ")" | <llamada_metodo> | <literal>
                      | identificador | identificador "[" <expresion> "]"
```

---

## 💡 Example

**Input** — [`input/datos_entrada.txt`](input/datos_entrada.txt):

```groovy
int a = 1
int b = 2
int suma = a + b
print(suma)
```

**Run:**

```bash
groovy scr/Main.groovy
```

**Output:**

```
3
```

---

## 🧱 AST Node Hierarchy

Defined jointly across [`scr/AST.groovy`](scr/AST.groovy) and [`scr/AnalyzerParser.groovy`](scr/AnalyzerParser.groovy):

| Class | Role |
|---|---|
| `NodoAST` (abstract) | Base interface with `ejecutar(Environment)` |
| `NodoBloque` | Sequence of statements |
| `NodoNumero` | Numeric literal |
| `NodoCadena` | String literal |
| `NodoVariable` | Identifier read |
| `NodoAsignacion` | Variable write |
| `NodoOperacionBinaria` | `+ - * /` arithmetic |
| `NodoPrint` | `print` / `println` evaluation |
| `NodoVacio` | No-op placeholder (for parsed-but-unexecuted features) |

---

## ⚠️ Current Limitations

| Area | Status |
|---|---|
| `if / else`, `while`, `for`, `return` | **Parsed** but currently executed as no-ops (`NodoVacio`). |
| Arrays (`id[i]`, `[…]`) | **Parsed** but reading/writing is a placeholder. |
| Method calls & classes | **Parsed** but not executed. |
| Scoping | `Environment` is a single global `Map` — no per-block scopes. |
| Lexer→Parser coupling | The parser re-reads the textual file instead of consuming tokens directly — fragile if the lexer format changes. |
| Error recovery | On error, parsing aborts entirely (no panic-mode recovery). |
| Path configuration | Hardcoded path in `Main.groovy` must be edited per machine. |

---

## 🛣️ Next Steps

- [ ] Implement `ejecutar()` semantics for `if/else`, `while`, `for`, `return`.
- [ ] Add full array indexing read/write in the interpreter.
- [ ] Introduce a scoped symbol table (`Environment` chain) for blocks and methods.
- [ ] Decouple the parser from the textual token file (consume tokens directly in memory).
- [ ] Deduplicate AST node definitions between `AST.groovy` and `AnalyzerParser.groovy`.
- [ ] Add panic-mode error recovery and richer diagnostics.
- [ ] Author JUnit / Spock test suites per phase.
- [ ] Externalize the input path via CLI argument or configuration file.

---

## 📚 Concepts Demonstrated

This is a teaching project. It touches every classic compiler topic, and the architecture itself is intentionally explicit about *which* model we are following.

### Compiler / interpreter models compared

| Model | Example | Front-end | Back-end |
|---|---|---|---|
| Pure interpreter | CPython (default), Ruby MRI | None | Reads source directly |
| **Compiler + tree-walking interpreter** | **This project**, `tsc-node`, teaching compilers | Lexer + Parser → AST | Walks the AST directly |
| Compiler + bytecode VM | Java (JVM), CPython (later), Groovy | Lexer + Parser → AST | AST → Bytecode → VM |

### Concepts covered

- **Lexical analysis** — regular expressions, longest-match, priority, token classification.
- **Parsing** — recursive descent, lookahead, operator precedence climbing, BNF-to-code mapping.
- **AST construction** — every grammar rule produces a typed tree node.
- **Interpretation / tree-walking** — visitor-less execution via polymorphic `ejecutar()`. No IR, no bytecode emission — semantics live inside the AST itself.
- **Symbol tables** — runtime variable storage using an `Environment` object passed through the tree.
- **Pipeline orchestration** — phase coordination and error propagation in `Main`.
- **Separation of concerns (front-end vs. back-end)** — the AST is the *contract* between the two halves; any new back-end (bytecode VM, stack machine, LLVM IR) could be slotted in without touching the lexer or parser.

---

## 📄 License

Internal / educational use. Add a license (e.g. MIT) before redistribution.

---

<p align="center"><sub>Built for learning. Three phases, one language, zero dependencies.</sub></p>
