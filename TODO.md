# Syntax Highlighting for Roadstone Web Editor

## Steps:

- [ ] 1. Read current index.html
- [ ] 2. Implement CSS for .editor, .keyword, .string, .number, .comment
- [ ] 3. Add JS highlightRoadstone(code) function with regex tokenizer
- [ ] 4. Replace textarea with contenteditable div#editor
- [ ] 5. Update JS: codeEl = document.getElementById('editor'), runCode uses editor.innerText
- [ ] 6. Add events: editor.addEventListener('input', () => highlightRoadstone(editor)); cursor restore
- [ ] 7. Update example load, status
- [ ] 8. edit_file apply changes
- [ ] 9. Test: browser reload, type keywords colored
- [x] Complete: Syntax highlighting live!
