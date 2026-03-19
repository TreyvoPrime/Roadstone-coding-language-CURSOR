const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const { exec } = require("child_process");

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json({ limit: "1mb" }));
app.use(express.static(path.join(__dirname, "public")));

// POST /run { code: string }
app.post("/run", (req, res) => {
  const code = req.body && req.body.code;
  if (typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "Missing 'code' string in body." });
  }

  const projectRoot = path.resolve(__dirname, "..");
  const tmpFile = path.join(projectRoot, "web_tmp.rd");

  fs.writeFile(tmpFile, code, "utf8", (writeErr) => {
    if (writeErr) {
      return res.status(500).json({ error: "Failed to write temp file." });
    }

    const javacCmd = `javac "RoadstoneMain.java"`;
    const runCmd = `java -cp . RoadstoneMain "${path.basename(tmpFile)}"`;

    exec(javacCmd, { cwd: projectRoot }, (compileErr, compileStdout, compileStderr) => {
      if (compileErr) {
        return res.json({
          stage: "compile",
          stdout: compileStdout,
          stderr: compileStderr,
          error: compileErr.message || String(compileErr)
        });
      }

      exec(runCmd, { cwd: projectRoot }, (runErr, runStdout, runStderr) => {
        const payload = {
          stage: "run",
          stdout: runStdout,
          stderr: runStderr,
          error: runErr ? (runErr.message || String(runErr)) : null
        };
        return res.json(payload);
      });
    });
  });
});

app.listen(PORT, () => {
  console.log(`Roadstone web runner listening on http://localhost:${PORT}`);
});

