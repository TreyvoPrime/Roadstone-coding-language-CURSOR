local answers = store("first"; 9, "second"; 3, "third"; 12)
local ordered = sort(values(answers))
for item in ordered loop
    print(item)
end

for key, value in answers loop
    print(key .. ":" .. value)
end

local roster = ["delta", "alpha", "charlie"]
push(roster, "bravo")
print(sort(roster)[1])
print(len("Roadstone"))
print(type(store("name"; "Roadstone")))
print(analyze("ping", "127.0.0.1")["reachable"])
