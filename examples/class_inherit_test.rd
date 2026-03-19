CLASS Parent(a)
    construct(x)
        self.a = x
    end

    defi get(self)
        return self.a
    end
end

CLASS Child(b) extends Parent
    construct(x, y)
        -- set base field + child field
        self.a = x
        self.b = y
    end

    defi getB(self)
        return self.b
    end
end

local c = Child(10, 20)
print(c.get())
print(c.getB())

