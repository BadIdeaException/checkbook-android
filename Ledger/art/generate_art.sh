#!/bin/bash

# Takes all the .svg files in the folder and exports it to ../res/drawable-<size>.
# If an argument is passed, only the file(s) in the argument are exported.

BASE=24;
RES_DIR="../res";

# Exports all found svg images to the subfolder of /res specified in arg1. Size will be BASE * arg2
# arg1: Subfolder of resource folder to use
# arg2: Factor to apply to $BASE to calculate size
function exportToDir() {
	targetDir="$RES_DIR/$1";
	size=$(( $2 * $BASE ));
	echo "Exporting $f: Exported with $size to $targetDir";
	inkscape -f $f -w $size -h $size -e "$targetDir/`basename $f .svg`.png";

}

function exportToDirIfAvailable() {
	if [ -d $RES_DIR/$1 ]; then
		exportToDir $1 $2;
	else
		echo "Exporting $f: Skipped dir $1 (not available)";
	fi;
}

files="*.svg";
if [ -n $1 ]; then
	files=$1;
	echo $files;
fi;

for f in $files; do 
	exportToDirIfAvailable "drawable-mdpi" 2;
	exportToDirIfAvailable "drawable-hdpi" 3;
	exportToDirIfAvailable "drawable-xhdpi" 4;
	exportToDirIfAvailable "drawable-xxhdpi" 6;
	exportToDirIfAvailable "drawable-xxxhdpi" 8;
#	inkscape -f $f -w $((3*$BASE)) -h $((3*$BASE)) -e "$RES_DIR/drawable-hdpi/`basename $f .svg`.png";
#	inkscape -f $f -w $((4*$BASE)) -h $((4*$BASE)) -e "$RES_DIR/drawable-xhdpi/`basename $f .svg`.png";
#	inkscape -f $f -w $((6*$BASE)) -h $((6*$BASE)) -e "$RES_DIR/drawable-xxhdpi/`basename $f .svg`.png";
#	inkscape -f $f -w $((8*$BASE)) -h $((8*$BASE)) -e "$RES_DIR/drawable-xxxhdpi/`basename $f .svg`.png";
	
done;
