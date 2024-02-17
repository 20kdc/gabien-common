#!/usr/bin/env lua
os.execute("rm -rf sf-comparison")
os.execute("mkdir -p sf-comparison")
for i = 0, 127 do
 f = io.open("tmp.mid", "wb")
 f:write("MThd\x00\x00\x00\x06\x00\x00\x00\x01\x00\x01")
 f:write("MTrk\x00\x00\x00\x0B")
 f:write(string.char(0, 0xC0, i))
 f:write(string.char(0, 0x90, 64, 127))
 f:write(string.char(16, 0x80, 64, 127))
 f:close()
 os.execute("fluidsynth -g1 -F sf-comparison/" .. tostring(i) .. ".wav /usr/share/sounds/sf2/TimGM6mb.sf2 tmp.mid")
end
