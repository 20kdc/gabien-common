; gabien-common - Cross-platform game and UI framework
; Written starting in 2016 by contributors (see CREDITS.txt)
; To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
; A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
; 
; This file is edited using gabien-tools.
patch (name "digital harp"
	sustainEnabled #t noiseEnabled #f strikeMs 100 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.25 0.0 0.16770833730697632 0.5149999856948853 0.25090035796165466 0.7450000047683716 0.3121248483657837 0.12999999523162842 0.4357742965221405 0.09000000357627869 0.5678271055221558 0.07000000029802322 0.75 0.0 1.0 0.0)
	wave (0.14165666699409485 0.019999999552965164 0.4000000059604645 0.0 0.45618247985839844 0.30000001192092896 0.46041667461395264 0.3700000047683716 0.8447916507720947 0.3050000071525574)
	pitchEnv (0.0 0.5 0.15625 0.36000001430511475 0.23333333432674408 0.5450000166893005 0.2857142984867096 0.6499999761581421 0.2809123694896698 0.4350000023841858 0.3097238838672638 0.6000000238418579 0.3925570249557495 0.49000000953674316 0.5006002187728882 0.5)
)
assign 1
assign 3
assign 5
assign 7
assign 9
assign 11
assign 13
assign 15
assign 17
assign 19
assign 21
assign 23
assign 25
assign 27
assign 29
assign 31
assign 33
assign 35
assign 37
assign 39
assign 43
assign 45
assign 47
assign 49
assign 51
assign 53
assign 55
assign 57
assign 59
assign 61
assign 63
assign 65
assign 67
assign 69
assign 71
assign 73
assign 75
assign 77
assign 79
assign 81
assign 83
assign 85
assign 87
assign 89
assign 91
assign 93
assign 95
assign 97
assign 100
assign 102
assign 104
assign 106
assign 108
assign 110
assign 112
assign 114
assign 116
assign 118
assign 120
assign 122
assign 124
assign 126
patch (name "rounded square"
	sustainEnabled #t noiseEnabled #f strikeMs 200 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.25 0.0 0.27010804414749146 0.41499999165534973 0.25090035796165466 0.35499998927116394 0.2785114049911499 0.2199999988079071 0.3853541314601898 0.08500000089406967 0.49940264225006104 0.09000000357627869 0.75 0.0 1.0 0.0)
	wave (0.0 0.0 0.12544803321361542 0.0 0.1636798083782196 0.0 0.26403823494911194 0.0 0.2664277255535126 1.0 0.38829150795936584 1.0 0.5531660914421082 1.0 0.5913978219032288 1.0 0.7359617948532104 0.0 0.9091995358467102 0.0)
	pitchEnv (0.0 0.5)
)
assign 2
assign 6
assign 10
assign 14
assign 18
assign 22
assign 26
assign 32
assign 36
assign 40
assign 41
assign 42
assign 46
assign 50
assign 54
assign 58
assign 62
assign 66
assign 70
assign 74
assign 78
assign 82
assign 86
assign 90
assign 94
assign 98
assign 101
assign 105
assign 109
assign 113
assign 117
assign 121
assign 125
patch (name "saw"
	sustainEnabled #t noiseEnabled #f strikeMs 200 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.25 0.0 0.16067415475845337 0.5249999761581421 0.25090035796165466 0.44999998807907104 0.30372148752212524 0.3400000035762787 0.36974790692329407 0.08500000089406967 0.5030012130737305 0.09000000357627869 0.75 0.0 1.0 0.0)
	wave (0.0 0.0 0.0 0.0 0.01560624223202467 0.0 0.7515006065368652 0.800000011920929 1.0 1.0 0.9663865566253662 0.0 0.9795918464660645 0.0 1.0 0.0)
	pitchEnv (0.0 0.5 0.18426966667175293 0.35499998927116394 0.2545017898082733 0.5149999856948853 0.27250900864601135 0.4449999928474426 0.31812724471092224 0.5049999952316284 0.37214887142181396 0.5)
)
assign 0
assign 4
assign 8
assign 12
assign 16
assign 20
assign 24
assign 28
assign 34
assign 38
assign 44
assign 48
assign 52
assign 56
assign 60
assign 64
assign 68
assign 72
assign 76
assign 80
assign 84
assign 88
assign 92
assign 96
assign 99
assign 103
assign 107
assign 111
assign 115
assign 119
assign 127
patch (name "saw/GFZ2 lead"
	sustainEnabled #t noiseEnabled #f strikeMs 200 releaseMs 100 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.21608643233776093 0.0 0.29411765933036804 0.0 0.27250900864601135 0.38499999046325684 0.3217287063598633 0.07999999821186066 0.5042017102241516 0.11500000208616257 0.5306122303009033 0.10000000149011612 0.6482592821121216 0.0 0.8559423685073853 0.0)
	wave (0.0 0.029999999329447746 0.291011244058609 0.0 0.949438214302063 1.0 1.0 1.0)
	pitchEnv (0.0 0.5 0.18426966667175293 0.35499998927116394 0.26516854763031006 0.4950000047683716 0.2775280773639679 0.375 0.324719101190567 0.5049999952316284 0.3709483742713928 0.5)
)
assign 30
patch (name "y d_bunny 7B"
	sustainEnabled #f noiseEnabled #f strikeMs 100 releaseMs 1 fixedFrequency 0 octaveShift 4 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.25 0.0 0.25999999046325684 0.5 0.25732484459877014 0.8849999904632568 0.2800000011920929 0.5 0.4993630647659302 0.0 0.6407643556594849 0.0)
	wave (0.0 0.5 0.5 0.25)
	pitchEnv (0.2471337616443634 0.0 0.27006369829177856 0.9950000047683716 0.30573248863220215 0.7250000238418579 0.37452229857444763 0.17000000178813934 0.45350319147109985 0.0 0.5031847357749939 0.0)
)
assign 123
patch (name "z perc_bonk_a"
	sustainEnabled #t noiseEnabled #t strikeMs 250 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 64
	env (0.0 0.0 0.125 0.0 0.25223612785339355 0.20000000298023224 0.25939178466796875 0.32499998807907104 0.3506261110305786 0.0 0.42497000098228455 0.0 0.4981992840766907 0.0 0.5798319578170776 0.0 1.0 0.0)
	wave (0.0 0.5)
	pitchEnv (0.09302325546741486 0.6000000238418579 0.41144901514053345 0.7400000095367432 0.375670850276947 0.11500000208616257 0.6314848065376282 0.054999999701976776 0.6618962287902832 0.5049999952316284)
)
assign 129
assign 131
assign 133
assign 135
assign 137
assign 139
assign 141
assign 143
assign 145
assign 147
assign 149
assign 151
assign 153
assign 155
assign 157
assign 161
assign 163
assign 165
assign 169
assign 171
assign 173
assign 175
assign 181
assign 186
assign 195
assign 199
assign 201
assign 203
assign 205
assign 207
assign 209
assign 211
assign 213
assign 215
assign 217
assign 219
assign 221
assign 223
assign 225
assign 227
assign 229
assign 231
assign 233
assign 235
assign 237
assign 239
assign 241
assign 243
assign 245
assign 247
assign 249
assign 251
assign 253
assign 255
patch (name "z perc_click"
	sustainEnabled #t noiseEnabled #t strikeMs 100 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.24508050084114075 0.5 0.25581395626068115 0.7300000190734863 0.3738819360733032 0.0 0.5008944272994995 0.0 1.0 0.0)
	wave (0.0 0.5 0.4988763928413391 0.5049999952316284)
	pitchEnv (0.0 0.5)
)
assign 128
assign 130
assign 132
assign 134
assign 136
assign 138
assign 140
assign 142
assign 144
assign 146
assign 148
assign 150
assign 152
assign 154
assign 156
assign 158
assign 159
assign 160
assign 162
assign 164
assign 166
assign 167
assign 168
assign 176
assign 178
assign 182
assign 184
assign 188
assign 189
assign 190
assign 191
assign 192
assign 193
assign 194
assign 196
assign 198
assign 200
assign 202
assign 204
assign 206
assign 208
assign 210
assign 212
assign 214
assign 216
assign 218
assign 220
assign 222
assign 224
assign 226
assign 228
assign 230
assign 232
assign 234
assign 236
assign 238
assign 240
assign 242
assign 244
assign 246
assign 248
assign 250
assign 252
assign 254
patch (name "z perc_crash"
	sustainEnabled #f noiseEnabled #t strikeMs 1500 releaseMs 500 fixedFrequency 400000 octaveShift 0 mainWaveformSamples 8192
	env (0.24865831434726715 0.12999999523162842 0.2987477779388428 0.0 0.6279069781303406 0.0 0.8711985945701599 0.0)
	wave (0.0 0.7450000047683716 0.3333333432674408 0.7400000095367432)
	pitchEnv (0.24865831434726715 0.5299999713897705 0.7370303869247437 0.44999998807907104)
)
assign 177
assign 183
assign 185
patch (name "z perc_shake"
	sustainEnabled #f noiseEnabled #t strikeMs 100 releaseMs 500 fixedFrequency 0 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.125 0.0 0.25 0.0 0.3577817678451538 0.33500000834465027 0.4812164604663849 0.0 0.5688729882240295 0.0 0.7531306147575378 0.0 1.0 0.0)
	wave (0.0 0.5 0.4973166286945343 0.48500001430511475)
	pitchEnv (0.0 0.5)
)
assign 170
assign 172
assign 174
assign 179
assign 180
assign 187
assign 197
patch (name "zzz null"
	sustainEnabled #f noiseEnabled #t strikeMs 50 releaseMs 50 fixedFrequency 440 octaveShift 0 mainWaveformSamples 8192
	env (0.0 0.0 0.4977477490901947 0.0)
	wave (0.4859813153743744 0.5049999952316284)
	pitchEnv (0.4953271150588989 0.5049999952316284)
)
