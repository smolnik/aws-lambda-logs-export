#!/bin/bash

updateAliasName=$1
description=$2
functionName=logs-export
version=$(aws lambda update-function-code --function-name $functionName --zip-file fileb://logs-export.jar --publish --query Version --output text)
echo "version="$version
aws lambda update-alias --name $updateAliasName --function-name $functionName --function-version $version --description "$description"

