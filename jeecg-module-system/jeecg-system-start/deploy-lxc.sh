#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage:
  bash deploy-lxc.sh --service UNIT --install-jar PATH --health-url URL \
      --repo CHECKOUT --branch BRANCH [--profile prod] [--preserve-config] [--health-timeout 120]
  bash deploy-lxc.sh --service UNIT --install-jar PATH --health-url URL \
      --jar FILE [--preserve-config] [--health-timeout 120]

Run inside the LXC with permission to replace the current JAR and restart UNIT.
Use the JAR path from the service's ExecStart and a local URL that returns HTTP
2xx when the application is ready. A symlink to the current JAR is supported.

Repository mode builds the selected branch from the checkout's origin remote.
Run the same command from your release pipeline or cron to pick up new commits.
JAR mode installs an already packaged jeecg-system-start JAR. Publish it to
the input path after the upload is complete. To record the commit in this
mode, package from a clean checkout with -Dwia-git-commit="$(git rev-parse HEAD)".

Use --preserve-config to carry the installed JAR's Spring application and
bootstrap configuration into the new JAR before switching versions.
This option requires Python 3.

The previous JAR is saved beside the installed JAR as <jar>.previous.
A failed upgrade restores it and exits with an error.
The <jar>.deploy-version file records the deployed app version and Git commit.
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
preserve_config=0

while (($#)); do
    case "$1" in
        -h|--help) usage; exit 0 ;;
        --preserve-config) preserve_config=1; shift ;;
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
if ((preserve_config)); then
    command -v python3 >/dev/null || die "Python 3 is required with --preserve-config"
fi
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
        restore_deployment_record &&
        rm -f -- "$pending"
}

