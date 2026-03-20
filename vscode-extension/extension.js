const vscode = require("vscode");
const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");

function execFilePromise(command, args, options, outputChannel) {
  return new Promise((resolve, reject) => {
    execFile(command, args, options, (err, stdout, stderr) => {
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
  const folder = vscode.workspace.getWorkspaceFolder(activeUri);
  if (!folder) {
    vscode.window.showErrorMessage("Roadstone: open the file inside a workspace folder first.");
    return;
  }
  const root = folder.uri.fsPath;

  const filePath = activeUri.fsPath;
  if (!filePath.toLowerCase().endsWith(".rd")) {
    vscode.window.showErrorMessage("Roadstone: active file must end with .rd");
    return;
  }

  const sourcePath = path.join(root, "RoadstoneMain.java");
  if (!fs.existsSync(sourcePath)) {
    vscode.window.showErrorMessage("Roadstone: could not find RoadstoneMain.java in the workspace root.");
    return;
  }

  const fileRel = path.relative(root, filePath).replace(/\\/g, "/");

  const output = vscode.window.createOutputChannel("Roadstone");
  output.clear();
  output.show(true);
  output.appendLine(`Roadstone: compiling RoadstoneMain.java...`);

  try {
    await execFilePromise("javac", [sourcePath], { cwd: root }, output);
  } catch (e) {
    output.appendLine("");
    output.appendLine("Roadstone: javac failed.");
    output.appendLine(String(e?.message || e));
    vscode.window.showErrorMessage("Roadstone: javac failed (see Output: Roadstone for details)");
    return;
  }

  output.appendLine("");
  output.appendLine(`Roadstone: running ${fileRel}...`);
  try {
    await execFilePromise("java", ["-cp", ".", "RoadstoneMain", fileRel], { cwd: root }, output);
    output.appendLine("");
    output.appendLine("Roadstone: done (no errors).");
  } catch (e) {
    output.appendLine("");
    output.appendLine("Roadstone: runtime error (see above output).");
    output.appendLine(String(e?.message || e));
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
    if (doc.isDirty) {
      await doc.save();
    }
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
