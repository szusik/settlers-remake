#!/bin/bash

for x in CROP          IRONORE          SPEAR          AXE          SCYTHE          MEAT          PLANK          WATER          SAW          NO_MATERIAL          FISHINGROD          BREAD          FISH          GOLDORE          HAMMER          GOLD          COAL          WINE          FLOUR          IRON          PIG          PICK          BOW          TRUNK          STONE          BLADE          ; do
gnuplot -c script.plot $x $1 $2
done
