#!/bin/bash

updateAliasName=$1
description=$2
functionName=logs-export

variables="{KeyName1=PROD-1a,KeyName2=PROD-2b}"

if [ "$updateAliasName" = "DEV" ]
then
  variables="{KeyName1=DEV-1,KeyName2=DEV-2}"
fi

revisionId=$(aws lambda update-function-configuration --function-name $functionName --environment Variables=$variables --description "$description" --query RevisionId --output text)
echo "revisionId="$revisionId
version=$(aws lambda update-function-code --function-name $functionName --zip-file fileb://target/logs-export.jar --publish --output text --revision-id $revisionId --query Version)
echo "version="$version
aws lambda update-alias --name $updateAliasName --function-name $functionName --function-version $version --revision-id $revisionI --description "$description"