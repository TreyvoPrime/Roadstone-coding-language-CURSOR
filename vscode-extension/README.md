# Roadstone VS Code extension (run command only)

This extension adds a single command:
`Roadstone: Run Current .rd file`

## How to use it (dev mode)
1. Open `c:\Users\trey2\Project-C\` in VS Code
2. Open Extensions view
3. Click the "Run and Debug" gear or use `F5`
4. In the Extension Development Host window:
   - Open any `*.rd` file from this repo
   - Run Command Palette → `Roadstone: Run Current .rd file`

## Notes
- It shells out to `javac` and `java`, so you need a working JDK installed and available in `PATH`.
- Output is shown in an Output panel named `Roadstone`.

