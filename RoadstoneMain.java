import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
<<<<<<< HEAD
import java.util.LinkedHashSet;
=======
import java.util.LinkedHashMap;
>>>>>>> b744ab7b603e7d84b7f56108d5a86746a80246da
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Roadstone v0.5 interpreter (prototype).
 *
 * Notes:
 * - No semicolons
 * - Blocks end with `end`
 * - if/elseif/else use `then` without colon (per user request)
 * - Comments use Lua-style `--`
 *
 * Supported so far:
 * - `local` and `global` declarations + assignment to existing bindings
 * - expressions: numbers, strings, booleans, nil, arithmetic, comparisons, and/or/not, concatenation `..`
 * - member access: a.b (primarily for self.x)
 * - if/elseif/else/end
 * - EXCEPT["NewName", OldName] error remapping (runtime inside blocks, parse/lexer via scanning source)
 * - while ... loop ... end
 * - for <count_expr> then loop ... end  (defines local iteration var `i` from 1..count)
 * - for item in store loop ... end / for key, value in store loop ... end
 * - functions: defi name(params) ... end
 * - return: return <expr> OR return <paramName> write-back to call-site argument if it was an identifier lvalue
 * - classes: CLASS Name(fields...) ... end with construct and methods via defi inside class
 * - Ask("question") / Ask(Int)("question") input helpers
 * - analyze("ping", "127.0.0.1") networking helper
 * - storage units via store(...)
 *
 * Notes:
 * - Lists/maps + indexing are implemented, but now normalize into a single StorageUnit runtime container
 */
