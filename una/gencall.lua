-- gabien-common - Cross-platform game and UI framework
-- Written starting in 2016 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

-- tawa kama pona tan jan ale --

local initialDisclaimer = [[
/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
/* THIS FILE IS MACHINE GENERATED BY gencall.lua */
]]
local c = io.open("c/unacall.c", "w")
local j = io.open("src/main/java/gabien/una/UNAC.java", "w")

c:write(initialDisclaimer)
c:write([[
#include <stdint.h>
#include <stddef.h>
]])
j:write(initialDisclaimer)

local types = {"I", "L", "F", "D"}
local typesJ = {"int ", "long ", "float ", "double "}
local typesCJ = {"int32_t ", "int64_t ", "float ", "double "}
local typesCN = {"int32_t", "int64_t", "float", "double"}

local function gen(ret, args)
 local signature = "c" .. types[ret]
 local argsHaveEnded = false
 local invalid = false
 local argsCJ = "void * env, void * self"
 local argsCN = ""
 local argsCC = ""
 local argsJ = ""
 local n = 0
 for _, v in ipairs(args) do
  signature = signature .. types[v]
  if n ~= 0 then
   argsCN = argsCN .. ", "
   argsCC = argsCC .. ", "
  end
  argsCJ = argsCJ .. ", " .. typesCJ[v] .. "a" .. tostring(n)
  argsCN = argsCN .. typesCN[v]
  argsCC = argsCC .. "(" .. typesCN[v] .. ") a" .. tostring(n)
  argsJ = argsJ .. typesJ[v] .. "a" .. tostring(n) .. ", "
  n = n + 1
 end
 if not invalid then
  j:write("    public static native " .. typesJ[ret] .. signature .. "(" .. argsJ .. "long code);\n")
  c:write("int64_t Java_gabien_una_UNAC_" .. signature .. "(" .. argsCJ .. ", int64_t code) {\n")
  c:write("    return ((int64_t (*)(" .. argsCN .. ")) (intptr_t) code)(" .. argsCC .. ");\n")
  c:write("}\n")
 end
end

j:write("\n")
j:write("package gabien.una;\n")
j:write("\n")
j:write("/**\n")
j:write(" * UNA function call natives. No shenanigans for maximum performance.\n")
j:write(" */\n")
j:write("public class UNAC {\n")
-- cdecl
for argCount = 0, 4 do
 local args = {}
 for i = 1, argCount do
  table.insert(args, 1)
 end
 -- outer loop
 while true do
  for ret = 1, #types do
   gen(ret, args)
  end
  -- increment loop
  local incPtr = argCount
  while incPtr > 0 do
   args[incPtr] = args[incPtr] + 1
   if args[incPtr] > #types then
    -- carry
    args[incPtr] = 1
    incPtr = incPtr - 1
   else
    -- done
    break
   end
  end
  if incPtr == 0 then
   -- escaped, we're done
   break
  end
 end
end
-- done!
j:write("}\n")

