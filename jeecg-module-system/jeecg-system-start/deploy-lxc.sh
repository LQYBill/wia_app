#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage:
  bash deploy-lxc.sh --service UNIT --install-jar PATH --health-url URL \
      --repo CHECKOUT --branch BRANCH [--profile prod] [--health-timeout 120]
  bash deploy-lxc.sh --service UNIT --install-jar PATH --health-url URL \
      --jar FILE [--health-timeout 120]

Run inside the LXC with permission to replace the current JAR and restart UNIT.
Use the JAR path from the service's ExecStart and a local URL that returns HTTP
2xx when the application is ready. A symlink to the current JAR is supported.

Repository mode builds the selected branch from the checkout's origin remote.
Run the same command from your release pipeline or cron to pick up new commits.
JAR mode installs an already packaged jeecg-system-start JAR. Publish it to
the input path after the upload is complete.

The previous JAR is saved beside the installed JAR as <jar>.previous.
A failed upgrade restores it and exits with an error.
EOF
}

log() { printf '[%s] %s\n' "$(date '+%F %T')" "$*" >&2; }
die() { log "$*"; exit 1; }

service=
install_jar=
health_url=
repo=
branch=
artifact=
profile=prod
health_timeout=120

while (($#)); do
    case "$1" in
        -h|--help) usage; exit 0 ;;
        --service|--install-jar|--health-url|--repo|--branch|--jar|--profile|--health-timeout)
            (($# >= 2)) && [[ -n $2 && $2 != --* ]] || die "Missing value for $1"
            case "$1" in
                --service) service=$2 ;;
                --install-jar) install_jar=$2 ;;
                --health-url) health_url=$2 ;;
                --repo) repo=$2 ;;
                --branch) branch=$2 ;;
                --jar) artifact=$2 ;;
                --profile) profile=$2 ;;
                --health-timeout) health_timeout=$2 ;;
            esac
            shift 2
            ;;
        *) die "Unknown argument $1 (see --help)" ;;
    esac
done

