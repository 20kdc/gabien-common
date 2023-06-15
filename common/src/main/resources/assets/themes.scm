; gabien-common - Cross-platform game and UI framework
; Written starting in 2016 by contributors (see CREDITS.txt)
; To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
; A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

; Theme configuration file.
; Defines border flags, border types, and image regions.
; Border flags are: moveDown, clear, lightBkg

iThemes (img "themes.png")

; Define each individual theme as a region of the main PNG file.

iT0 (reg iThemes (rect 0  0 168 18))
iT1 (reg iThemes (rect 0 18 168 18))
iT2 (reg iThemes (rect 0 36 168 18))
iT3 (reg iThemes (rect 0 54 168 18))

rB0  (rect   0 0 12 18)
rB1  (rect  12 0 12 18)
rB2  (rect  24 0 12 18)
rB3  (rect  36 0 12 18)
rB4  (rect  48 0 12 18)
rB5  (rect  60 0 12 18)
rB6  (rect  72 0 12 18)
rB7  (rect  84 0 12 18)
rB8  (rect  96 0 12 18)
rB9  (rect 108 0 12 18)
rB10 (rect 120 0 12 18)
rB11 (rect 132 0 12 18)
rB12 (rect 144 0 12 18)
rB13 (rect 156 0 12 18)

t0 (theme
	btn (border
		(reg iT0 rB0)
	)
	btnP (border
		(reg iT0 rB1)
		moveDown
	)
	label (border
		(reg iT0 rB2)
	)
	textBox (border
		(reg iT0 rB3)
	)
	textBoxF (border
		(reg iT0 rB4)
	)
	window (border
		(reg iT0 rB5)
		clear
	)
	sbTray (border
		(reg iT0 rB6)
		tiled
	)
	sbNub (border
		(reg iT0 rB7)
		tiled
	)
	tabA (border
		(reg iT0 rB8)
	)
	tabB (border
		(reg iT0 rB9)
	)
	tabSel (border
		(reg iT0 rB10)
	)
	i11 (border
		(reg iT0 rB11)
	)
	i12 (border
		(reg iT0 rB12)
	)
	r48Overlay (border
		(reg iT0 rB13)
	)
)

t1 (theme
	btn (border
		(reg iT1 rB0)
		tiled
	)
	btnP (border
		(reg iT1 rB1)
		tiled moveDown
	)
	label (border
		(reg iT1 rB2)
		tiled
	)
	textBox (border
		(reg iT1 rB3)
		tiled
	)
	textBoxF (border
		(reg iT1 rB4)
		tiled
	)
	window (border
		(reg iT1 rB5)
		tiled
	)
	sbTray (border
		(reg iT1 rB6)
	)
	sbNub (border
		(reg iT1 rB7)
		tiled
	)
	tabA (border
		(reg iT1 rB8)
		tiled
	)
	tabB (border
		(reg iT1 rB9)
		tiled
	)
	tabSel (border
		(reg iT1 rB10)
		tiled
	)
	i11 (border
		(reg iT1 rB11)
		tiled
	)
	i12 (border
		(reg iT1 rB12)
		tiled
	)
	r48Overlay (border
		(reg iT1 rB13)
	)
)

t2 (theme
	btn (border
		(reg iT2 rB0)
		tiled lightBkg
	)
	btnP (border
		(reg iT2 rB1)
		tiled moveDown lightBkg
	)
	label (border
		(reg iT2 rB2)
		tiled lightBkg
	)
	textBox (border
		(reg iT2 rB3)
		tiled lightBkg
	)
	textBoxF (border
		(reg iT2 rB4)
		tiled lightBkg
	)
	window (border
		(reg iT2 rB5)
		tiled lightBkg
	)
	sbTray (border
		(reg iT2 rB6)
		lightBkg
	)
	sbNub (border
		(reg iT2 rB7)
		tiled lightBkg
	)
	tabA (border
		(reg iT2 rB8)
		tiled lightBkg
	)
	tabB (border
		(reg iT2 rB9)
		tiled lightBkg
	)
	tabSel (border
		(reg iT2 rB10)
		tiled lightBkg
	)
	i11 (border
		(reg iT2 rB11)
		tiled lightBkg
	)
	i12 (border
		(reg iT2 rB12)
		tiled lightBkg
	)
	r48Overlay (border
		(reg iT2 rB13)
	)
)

t3 (theme
	btn (border
		(reg iT3 rB0)
	)
	btnP (border
		(reg iT3 rB1)
	)
	label (border
		(reg iT3 rB2)
	)
	textBox (border
		(reg iT3 rB3)
	)
	textBoxF (border
		(reg iT3 rB4)
	)
	window (border
		(reg iT3 rB5)
		clear
	)
	sbTray (border
		(reg iT3 rB6)
		tiled
	)
	sbNub (border
		(reg iT3 rB7)
		tiled
	)
	tabA (border
		(reg iT3 rB8)
	)
	tabB (border
		(reg iT3 rB9)
	)
	tabSel (border
		(reg iT3 rB10)
	)
	i11 (border
		(reg iT3 rB11)
	)
	i12 (border
		(reg iT3 rB12)
	)
	r48Overlay (border
		(reg iT3 rB13)
	)
)
