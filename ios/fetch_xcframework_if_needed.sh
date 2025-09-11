#!/bin/bash

set -euo pipefail

readonly SDK_VERSION="5.34.4%2b6e3d77c3"

readonly SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
readonly SDK_SHORT_VERSION=$(echo $SDK_VERSION | cut -d% -f1)
readonly SDK_URL="https://download.sinch.com/ios/$SDK_SHORT_VERSION/SinchRTC-iOS-$SDK_VERSION.tar.bz2"
readonly XCFRAMEWORK_PATH="$SCRIPT_DIR/SinchRTC.xcframework"
readonly VERSION_FILE_PATH="$SCRIPT_DIR/version.txt"

log() {
  # Redirect to stderr to get the output in XCode build log.
  >&2 echo "$@";
}

extract_xcframework() {
  log "Extracting XCFramework from distribution archive..."
  tar -xvf *.tar.bz2 &> /dev/null
  # Extracting the dynamic lib version of the SDK.
  tar -xvf ./dynamic/*.tar.bz2 &> /dev/null
  mv ./SinchRTC/SinchRTC.xcframework $XCFRAMEWORK_PATH
  log "Archive extracted."
}

download_dist_archive() {
  log "Downloading SinchRTC.xcframework..."
  curl -O $SDK_URL --fail
  log "Download completed."
}

cleanup() {
  rm -rf "$TMP_DIR"
}

# Re-download the Sinch SDK if it's not there, or if the target version has changed.
if [ ! -d $XCFRAMEWORK_PATH ] || [ ! -f $VERSION_FILE_PATH ] || [ "$(cat $VERSION_FILE_PATH)" != $SDK_VERSION ]; then
  rm -rf $XCFRAMEWORK_PATH
  rm -rf $VERSION_FILE_PATH
  TMP_DIR=$(mktemp -d)
  trap cleanup EXIT
  pushd $TMP_DIR > /dev/null
  download_dist_archive
  extract_xcframework
  popd > /dev/null
  echo $SDK_VERSION > $VERSION_FILE_PATH
else
  log "SinchRTC.xcframework already exists, version $SDK_VERSION; skip downloading."
fi
