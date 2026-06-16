
function push_version_update_to_gitlab() {
    if [ -z "$1" ]; then
        echo "No commit message provided. Exiting"
        exit 1
    else
        echo "Will push changes with commit message: $1"
    fi
    if [ -z "$2" ]; then
        echo "No update file path provided. Exiting"
        exit 1
    else
        echo "Will commit changes inside file: $2"
    fi
    git config user.name "CI Pipeline"
    git config user.email "dev@sinch.com"
    git remote add gitlab_origin https://oauth2:$CI_GITLAB_TOKEN@gitlab.com/sinch/sinch-projects/voice/vvc-client-sdk/rtc-vvc-reference-applications.git
    git fetch gitlab_origin
    git checkout -t gitlab_origin/master
    git add "$2"
    git commit -m "$1"
    git push gitlab_origin master -o ci.skip
}