public class RoadstoneMain {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java RoadstoneMain <file.rd>");
            System.out.println("Example: java RoadstoneMain examples/hello.rd");
            return;
        }
        Path p = Path.of(args[0]);
        String src = Files.readString(p, StandardCharsets.UTF_8);

        Map<String, String> globalExcept = scanExceptMappings(src); // OldErrorName -> ReplacementErrorName (used for parse/lexer remap)
        try {
            Lexer lexer = new Lexer(src);
            List<Token> tokens = lexer.lex();

            Parser parser = new Parser(tokens);
            Program program = parser.parseProgram();

            Interpreter interpreter = new Interpreter();
            interpreter.execute(program);
        } catch (Interpreter.RoadstoneRuntimeError re) {
            // Runtime errors are remapped block-locally by EXCEPT[...] directives inside execBlock.
            System.err.println("Roadstone " + re.errorName + ": " + re.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            // Lexer/Parser errors happen before we can execute EXCEPT directives.
            // For this v0, we remap using any EXCEPT[...] found in the source text.
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            String oldName = null;
            String detail = msg;

            String prefixLexer = "Roadstone LexerError:";
            String prefixParser = "Roadstone ParserError:";
            String prefixRuntime = "Roadstone RuntimeError:";

            if (msg.startsWith(prefixLexer)) {
                oldName = "LexerError";
                detail = msg.substring(prefixLexer.length()).trim();
            } else if (msg.startsWith(prefixParser)) {
                oldName = "ParserError";
                detail = msg.substring(prefixParser.length()).trim();
            } else if (msg.startsWith(prefixRuntime)) {
                oldName = "RuntimeError";
                detail = msg.substring(prefixRuntime.length()).trim();
            } else {
                // Unknown failure type
                oldName = "RuntimeError";
            }

            String replacement = globalExcept.getOrDefault(oldName, oldName);
            System.err.println("Roadstone " + replacement + ": " + detail);
            System.exit(1);
        }
    }

    // Extracts EXCEPT["NewName", OldName] directives from raw source text.
    // Returned mapping is OldName -> NewName.
    private static Map<String, String> scanExceptMappings(String src) {
        Map<String, String> out = new HashMap<>();
        Pattern p = Pattern.compile(
                "EXCEPT\\s*\\[\\s*(?:\"([^\"]+)\"|([A-Za-z_][A-Za-z0-9_]*))\\s*,\\s*(?:\"([^\"]+)\"|([A-Za-z_][A-Za-z0-9_]*))\\s*\\]"
        );
        Matcher m = p.matcher(src);
        while (m.find()) {
            String replacement = m.group(1) != null ? m.group(1) : m.group(2);
            String oldName = m.group(3) != null ? m.group(3) : m.group(4);
            out.put(oldName, replacement);
        }
        return out;
    }

    // =========================
    // Lexer
    // =========================

    enum TokenType {
        // Special
        EOF,
        NEWLINE,
        IDENT,
        NUMBER,
        STRING,

        // Operators / punctuation
        PLUS, MINUS, STAR, SLASH, PERCENT,
        CONCAT, // ..
        EQEQ, BANGEQ,
        LT, LTE, GT, GTE,
        ASSIGN, // =
        LPAREN, RPAREN,
        COMMA,
        SEMICOLON,
        DOT,
        LBRACKET, RBRACKET,
        LBRACE, RBRACE,
        COLON,

        // Keywords (subset)
        EXCEPT,
        EXOUTPUT,
        LOCAL,
        GLOBAL,
        FOR, THEN, LOOP, END,
        IF, ELSEIF, ELSE,
        WHILE,
        DEFINI, // defi
        RETURN,
        AND, OR, NOT,
        TRUE, FALSE, NIL,
        CLASS, CONSTRUCT, SELF,
        EXTENDS,
        IN,
        STORE,
        ANALYZE,
        INPUT
    }

    static class Token {
        final TokenType type;
        final String lexeme;
        final int line;
        final int col;

        Token(TokenType type, String lexeme, int line, int col) {
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
            this.col = col;
        }

        @Override
        public String toString() {
            return type + "('" + lexeme + "')@" + line + ":" + col;
        }
    }

    static class Lexer {
        private final String src;
        private int idx = 0;
        private int line = 1;
        private int col = 1;

        Lexer(String src) {
            this.src = src;
        }

        List<Token> lex() {
            List<Token> out = new ArrayList<>();
            while (true) {
                Token t = nextToken();
                out.add(t);
                if (t.type == TokenType.EOF) return out;
            }
        }

        private Token nextToken() {
            while (idx < src.length()) {
                char c = src.charAt(idx);

                // Whitespace
                if (c == ' ' || c == '\t' || c == '\r') {
                    advance(c);
                    continue;
                }

                // Newline
                if (c == '\n') {
                    int tLine = line;
                    int tCol = col;
                    advance(c);
                    return new Token(TokenType.NEWLINE, "\n", tLine, tCol);
                }

                // Comment: -- until end of line
                if (c == '-' && peek(1) == '-') {
                    while (idx < src.length() && src.charAt(idx) != '\n') {
                        advance(src.charAt(idx));
                    }
                    continue; // loop to read next token
                }

                // String literal
                if (c == '"') {
                    return stringToken();
                }

                // Number
                if (isDigit(c)) {
                    return numberToken();
                }

                // Identifier / keyword
                if (isIdentStart(c)) {
                    return identToken();
                }

                // Multi-character operators
                if (c == '.' && peek(1) == '.') {
                    int tLine = line, tCol = col;
                    advance(c); advance('.');
                    return new Token(TokenType.CONCAT, "..", tLine, tCol);
                }
                if (c == '=' && peek(1) == '=') {
                    int tLine = line, tCol = col;
                    advance(c); advance('=');
                    return new Token(TokenType.EQEQ, "==", tLine, tCol);
                }
                if (c == '!' && peek(1) == '=') {
                    int tLine = line, tCol = col;
                    advance(c); advance('=');
                    return new Token(TokenType.BANGEQ, "!=", tLine, tCol);
                }
                if (c == '<' && peek(1) == '=') {
                    int tLine = line, tCol = col;
                    advance(c); advance('=');
                    return new Token(TokenType.LTE, "<=", tLine, tCol);
                }
                if (c == '>' && peek(1) == '=') {
                    int tLine = line, tCol = col;
                    advance(c); advance('=');
                    return new Token(TokenType.GTE, ">=", tLine, tCol);
                }

                // Single-char tokens
                switch (c) {
                    case '+': return simple(TokenType.PLUS, c);
                    case '-': return simple(TokenType.MINUS, c);
                    case '*': return simple(TokenType.STAR, c);
                    case '/': return simple(TokenType.SLASH, c);
                    case '%': return simple(TokenType.PERCENT, c);
                    case '<': return simple(TokenType.LT, c);
                    case '>': return simple(TokenType.GT, c);
                    case '=': return simple(TokenType.ASSIGN, c);
                    case '(': return simple(TokenType.LPAREN, c);
                    case ')': return simple(TokenType.RPAREN, c);
                    case ',': return simple(TokenType.COMMA, c);
                    case ';': return simple(TokenType.SEMICOLON, c);
                    case '.': return simple(TokenType.DOT, c);
                    case '[': return simple(TokenType.LBRACKET, c);
                    case ']': return simple(TokenType.RBRACKET, c);
                    case '{': return simple(TokenType.LBRACE, c);
                    case '}': return simple(TokenType.RBRACE, c);
                    case ':': return simple(TokenType.COLON, c);
                }

                throw error("Unexpected character: " + c, line, col);
            }
            return new Token(TokenType.EOF, "", line, col);
        }

        private Token simple(TokenType type, char c) {
            int tLine = line, tCol = col;
            advance(c);
            return new Token(type, String.valueOf(c), tLine, tCol);
        }

        private Token stringToken() {
            int tLine = line;
            int tCol = col;
            advance('"'); // opening quote
            StringBuilder sb = new StringBuilder();
            while (idx < src.length()) {
                char c = src.charAt(idx);
                if (c == '"') {
                    advance('"');
                    return new Token(TokenType.STRING, sb.toString(), tLine, tCol);
                }
                if (c == '\\') {
                    advance('\\');
                    if (idx >= src.length()) throw error("Unterminated string", tLine, tCol);
                    char esc = src.charAt(idx);
                    switch (esc) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(esc); break;
                    }
                    advance(esc);
                    continue;
                }
                sb.append(c);
                advance(c);
            }
            throw error("Unterminated string", tLine, tCol);
        }

        private Token numberToken() {
            int tLine = line;
            int tCol = col;
            StringBuilder sb = new StringBuilder();
            while (idx < src.length() && isDigit(src.charAt(idx))) {
                sb.append(src.charAt(idx));
                advance(src.charAt(idx));
            }
            if (idx < src.length() && src.charAt(idx) == '.' && idx + 1 < src.length() && isDigit(src.charAt(idx + 1))) {
                sb.append('.');
                advance('.');
                while (idx < src.length() && isDigit(src.charAt(idx))) {
                    sb.append(src.charAt(idx));
                    advance(src.charAt(idx));
                }
            }
            return new Token(TokenType.NUMBER, sb.toString(), tLine, tCol);
        }

        private Token identToken() {
            int tLine = line;
            int tCol = col;
            StringBuilder sb = new StringBuilder();
            while (idx < src.length() && isIdentPart(src.charAt(idx))) {
                sb.append(src.charAt(idx));
                advance(src.charAt(idx));
            }
            String s = sb.toString();
            TokenType kw = keywordType(s);
            if (kw != null) return new Token(kw, s, tLine, tCol);
            return new Token(TokenType.IDENT, s, tLine, tCol);
        }

        private TokenType keywordType(String s) {
            return switch (s) {
                case "local" -> TokenType.LOCAL;
                case "global" -> TokenType.GLOBAL;
                case "for" -> TokenType.FOR;
                case "then" -> TokenType.THEN;
                case "loop" -> TokenType.LOOP;
                case "end" -> TokenType.END;
                case "if" -> TokenType.IF;
                case "elseif" -> TokenType.ELSEIF;
                case "else" -> TokenType.ELSE;
                case "while" -> TokenType.WHILE;
                case "defi" -> TokenType.DEFINI;
                case "return" -> TokenType.RETURN;
                case "and" -> TokenType.AND;
                case "or" -> TokenType.OR;
                case "not" -> TokenType.NOT;
                case "true" -> TokenType.TRUE;
                case "false" -> TokenType.FALSE;
                case "nil" -> TokenType.NIL;
                case "CLASS" -> TokenType.CLASS;
                case "construct" -> TokenType.CONSTRUCT;
                case "self" -> TokenType.SELF;
                case "extends" -> TokenType.EXTENDS;
                case "EXCEPT" -> TokenType.EXCEPT;
<<<<<<< HEAD
                case "exoutput" -> TokenType.EXOUTPUT;
=======
                case "in" -> TokenType.IN;
                case "store" -> TokenType.STORE;
                case "analyze" -> TokenType.ANALYZE;
                case "input" -> TokenType.INPUT;
>>>>>>> b744ab7b603e7d84b7f56108d5a86746a80246da
                default -> null;
            };
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isIdentStart(char c) {
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
        }

        private boolean isIdentPart(char c) {
            return isIdentStart(c) || isDigit(c);
        }

        private char peek(int off) {
            int i = idx + off;
            if (i >= src.length()) return '\0';
            return src.charAt(i);
        }

        private void advance(char c) {
            idx++;
            if (c == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }

        private RuntimeException error(String msg, int l, int c) {
            return new RuntimeException("Roadstone LexerError: " + msg + " at " + l + ":" + c);
        }
    }

    // =========================
    // AST
    // =========================

    static class Program {
        final List<Stmt> statements;
        Program(List<Stmt> statements) {
            this.statements = statements;
        }
    }

    interface Stmt {}
    interface Expr {}

    static class Block {
        final List<Stmt> statements;
        Block(List<Stmt> statements) { this.statements = statements; }
    }

    static class LocalDecl implements Stmt {
        final String name;
        final Expr init; // may be null
        LocalDecl(String name, Expr init) { this.name = name; this.init = init; }
    }

    static class GlobalDecl implements Stmt {
        final String name;
        final Expr init; // may be null
        GlobalDecl(String name, Expr init) { this.name = name; this.init = init; }
    }

    static class Assign implements Stmt {
        final LValue target;
        final Expr value;
        Assign(LValue target, Expr value) { this.target = target; this.value = value; }
    }

    interface LValue {}
    static class LVar implements LValue {
        final String name;
        LVar(String name) { this.name = name; }
    }
    static class LMember implements LValue {
        final Expr obj;
        final String field;
        LMember(Expr obj, String field) { this.obj = obj; this.field = field; }
    }
    static class LIndex implements LValue {
        final Expr obj;
        final Expr index;
        LIndex(Expr obj, Expr index) { this.obj = obj; this.index = index; }
    }

    static class IfStmt implements Stmt {
        final Expr cond;
        final Block thenBlock;
        final List<ElseIf> elseIfs;
        final Block elseBlock; // may be null

        IfStmt(Expr cond, Block thenBlock, List<ElseIf> elseIfs, Block elseBlock) {
            this.cond = cond;
            this.thenBlock = thenBlock;
            this.elseIfs = elseIfs;
            this.elseBlock = elseBlock;
        }
    }

    static class ElseIf {
        final Expr cond;
        final Block block;
        ElseIf(Expr cond, Block block) { this.cond = cond; this.block = block; }
    }

    static class WhileStmt implements Stmt {
        final Expr cond;
        final Block body;
        WhileStmt(Expr cond, Block body) { this.cond = cond; this.body = body; }
    }

    static class ForStmt implements Stmt {
        final Expr countExpr;
        final Block body;
        ForStmt(Expr countExpr, Block body) { this.countExpr = countExpr; this.body = body; }
    }

    static class ForEachStmt implements Stmt {
        final String keyName;
        final String valueName; // nullable; when null keyName receives each value
        final Expr iterableExpr;
        final Block body;

        ForEachStmt(String keyName, String valueName, Expr iterableExpr, Block body) {
            this.keyName = keyName;
            this.valueName = valueName;
            this.iterableExpr = iterableExpr;
            this.body = body;
        }
    }

    static class FunctionDef implements Stmt {
        final String name;
        final List<String> params;
        final Block body;
        FunctionDef(String name, List<String> params, Block body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }
    }

    static class ReturnStmt implements Stmt {
        final Expr expr; // may be null
        ReturnStmt(Expr expr) { this.expr = expr; }
    }

    static class ExprStmt implements Stmt {
        final Expr expr;
        ExprStmt(Expr expr) { this.expr = expr; }
    }

    static class ExceptStmt implements Stmt {
        final String replacementErrorName;
        final String targetErrorName;
        ExceptStmt(String replacementErrorName, String targetErrorName) {
            this.replacementErrorName = replacementErrorName;
            this.targetErrorName = targetErrorName;
        }
    }

    static class ExceptBlockStmt implements Stmt {
        final String replacementErrorName;
        final String targetErrorName;
        final Block tryBlock;
        final Expr exoutputExpr; // may be null

        ExceptBlockStmt(String replacementErrorName, String targetErrorName, Block tryBlock, Expr exoutputExpr) {
            this.replacementErrorName = replacementErrorName;
            this.targetErrorName = targetErrorName;
            this.tryBlock = tryBlock;
            this.exoutputExpr = exoutputExpr;
        }
    }

    static class ClassDef implements Stmt {
        final String name;
        final List<String> fields;
        final String baseName; // may be null
        final FunctionDef constructor; // may be null
        final List<FunctionDef> methods;
        ClassDef(String name, List<String> fields, String baseName, FunctionDef constructor, List<FunctionDef> methods) {
            this.name = name;
            this.fields = fields;
            this.baseName = baseName;
            this.constructor = constructor;
            this.methods = methods;
        }
    }

    // Expressions
    static class NumberLit implements Expr { final double value; NumberLit(double value){this.value=value;} }
    static class StringLit implements Expr { final String value; StringLit(String value){this.value=value;} }
    static class BoolLit implements Expr { final boolean value; BoolLit(boolean value){this.value=value;} }
    static class NilLit implements Expr {}
    static class VarExpr implements Expr { final String name; VarExpr(String name){this.name=name;} }

    static class MemberAccess implements Expr {
        final Expr obj;
        final String field;
        MemberAccess(Expr obj, String field) { this.obj = obj; this.field = field; }
    }

    static class BinaryExpr implements Expr {
        final String op;
        final Expr left, right;
        BinaryExpr(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    static class UnaryExpr implements Expr {
        final String op;
        final Expr expr;
        UnaryExpr(String op, Expr expr){this.op = op; this.expr = expr;}
    }

    static class CallExpr implements Expr {
        final Expr callee;
        final List<Expr> args;
        CallExpr(Expr callee, List<Expr> args) { this.callee = callee; this.args = args; }
    }

    static class ListLit implements Expr {
        final List<Expr> elements;
        ListLit(List<Expr> elements) { this.elements = elements; }
    }

    static class MapLit implements Expr {
        final List<Expr> keys;
        final List<Expr> values;
        MapLit(List<Expr> keys, List<Expr> values) { this.keys = keys; this.values = values; }
    }

    static class IndexAccess implements Expr {
        final Expr obj;
        final Expr index;
        IndexAccess(Expr obj, Expr index) { this.obj = obj; this.index = index; }
    }

    static class StoreEntry {
        final Expr key;
        final Expr value;

        StoreEntry(Expr key, Expr value) {
            this.key = key;
            this.value = value;
        }
    }

    static class StoreLit implements Expr {
        final List<StoreEntry> entries;

        StoreLit(List<StoreEntry> entries) {
            this.entries = entries;
        }
    }

    // =========================
    // Parser
    // =========================

    static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        Program parseProgram() {
            List<Stmt> stmts = new ArrayList<>();
            skipNewlines();
            while (!check(TokenType.EOF)) {
                stmts.add(parseStatement());
                skipNewlines();
            }
            return new Program(stmts);
        }

        private Stmt parseStatement() {
            Token t = peek();
            return switch (t.type) {
                case GLOBAL -> parseGlobalDecl();
                case EXCEPT -> parseExceptStmt();
                case LOCAL -> parseLocalDecl();
                case IF -> parseIf();
                case WHILE -> parseWhile();
                case FOR -> parseFor();
                case DEFINI -> parseFunctionDef();
                case RETURN -> parseReturn();
                case CLASS -> parseClassDef();
                case IDENT -> parseAssignmentOrExprStmt();
                case SELF -> parseAssignmentOrExprStmt();
                default -> throw parseError("Unexpected token at start of statement: " + t.type, t);
            };
        }

        private GlobalDecl parseGlobalDecl() {
            consume(TokenType.GLOBAL, "Expected 'global'");
            Token nameTok = consume(TokenType.IDENT, "Expected identifier after global");
            String name = nameTok.lexeme;
            Expr init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            }
            return new GlobalDecl(name, init);
        }

        private Stmt parseExceptStmt() {
            consume(TokenType.EXCEPT, "Expected EXCEPT");
            consume(TokenType.LBRACKET, "Expected '[' after EXCEPT");

            // EXCEPT["SigmaError", ZeroDivisionError]
            Token first = advance(); // STRING or IDENT
            String replacement = parseExceptName(first, "Expected string or identifier error name in EXCEPT");

            consume(TokenType.COMMA, "Expected ',' in EXCEPT");

            Token second = advance(); // IDENT or STRING
            String target = parseExceptName(second, "Expected identifier or string error name in EXCEPT");

            consume(TokenType.RBRACKET, "Expected ']' to close EXCEPT");
            if (match(TokenType.THEN)) {
                skipOptionalNewlines();
                Block tryBlock = parseUntilBlockEnd(java.util.Set.of(TokenType.EXOUTPUT, TokenType.END));
                Expr exoutputExpr = null;
                if (match(TokenType.EXOUTPUT)) {
                    exoutputExpr = parseExpression();
                    skipOptionalNewlines();
                }
                consume(TokenType.END, "Expected 'end' to close EXCEPT block");
                return new ExceptBlockStmt(replacement, target, tryBlock, exoutputExpr);
            }
            skipOptionalNewlines();
            return new ExceptStmt(replacement, target);
        }

        private String parseExceptName(Token token, String errorMessage) {
            if (token.type == TokenType.STRING || token.type == TokenType.IDENT) {
                return token.lexeme;
            }
            if (token.type == TokenType.SELF) {
                return "self";
            }
            throw parseError(errorMessage, token);
        }

        private Stmt parseAssignmentOrExprStmt() {
            // Could be assignment (lvalue = expr) or just an expression statement.
            // We'll parse an expression first, then if it's an lvalue and next token is '=', make it assignment.
            Expr left = parseExpression();
            if (match(TokenType.ASSIGN)) {
                Expr value = parseExpression();
                skipOptionalNewlines();
                LValue lv = exprToLValue(left);
                return new Assign(lv, value);
            }
            return new ExprStmt(left);
        }

        private LValue exprToLValue(Expr e) {
            if (e instanceof VarExpr v) return new LVar(v.name);
            if (e instanceof MemberAccess m) return new LMember(m.obj, m.field);
            if (e instanceof IndexAccess ix) return new LIndex(ix.obj, ix.index);
            throw new RuntimeException("Roadstone ParserError: Invalid assignment target");
        }

        private LocalDecl parseLocalDecl() {
            consume(TokenType.LOCAL, "Expected 'local'");
            Token nameTok = consume(TokenType.IDENT, "Expected identifier after local");
            String name = nameTok.lexeme;
            Expr init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            }
            return new LocalDecl(name, init);
        }

        private IfStmt parseIf() {
            consume(TokenType.IF, "Expected 'if'");
            Expr cond = parseExpression();
            consume(TokenType.THEN, "Expected 'then' after if condition");
            skipOptionalNewlines();
            Block thenBlock = parseUntilBlockEnd(java.util.Set.of(TokenType.ELSEIF, TokenType.ELSE, TokenType.END));

            List<ElseIf> elseIfs = new ArrayList<>();
            Block elseBlock = null;
            while (match(TokenType.ELSEIF)) {
                Expr ec = parseExpression();
                consume(TokenType.THEN, "Expected 'then' after elseif condition");
                skipOptionalNewlines();
                Block b = parseUntilBlockEnd(java.util.Set.of(TokenType.ELSEIF, TokenType.ELSE, TokenType.END));
                elseIfs.add(new ElseIf(ec, b));
            }
            if (match(TokenType.ELSE)) {
                skipOptionalNewlines();
                elseBlock = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
            }
            consume(TokenType.END, "Expected 'end' to close if");
            return new IfStmt(cond, thenBlock, elseIfs, elseBlock);
        }

        private WhileStmt parseWhile() {
            consume(TokenType.WHILE, "Expected 'while'");
            Expr cond = parseExpression();
            consume(TokenType.LOOP, "Expected 'loop' after while condition");
            skipOptionalNewlines();
            Block body = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
            consume(TokenType.END, "Expected 'end' to close while");
            return new WhileStmt(cond, body);
        }

        private Stmt parseFor() {
            consume(TokenType.FOR, "Expected 'for'");
            if (check(TokenType.IDENT) || check(TokenType.SELF)) {
                Token firstName = advance();
                String secondName = null;
                if (match(TokenType.COMMA)) {
                    Token secondTok = (check(TokenType.SELF)) ? advance() : consume(TokenType.IDENT, "Expected iterator variable after ','");
                    secondName = secondTok.lexeme;
                }
                if (match(TokenType.IN)) {
                    Expr iterable = parseExpression();
                    consume(TokenType.LOOP, "Expected 'loop' after iterable");
                    skipOptionalNewlines();
                    Block body = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
                    consume(TokenType.END, "Expected 'end' to close for loop");
                    return new ForEachStmt(firstName.lexeme, secondName, iterable, body);
                }
                pos -= secondName == null ? 1 : 3;
            }
            Expr count = parseExpression();
            consume(TokenType.THEN, "Expected 'then' after for count");
            consume(TokenType.LOOP, "Expected 'loop' after for then");
            skipOptionalNewlines();
            Block body = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
            consume(TokenType.END, "Expected 'end' to close for");
            return new ForStmt(count, body);
        }

        private FunctionDef parseFunctionDef() {
            consume(TokenType.DEFINI, "Expected 'defi'");
            Token nameTok = consume(TokenType.IDENT, "Expected function name");
            String name = nameTok.lexeme;
            consume(TokenType.LPAREN, "Expected '(' after function name");
            List<String> params = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                Token p = (check(TokenType.SELF)) ? advance() : consume(TokenType.IDENT, "Expected parameter name");
                params.add(p.lexeme);
                while (match(TokenType.COMMA)) {
                    Token p2 = (check(TokenType.SELF)) ? advance() : consume(TokenType.IDENT, "Expected parameter name");
                    params.add(p2.lexeme);
                }
            }
            consume(TokenType.RPAREN, "Expected ')'");
            skipOptionalNewlines();
            Block body = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
            consume(TokenType.END, "Expected 'end' to close function");
            return new FunctionDef(name, params, body);
        }

        private ReturnStmt parseReturn() {
            consume(TokenType.RETURN, "Expected 'return'");
            // return can be `return` or `return <expr>`
            if (peek().type == TokenType.NEWLINE || peek().type == TokenType.END) {
                return new ReturnStmt(null);
            }
            // If next token is one of our statement terminators, treat as no expression.
            if (peek().type == TokenType.ELSE || peek().type == TokenType.ELSEIF || peek().type == TokenType.EOF) {
                return new ReturnStmt(null);
            }
            Expr expr = parseExpression();
            return new ReturnStmt(expr);
        }

        private ClassDef parseClassDef() {
            consume(TokenType.CLASS, "Expected 'CLASS'");
            Token nameTok = consume(TokenType.IDENT, "Expected class name");
            String name = nameTok.lexeme;
            consume(TokenType.LPAREN, "Expected '(' after class name");

            List<String> fields = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                Token f = consume(TokenType.IDENT, "Expected field name");
                fields.add(f.lexeme);
                while (match(TokenType.COMMA)) {
                    Token f2 = consume(TokenType.IDENT, "Expected field name");
                    fields.add(f2.lexeme);
                }
            }
            consume(TokenType.RPAREN, "Expected ')'");

            String baseName = null;
            if (match(TokenType.EXTENDS)) {
                Token baseTok = consume(TokenType.IDENT, "Expected base class name after extends");
                baseName = baseTok.lexeme;
            }

            skipOptionalNewlines();

            FunctionDef constructor = null;
            List<FunctionDef> methods = new ArrayList<>();

            while (true) {
                skipNewlines();
                if (check(TokenType.END) || check(TokenType.EOF)) break;
                if (match(TokenType.CONSTRUCT)) {
                    // construct already consumed; we parse like a function but store as constructor
                    Token ctorNameTok = new Token(TokenType.IDENT, "<construct>", -1, -1);
                    consume(TokenType.LPAREN, "Expected '(' after construct");
                    List<String> params = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        Token p = (check(TokenType.SELF)) ? advance() : consume(TokenType.IDENT, "Expected parameter name");
                        params.add(p.lexeme);
                        while (match(TokenType.COMMA)) {
                            Token p2 = (check(TokenType.SELF)) ? advance() : consume(TokenType.IDENT, "Expected parameter name");
                            params.add(p2.lexeme);
                        }
                    }
                    consume(TokenType.RPAREN, "Expected ')'");
                    skipOptionalNewlines();
                    Block body = parseUntilBlockEnd(java.util.Set.of(TokenType.END));
                    consume(TokenType.END, "Expected 'end' to close construct");
                    constructor = new FunctionDef("<construct>", params, body);
                    continue;
                }
                if (check(TokenType.DEFINI)) {
                    FunctionDef fn = parseFunctionDef();
                    methods.add(fn);
                    continue;
                }
                // Allow other statements inside class for future features
                throw parseError("Only constructor (construct ...) and methods (defi ...) are supported inside CLASS in this v0", peek());
            }

            consume(TokenType.END, "Expected 'end' to close CLASS");
            return new ClassDef(name, fields, baseName, constructor, methods);
        }

        // Expression parsing with precedence climbing / precedence functions
        Expr parseExpression() {
            return parseOr();
        }

        Expr parseOr() {
            Expr e = parseAnd();
            while (match(TokenType.OR)) {
                Expr r = parseAnd();
                e = new BinaryExpr("or", e, r);
            }
            return e;
        }

        Expr parseAnd() {
            Expr e = parseEquality();
            while (match(TokenType.AND)) {
                Expr r = parseEquality();
                e = new BinaryExpr("and", e, r);
            }
            return e;
        }

        Expr parseEquality() {
            Expr e = parseCompare();
            while (true) {
                if (match(TokenType.EQEQ)) {
                    Expr r = parseCompare();
                    e = new BinaryExpr("==", e, r);
                } else if (match(TokenType.BANGEQ)) {
                    Expr r = parseCompare();
                    e = new BinaryExpr("!=", e, r);
                } else {
                    break;
                }
            }
            return e;
        }

        Expr parseCompare() {
            Expr e = parseConcat();
            while (true) {
                if (match(TokenType.LT)) {
                    e = new BinaryExpr("<", e, parseConcat());
                } else if (match(TokenType.LTE)) {
                    e = new BinaryExpr("<=", e, parseConcat());
                } else if (match(TokenType.GT)) {
                    e = new BinaryExpr(">", e, parseConcat());
                } else if (match(TokenType.GTE)) {
                    e = new BinaryExpr(">=", e, parseConcat());
                } else {
                    break;
                }
            }
            return e;
        }

        Expr parseConcat() {
            Expr e = parseAdd();
            while (match(TokenType.CONCAT)) {
                Expr r = parseAdd();
                e = new BinaryExpr("..", e, r);
            }
            return e;
        }

        Expr parseAdd() {
            Expr e = parseMul();
            while (true) {
                if (match(TokenType.PLUS)) {
                    e = new BinaryExpr("+", e, parseMul());
                } else if (match(TokenType.MINUS)) {
                    e = new BinaryExpr("-", e, parseMul());
                } else {
                    break;
                }
            }
            return e;
        }

        Expr parseMul() {
            Expr e = parseUnary();
            while (true) {
                if (match(TokenType.STAR)) {
                    e = new BinaryExpr("*", e, parseUnary());
                } else if (match(TokenType.SLASH)) {
                    e = new BinaryExpr("/", e, parseUnary());
                } else if (match(TokenType.PERCENT)) {
                    e = new BinaryExpr("%", e, parseUnary());
                } else {
                    break;
                }
            }
            return e;
        }

        Expr parseUnary() {
            if (match(TokenType.NOT)) {
                return new UnaryExpr("not", parseUnary());
            }
            if (match(TokenType.MINUS)) {
                return new UnaryExpr("-", parseUnary());
            }
            return parsePostfix();
        }

        Expr parsePostfix() {
            Expr e = parsePrimary();
            while (true) {
                if (match(TokenType.DOT)) {
                    Token field = consume(TokenType.IDENT, "Expected field name after '.'");
                    e = new MemberAccess(e, field.lexeme);
                } else if (match(TokenType.LBRACKET)) {
                    Expr idx = parseExpression();
                    consume(TokenType.RBRACKET, "Expected ']' after index");
                    e = new IndexAccess(e, idx);
                } else if (match(TokenType.LPAREN)) {
                    // call
                    List<Expr> args = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        args.add(parseExpression());
                        while (match(TokenType.COMMA)) {
                            args.add(parseExpression());
                        }
                    }
                    consume(TokenType.RPAREN, "Expected ')' after call arguments");
                    e = new CallExpr(e, args);
                } else {
                    break;
                }
            }
            return e;
        }

        Expr parsePrimary() {
            Token t = peek();
            return switch (t.type) {
                case NUMBER -> {
                    advance();
                    yield new NumberLit(Double.parseDouble(t.lexeme));
                }
                case STRING -> {
                    advance();
                    yield new StringLit(t.lexeme);
                }
                case TRUE -> {
                    advance();
                    yield new BoolLit(true);
                }
                case FALSE -> {
                    advance();
                    yield new BoolLit(false);
                }
                case NIL -> {
                    advance();
                    yield new NilLit();
                }
                case IDENT -> {
                    advance();
                    yield new VarExpr(t.lexeme);
                }
                case ANALYZE -> {
                    advance();
                    yield new VarExpr("analyze");
                }
                case INPUT -> {
                    advance();
                    yield new VarExpr("input");
                }
                case SELF -> {
                    advance();
                    yield new VarExpr(t.lexeme);
                }
                case STORE -> parseStoreLiteral();
                case LPAREN -> {
                    consume(TokenType.LPAREN, "Expected '('");
                    Expr e = parseExpression();
                    consume(TokenType.RPAREN, "Expected ')'");
                    yield e;
                }
                case LBRACKET -> {
                    // list literal
                    consume(TokenType.LBRACKET, "Expected '['");
                    List<Expr> els = new ArrayList<>();
                    if (!check(TokenType.RBRACKET)) {
                        els.add(parseExpression());
                        while (match(TokenType.COMMA)) {
                            els.add(parseExpression());
                        }
                    }
                    consume(TokenType.RBRACKET, "Expected ']'");
                    yield new ListLit(els);
                }
                case LBRACE -> {
                    // map literal
                    consume(TokenType.LBRACE, "Expected '{'");
                    List<Expr> keys = new ArrayList<>();
                    List<Expr> values = new ArrayList<>();
                    if (!check(TokenType.RBRACE)) {
                        // map_entry := expression ':' expression
                        Expr k = parseExpression();
                        consume(TokenType.COLON, "Expected ':' after map key");
                        Expr v = parseExpression();
                        keys.add(k);
                        values.add(v);
                        while (match(TokenType.COMMA)) {
                            Expr k2 = parseExpression();
                            consume(TokenType.COLON, "Expected ':' after map key");
                            Expr v2 = parseExpression();
                            keys.add(k2);
                            values.add(v2);
                        }
                    }
                    consume(TokenType.RBRACE, "Expected '}'");
                    yield new MapLit(keys, values);
                }
                default -> throw parseError("Unexpected token in expression: " + t.type, t);
            };
        }

        // Helpers
        private Expr parseStoreLiteral() {
            consume(TokenType.STORE, "Expected 'store'");
            consume(TokenType.LPAREN, "Expected '(' after store");
            List<StoreEntry> entries = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                entries.add(parseStoreEntry());
                while (match(TokenType.COMMA)) {
                    entries.add(parseStoreEntry());
                }
            }
            consume(TokenType.RPAREN, "Expected ')'");
            return new StoreLit(entries);
        }

        private StoreEntry parseStoreEntry() {
            Expr first = parseExpression();
            if (match(TokenType.SEMICOLON)) {
                Expr value = parseExpression();
                return new StoreEntry(first, value);
            }
            return new StoreEntry(null, first);
        }

        private void skipNewlines() {
            while (check(TokenType.NEWLINE)) advance();
        }

        private void skipOptionalNewlines() {
            while (check(TokenType.NEWLINE)) advance();
        }

        private Block parseUntilBlockEnd(java.util.Set<TokenType> terminators) {
            List<Stmt> stmts = new ArrayList<>();
            skipNewlines();
            while (!check(TokenType.EOF) && !terminators.contains(peek().type)) {
                stmts.add(parseStatement());
                skipNewlines();
            }
            return new Block(stmts);
        }

        private boolean check(TokenType type) {
            return peek().type == type;
        }

        private Token peek() {
            if (pos >= tokens.size()) return tokens.get(tokens.size() - 1);
            return tokens.get(pos);
        }

        private boolean match(TokenType type) {
            if (check(type)) {
                advance();
                return true;
            }
            return false;
        }

        private Token consume(TokenType type, String message) {
            if (check(type)) return advance();
            throw parseError(message + ", got " + peek().type, peek());
        }

        private Token consume(TokenType type) {
            return consume(type, "Expected token: " + type);
        }

        private Token advance() {
            return tokens.get(pos++);
        }

        private RuntimeException parseError(String msg, Token t) {
            return new RuntimeException("Roadstone ParserError: " + msg + " at " + t.line + ":" + t.col);
        }
    }

    // =========================
    // Interpreter
    // =========================

    static class Interpreter {
        // Globals are separate from local frames.
        private final Map<String, Object> globals = new HashMap<>();

        // Function and class values
        Interpreter() {
            // Builtins
            globals.put("print", new BuiltinFunction("print") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() > 0) System.out.println(stringify(args.get(0)));
                    else System.out.println();
                    return null;
                }
            });

            globals.put("len", new BuiltinFunction("len") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "len(value) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (v instanceof StorageUnit store) return (double) store.size();
                    if (v instanceof String s) return (double) s.length();
                    if (v instanceof RoadInstance inst) return (double) inst.props.size();
                    throw new RoadstoneRuntimeError("TypeError", "len() unsupported for this value");
                }
            });

            globals.put("keys", new BuiltinFunction("keys") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "keys(store) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (!(v instanceof StorageUnit store)) {
                        throw new RoadstoneRuntimeError("TypeError", "keys() expects a storage unit");
                    }
                    return store.keysAsStore();
                }
            });

            globals.put("values", new BuiltinFunction("values") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "values(store) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (!(v instanceof StorageUnit store)) {
                        throw new RoadstoneRuntimeError("TypeError", "values() expects a storage unit");
                    }
                    return store.valuesAsStore();
                }
            });

            globals.put("sort", new BuiltinFunction("sort") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "sort(store) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (!(v instanceof StorageUnit store)) {
                        throw new RoadstoneRuntimeError("TypeError", "sort() expects a storage unit");
                    }
                    return store.sortedCopy();
                }
            });

            globals.put("push", new BuiltinFunction("push") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 2) {
                        throw new RoadstoneRuntimeError("ArgumentError", "push(store, value) expects 2 arguments");
                    }
                    Object target = args.get(0);
                    if (!(target instanceof StorageUnit store)) {
                        throw new RoadstoneRuntimeError("TypeError", "push() expects a storage unit");
                    }
                    store.putAuto(args.get(1));
                    return target;
                }
            });

            globals.put("contains", new BuiltinFunction("contains") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 2) {
                        throw new RoadstoneRuntimeError("ArgumentError", "contains(value, search) expects 2 arguments");
                    }
                    Object container = args.get(0);
                    Object needle = args.get(1);
                    if (container instanceof String s) return s.contains(stringify(needle));
                    if (container instanceof StorageUnit store) return store.containsValue(needle) || store.containsKey(needle);
                    throw new RoadstoneRuntimeError("TypeError", "contains() expects a string or storage unit");
                }
            });

            globals.put("Int", new BuiltinFunction("Int") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "Int(value) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (v instanceof Double d) return (double) toInt(d, "Int() expects a whole number or numeric string");
                    if (v instanceof String s) {
                        try {
                            return (double) Integer.parseInt(s.trim());
                        } catch (NumberFormatException ex) {
                            throw new RoadstoneRuntimeError("TypeError", "Int() could not parse '" + s + "'");
                        }
                    }
                    throw new RoadstoneRuntimeError("TypeError", "Int() expects a number or string");
                }
            });

            globals.put("Ask", new BuiltinFunction("Ask") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() == 1 && args.get(0) instanceof String prompt) {
                        return askValue(prompt, null);
                    }
                    if (args.size() == 1 && args.get(0) instanceof BuiltinFunction converter) {
                        return new BuiltinFunction("Ask<" + converter.name + ">") {
                            @Override
                            public Object call(Interpreter nestedItp, List<Object> nestedArgs) {
                                if (nestedArgs.size() != 1 || !(nestedArgs.get(0) instanceof String prompt)) {
                                    throw new RoadstoneRuntimeError("ArgumentError", "Ask(Type)(\"question\") expects a single prompt string");
                                }
                                return askValue(prompt, converter);
                            }
                        };
                    }
                    throw new RoadstoneRuntimeError("ArgumentError", "Ask(\"question\") or Ask(Int)(\"question\") expected");
                }
            });

            globals.put("analyze", new BuiltinFunction("analyze") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.isEmpty() || args.size() > 2) {
                        throw new RoadstoneRuntimeError("ArgumentError", "analyze(mode, target?) expects 1 or 2 arguments");
                    }
                    String mode = stringify(args.get(0)).toLowerCase();
                    String target = args.size() >= 2 ? stringify(args.get(1)) : "";
                    return analyzeNetwork(mode, target);
                }
            });

            globals.put("type", new BuiltinFunction("type") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.size() != 1) {
                        throw new RoadstoneRuntimeError("ArgumentError", "type(value) expects 1 argument");
                    }
                    Object v = args.get(0);
                    if (v == null) return "nil";
                    if (v instanceof Double) return "number";
                    if (v instanceof String) return "string";
                    if (v instanceof Boolean) return "boolean";
                    if (v instanceof StorageUnit) return "store";
                    if (v instanceof RoadInstance) return "object";
                    if (v instanceof BuiltinFunction) return "builtin";
                    return "unknown";
                }
            });