restore_deployment_record() {
    # Older versions of this script created an empty pending file.
    if [[ -s $pending ]]; then
        tail -n +2 "$pending" > "$stage/restore-version" || return 1
        if [[ -s $stage/restore-version ]]; then
            mv -fT -- "$stage/restore-version" "$deployed"
        else
            rm -f -- "$deployed"
        fi
    fi
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

read_release_metadata() {
    local metadata banner
    metadata=$(unzip -p "$1" BOOT-INF/classes/wia-release.properties 2>/dev/null) || metadata=
    metadata=${metadata//$'\r'/}
    app_version=$(sed -n 's/^app_version=//p' <<< "$metadata")
    git_commit=$(sed -n 's/^git_commit=//p' <<< "$metadata")
    if [[ -z $app_version ]]; then
        # Older releases have the app version only in the banner.
        banner=$(unzip -p "$1" BOOT-INF/classes/banner.txt 2>/dev/null) || banner=
        app_version=$(sed -n 's/^WIA APP Version: //p' <<< "${banner//$'\r'/}")
    fi
    app_version=${app_version:-unknown}
    git_commit=${git_commit:-unknown}
    [[ $app_version =~ ^[[:alnum:]][[:alnum:].+_-]*$ ]] || die "Invalid app version in JAR metadata"
    [[ $git_commit == unknown || $git_commit =~ ^([a-f0-9]{40}|[a-f0-9]{64})$ ]] ||
        die "Invalid Git commit in JAR metadata"
    if [[ -n $revision ]]; then
        [[ $git_commit == unknown || $git_commit == "$revision" ]] ||
            die "JAR Git commit does not match the fetched revision"
        git_commit=$revision
    fi
}

write_deployment_record() {
    printf '%s %s\napp_version=%s\ngit_commit=%s\n' \
        "$release_id" "$1" "$app_version" "$git_commit" > "$stage/version"
    if ! cmp -s -- "$stage/version" "$deployed"; then
        mv -fT -- "$stage/version" "$deployed"
    fi
}

copy_installed_config() {
    python3 - "$1" "$2" "$3" <<'PY'
import copy
import re
import shutil
import sys
from zipfile import ZipFile

installed, release, staged = sys.argv[1:]
config_path = re.compile(
    r"BOOT-INF/classes/(?:config/)?(?:application|bootstrap)"
    r"(?:-[^/]+)?\.(?:ya?ml|properties)"
)

with ZipFile(installed) as old, ZipFile(release) as new:
    configs = {entry.filename: entry for entry in old.infolist() if config_path.fullmatch(entry.filename)}
    if not configs:
        raise SystemExit("No embedded Spring config found in the installed JAR")
    new_names = set(new.namelist())
    if all(name in new_names and old.read(entry) == new.read(name) for name, entry in configs.items()):
        shutil.copyfile(release, staged)
    else:
        with ZipFile(staged, "w") as result:
            result.comment = new.comment
            for entry in new.infolist():
                config = configs.get(entry.filename)
                archive = old if config is not None else new
                source = config if config is not None else entry
                with archive.open(source) as src, result.open(copy.copy(entry), "w") as dst:
                    shutil.copyfileobj(src, dst)
            for name in sorted(configs.keys() - new_names):
                entry = configs[name]
                result.writestr(copy.copy(entry), old.read(entry))

    with ZipFile(staged) as result:
        if any(result.read(name) != old.read(entry) for name, entry in configs.items()):
            raise SystemExit("Staged JAR failed embedded config verification")
    print(f"Verified {len(configs)} preserved Spring config files")
PY
}

release_id=jar
revision=
if [[ -n $repo ]]; then
    remote_ref="refs/remotes/origin/$branch"
    GIT_TERMINAL_PROMPT=0 git -C "$repo" fetch --no-tags origin "+refs/heads/$branch:$remote_ref"
    revision=$(git -C "$repo" rev-parse --verify "$remote_ref^{commit}")
    release_id="$revision/$profile"
    if ((preserve_config)); then
        release_id="$release_id/preserve-config"
    fi
    if [[ -f $deployed ]] && read -r previous_release previous_digest < "$deployed" &&
        [[ $previous_release == "$release_id" && $previous_digest == "$(fingerprint "$install_jar")" ]]; then
        healthy || die "This commit is already deployed, but $service failed its health check"
        read_release_metadata "$install_jar"
        write_deployment_record "$previous_digest"
        log "WIA App $app_version (commit $git_commit) is already deployed."
        exit 0
    fi

    log "Building $branch at $revision with Maven profile $profile"
    mkdir -- "$stage/source"
    git -C "$repo" archive "$revision" | tar -xf - -C "$stage/source"
    (
        cd -- "$stage/source"
        mvn -B -ntp "-P$profile" "-Dwia-git-commit=$revision" \
            -pl jeecg-module-system/jeecg-system-start -am -Dmaven.test.skip=true package
    )
    shopt -s nullglob
    jars=("$stage"/source/jeecg-module-system/jeecg-system-start/target/jeecg-system-start-*.jar)
    shopt -u nullglob
    ((${#jars[@]} == 1)) || die "Expected one jeecg-system-start release JAR"
    artifact=${jars[0]}
fi

[[ -f $artifact && -s $artifact ]] || die "Release JAR is missing or empty: $artifact"
next_jar="$stage/next.jar"
if ((preserve_config)); then
    copy_installed_config "$install_jar" "$artifact" "$next_jar" || die "Could not preserve the installed JAR configuration"
else
    cp -- "$artifact" "$next_jar"
fi
manifest=$(unzip -p "$next_jar" META-INF/MANIFEST.MF) || die "Could not read JAR manifest"
grep -Fxq 'Start-Class: org.jeecg.JeecgSystemApplication' <<< "${manifest//$'\r'/}" ||
    die "The release is not a packaged WIA App JAR"
read_release_metadata "$next_jar"
next_digest=$(fingerprint "$next_jar")

if cmp -s -- "$next_jar" "$install_jar"; then
    healthy || die "The JAR is already installed, but $service failed its health check"
    log "This JAR is already installed."
else
    unzip -tq "$next_jar" >/dev/null || die "Release JAR failed its archive integrity check"
    cp --attributes-only --preserve=mode,ownership -- "$install_jar" "$next_jar"
    cp -p -- "$install_jar" "$stage/previous.jar"
    {
        printf '%s\n' 'Previous deployment record'
        if [[ -f $deployed ]]; then
            cat -- "$deployed"
        fi
    } > "$stage/pending"
    mv -fT -- "$stage/previous.jar" "$backup"
    mv -fT -- "$stage/pending" "$pending"
    mv -fT -- "$next_jar" "$install_jar"

    log "Restarting $service"
    systemctl restart -- "$service" || die "Systemd could not restart $service"
    wait_until_healthy || die "The new version did not pass its health check"
fi

write_deployment_record "$next_digest"
rm -f -- "$pending"
log "Deployment complete for $service (WIA App $app_version, commit $git_commit)."
