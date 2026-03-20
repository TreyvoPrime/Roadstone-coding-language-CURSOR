const express = require("express");
const cors = require("cors");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");
const { execFile } = require("child_process");

const app = express();
const PORT = process.env.PORT || 3000;
const projectRoot = path.resolve(__dirname, "..");
const sourceFile = path.join(projectRoot, "RoadstoneMain.java");
const classFile = path.join(projectRoot, "RoadstoneMain.class");

app.use(cors());
app.use(express.json({ limit: "1mb" }));
app.use(express.static(path.join(__dirname, "public")));

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
    return { skipped: true };
  }

  await execFilePromise("javac", [sourceFile], { cwd: projectRoot });
  return { skipped: false };
}

app.post("/run", async (req, res) => {
  const code = req.body && req.body.code;
  if (typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "Missing 'code' string in body." });
  }

  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), "roadstone-web-"));
  const tempFile = path.join(tempDir, "main.rd");

  try {
    await fs.writeFile(tempFile, code, "utf8");

    try {
      await compileIfNeeded();
    } catch (compileFailure) {
      return res.json({
        stage: "compile",
        stdout: compileFailure.stdout || "",
        stderr: compileFailure.stderr || "",
        error: compileFailure.error?.message || String(compileFailure.error || compileFailure)
      });
    }

    try {
      const runResult = await execFilePromise("java", ["-cp", projectRoot, "RoadstoneMain", tempFile], {
        cwd: projectRoot,
        timeout: 12000
      });
      return res.json({
        stage: "run",
        stdout: runResult.stdout,
        stderr: runResult.stderr,
        error: null
      });
    } catch (runFailure) {
      return res.json({
        stage: "run",
        stdout: runFailure.stdout || "",
        stderr: runFailure.stderr || "",
        error: runFailure.error?.message || String(runFailure.error || runFailure)
      });
    }
  } catch (error) {
    return res.status(500).json({ error: error.message || String(error) });
  } finally {
    await fs.rm(tempDir, { recursive: true, force: true }).catch(() => {});
  }
});

app.listen(PORT, () => {
  console.log(`Roadstone web runner listening on http://localhost:${PORT}`);
});
