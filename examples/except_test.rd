defi divide(a, b)
    return a / b
end

defi run()
    local x = divide(1, 0)
    EXCEPT["SigmaError", ZeroDivisionError]
end

run()