[[ $service == *.service && -n $install_jar && $health_url =~ ^https?:// ]] ||
    die "--service, --install-jar and --health-url are required (see --help)"
[[ $health_timeout =~ ^[1-9][0-9]{0,4}$ ]] || die "Invalid health check timeout"
if [[ -n $repo ]]; then
    [[ -n $branch && -z $artifact ]] || die "Use --repo with --branch, or use --jar"
    git check-ref-format "refs/heads/$branch" || die "Invalid branch name"
    command -v mvn >/dev/null || die "Maven is required in repository mode"
else
    [[ -n $artifact && -z $branch ]] || die "Use --jar, or use --repo with --branch"
fi

for command in systemctl curl flock unzip; do
    command -v "$command" >/dev/null || die "Missing required command: $command"
done
systemctl cat -- "$service" >/dev/null || die "Cannot find systemd unit $service"
install_jar=$(realpath -e -- "$install_jar") || die "Current JAR does not exist"
[[ -f $install_jar && -s $install_jar ]] || die "Invalid current JAR: $install_jar"

backup="$install_jar.previous"
deployed="$install_jar.deploy-version"
pending="$install_jar.deploy-pending"
exec {lock_fd}>"$install_jar.deploy-lock"
if ! flock -n "$lock_fd"; then
    log "Another deployment is already running."
    exit 0
fi
stage=$(mktemp -d -- "$(dirname -- "$install_jar")/.wia-deploy.XXXXXXXX")

healthy() {
    local status
    systemctl is-active --quiet -- "$service" &&
        status=$(curl -q --silent --max-time 5 --output /dev/null --write-out '%{http_code}' -- "$health_url") &&
        [[ $status == 2[0-9][0-9] ]]
}

wait_until_healthy() {
    local deadline=$((SECONDS + health_timeout))
    while ((SECONDS < deadline)); do
        if healthy; then
            return 0
        fi
        sleep 2
    done
    return 1
}

restore_previous() {
    log "Restoring the previous JAR for $service"
    [[ -s $backup ]] &&
        cp -p -- "$backup" "$stage/restore.jar" &&
        mv -fT -- "$stage/restore.jar" "$install_jar" &&
        systemctl restart -- "$service" &&
        wait_until_healthy &&
        rm -f -- "$pending"
}

cleanup() {
    local rc=$?
    trap - EXIT
    trap '' INT TERM
    if [[ -e $pending ]]; then
        if restore_previous; then
            log "Rollback complete. The previous version is healthy."
        else
            log "Rollback could not be verified. Previous JAR: $backup"
        fi
        rc=1
    fi
    rm -rf -- "$stage"
    exit "$rc"
}

# Recover an interrupted cutover before checking for new releases.
if [[ -e $pending ]] && ! restore_previous; then
    rm -rf -- "$stage"
    die "Failed to recover interrupted deployment. Previous JAR: $backup"
fi
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

fingerprint() {
    local digest
    digest=$(sha256sum < "$1") || return 1
    printf '%s\n' "${digest%% *}"
}

release_id=jar
if [[ -n $repo ]]; then
    remote_ref="refs/remotes/origin/$branch"
    GIT_TERMINAL_PROMPT=0 git -C "$repo" fetch --no-tags origin "+refs/heads/$branch:$remote_ref"
    revision=$(git -C "$repo" rev-parse --verify "$remote_ref^{commit}")
    release_id="$revision/$profile"
    if [[ -f $deployed ]] && read -r previous_release previous_digest < "$deployed" &&
        [[ $previous_release == "$release_id" && $previous_digest == "$(fingerprint "$install_jar")" ]]; then
        healthy || die "This commit is already deployed, but $service failed its health check"
        log "Commit $revision is already deployed."
        exit 0
    fi

    log "Building $branch at $revision with Maven profile $profile"
    mkdir -- "$stage/source"
    git -C "$repo" archive "$revision" | tar -xf - -C "$stage/source"
    (
        cd -- "$stage/source"
        mvn -B -ntp "-P$profile" -pl jeecg-module-system/jeecg-system-start -am -Dmaven.test.skip=true package
    )
    shopt -s nullglob
    jars=("$stage"/source/jeecg-module-system/jeecg-system-start/target/jeecg-system-start-*.jar)
    shopt -u nullglob
    ((${#jars[@]} == 1)) || die "Expected one jeecg-system-start release JAR"
    artifact=${jars[0]}
fi

[[ -f $artifact && -s $artifact ]] || die "Release JAR is missing or empty: $artifact"
next_jar="$stage/next.jar"
cp -- "$artifact" "$next_jar"
manifest=$(unzip -p "$next_jar" META-INF/MANIFEST.MF) || die "Could not read JAR manifest"
grep -Fxq 'Start-Class: org.jeecg.JeecgSystemApplication' <<< "${manifest//$'\r'/}" ||
    die "The release is not a packaged WIA App JAR"
next_digest=$(fingerprint "$next_jar")

if cmp -s -- "$next_jar" "$install_jar"; then
    healthy || die "The JAR is already installed, but $service failed its health check"
    log "This JAR is already installed."
else
    unzip -tq "$next_jar" >/dev/null || die "Release JAR failed its archive integrity check"
    cp --attributes-only --preserve=mode,ownership -- "$install_jar" "$next_jar"
    cp -p -- "$install_jar" "$stage/previous.jar"
    mv -fT -- "$stage/previous.jar" "$backup"
    : > "$pending"
    mv -fT -- "$next_jar" "$install_jar"

    log "Restarting $service"
    systemctl restart -- "$service" || die "Systemd could not restart $service"
    wait_until_healthy || die "The new version did not pass its health check"
fi

printf '%s %s\n' "$release_id" "$next_digest" > "$stage/version"
mv -fT -- "$stage/version" "$deployed"
rm -f -- "$pending"
log "Deployment complete for $service ($release_id)."
