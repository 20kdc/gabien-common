-- gabien-common - Cross-platform game and UI framework
-- Written starting in 2016 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

local ifn, ofn = ...

print("TimeLogger -> Chrome JSON")
print("Converting: " .. ifn .. " -> " .. ofn)

local i = io.open(ifn, "rb")
local ic = i:read("*a")
i:close()

local ptr = 1
local o = io.open(ofn, "w")

-- Writing Side
o:write("[")
local isFirstEvent = true

local function writeEventHdr(et, tid)
 if not isFirstEvent then 
  o:write(",")
 end
 isFirstEvent = false
 o:write("{\"ph\":\"" .. et .. "\",\"pid\":0,\"tid\":" .. tostring(tid))
end

local function writeTIDEvent(tid, name)
 writeEventHdr("M", tid)
 o:write(",\"name\":\"thread_name\"")
 o:write(",\"args\":{")
 o:write("\"name\":\"" .. name .. "\"")
 o:write("}")
 o:write("}\n")
end
local function writeZone(tid, name, close, ts)
 if close then
  writeEventHdr("E", tid)
 else
  writeEventHdr("B", tid)
 end
 o:write(",\"name\":\"" .. name .. "\"")
 -- Chrome format uses nanoseconds rather than microseconds
 o:write(",\"ts\":" .. tostring(ts // 1000))
 o:write("}\n")
end

local recordedNames = {}

-- Reading Side
while ptr <= #ic do
 local hdr = ic:sub(ptr, ptr + 12)
 if #hdr < 13 then break end
 local rType, rID, rTS = string.unpack(">i1i4i8", hdr)
 ptr = ptr + 13
 if rType == 0 then
  -- has string
  local len = string.unpack(">I2", ic, ptr)
  ptr = ptr + 2
  local name = ic:sub(ptr, ptr + len - 1)
  ptr = ptr + len
  writeTIDEvent(rID, name)
  recordedNames[rID] = name
 elseif rType == 1 then
  writeZone(rID, recordedNames[rID], false, rTS)
 elseif rType == 2 then
  writeZone(rID, recordedNames[rID], true, rTS)
 end
end
o:write("]")
