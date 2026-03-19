const vscode = require("vscode");
const path = require("path");
const { exec } = require("child_process");

function execPromise(cmd, options, outputChannel) {
  return new Promise((resolve, reject) => {
    exec(cmd, options, (err, stdout, stderr) => {
      if (stdout && outputChannel) {
        outputChannel.append(stdout);
      }
      if (stderr && outputChannel) {
        outputChannel.append(stderr);
      }
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
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

  const fileRel = path.relative(root, filePath).replace(/\\/g, "/");

  const output = vscode.window.createOutputChannel("Roadstone");
  output.clear();
  output.show(true);
  output.appendLine(`Roadstone: compiling RoadstoneMain.java...`);

  try {
    await execPromise(`javac "${path.join(root, "RoadstoneMain.java")}"`, { cwd: root }, output);
  } catch (e) {
    output.appendLine("");
    output.appendLine("Roadstone: javac failed.");
    output.appendLine(String(e));
    vscode.window.showErrorMessage("Roadstone: javac failed (see Output: Roadstone)");
    return;
  }

  output.appendLine("");
  output.appendLine(`Roadstone: running ${fileRel}...`);
  try {
    await execPromise(`java -cp . RoadstoneMain "${fileRel}"`, { cwd: root }, output);
    output.appendLine("");
    output.appendLine("Roadstone: done (no errors).");
  } catch (e) {
    output.appendLine("");
    output.appendLine("Roadstone: runtime error (see above output).");
    output.appendLine(String(e));
    vscode.window.showErrorMessage("Roadstone: runtime error (see Output: Roadstone)");
  }
}

async function openRoadstoneHelp() {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    vscode.window.showErrorMessage("Roadstone: open a folder first.");
    return;
  }
  const root = folders[0].uri.fsPath;
  const docsPath = path.join(root, "ROADSTONE_DOCS.md");

  try {
    const doc = await vscode.workspace.openTextDocument(docsPath);
    await vscode.window.showTextDocument(doc, vscode.ViewColumn.Two, true);
  } catch (e) {
    vscode.window.showErrorMessage("Roadstone: could not open ROADSTONE_DOCS.md");
  }
}

function activate(context) {
  const runCommand = vscode.commands.registerCommand("roadstone.runCurrentFile", async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showErrorMessage("Roadstone: no active editor.");
      return;
    }
    const doc = editor.document;
    await runRoadstone(doc.uri);
  });

  const helpCommand = vscode.commands.registerCommand("roadstone.openHelp", async () => {
    await openRoadstoneHelp();
  });

  context.subscriptions.push(runCommand, helpCommand);
}

function deactivate() {}

module.exports = {
  activate,
  deactivate
};

