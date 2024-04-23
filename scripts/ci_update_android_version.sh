#!/bin/bash

set -euo pipefail
if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 <SDK_VERSION> <SDK_REVISION>"
  exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source "${SCRIPT_DIR}/ci_update_in_pipeline.include.sh"

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

push_version_update_to_gitlab "Auto update Android SDK to $NEW_VERSION+$NEW_REVISION" "$MAKEFILE"

echo "DONE"