<<<<<<< HEAD
            BuiltinFunction raiseBuiltin = new BuiltinFunction("raise") {
                @Override
                public Object call(Interpreter itp, List<Object> args) {
                    if (args.isEmpty() || args.size() > 2) {
                        throw new RoadstoneRuntimeError("ArgumentError", "raise(name, message?) expects 1 or 2 arguments");
                    }
                    String errorName = stringify(args.get(0));
                    String message = args.size() == 2 ? stringify(args.get(1)) : errorName;
                    throw new RoadstoneRuntimeError(errorName, message);
                }
            };
            globals.put("raise", raiseBuiltin);
            globals.put("error", raiseBuiltin);
=======
            globals.put("input", globals.get("Ask"));
>>>>>>> b744ab7b603e7d84b7f56108d5a86746a80246da
        }

        void execute(Program program) {
            Env env = new Env(null);
            env.locals.putAll(globalsAsDefaults());

            execBlock(env, new Block(program.statements));
        }

        private Map<String, Object> globalsAsDefaults() {
            // Ensure builtins are resolvable as "globals".
            // We'll keep env.locals empty and use globals map for global lookups.
            return Map.of();
        }

        // Signal used for return (implemented via exceptions for simplicity in this v0).
        static class ReturnSignal extends RuntimeException {
            final Object value;        // for: return <expr> where <expr> is not a bare identifier
            final String identifier;   // for: return <identifier> (may be a param or a normal variable)

            ReturnSignal(Object value, String identifier) {
                super(null, null, false, false);
                this.value = value;
                this.identifier = identifier;
            }
        }

        static class RoadstoneRuntimeError extends RuntimeException {
            final String errorName;
            RoadstoneRuntimeError(String errorName, String message) {
                super(message);
                this.errorName = errorName;
            }
        }

        static class Env {
            final Env parent;
            final Map<String, Object> locals = new HashMap<>();
            Env(Env parent) { this.parent = parent; }

            // local binding lookup (walk chain)
            VarRef findLocal(String name) {
                Env cur = this;
                while (cur != null) {
                    if (cur.locals.containsKey(name)) return new VarRef(cur, name, false);
                    cur = cur.parent;
                }
                return null;
            }
        }

        static class VarRef {
            final Env env;
            final String name;
            final boolean isGlobal;
            VarRef(Env env, String name, boolean isGlobal) { this.env = env; this.name = name; this.isGlobal = isGlobal; }
        }

        // Call context for parameter write-back
        static class CallContext {
            final List<String> paramNames;
            final List<VarRef> argVarRefs; // nullable; aligned with paramNames
            CallContext(List<String> paramNames, List<VarRef> argVarRefs) {
                this.paramNames = paramNames;
                this.argVarRefs = argVarRefs;
            }
        }

        Object execStmt(Env env, Stmt stmt, ReturnSignal returnSignalTemplate) {
            // returnSignalTemplate unused; placeholder to keep signature consistent while avoiding polymorphism.
            if (stmt instanceof LocalDecl ld) {
                Object v = ld.init == null ? null : evalExpr(env, ld.init, null);
                env.locals.put(ld.name, v);
                return null;
            }

            if (stmt instanceof GlobalDecl gd) {
                Object v = gd.init == null ? null : evalExpr(env, gd.init, null);
                globals.put(gd.name, v);
                return null;
            }

            if (stmt instanceof Assign a) {
                Object v = evalExpr(env, a.value, null);
                setLValue(env, a.target, v);
                return null;
            }

            if (stmt instanceof IfStmt ifs) {
                Object cv = evalExpr(env, ifs.cond, null);
                if (isTruthy(cv)) {
                    execBlock(env, ifs.thenBlock);
                } else {
                    boolean done = false;
                    for (ElseIf ei : ifs.elseIfs) {
                        Object ev = evalExpr(env, ei.cond, null);
                        if (isTruthy(ev)) {
                            execBlock(env, ei.block);
                            done = true;
                            break;
                        }
                    }
                    if (!done && ifs.elseBlock != null) {
                        execBlock(env, ifs.elseBlock);
                    }
                }
                return null;
            }

            if (stmt instanceof WhileStmt ws) {
                while (isTruthy(evalExpr(env, ws.cond, null))) {
                    Env loopEnv = new Env(env); // scope for loop
                    execBlock(loopEnv, ws.body);
                }
                return null;
            }

            if (stmt instanceof ForStmt fs) {
                Object countV = evalExpr(env, fs.countExpr, null);
                int count = toInt(countV, "for-loop count must be an integer");
                Env loopEnv = new Env(env);
                for (int i = 1; i <= count; i++) {
                    loopEnv = new Env(env);
                    loopEnv.locals.put("i", (double) i);
                    execBlock(loopEnv, fs.body);
                }
                return null;
            }

            if (stmt instanceof ForEachStmt fe) {
                Object iterable = evalExpr(env, fe.iterableExpr, null);
                if (iterable instanceof StorageUnit store) {
                    if (fe.valueName == null) {
                        for (Object value : store.entryValues()) {
                            Env loopEnv = new Env(env);
                            loopEnv.locals.put(fe.keyName, value);
                            execBlock(loopEnv, fe.body);
                        }
                    } else {
                        for (Map.Entry<Object, Object> entry : store.entrySet()) {
                            Env loopEnv = new Env(env);
                            loopEnv.locals.put(fe.keyName, entry.getKey());
                            loopEnv.locals.put(fe.valueName, entry.getValue());
                            execBlock(loopEnv, fe.body);
                        }
                    }
                    return null;
                }
                if (iterable instanceof String s) {
                    for (int idx = 0; idx < s.length(); idx++) {
                        Env loopEnv = new Env(env);
                        loopEnv.locals.put(fe.keyName, String.valueOf(s.charAt(idx)));
                        if (fe.valueName != null) {
                            loopEnv.locals.put(fe.valueName, (double) (idx + 1));
                        }
                        execBlock(loopEnv, fe.body);
                    }
                    return null;
                }
                throw new RoadstoneRuntimeError("TypeError", "for ... in expects a storage unit or string");
            }

            if (stmt instanceof FunctionDef fd) {
                RoadFunction fn = new RoadFunction(fd.params, fd.body, env);
                globals.put(fd.name, fn);
                return null;
            }

            if (stmt instanceof ReturnStmt rs) {
                if (rs.expr == null) {
                    throw new ReturnSignal(null, null);
                }
                // Special-case: `return <identifier>` so write-back can happen when that identifier matches a parameter.
                if (rs.expr instanceof VarExpr ve) {
                    throw new ReturnSignal(null, ve.name);
                }
                Object retVal = evalExpr(env, rs.expr, null);
                throw new ReturnSignal(retVal, null);
            }

            if (stmt instanceof ExprStmt es) {
                evalExpr(env, es.expr, null);
                return null;
            }

            if (stmt instanceof ExceptBlockStmt ebs) {
                try {
                    execBlock(env, ebs.tryBlock);
                } catch (RoadstoneRuntimeError re) {
                    if (!re.errorName.equals(ebs.targetErrorName)) {
                        throw re;
                    }
                    if (ebs.exoutputExpr != null) {
                        Env catchEnv = new Env(env);
                        catchEnv.locals.put("exname", ebs.replacementErrorName);
                        catchEnv.locals.put("extarget", re.errorName);
                        catchEnv.locals.put("exmessage", re.getMessage());
                        Object value = evalExpr(catchEnv, ebs.exoutputExpr, null);
                        System.out.println(stringify(value));
                    }
                    return null;
                }
                return null;
            }

            if (stmt instanceof ClassDef cd) {
                RoadClass cls = new RoadClass(cd.name, cd.fields, cd.baseName, cd.constructor, cd.methods, this);
                globals.put(cd.name, cls);
                return null;
            }

            throw new RoadstoneRuntimeError("RuntimeError", "unsupported statement: " + stmt.getClass().getSimpleName());
        }

        private void execBlock(Env env, Block block) {
            // EXCEPT["NewError", OldError] remaps any RoadstoneRuntimeError with errorName == OldError
            // to errorName == NewError for the entire block.
            ExceptStmt except = null;
            List<Stmt> execStmts = new ArrayList<>();
            for (Stmt s : block.statements) {
                if (s instanceof ExceptStmt es) {
                    except = es; // if multiple, the last one wins
                    continue;
                }
                execStmts.add(s);
            }

            for (Stmt s : execStmts) {
                try {
                    execStmt(env, s, new ReturnSignal(null, null));
                } catch (RoadstoneRuntimeError re) {
                    if (except != null && re.errorName.equals(except.targetErrorName)) {
                        throw new RoadstoneRuntimeError(except.replacementErrorName, re.getMessage());
                    }
                    throw re;
                } catch (ReturnSignal rs) {
                    throw rs;
                }
            }
        }

        private Object evalExpr(Env env, Expr expr, CallContext callContext) {
            if (expr instanceof NumberLit n) return n.value;
            if (expr instanceof StringLit s) return s.value;
            if (expr instanceof BoolLit b) return b.value;
            if (expr instanceof NilLit) return null;
            if (expr instanceof VarExpr v) return getVar(env, v.name);
            if (expr instanceof ListLit l) {
                StorageUnit out = new StorageUnit();
                for (Expr e : l.elements) out.putAuto(evalExpr(env, e, callContext));
                return out;
            }
            if (expr instanceof MapLit m) {
                StorageUnit out = new StorageUnit();
                for (int i = 0; i < m.keys.size(); i++) {
                    Object k = evalExpr(env, m.keys.get(i), callContext);
                    Object v = evalExpr(env, m.values.get(i), callContext);
                    out.put(k, v);
                }
                return out;
            }
            if (expr instanceof StoreLit s) {
                StorageUnit out = new StorageUnit();
                for (StoreEntry entry : s.entries) {
                    if (entry.key == null) {
                        out.putAuto(evalExpr(env, entry.value, callContext));
                    } else {
                        out.put(evalExpr(env, entry.key, callContext), evalExpr(env, entry.value, callContext));
                    }
                }
                return out;
            }
            if (expr instanceof MemberAccess m) {
                Object obj = evalExpr(env, m.obj, callContext);
                if (!(obj instanceof RoadInstance inst)) {
                    throw new RoadstoneRuntimeError("TypeError", "attempted member access on non-object");
                }
                // If it's a field, return the value.
                if (inst.props.containsKey(m.field)) {
                    return inst.props.get(m.field);
                }
                // Otherwise, if it's a method, return a bound method callable.
                RoadFunction method = inst.classRef.getMethod(m.field);
                if (method != null) {
                    // Method params are defined as: defi name(self, a, b, ...)
                    List<String> explicitParams = new ArrayList<>();
                    for (int i = 1; i < method.paramNames.size(); i++) {
                        explicitParams.add(method.paramNames.get(i));
                    }
                    return new BoundMethod(inst, method, explicitParams, m.field);
                }
                return null;
            }
            if (expr instanceof IndexAccess ix) {
                Object obj = evalExpr(env, ix.obj, callContext);
                Object idxV = evalExpr(env, ix.index, callContext);
                if (obj instanceof List<?> lst) {
                    int idx = toInt(idxV, "Index must be an integer for list access");
                    int zero = idx - 1;
                    if (zero < 0 || zero >= lst.size()) {
                        throw new RoadstoneRuntimeError("IndexError", "list index out of range");
                    }
                    return ((List<?>) obj).get(zero);
                }
                if (obj instanceof StorageUnit store) {
                    return store.get(idxV);
                }
                throw new RoadstoneRuntimeError("TypeError", "attempted indexing on unsupported value");
            }
            if (expr instanceof UnaryExpr u) {
                Object vv = evalExpr(env, u.expr, callContext);
                return switch (u.op) {
                    case "not" -> !isTruthy(vv);
                    case "-" -> -toNumber(vv, "Unary '-' expects a number");
                    default -> throw new RoadstoneRuntimeError("RuntimeError", "unknown unary operator: " + u.op);
                };
            }
            if (expr instanceof BinaryExpr b) {
                // Implement short-circuit for and/or
                if (b.op.equals("and")) {
                    Object lv = evalExpr(env, b.left, callContext);
                    if (!isTruthy(lv)) return false;
                    Object rv = evalExpr(env, b.right, callContext);
                    return isTruthy(rv);
                }
                if (b.op.equals("or")) {
                    Object lv = evalExpr(env, b.left, callContext);
                    if (isTruthy(lv)) return true;
                    Object rv = evalExpr(env, b.right, callContext);
                    return isTruthy(rv);
                }

                Object lv = evalExpr(env, b.left, callContext);
                Object rv = evalExpr(env, b.right, callContext);
                return switch (b.op) {
                    case "+" -> toNumber(lv, "Operator '+' expects numbers") + toNumber(rv, "Operator '+' expects numbers");
                    case "-" -> toNumber(lv, "Operator '-' expects numbers") - toNumber(rv, "Operator '-' expects numbers");
                    case "*" -> toNumber(lv, "Operator '*' expects numbers") * toNumber(rv, "Operator '*' expects numbers");
                    case "/" -> {
                        double a = toNumber(lv, "Operator '/' expects numbers");
                        double bnum = toNumber(rv, "Operator '/' expects numbers");
                        if (Math.abs(bnum) < 1e-12) throw new RoadstoneRuntimeError("ZeroDivisionError", "division by zero");
                        yield a / bnum;
                    }
                    case "%" -> {
                        double a = toNumber(lv, "Operator '%' expects numbers");
                        double bnum = toNumber(rv, "Operator '%' expects numbers");
                        if (Math.abs(bnum) < 1e-12) throw new RoadstoneRuntimeError("ZeroDivisionError", "modulo by zero");
                        yield a % bnum;
                    }
                    case ".." -> stringify(lv) + stringify(rv);
                    case "==" -> equalsValue(lv, rv);
                    case "!=" -> !equalsValue(lv, rv);
                    case "<" -> toNumber(lv, "Operator '<' expects numbers") < toNumber(rv, "Operator '<' expects numbers");
                    case "<=" -> toNumber(lv, "Operator '<=' expects numbers") <= toNumber(rv, "Operator '<=' expects numbers");
                    case ">" -> toNumber(lv, "Operator '>' expects numbers") > toNumber(rv, "Operator '>' expects numbers");
                    case ">=" -> toNumber(lv, "Operator '>=' expects numbers") >= toNumber(rv, "Operator '>=' expects numbers");
                    default -> throw new RoadstoneRuntimeError("RuntimeError", "unknown binary operator: " + b.op);
                };
            }
            if (expr instanceof CallExpr c) {
                Object fnVal = evalExpr(env, c.callee, callContext);
                List<Object> argVals = new ArrayList<>();

                // For write-back: capture call-site lvalues when arguments are identifiers.
                // (Only supported for identifier arguments in this v0.)
                List<VarRef> argVarRefs = null;
                if (fnVal instanceof RoadFunction rf) {
                    argVarRefs = new ArrayList<>();
                    for (Expr a : c.args) {
                        if (a instanceof VarExpr ve) {
                            VarRef ref = env.findLocal(ve.name);
                            if (ref == null && globals.containsKey(ve.name)) {
                                ref = new VarRef(null, ve.name, true);
                            }
                            argVarRefs.add(ref);
                            argVals.add(getVar(env, ve.name));
                        } else {
                            argVarRefs.add(null);
                            argVals.add(evalExpr(env, a, callContext));
                        }
                    }
                    CallContext ctx = new CallContext(rf.paramNames, argVarRefs);
                    return rf.call(this, env, argVals, ctx);
                }

                if (fnVal instanceof BoundMethod bm) {
                    // BoundMethod handles implicit self already as first argument.
                    // For v0: write-back for method params only when the explicit arguments are identifier lvalues.
                    argVals = new ArrayList<>();
                    List<VarRef> methodArgRefs = new ArrayList<>();
                    for (Expr a : c.args) {
                        if (a instanceof VarExpr ve) {
                            VarRef ref = env.findLocal(ve.name);
                            if (ref == null && globals.containsKey(ve.name)) {
                                ref = new VarRef(null, ve.name, true);
                            }
                            methodArgRefs.add(ref);
                            argVals.add(getVar(env, ve.name));
                        } else {
                            methodArgRefs.add(null);
                            argVals.add(evalExpr(env, a, callContext));
                        }
                    }
                    CallContext ctx = new CallContext(bm.paramNames, methodArgRefs);
                    return bm.call(this, env, argVals, ctx);
                }

                if (fnVal instanceof RoadClass cls) {
                    // Classes are callable: create instance and run constructor if present.
                    List<Object> argV = new ArrayList<>();
                    for (Expr a : c.args) argV.add(evalExpr(env, a, callContext));
                    return cls.instantiate(this, argV);
                }

                if (fnVal instanceof BuiltinFunction bi) {
                    for (Expr a : c.args) argVals.add(evalExpr(env, a, callContext));
                    return bi.call(this, argVals);
                }

                throw new RoadstoneRuntimeError("TypeError", "attempted to call non-function value");
            }
            throw new RoadstoneRuntimeError("RuntimeError", "unsupported expression: " + expr.getClass().getSimpleName());
        }

        private void setLValue(Env env, LValue target, Object value) {
            if (target instanceof LVar lv) {
                // If it already exists in locals, update it.
                VarRef localRef = env.findLocal(lv.name);
                if (localRef != null) {
                    localRef.env.locals.put(lv.name, value);
                    return;
                }
                // Otherwise, update global only if it already exists.
                if (globals.containsKey(lv.name)) {
                    globals.put(lv.name, value);
                    return;
                }
                throw new RoadstoneRuntimeError("NameError", "undefined variable '" + lv.name + "' (declare it with `local` or `global` first)");
            }
            if (target instanceof LMember lm) {
                Object obj = evalExpr(env, lm.obj, null);
                if (!(obj instanceof RoadInstance inst)) {
                    throw new RoadstoneRuntimeError("TypeError", "attempted member assignment on non-object");
                }
                inst.props.put(lm.field, value);
                return;
            }
            if (target instanceof LIndex li) {
                Object obj = evalExpr(env, li.obj, null);
                Object idxV = evalExpr(env, li.index, null);
                if (obj instanceof List<?> lst) {
                    int idx = toInt(idxV, "Index must be an integer for list assignment");
                    int zero = idx - 1;
                    if (zero < 0 || zero >= ((List<?>) obj).size()) {
                        throw new RoadstoneRuntimeError("IndexError", "list index out of range");
                    }
                    @SuppressWarnings("unchecked")
                    List<Object> mut = (List<Object>) obj;
                    mut.set(zero, value);
                    return;
                }
                if (obj instanceof StorageUnit store) {
                    store.put(idxV, value);
                    return;
                }
                throw new RoadstoneRuntimeError("TypeError", "attempted indexing assignment on unsupported value");
            }
            throw new RoadstoneRuntimeError("RuntimeError", "unsupported LValue");
        }

        private Object getVar(Env env, String name) {
            VarRef localRef = env.findLocal(name);
            if (localRef != null) return localRef.env.locals.get(name);
            if (globals.containsKey(name)) return globals.get(name);
            throw new RoadstoneRuntimeError("NameError", "undefined variable '" + name + "'");
        }

        private boolean equalsValue(Object a, Object b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }

        private boolean isTruthy(Object v) {
            return !(v == null || (v instanceof Boolean b && !b));
        }

        private double toNumber(Object v, String msg) {
            if (v instanceof Double d) return d;
            if (v instanceof Integer i) return i.doubleValue();
            throw new RoadstoneRuntimeError("TypeError", msg);
        }

        private int toInt(Object v, String msg) {
            double d = toNumber(v, msg);
            int i = (int) d;
            if (Math.abs(d - i) > 1e-9) throw new RoadstoneRuntimeError("TypeError", msg);
            return i;
        }

        private String stringify(Object v) {
            if (v == null) return "nil";
            if (v instanceof Double d) {
                // Print ints without .0 when possible.
                if (Math.abs(d - Math.rint(d)) < 1e-9) return String.valueOf((long) Math.rint(d));
                return String.valueOf(d);
            }
            if (v instanceof StorageUnit store) return store.toDisplayString();
            return String.valueOf(v);
        }

        private Object askValue(String prompt, BuiltinFunction converter) {
            System.out.print(prompt);
            Scanner scanner = new Scanner(System.in);
            String raw = scanner.nextLine();
            if (converter == null) {
                return raw;
            }
            return converter.call(this, List.of(raw));
        }

        private StorageUnit analyzeNetwork(String mode, String target) {
            try {
                StorageUnit result = new StorageUnit();
                result.put("mode", mode);
                if ("local".equals(mode) || "localhost".equals(mode)) {
                    InetAddress local = InetAddress.getLocalHost();
                    result.put("address", local.getHostAddress());
                    result.put("hostname", local.getHostName());
                    return result;
                }

                if (target.isBlank()) {
                    throw new RoadstoneRuntimeError("ArgumentError", "analyze(\"" + mode + "\", target) needs a target");
                }

                InetAddress addr = InetAddress.getByName(target);
                result.put("target", target);
                result.put("address", addr.getHostAddress());
                result.put("hostname", addr.getCanonicalHostName());

                switch (mode) {
                    case "ping" -> result.put("reachable", addr.isReachable(2000));
                    case "resolve", "lookup", "host" -> result.put("resolved", true);
                    default -> throw new RoadstoneRuntimeError("ArgumentError", "analyze() unsupported mode '" + mode + "'");
                }
                return result;
            } catch (RoadstoneRuntimeError ex) {
                throw ex;
            } catch (IOException ex) {
                throw new RoadstoneRuntimeError("NetworkError", ex.getMessage());
            }
        }

        class StorageUnit {
            private final LinkedHashMap<Object, Object> entries = new LinkedHashMap<>();
            private int nextAutoIndex = 1;

            void putAuto(Object value) {
                while (entries.containsKey((double) nextAutoIndex)) {
                    nextAutoIndex++;
                }
                entries.put((double) nextAutoIndex, value);
                nextAutoIndex++;
            }

            void put(Object key, Object value) {
                Object normalized = normalizeKey(key);
                entries.put(normalized, value);
                if (normalized instanceof Double d && Math.abs(d - Math.rint(d)) < 1e-9 && d >= nextAutoIndex) {
                    nextAutoIndex = ((int) Math.rint(d)) + 1;
                }
            }

            Object get(Object key) {
                return entries.get(normalizeKey(key));
            }

            boolean containsKey(Object key) {
                return entries.containsKey(normalizeKey(key));
            }

            boolean containsValue(Object value) {
                return entries.containsValue(value);
            }

            int size() {
                return entries.size();
            }

            List<Map.Entry<Object, Object>> entrySet() {
                return new ArrayList<>(entries.entrySet());
            }

            List<Object> entryValues() {
                return new ArrayList<>(entries.values());
            }

            StorageUnit keysAsStore() {
                StorageUnit out = new StorageUnit();
                for (Object key : entries.keySet()) out.putAuto(key);
                return out;
            }

            StorageUnit valuesAsStore() {
                StorageUnit out = new StorageUnit();
                for (Object value : entries.values()) out.putAuto(value);
                return out;
            }

            StorageUnit sortedCopy() {
                List<Map.Entry<Object, Object>> sorted = new ArrayList<>(entries.entrySet());
                boolean sequentialNumericKeys = true;
                int expected = 1;
                for (Object key : entries.keySet()) {
                    if (!(key instanceof Double d) || Math.abs(d - expected) > 1e-9) {
                        sequentialNumericKeys = false;
                        break;
                    }
                    expected++;
                }

                StorageUnit out = new StorageUnit();
                if (sequentialNumericKeys) {
                    List<Object> values = new ArrayList<>(entries.values());
                    values.sort((a, b) -> stringify(a).compareToIgnoreCase(stringify(b)));
                    for (Object value : values) out.putAuto(value);
                    return out;
                }

                sorted.sort((a, b) -> stringify(a.getKey()).compareToIgnoreCase(stringify(b.getKey())));
                for (Map.Entry<Object, Object> entry : sorted) {
                    out.put(entry.getKey(), entry.getValue());
                }
                return out;
            }

            String toDisplayString() {
                StringBuilder sb = new StringBuilder("store(");
                boolean first = true;
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(stringify(entry.getKey())).append(";").append(stringify(entry.getValue()));
                    first = false;
                }
                sb.append(")");
                return sb.toString();
            }

            private Object normalizeKey(Object key) {
                if (key instanceof Integer i) return (double) i;
                if (key instanceof Double d && Math.abs(d - Math.rint(d)) < 1e-9) return (double) Math.rint(d);
                return key;
            }
        }

        // Function values
        abstract class BuiltinFunction {
            final String name;
            BuiltinFunction(String name) { this.name = name; }
            abstract Object call(Interpreter itp, List<Object> args);
        }

        interface CallableLike {}

        class RoadFunction implements CallableLike {
            final List<String> paramNames;
            final Block body;
            final Env closureEnv;
            final Map<String, Integer> paramIndexes = new HashMap<>();

            RoadFunction(List<String> paramNames, Block body, Env closureEnv) {
                this.paramNames = paramNames;
                this.body = body;
                this.closureEnv = closureEnv;
                for (int i = 0; i < paramNames.size(); i++) {
                    paramIndexes.put(paramNames.get(i), i);
                }
            }

            Object call(Interpreter itp, Env callerEnv, List<Object> argVals, CallContext ctx) {
                Env callEnv = new Env(closureEnv);
                for (int i = 0; i < paramNames.size(); i++) {
                    Object v = argVals.size() > i ? argVals.get(i) : null;
                    callEnv.locals.put(paramNames.get(i), v);
                }
                try {
                    execBlock(callEnv, body);
                    return null;
                } catch (ReturnSignal rs) {
                    if (rs.identifier != null) {
                        return resolveReturnedIdentifier(callEnv, rs.identifier, ctx, 0);
                    }
                    return rs.value;
                }
            }

            Integer paramIndex(String name) {
                return paramIndexes.get(name);
            }

            Object resolveReturnedIdentifier(Env callEnv, String identifier, CallContext ctx, int implicitParamOffset) {
                Integer paramIndex = paramIndex(identifier);
                if (paramIndex != null) {
                    Object newVal = callEnv.locals.get(identifier);
                    int explicitIndex = paramIndex - implicitParamOffset;
                    if (explicitIndex >= 0 && ctx != null && explicitIndex < ctx.argVarRefs.size()) {
                        VarRef ref = ctx.argVarRefs.get(explicitIndex);
                        if (ref != null) {
                            if (ref.isGlobal) {
                                globals.put(ref.name, newVal);
                            } else {
                                ref.env.locals.put(ref.name, newVal);
                            }
                        }
                    }
                    return newVal;
                }
                return getVar(callEnv, identifier);
            }
        }

        // Bound method (auto-binds self)
        class BoundMethod implements CallableLike {
            final RoadInstance selfInstance;
            final RoadFunction fn;
            final List<String> paramNames; // method params excluding self (we keep it explicit in parsing but bind it here)
            final String methodName;

            BoundMethod(RoadInstance selfInstance, RoadFunction fn, List<String> paramNames, String methodName) {
                this.selfInstance = selfInstance;
                this.fn = fn;
                this.paramNames = paramNames;
                this.methodName = methodName;
            }

            Object call(Interpreter itp, Env callerEnv, List<Object> explicitArgs, CallContext ctx) {
                // Method functions in this v0 are defined as: defi methodName(self, a, b, ...)
                // We bind self into the first param slot and pass explicit args after.
                List<String> allParams = fn.paramNames;
                if (allParams.isEmpty() || !allParams.get(0).equals("self")) {
                    throw new RoadstoneRuntimeError("RuntimeError", "method must declare first parameter as self");
                }
                Env callEnv = new Env(fn.closureEnv);
                callEnv.locals.put("self", selfInstance);
                for (int i = 1; i < allParams.size(); i++) {
                    Object v = explicitArgs.size() >= (i - 1) ? explicitArgs.get(i - 1) : null;
                    callEnv.locals.put(allParams.get(i), v);
                }
                try {
                    execBlock(callEnv, fn.body);
                    return null;
                } catch (ReturnSignal rs) {
                    if (rs.identifier != null) {
                        return fn.resolveReturnedIdentifier(callEnv, rs.identifier, ctx, 1);
                    }
                    return rs.value;
                }
            }
        }

        // Class / instance values
        class RoadClass implements CallableLike {
            final String name;
            final List<String> fields;
            final String baseName;
            final FunctionDef constructorDef; // may be null
            final List<FunctionDef> methodDefs;
            final Interpreter itp;
            final RoadFunction constructorFn; // may be null
            final Map<String, RoadFunction> methods = new HashMap<>();

            RoadClass(String name, List<String> fields, String baseName, FunctionDef constructorDef, List<FunctionDef> methodDefs, Interpreter itp) {
                this.name = name;
                this.fields = fields;
                this.baseName = baseName;
                this.constructorDef = constructorDef;
                this.methodDefs = methodDefs;
                this.itp = itp;

                // Compile members into runtime functions (closures point to global env only).
                if (constructorDef != null) {
                    this.constructorFn = new RoadFunction(constructorDef.params, constructorDef.body, new Env(null));
                } else {
                    this.constructorFn = null;
                }
                for (FunctionDef md : methodDefs) {
                    // Methods are functions stored under their name.
                    this.methods.put(md.name, new RoadFunction(md.params, md.body, new Env(null)));
                }
            }

            Object instantiate(Interpreter itp, List<Object> args) {
                RoadInstance inst = new RoadInstance(this);
                // Initialize fields to nil (including inherited base fields).
                for (String f : getAllFields()) inst.props.put(f, null);

                // Run constructor if present
                if (constructorFn != null) {
                    // constructor is declared as: construct(p1, p2, ...) where body uses self.<field>
                    // We'll call constructor like a function with env locals, with `self` inserted.
                    Env callEnv = new Env(null);
                    callEnv.locals.put("self", inst);
                    for (int i = 0; i < constructorFn.paramNames.size(); i++) {
                        String pName = constructorFn.paramNames.get(i);
                        Object v = args.size() > i ? args.get(i) : null;
                        callEnv.locals.put(pName, v);
                    }
                    try {
                        execBlock(callEnv, constructorFn.body);
                    } catch (ReturnSignal rs) {
                        // constructor return value ignored in this v0
                    }
                } else {
                    // If no constructor, map arguments to fields in order.
                    for (int i = 0; i < fields.size() && i < args.size(); i++) {
                        inst.props.put(fields.get(i), args.get(i));
                    }
                }
                return inst;
            }

            RoadFunction getMethod(String methodName) {
                if (methods.containsKey(methodName)) return methods.get(methodName);
                if (baseName != null) {
                    Object baseVal = globals.get(baseName);
                    if (baseVal instanceof RoadClass baseCls) {
                        return baseCls.getMethod(methodName);
                    }
                }
                return null;
            }

            private List<String> getAllFields() {
                LinkedHashSet<String> out = new LinkedHashSet<>();
                if (baseName != null) {
                    Object baseVal = globals.get(baseName);
                    if (baseVal instanceof RoadClass baseCls) {
                        out.addAll(baseCls.getAllFields());
                    }
                }
                out.addAll(fields);
                return new ArrayList<>(out);
            }
        }

        class RoadInstance {
            final RoadClass classRef;
            final Map<String, Object> props = new HashMap<>();
            RoadInstance(RoadClass classRef) { this.classRef = classRef; }
        }

        // Evaluate member access result: if field matches method name, return bound method.
        // (In this v0 we keep it minimal by treating member access as:
        // - if property exists => value
        // - else if method exists => bound method object
        // else nil)
        {
            // no-op
        }
    }

    // Patch: member access currently only supports properties.
    // We keep this placeholder to avoid changing many lines.
}
