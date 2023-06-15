; gabien-common - Cross-platform game and UI framework
; Written starting in 2016 by contributors (see CREDITS.txt)
; To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
; A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

; This would be a theme configuration file, but right now it's not much of one.
; Still, the border flags are saner now.
; Border flags are: moveDown, clear, tiled, lightBkg
t0 (theme
	btn (borderR
	)
	btnP (borderR
		moveDown #t
	)
	label (borderR
	)
	textBox (borderR
	)
	textBoxF (borderR
	)
	window (borderR
		clear #t
	)
	sbTray (borderT
	)
	sbNub (borderT
	)
	tabA (borderR
	)
	tabB (borderR
	)
	tabSel (borderR
	)
	i11 (borderR
	)
	i12 (borderR
	)
	r48Overlay (borderR
	)
)

t1 (theme
	btn (borderT
	)
	btnP (borderT
		moveDown #t
	)
	label (borderT
	)
	textBox (borderT
	)
	textBoxF (borderT
	)
	window (borderT
	)
	sbTray (borderR
	)
	sbNub (borderT
	)
	tabA (borderT
	)
	tabB (borderT
	)
	tabSel (borderT
	)
	i11 (borderT
	)
	i12 (borderT
	)
	r48Overlay (borderR
	)
)

t2 (theme
	btn (borderT
		lightBkg #t
	)
	btnP (borderT
		moveDown #t lightBkg #t
	)
	label (borderT
		lightBkg #t
	)
	textBox (borderT
		lightBkg #t
	)
	textBoxF (borderT
		lightBkg #t
	)
	window (borderT
		lightBkg #t
	)
	sbTray (borderR
		lightBkg #t
	)
	sbNub (borderT
		lightBkg #t
	)
	tabA (borderT
		lightBkg #t
	)
	tabB (borderT
		lightBkg #t
	)
	tabSel (borderT
		lightBkg #t
	)
	i11 (borderT
		lightBkg #t
	)
	i12 (borderT
		lightBkg #t
	)
	r48Overlay (borderR
	)
)

t3 (theme
	btn (borderR
	)
	btnP (borderR
	)
	label (borderR
	)
	textBox (borderR
	)
	textBoxF (borderR
	)
	window (borderR
		clear #t
	)
	sbTray (borderT
	)
	sbNub (borderT
	)
	tabA (borderR
	)
	tabB (borderR
	)
	tabSel (borderR
	)
	i11 (borderR
	)
	i12 (borderR
	)
	r48Overlay (borderR
	)
)
