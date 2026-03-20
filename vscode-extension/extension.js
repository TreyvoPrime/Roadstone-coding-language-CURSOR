const vscode = require("vscode");
const path = require("path");
const fs = require("fs/promises");
const { execFile } = require("child_process");

function execFilePromise(command, args, options, outputChannel) {
  return new Promise((resolve, reject) => {
    execFile(command, args, options, (error, stdout, stderr) => {
      if (stdout && outputChannel) {
        outputChannel.append(stdout);
      }
      if (stderr && outputChannel) {
        outputChannel.append(stderr);
      }
      if (error) {
        reject(error);
        return;
      }
      resolve({ stdout, stderr });
    });
  });
}

async function compileIfNeeded(root, output) {
  const sourceFile = path.join(root, "RoadstoneMain.java");
  const classFile = path.join(root, "RoadstoneMain.class");
  const [sourceStats, classStats] = await Promise.all([
    fs.stat(sourceFile),
    fs.stat(classFile).catch(() => null)
  ]);

  if (classStats && classStats.mtimeMs >= sourceStats.mtimeMs) {
    output.appendLine("Roadstone: interpreter already up to date.");
    return;
  }

  output.appendLine("Roadstone: compiling RoadstoneMain.java...");
  await execFilePromise("javac", [sourceFile], { cwd: root }, output);
}

async function runRoadstone(activeUri) {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    vscode.window.showErrorMessage("Roadstone: open a folder first.");
    return;
  }

  const root = folders[0].uri.fsPath;
  const filePath = activeUri.fsPath;
  if (!filePath.toLowerCase().endsWith(".rd")) {
    vscode.window.showErrorMessage("Roadstone: active file must end with .rd");
    return;
  }

  const output = vscode.window.createOutputChannel("Roadstone");
  output.clear();
  output.show(true);

  try {
    await compileIfNeeded(root, output);
  } catch (error) {
    output.appendLine("");
    output.appendLine("Roadstone: javac failed.");
    output.appendLine(String(error));
    vscode.window.showErrorMessage("Roadstone: javac failed (see Output: Roadstone)");
    return;
  }

  output.appendLine("");
  output.appendLine(`Roadstone: running ${path.relative(root, filePath).replace(/\\/g, "/")}...`);
  try {
    await execFilePromise("java", ["-cp", ".", "RoadstoneMain", filePath], { cwd: root }, output);
    output.appendLine("");
    output.appendLine("Roadstone: done.");
  } catch (error) {
    output.appendLine("");
    output.appendLine("Roadstone: runtime error (see above output).");
    output.appendLine(String(error));
    vscode.window.showErrorMessage("Roadstone: runtime error (see Output: Roadstone)");
  }
}

async function openRoadstoneHelp() {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    vscode.window.showErrorMessage("Roadstone: open a folder first.");
    return;
  }

  const docsPath = path.join(folders[0].uri.fsPath, "ROADSTONE_DOCS.md");
  try {
    const doc = await vscode.workspace.openTextDocument(docsPath);
    await vscode.window.showTextDocument(doc, vscode.ViewColumn.Two, true);
  } catch {
    vscode.window.showErrorMessage("Roadstone: could not open ROADSTONE_DOCS.md");
  }
}

function registerCompletions(context) {
  const keywords = [
    ["local", "Declare a local variable."],
    ["global", "Declare a global variable."],
    ["defi", "Define a function."],
    ["return", "Return from a function."],
    ["if", "Start a conditional block."],
    ["elseif", "Branch inside an if block."],
    ["else", "Fallback branch."],
    ["for", "Start a counted loop."],
    ["while", "Start a while loop."],
    ["then", "Block separator for if/for/EXCEPT."],
    ["loop", "Loop separator for for/while."],
    ["end", "Close a block."],
    ["CLASS", "Define a class."],
    ["construct", "Define a constructor."],
    ["self", "Reference the current instance."],
    ["extends", "Inherit from a class."],
    ["EXCEPT", "Legacy remap or 0.6 exception catch block."],
    ["exoutput", "Emit output after an EXCEPT catch succeeds."]
  ];

  const builtins = [
    ["print(value)", "Print a value."],
    ["len(value)", "Get length of a list, map, string, or object fields."],
    ["keys(map)", "Return map keys."],
    ["type(value)", "Return the Roadstone type name."],
    ["raise(name, message)", "Raise a Roadstone runtime error manually."],
    ["error(name, message)", "Alias of raise()."]
  ];

  const provider = vscode.languages.registerCompletionItemProvider(
    "roadstone",
    {
      provideCompletionItems() {
        const items = [];

        for (const [label, detail] of keywords) {
          const item = new vscode.CompletionItem(label, vscode.CompletionItemKind.Keyword);
          item.detail = detail;
          items.push(item);
        }

        for (const [label, detail] of builtins) {
          const item = new vscode.CompletionItem(label, vscode.CompletionItemKind.Function);
          item.detail = detail;
          items.push(item);
        }

        const exceptSnippet = new vscode.CompletionItem("EXCEPT block", vscode.CompletionItemKind.Snippet);
        exceptSnippet.insertText = new vscode.SnippetString('EXCEPT["${1:FriendlyError}", ${2:ZeroDivisionError}] then\n  ${3:raise("${2:ZeroDivisionError}", "${4:message}")}\nexoutput ${5:"Caught " .. exmessage}\nend');
        exceptSnippet.detail = "Roadstone 0.6 EXCEPT catch block";
        items.push(exceptSnippet);

        return items;
      }
    },
    ".",
    '"'
  );

  context.subscriptions.push(provider);
}

function activate(context) {
  context.subscriptions.push(
    vscode.commands.registerCommand("roadstone.runCurrentFile", async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor) {
        vscode.window.showErrorMessage("Roadstone: no active editor.");
        return;
      }
      await runRoadstone(editor.document.uri);
    }),
    vscode.commands.registerCommand("roadstone.openHelp", async () => {
      await openRoadstoneHelp();
    })
  );

  registerCompletions(context);
}

function deactivate() {}

module.exports = {
  activate,
  deactivate
};
