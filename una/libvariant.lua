-- gabien-common - Cross-platform game and UI framework
-- Written starting in 2016 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

local lv = {}

local typeCount = 3
lv.typeCount = typeCount

function lv.variantCount(argCount)
 local count = typeCount
 for i = 1, argCount do
  count = count * typeCount
 end
 return count
end

function lv.compile(comp)
 local variant = 0
 for _, v in ipairs(comp) do
  variant = (variant * typeCount) + (v - 1)
 end
 return variant
end

function lv.decompile(argCount, variant)
 local decomp = {}
 for i = 1, argCount + 1 do
  table.insert(decomp, 1, (variant % typeCount) + 1)
  variant = variant // typeCount
 end
 return decomp
end

return lv

