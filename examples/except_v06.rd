EXCEPT["FriendlyDivideError", ZeroDivisionError] then
  local divider = 0
  raise("ZeroDivisionError", "divider cannot be zero")
exoutput "Caught " .. exname .. ": " .. exmessage
end

print("Program kept going.")
