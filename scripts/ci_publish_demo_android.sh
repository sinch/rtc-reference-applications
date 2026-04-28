#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$SCRIPT_DIR/.."
SIGNED_APK="$REPO_ROOT/build/sinch-android-reference-app.apk"

echo "Uploading signed APK to s3://${CI_SALES_DEMO_UPLOAD_PATH}"
aws s3 cp "$SIGNED_APK" "s3://${CI_SALES_DEMO_UPLOAD_PATH}sinch-demo-reference-app.apk" --acl public-read
aws cloudfront create-invalidation --distribution-id "$CLOUDFRONT_DOWNLOAD_ID" --paths "$CI_SALES_DEMO_INVALIDATION_PATH"

echo "Signed APK published successfully"
