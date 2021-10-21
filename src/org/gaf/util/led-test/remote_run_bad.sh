#!/bin/sh

# Assign variables from command line arguments
jar_file=$1
scp_target=$2
target_dir=$3
source_file=target/$jar_file
#destination_file=$scp_target:$target_dir/.

echo "jar_file = $jar_file"
echo "scp_target = $scp_target"
echo "source_file = $source_file"
#echo "destination_file = $destination_file"

#exit $?
