#!/bin/bash

set -euo pipefail
if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 <SDK_VERSION_FULL>"
  exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/ci_update_in_pipeline.include.sh"

NEW_VERSION="$1"
UPDATE_SCRIPT="$SCRIPT_DIR/../ios/fetch_xcframework_if_needed.sh"

sed -i '' "/^readonly SDK_VERSION/c\\
readonly SDK_VERSION=\"$NEW_VERSION\"\\
" $UPDATE_SCRIPT

echo "Changed SDK version inside iOS SDK download script to new version: $NEW_VERSION"

push_version_update_to_gitlab "Auto update iOS SDK to $NEW_VERSION" "$UPDATE_SCRIPT"

echo "DONE"
