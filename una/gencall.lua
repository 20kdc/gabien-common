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

local conventions = {"c", "s"}
local conventionsC = {"", "__attribute__((stdcall))"}
local types = {"V", "I", "L", "P"}
local typesJ = {"void ", "int ", "long ", "long "}
local typesCJ = {"void ", "int32_t ", "int64_t ", "int64_t "}
local typesCN = {"void", "int32_t", "int64_t", "intptr_t"}

local function gen(ccv, args)
 local signature = conventions[ccv]
 local argsHaveEnded = false
 local invalid = false
 local argsCJ = "void * env, void * self, int64_t code"
 local argsCN = ""
 local argsCC = ""
 local argsJ = "long code"
 local n = 0
 for _, v in ipairs(args) do
  local isVoid = types[v] == "V"
  if isVoid then
   argsHaveEnded = true
  elseif argsHaveEnded then
   invalid = true
  else
   signature = signature .. types[v]
   argsCJ = argsCJ .. ", " .. typesCJ[v] .. "a" .. tostring(n)
   if n ~= 0 then
    argsCN = argsCN .. ", "
    argsCC = argsCC .. ", "
   end
   argsCN = argsCN .. typesCN[v]
   argsCC = argsCC .. "(" .. typesCN[v] .. ") a" .. tostring(n)
   argsJ = argsJ .. ", " .. typesJ[v] .. "a" .. tostring(n)
  end
  n = n + 1
 end
 if not invalid then
  j:write("    public static native long " .. signature .. "(" .. argsJ .. ");\n")
  c:write("int64_t Java_gabien_una_UNAC_" .. signature .. "(" .. argsCJ .. ") {\n")
  c:write("    return ((int64_t (" .. conventionsC[ccv] .. "*)(" .. argsCN .. ")) (intptr_t) code)(" .. argsCC .. ");\n")
  c:write("}\n")
 end
end

j:write("\n")
j:write("package gabien.una;\n")
j:write("\n")
j:write("/**\n")
j:write(" * UNA function call natives. 0-6 arguments with no shenanigans for maximum performance.\n")
j:write(" */\n")
j:write("public class UNAC {\n")
for ccv = 1, #conventions do
 for a0 = 1, #types do
  for a1 = 1, #types do
   for a2 = 1, #types do
    for a3 = 1, #types do
     for a4 = 1, #types do
      for a5 = 1, #types do
       gen(ccv, {a0, a1, a2, a3, a4, a5})
      end
     end
    end
   end
  end
 end
end
j:write("}\n")

