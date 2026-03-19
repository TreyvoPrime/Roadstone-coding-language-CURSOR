# Roadstone (v0) prototype

This repo currently contains a **minimal Roadstone interpreter prototype** written in **Java** (so you can run it immediately with `javac`).

## Run

1. Run (wrapper scripts)
   - PowerShell: `.\run.ps1 examples/hello.rd`
   - CMD: `.\run.bat examples\hello.rd`

2. Or manually
   - Compile: `javac RoadstoneMain.java`
   - Run: `java -cp . RoadstoneMain examples/hello.rd`

## Supported syntax in this v0

- No semicolons
- Block terminator: `end`
- Comments: `-- ...`
- `if <cond> then ... elseif <cond> then ... else ... end` (no colon after `then`/`else`)
- `for <count_expr> then loop ... end` (defines local `i` from `1..count`)
- `while <cond> loop ... end`
- Variables
  - `local x = ...` makes `x` local
  - `x = ...` assigns to an existing local if present, otherwise to a global
- Functions
  - `defi name(a, b) ... end`
  - `return` works
-  **Write-back return (your rule):** `return <paramName>` updates the caller’s argument variable when that argument was an identifier lvalue
- Classes (minimal)
  - `CLASS Name(field1, field2, ...) ... end`
  - `construct(p1, p2, ...) ... end` uses `self.<field> = ...`
  - Methods: `defi methodName(self, ...) ... end`
  - Instantiate by calling the class like a function: `local obj = Name(arg1, arg2)`
- Inheritance (methods only for v0)
  - `CLASS Child(...) extends Parent` enables inherited method lookup
- Lists / Maps / Indexing
  - List literal: `[expr1, expr2, ...]`
  - Map literal: `{ keyExpr: valueExpr, ... }`
  - Indexing: `obj[index]` (1-based for lists)
- Error remapping
  - Runtime errors: use `EXCEPT["NewErrorName", OldErrorName]` inside a block to rename matching runtime errors
  - Lexer/Parser errors: since parsing happens before execution, Roadstone v0 remaps them by scanning the source text for `EXCEPT[...]`
  - Example: `EXCEPT["SigmaError", ZeroDivisionError]` renames `ZeroDivisionError` to `SigmaError`

## Examples

- `examples/hello.rd`
- `examples/if_test.rd`
- `examples/loops.rd`

- `examples/return_writeback.rd`
- `examples/return_normal.rd`
- `examples/return_global_writeback.rd`
- `examples/class_test.rd`
- `examples/class_inherit_test.rd`
- `examples/list_map_test.rd`
- `examples/builtins_test.rd`
- `examples/except_test.rd`
- `examples/except_index_test.rd`
- `examples/except_parse_test.rd`

