# Roadstone 0.6

This repo contains the Roadstone interpreter in Java, a rebuilt orange-black web IDE, and a VS Code extension for running and authoring `.rd` files.

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

## Supported syntax in 0.6

- No semicolons
- Block terminator: `end`
- Comments: `-- ...`
- `if <cond> then ... elseif <cond> then ... else ... end` (no colon after `then`/`else`)
- `for <count_expr> then loop ... end` (defines local `i` from `1..count`)
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
- Lists / Maps / Indexing
  - List literal: `[expr1, expr2, ...]`
  - Map literal: `{ keyExpr: valueExpr, ... }`
  - Indexing: `obj[index]` (1-based for lists)
- Error handling
  - Legacy remap still works: `EXCEPT["NewErrorName", OldErrorName]`
  - New 0.6 catch block: `EXCEPT["NewErrorName", OldErrorName] then ... exoutput ... end`
  - Manual runtime errors: `raise(name, message)` or `error(name, message)`
  - `exoutput` can use `exname`, `extarget`, and `exmessage` after a catch succeeds

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

