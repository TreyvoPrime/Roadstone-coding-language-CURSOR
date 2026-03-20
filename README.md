# Roadstone (v0.5) prototype

This repo currently contains a **minimal Roadstone interpreter prototype** written in **Java** (so you can run it immediately with `javac`).

## Run

1. Run (wrapper scripts)
   - PowerShell: `.\run.ps1 examples/hello.rd`
   - CMD: `.\run.bat examples\hello.rd`

2. Or manually
   - Compile: `javac RoadstoneMain.java`
   - Run: `java -cp . RoadstoneMain examples/hello.rd`

## VS Code (optional)
This repo also includes a tiny VS Code extension under `vscode-extension/` that adds:
- `Roadstone: Run Current .rd file`

To try it quickly:
- Open `vscode-extension/` in a VS Code Extension Development Host (use `F5` from the Extensions panel)
- Open any `*.rd` file from this repo in that host window
- Run the command from Command Palette

## Roadstone Docs
For a full syntax + behavior sheet, see `ROADSTONE_DOCS.md`.

## Supported syntax in this v0.5

- No semicolons
- Block terminator: `end`
- Comments: `-- ...`
- `if <cond> then ... elseif <cond> then ... else ... end` (no colon after `then`/`else`)
- `for <count_expr> then loop ... end` (defines local `i` from `1..count`)
- `for item in store loop ... end` and `for key, value in store loop ... end`
- `while <cond> loop ... end`
- Variables
  - `local x = ...` makes `x` local
  - `global x = ...` makes `x` global
  - `x = ...` updates an existing `local` or `global` (you must declare with `local` or `global` first)
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
- Unified storage units
  - List literal: `[expr1, expr2, ...]`
  - Map literal: `{ keyExpr: valueExpr, ... }`
  - Store literal: `store("name"; "Roadstone", "version"; 0.5)`
  - Indexing: `obj[index]`
- Input helpers
  - `Ask("What is your opinion?")`
  - `Ask(Int)("How many players?")`
- Expanded builtins
  - `len`, `keys`, `values`, `sort`, `push`, `contains`, `type`
- Networking helper
  - `analyze("ping", "127.0.0.1")`
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
- `examples/roadstone_0_5_test.rd`
- `examples/except_test.rd`
- `examples/except_index_test.rd`
- `examples/except_parse_test.rd`
