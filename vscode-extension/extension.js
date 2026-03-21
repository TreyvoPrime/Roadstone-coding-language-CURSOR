const vscode = require("vscode");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");

const DOCUMENT_SELECTOR = { language: "roadstone", scheme: "file" };
const RUNTIME_FILENAMES = ["roadstone-cli.js", "roadstone.cmd"];

function statSafe(targetPath) {
  return fs.stat(targetPath).catch(() => null);
}

async function exists(targetPath) {
  return Boolean(await statSafe(targetPath));
}

function expandConfiguredPath(rawValue, uri) {
  if (!rawValue || !rawValue.trim()) {
    return null;
  }

  const workspaceFolder = uri ? vscode.workspace.getWorkspaceFolder(uri) : null;
  let expanded = rawValue.trim();

  if (workspaceFolder) {
    expanded = expanded.replace(/\$\{workspaceFolder\}/g, workspaceFolder.uri.fsPath);
  }

  if (expanded.startsWith("~")) {
    expanded = path.join(os.homedir(), expanded.slice(1));
  }

  if (path.isAbsolute(expanded)) {
    return path.normalize(expanded);
  }

  if (workspaceFolder) {
    return path.normalize(path.join(workspaceFolder.uri.fsPath, expanded));
  }

  return path.normalize(expanded);
}

async function resolveRuntimeFromPath(targetPath) {
  if (!targetPath) {
    return null;
  }

  const stats = await statSafe(targetPath);
  if (!stats) {
    return null;
  }

  if (stats.isDirectory()) {
    const orderedNames = process.platform === "win32"
      ? ["roadstone.cmd", "roadstone-cli.js"]
      : ["roadstone-cli.js", "roadstone.cmd"];

    for (const filename of orderedNames) {
      const candidate = path.join(targetPath, filename);
      if (await exists(candidate)) {
        return resolveRuntimeFromPath(candidate);
      }
    }
    return null;
  }

  const lowerName = path.basename(targetPath).toLowerCase();
  if (lowerName === "roadstone.cmd") {
    return {
      command: "cmd.exe",
      args: ["/d", "/c", targetPath],
      cwd: path.dirname(targetPath),
      label: "roadstone.cmd"
    };
  }

  if (lowerName === "roadstone-cli.js") {
    return {
      command: "node",
      args: [targetPath],
      cwd: path.dirname(targetPath),
      label: "roadstone-cli.js"
    };
  }

  return null;
}

async function findRuntimeNearFile(uri) {
  const workspaceFolder = uri ? vscode.workspace.getWorkspaceFolder(uri) : null;
  if (!workspaceFolder) {
    return null;
  }

  const workspaceRoot = path.resolve(workspaceFolder.uri.fsPath);
  let currentDir = path.dirname(uri.fsPath);

  while (true) {
    for (const filename of RUNTIME_FILENAMES) {
      const runtime = await resolveRuntimeFromPath(path.join(currentDir, filename));
      if (runtime) {
        return runtime;
      }
    }

    if (path.resolve(currentDir) === workspaceRoot) {
      break;
    }

    const parentDir = path.dirname(currentDir);
    if (parentDir === currentDir) {
      break;
    }
    currentDir = parentDir;
  }

  return null;
}

async function resolveRuntime(uri) {
  const configuredPath = vscode.workspace
    .getConfiguration("roadstone", uri)
    .get("runtimePath", "");

  const expandedConfiguredPath = expandConfiguredPath(configuredPath, uri);
  const configuredRuntime = await resolveRuntimeFromPath(expandedConfiguredPath);
  if (configuredRuntime) {
    return configuredRuntime;
  }

  return findRuntimeNearFile(uri);
}

async function resolveDocsPath(uri) {
  const configuredPath = vscode.workspace
    .getConfiguration("roadstone", uri)
    .get("runtimePath", "");

  const workspaceFolder = uri ? vscode.workspace.getWorkspaceFolder(uri) : null;
  const candidates = [];

  const expandedConfiguredPath = expandConfiguredPath(configuredPath, uri);
  if (expandedConfiguredPath) {
    const configuredStats = await statSafe(expandedConfiguredPath);
    if (configuredStats) {
      candidates.push(
        configuredStats.isDirectory()
          ? path.join(expandedConfiguredPath, "ROADSTONE_DOCS.md")
          : path.join(path.dirname(expandedConfiguredPath), "ROADSTONE_DOCS.md")
      );
    }
  }

  if (workspaceFolder) {
    candidates.push(path.join(workspaceFolder.uri.fsPath, "ROADSTONE_DOCS.md"));
  }

  for (const candidate of candidates) {
    if (await exists(candidate)) {
      return candidate;
    }
  }

  return null;
}

async function runRoadstone(uri) {
  if (!uri || uri.scheme !== "file") {
    vscode.window.showErrorMessage("Roadstone: open a saved .rd file first.");
    return;
  }

  if (!uri.fsPath.toLowerCase().endsWith(".rd")) {
    vscode.window.showErrorMessage("Roadstone: active file must end with .rd.");
    return;
  }

  const runtime = await resolveRuntime(uri);
  if (!runtime) {
    vscode.window.showErrorMessage(
      "Roadstone: no runtime found. Add roadstone.cmd or roadstone-cli.js to the workspace, or set roadstone.runtimePath."
    );
    return;
  }

  const document = await vscode.workspace.openTextDocument(uri);
  if (document.isDirty) {
    await document.save();
  }

  const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
  const task = new vscode.Task(
    { type: "roadstone", file: uri.fsPath },
    workspaceFolder ?? vscode.TaskScope.Global,
    `Run ${path.basename(uri.fsPath)}`,
    "Roadstone",
    new vscode.ProcessExecution(runtime.command, [...runtime.args, uri.fsPath], { cwd: runtime.cwd })
  );

  task.presentationOptions = {
    reveal: vscode.TaskRevealKind.Always,
    panel: vscode.TaskPanelKind.Dedicated,
    clear: true,
    focus: true
  };

  await vscode.tasks.executeTask(task);
}

