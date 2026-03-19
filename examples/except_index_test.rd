defi run()
    local nums = [1, 2]
    local x = nums[10]
    EXCEPT["MyIndexError", IndexError]
    return nil
end

run()

