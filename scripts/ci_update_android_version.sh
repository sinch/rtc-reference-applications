#!/bin/bash

set -euo pipefail
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 <SDK_VERSION> <SDK_REVISION>"
  exit 1
fi

NEW_VERSION="$1"
NEW_REVISION="$2"
MAKEFILE="$SCRIPT_DIR/../android/Makefile"

sed -i '' "/^SDK_VERSION/c\\
SDK_VERSION=$NEW_VERSION\\
" $MAKEFILE

sed -i '' "/^SDK_REVISION/c\\
SDK_REVISION=$NEW_REVISION\\
" $MAKEFILE

echo "Makefile version updated new version: $NEW_VERSION revision: $NEW_REVISION"
echo "Pushing updated Makefile to master..."

git config user.name "CI Pipeline"
git config user.email "dev@sinch.com"
git remote add gitlab_origin https://oauth2:$CI_GITLAB_TOKEN@gitlab.com/sinch/sinch-projects/voice/vvc-client-sdk/rtc-vvc-reference-applications.git
git fetch gitlab_origin
git checkout -t gitlab_origin/master
git add ./android/Makefile
git commit -m "Auto update Android SDK to:$NEW_VERSION+$NEW_REVISION"
git push gitlab_origin master -o ci.skip

echo "DONE"