async function openRoadstoneHelp(uri) {
  const docsPath = await resolveDocsPath(uri);
  if (!docsPath) {
    vscode.window.showErrorMessage(
      "Roadstone: could not find ROADSTONE_DOCS.md. Set roadstone.runtimePath or open a Roadstone workspace."
    );
    return;
  }

  const doc = await vscode.workspace.openTextDocument(docsPath);
  await vscode.window.showTextDocument(doc, { preview: false, viewColumn: vscode.ViewColumn.Beside });
}

function registerCompletions(context) {
  const keywords = [
    ["local", "Declare a local variable."],
    ["global", "Declare a global variable."],
    ["if", "Start a conditional block."],
    ["elseif", "Add another conditional branch."],
    ["else", "Fallback branch in an if block."],
    ["then", "Block separator for if, for, and EXCEPT."],
    ["while", "Start a while loop."],
    ["for", "Start a counted loop or foreach loop."],
    ["in", "Iterate through a storage unit or string."],
    ["loop", "Loop separator for while and for."],
    ["defi", "Define a function or method."],
    ["return", "Return a value from a function."],
    ["CLASS", "Define a class."],
    ["construct", "Define a class constructor."],
    ["self", "Reference the current instance."],
    ["extends", "Inherit methods from another class."],
    ["EXCEPT", "Remap or catch a runtime error."],
    ["exoutput", "Print the catch result for a handled EXCEPT block."],
    ["end", "Close the current block."],
    ["and", "Logical and."],
    ["or", "Logical or."],
    ["not", "Logical negation."],
    ["true", "Boolean true value."],
    ["false", "Boolean false value."],
    ["nil", "Empty Roadstone value."]
  ];

  const builtins = [
    ["print(value)", "Print a value."],
    ["len(value)", "Get the length of a string, storage unit, or object fields."],
    ["keys(store)", "Return the keys of a storage unit."],
    ["values(store)", "Return the values of a storage unit."],
    ["sort(store)", "Return a sorted copy of a storage unit."],
    ["push(store, value)", "Append a value to a storage unit."],
    ["contains(value, search)", "Check whether a string or storage unit contains a value."],
    ["type(value)", "Return the Roadstone type name."],
    ["raise(name, message?)", "Raise a runtime error."],
    ["error(name, message?)", "Alias of raise()."],
    ["Ask(\"prompt\")", "Prompt for input as a string."],
    ["Ask(Int)(\"prompt\")", "Prompt for input and convert it with Int."],
    ["Int(value)", "Convert a string or number to an integer."],
    ["analyze(mode, target?)", "Inspect networking information like ping or host lookup."],
    ["input(value)", "Reserved runtime name exposed by the interpreter."],
    ["store(...)", "Create a storage unit."]
  ];

  const snippets = [
    {
      label: "if",
      detail: "Roadstone if block",
      body: "if ${1:condition} then\n\t${2}\nend"
    },
    {
      label: "for",
      detail: "Roadstone counted loop",
      body: "for ${1:count} then loop\n\t${2}\nend"
    },
    {
      label: "for in",
      detail: "Roadstone foreach loop",
      body: "for ${1:item} in ${2:items} loop\n\t${3}\nend"
    },
    {
      label: "defi",
      detail: "Roadstone function",
      body: "defi ${1:name}(${2:args})\n\t${3}\nend"
    },
    {
      label: "CLASS",
      detail: "Roadstone class",
      body: "CLASS ${1:Name}(${2:fields})\n\tconstruct(${3:args})\n\t\t${4}\n\tend\nend"
    },
    {
      label: "EXCEPT block",
      detail: "Roadstone 0.6 EXCEPT catch block",
      body:
        'EXCEPT["${1:FriendlyError}", ${2:ZeroDivisionError}] then\n\t${3:raise("${2:ZeroDivisionError}", "${4:message}")}\nexoutput ${5:"Caught " .. exmessage}\nend'
    }
  ];

  const provider = vscode.languages.registerCompletionItemProvider(DOCUMENT_SELECTOR, {
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

      for (const snippet of snippets) {
        const item = new vscode.CompletionItem(snippet.label, vscode.CompletionItemKind.Snippet);
        item.detail = snippet.detail;
        item.insertText = new vscode.SnippetString(snippet.body);
        items.push(item);
      }

      return items;
    }
  });

  context.subscriptions.push(provider);
}

function activate(context) {
  context.subscriptions.push(
    vscode.commands.registerCommand("roadstone.runCurrentFile", async (uri) => {
      const targetUri = uri ?? vscode.window.activeTextEditor?.document.uri;
      if (!targetUri) {
        vscode.window.showErrorMessage("Roadstone: no active editor.");
        return;
      }
      await runRoadstone(targetUri);
    }),
    vscode.commands.registerCommand("roadstone.openHelp", async (uri) => {
      const targetUri = uri ?? vscode.window.activeTextEditor?.document.uri;
      await openRoadstoneHelp(targetUri);
    })
  );

  registerCompletions(context);
}

function deactivate() {}

module.exports = {
  activate,
  deactivate
};
