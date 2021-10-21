#!/bin/sh

# Assign variables from command line arguments
jar_file=$1
scp_target=$2
target_dir=$3
source_file=target/$jar_file
destination_file=$scp_target:$target_dir/.

#echo "jar_file = $jar_file"
#echo "scp_target = $scp_target"
#echo "source_file = $source_file"
#echo "destination_file = $destination_file"

# Copy the JAR file to the remote server
echo "Copying $source_file to $destination_file ..."
scp $source_file $destination_file
if [ $? -ne 0 ]; then
	echo "Error copying $source_file to $destination_file"
	exit -1
fi

# SSH to the remote server, change directory into the target directory and run the Java application
echo "Running the application on the remote server $scp_target..."
ssh $scp_target "cd $target_dir && java -jar $jar_file"

exit $?