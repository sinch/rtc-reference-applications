#/usr/bin/env bash

set -euo pipefail

export LC_ALL="en_US.UTF-8"

if [[ -z $CI_APPLEDEV_KEYCHAIN_PASS ]]; then
  echo "appledev password must be securely provided by CI using env vars"
  exit 1
fi

KEYCHAIN_PATH=$(security list-keychains | grep 'appledev.keychain' | tr -d ' ' | tr -d '"')

if [[ -z $KEYCHAIN_PATH ]]; then
  echo "Unable to find appledev keychain path"
  exit 2
fi

echo "Using existing keychain: ${KEYCHAIN_PATH}"

security unlock-keychain -p ${CI_APPLEDEV_KEYCHAIN_PASS} ${KEYCHAIN_PATH}
# lock after timeout and when sleep
security set-keychain-settings -t 1800 -l ${KEYCHAIN_PATH}
