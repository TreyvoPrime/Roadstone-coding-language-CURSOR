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

