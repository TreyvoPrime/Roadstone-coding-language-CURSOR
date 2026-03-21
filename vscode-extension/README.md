# Roadstone VS Code Extension

This extension adds first-pass Roadstone authoring support in VS Code:

- syntax highlighting for `.rd` files
- keyword and builtin completions
- snippets for common Roadstone blocks
- `Roadstone: Run Current .rd file`
- `Roadstone: Open Help (Docs)`

## What you need

The extension needs access to a Roadstone runtime. It can discover one in either of these ways:

1. Put `roadstone.cmd` or `roadstone-cli.js` somewhere inside your Roadstone workspace.
2. Set `Roadstone > Runtime Path` in VS Code settings to:
   - a `roadstone.cmd` file
   - a `roadstone-cli.js` file
   - or a folder containing either one

The current repo already includes `roadstone.cmd` and `roadstone-cli.js` at the workspace root.

## Running Roadstone

Open a `.rd` file and then use one of these:

- `F6`
- Command Palette -> `Roadstone: Run Current .rd file`
- the editor title menu command

The extension runs Roadstone in the VS Code task terminal, which means interactive programs using `Ask(...)` can still prompt for input.

## Opening Docs

Use Command Palette -> `Roadstone: Open Help (Docs)` or press `Ctrl+Shift+H` while editing a Roadstone file.

The extension looks for `ROADSTONE_DOCS.md` in your workspace or beside the configured runtime.

## Packaging A Downloadable `.vsix`

From this `vscode-extension` folder:

```powershell
npm install -g @vscode/vsce
vsce package
```

That creates a `.vsix` file you can install in VS Code with:

```powershell
code --install-extension roadstone-language-0.6.1.vsix
```

## Dev Mode

1. Open `C:\Users\trey2\Roadstone-coding-language-CURSOR\vscode-extension` in VS Code.
2. Press `F5` to launch an Extension Development Host.
3. In the new window, open a Roadstone workspace and run a `.rd` file.
