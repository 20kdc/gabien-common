--[[
gabien-common - Cross-platform game and UI framework
Written starting in 2016 by contributors (see CREDITS.txt)
To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
--]]

local inCode = false

while true do
 local ln = io.read()
 if not ln then break end
 if ln == "" then
  print()
 elseif ln:sub(1, 2) == " *" then
  if inCode then
   print("```")
   print()
   inCode = false
  end
  print(ln:sub(4))
 elseif ln ~= "/*" and ln ~= " */" then
  if not inCode then
   print()
   print("```c")
   inCode = true
  end
  print(ln)
 end
end

if inCode then
 print("```")
 print()
else
 print()
end

