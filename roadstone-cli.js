const fs = require("fs/promises");
const os = require("os");
const path = require("path");
const { execFile } = require("child_process");

const projectRoot = __dirname;
const sourceFile = path.join(projectRoot, "RoadstoneMain.java");
const classFile = path.join(projectRoot, "RoadstoneMain.class");

function execFilePromise(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    execFile(command, args, { maxBuffer: 1024 * 1024, ...options }, (error, stdout, stderr) => {
      if (error) {
        reject({ error, stdout, stderr });
        return;
      }
      resolve({ stdout, stderr });
    });
  });
}

async function compileIfNeeded() {
  const [sourceStats, classStats] = await Promise.all([
    fs.stat(sourceFile),
    fs.stat(classFile).catch(() => null)
  ]);

  if (classStats && classStats.mtimeMs >= sourceStats.mtimeMs) {
    return;
  }

  process.stdout.write("Roadstone: compiling RoadstoneMain.java...\n");
  await execFilePromise("javac", [sourceFile], { cwd: projectRoot });
}

function printUsage() {
  process.stdout.write(
    "Usage:\n" +
    "  roadstone.cmd <file.rd>\n" +
    "  roadstone.cmd --code \"print(\\\"hello\\\")\"\n" +
    "  type file.rd | node roadstone-cli.js --stdin\n"
  );
}

async function readStdin() {
  const chunks = [];
  for await (const chunk of process.stdin) {
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

async function resolveInput(argv) {
  if (argv.length === 0) {
    return { kind: "file", filePath: path.join(projectRoot, "examples", "hello.rd"), tempDir: null };
  }

  if (argv[0] === "--help" || argv[0] === "-h") {
    printUsage();
    process.exit(0);
  }

  if (argv[0] === "--code") {
    const code = argv.slice(1).join(" ");
    if (!code.trim()) {
      throw new Error("Missing code after --code");
    }
    const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "roadstone-cli-"));
    const tempFile = path.join(tempDir, "main.rd");
    await fs.writeFile(tempFile, code, "utf8");
    return { kind: "code", filePath: tempFile, tempDir };
  }

  if (argv[0] === "--stdin") {
    const code = await readStdin();
    if (!code.trim()) {
      throw new Error("No code received on stdin");
    }
    const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "roadstone-cli-"));
    const tempFile = path.join(tempDir, "main.rd");
    await fs.writeFile(tempFile, code, "utf8");
    return { kind: "stdin", filePath: tempFile, tempDir };
  }

  const filePath = path.resolve(projectRoot, argv[0]);
  return { kind: "file", filePath, tempDir: null };
}

async function main() {
  let tempDir = null;
  try {
    const input = await resolveInput(process.argv.slice(2));
    tempDir = input.tempDir;

    await compileIfNeeded();

    process.stdout.write(`Roadstone: running ${input.filePath}...\n`);
    const result = await execFilePromise("java", ["-cp", projectRoot, "RoadstoneMain", input.filePath], {
      cwd: projectRoot,
      timeout: 12000
    });

    if (result.stdout) process.stdout.write(result.stdout);
    if (result.stderr) process.stderr.write(result.stderr);
  } catch (failure) {
    if (failure && failure.stdout) process.stdout.write(failure.stdout);
    if (failure && failure.stderr) process.stderr.write(failure.stderr);
    const message = failure?.error?.message || failure?.message || String(failure);
    process.stderr.write(`Roadstone runner failed: ${message}\n`);
    process.exitCode = 1;
  } finally {
    if (tempDir) {
      await fs.rm(tempDir, { recursive: true, force: true }).catch(() => {});
    }
  }
}

main();
