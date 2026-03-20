# Roadstone Language 0.6 Documentation Sheet

This sheet documents the syntax and runtime behavior implemented by the current `RoadstoneMain.java` interpreter prototype.

## Quick Notes / Rules
- No semicolons
- Blocks end with `end`
- Single-line comments use `--`
- `if` / `elseif` / `else` use `then` but DO NOT use a colon:
  - `if <cond> then ... end`
  - `elseif <cond> then ... end`

## Comments
```text
-- comment until the end of the line
```

## Keywords
`local`, `global`, `for`, `then`, `loop`, `end`, `if`, `elseif`, `else`, `while`, `defi`, `return`, `and`, `or`, `not`, `true`, `false`, `nil`, `CLASS`, `construct`, `self`, `extends`, `EXCEPT`, `exoutput`

## Variables and Scopes

### Declare variables
```text
local x = <expr>
local x

global g = <expr>
global g
```

### Update existing variables
```text
x = <expr>
```

Important rule: `x = ...` only works if `x` was already declared as `local` or `global`.
If not, the program throws `NameError`.

### Assignment targets (lvalues)
These are valid on the left side of `=`.
- simple variable: `x = <expr>`
- object field: `self.x = <expr>` or `obj.x = <expr>`
- list index: `nums[1] = <expr>`
- map key: `m["wins"] = <expr>` (any expression allowed as key)

## Blocks and Control Flow

### If / Elseif / Else
```text
if <condition> then
    <statements>
elseif <condition> then
    <statements>
else
    <statements>
end
```

### While
```text
while <condition> loop
    <statements>
end
```

### For (counted loop)
Chosen syntax (matches the implemented interpreter):
```text
for <count_expr> then loop
    <statements>
end
```

Semantics:
- `<count_expr>` is evaluated once
- the loop runs with `i` from `1` to `<count_expr>` inclusive
- `<count_expr>` must be an integer-valued number (non-integer triggers `TypeError`)

Example:
```text
local sum = 0
for 5 then loop
    sum = sum + i
end
print(sum)
```

## Truthiness (conditions)
In conditions (`if`, `while`, `and`, `or`):
- falsey values: `false`, `nil`
- everything else is truthy (including `0`, `""`, empty lists/maps)

## Expressions

### Literals
- Number: `42`, `3.14`
- String (double quotes): `"hello"`
- Boolean: `true`, `false`
- Nil: `nil`

### Arithmetic / Operators
Operator precedence (highest to lowest):
1. Unary: `-expr`, `not expr`
2. Multiplication: `*`, `/`, `%`
3. Addition: `+`, `-`
4. Concatenation: `..` (string concatenation)
5. Comparisons: `==`, `!=`, `<`, `<=`, `>`, `>=`
6. Logical AND: `and`
7. Logical OR: `or`

Notes:
- `..` is string concatenation
- `+ - * / %` are numeric operators in this v0 (using them with non-numbers throws `TypeError`)
- Equality `==` compares by Java value equality for primitives; for lists/maps/objects, equality behaves like reference/identity behavior typical of dynamic languages.

### Parentheses
```text
( <expr> )
```

## Functions

### Define
```text
defi functionName(param1, param2, ...)
    <statements>
end
```

### Call
```text
functionName(arg1, arg2, ...)
```

### Return
`return` exits the function.

Forms:
- `return` (no expression) => returns `nil`
- `return <expr>` => returns the value of `<expr>`
- Your special write-back rule:
  - `return <paramName>`
  - where `<paramName>` is exactly one of the function parameters
  - if the caller passed that corresponding argument as an identifier lvalue, then the caller’s variable is updated with the parameter’s updated value

Example (write-back into a local caller variable):
```text
defi chicken(egg)
    egg = egg + egg
    return egg
end

local egg = 3
chicken(egg)
print(egg)  -- prints 6
```

Example (write-back into a global caller variable):
```text
defi chicken(egg)
    egg = egg + egg
    return egg
end

global egg = 3
chicken(egg)
print(egg) -- prints 6
```

## Classes

### Declare a class
```text
CLASS ClassName(field1, field2, ...)
    [extends BaseClassName]

    -- class body members (v0 supports constructor + methods)
end
```

### Constructor: `construct`
Inside the class body:
```text
construct(p1, p2, ...)
    <statements>
end
```

Typical constructor usage:
- assign instance fields through `self.<field>`
```text
CLASS Circle(radius)
    construct(r)
        self.radius = r
    end

    defi area(self)
        return 3.14159 * self.radius * self.radius
    end
end

local c = Circle(2)
print(c.area())
```

### Methods
Define methods inside the class using `defi`:
```text
defi methodName(self, param1, param2, ...)
    <statements>
end
```

