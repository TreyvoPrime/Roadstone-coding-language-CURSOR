# Roadstone 0.6

Roadstone 0.6 is the current version of the language in this repo. This release expands the runtime, adds stronger error handling, and improves the editor/tooling story with a VS Code extension and CLI wrappers.

## 0.6 Update Log

### Added in 0.6

- Catch-style exception handling with:
  - `EXCEPT["NewErrorName", OldErrorName] then ... exoutput ... end`
- Catch output support with:
  - `exoutput`
- Catch context values:
  - `exname`
  - `extarget`
  - `exmessage`
- Manual runtime error helpers:
  - `raise(name, message)`
  - `error(name, message)`
- Example program for the new EXCEPT flow:
  - `examples/except_v06.rd`

### Features available in the current 0.6 language build

- Block-based syntax with `end`
- Comments with `--`
- `if / elseif / else`
- `while ... loop`
- counted `for ... then loop`
- foreach `for item in store loop`
- local/global declarations
- reassignment of declared variables
- functions with `defi`
- write-back returns with `return <paramName>`
- classes with `CLASS`, `construct`, methods, and `extends`
- list literals with `[ ... ]`
- map literals with `{ key: value }`
- indexing support
- `store(...)` storage units
- builtins:
  - `print`
  - `len`
  - `keys`
  - `values`
  - `sort`
  - `push`
  - `contains`
  - `type`
  - `Ask`
  - `Int`
  - `analyze`

### Tooling around 0.6

- `roadstone-cli.js` and `roadstone.cmd` for easier command-line execution
- VS Code extension with:
  - syntax highlighting
  - completions
  - snippets
  - run command for `.rd` files
  - docs command
- updated web editor/runtime files under `web-runner/`

## Run Roadstone

### Wrapper scripts

- PowerShell:
  - `.\run.ps1 examples/hello.rd`
- CMD:
  - `.\run.bat examples\hello.rd`
- CLI wrapper:
  - `.\roadstone.cmd examples\hello.rd`

### Manual

- Compile:
  - `javac RoadstoneMain.java`
- Run:
  - `java -cp . RoadstoneMain examples/hello.rd`

## VS Code

The extension lives in [vscode-extension](C:\Users\trey2\Roadstone-coding-language-CURSOR\vscode-extension).

Build a downloadable package:

```powershell
cd vscode-extension
npm install -g @vscode/vsce
vsce package
```

Install the `.vsix` locally:

```powershell
code --install-extension roadstone-language-0.6.1.vsix
```

## Docs

For the full syntax and behavior sheet, see [ROADSTONE_DOCS.md](C:\Users\trey2\Roadstone-coding-language-CURSOR\ROADSTONE_DOCS.md).

## Example Programs

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
- `examples/except_v06.rd`