### Call methods
Method call sugar:
```text
obj.methodName(arg1, arg2, ...)
```

The interpreter binds `self` automatically to `obj`.

### Inheritance (`extends`)
Inheritance is supported for inherited method lookup:
```text
CLASS Child(b) extends Parent
    construct(x, y)
        -- set fields yourself in this v0
    end

    defi getB(self)
        return self.b
    end
end
```

In this v0:
- inherited methods can be called normally
- inherited fields are included in the instance initialization
- base constructor is NOT automatically called; you typically set `self.<baseField>` yourself in the child constructor if you need it

## Data Types

### Lists (arrays)
List literal:
```text
local nums = [10, 20, 30]
```

Indexing is 1-based:
```text
print(nums[1])     -- 10
nums[2] = 99
print(nums[2])     -- 99
```

### Maps (dictionaries / tables)
Map literal:
```text
local m = { "wins": 3, "losses": 1 }
```

Access / update:
```text
print(m["wins"])
m["wins"] = m["wins"] + 1
```

## Built-in Functions (v0)
These are available globally.

### `print(value)`
Prints the value representation (numbers, strings, booleans, nil, lists, maps, objects).
```text
print("hello")
print(123)
```

### `len(value)`
Works on:
- list
- map
- string
- object (instance fields count)
```text
print(len([1,2,3]))        -- 3
print(len({ "a": 1 }))     -- 1
```

### `keys(map)`
Returns a list of keys from a map.
```text
local ks = keys({ "a": 1, "b": 2 })
print(ks[1])
```

### `type(value)`
Returns one of: `nil`, `number`, `string`, `boolean`, `list`, `map`, `object`.
```text
print(type(nil))
print(type(3))
```

## Error Handling

### Legacy remap syntax
```text
EXCEPT["NewErrorName", OldErrorName]
```

### Runtime remapping (legacy behavior)
If this statement appears in a block, and during execution a runtime error occurs with `errorName == OldErrorName`, then the interpreter throws the same error message but with `errorName == NewErrorName`.

Example (rename division-by-zero):
```text
defi divide(a, b)
    return a / b
end

defi run()
    local x = divide(1, 0)
    EXCEPT["SigmaError", ZeroDivisionError]
end

run()
```

### Lexer/Parser error remapping (whole file scan)
Because parsing happens before execution, the interpreter also supports remapping lexer/parser errors by scanning the whole file for any `EXCEPT[...]` directives.

Example:
```text
EXCEPT["MyParserError", ParserError]

if true then
    print("missing end")
```

This remaps the thrown parser error name accordingly.

### 0.6 catch block with `exoutput`
```text
EXCEPT["FriendlyError", ZeroDivisionError] then
    <statements>
exoutput <expr>
end
```

Behavior:
- the statements after `then` run inside a protected EXCEPT block
- if a `RoadstoneRuntimeError` with name `OldErrorName` happens, it is caught
- the catch exposes:
  - `exname` => replacement error name
  - `extarget` => original runtime error name
  - `exmessage` => runtime error message
- `exoutput <expr>` evaluates and prints the expression
- execution then continues after the EXCEPT block

Example:
```text
EXCEPT["FriendlyDivideError", ZeroDivisionError] then
    raise("ZeroDivisionError", "divider cannot be zero")
exoutput "Caught " .. exname .. ": " .. exmessage
end

print("still running")
```

### Manual error creation
Roadstone 0.6 adds:
```text
raise(name, message)
error(name, message)
```

Both create a runtime error immediately. `error(...)` is an alias of `raise(...)`.

## Operators and Examples (Copy/Paste)

### If
```text
if x > 0 then
    print("positive")
else
    print("non-positive")
end
```

### Concatenation
```text
local s = "hi " .. "there"
print(s)
```

### Loops
```text
local n = 5
while n > 0 loop
    print(n)
    n = n - 1
end
```

## Summary: “What do I write?”
If you remember nothing else:
- declare: `local x = ...` or `global x = ...`
- update existing: `x = ...`
- blocks: `... end`
- conditionals: `if ... then ... else ... end`
- loops:
  - `for <count> then loop ... end`
  - `while <cond> loop ... end`
- functions: `defi name(args) ... end`
- return:
  - `return <expr>`
  - `return <paramName>` triggers write-back
- classes:
  - `CLASS Name(fields...) ... end`
  - `construct(...) ... end`
  - methods: `defi methodName(self, ...) ... end`
  - instantiate: `local obj = Name(arg1, ...)`
- data:
  - list: `[ ... ]` (1-based index)
  - map: `{ key: value, ... }`
- errors:
  - legacy remap: `EXCEPT["New", Old]`
  - catch block: `EXCEPT["New", Old] then ... exoutput ... end`
  - manual raise: `raise("MyError", "details")`

