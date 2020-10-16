#!/bin/bash
set -euf -o pipefail

#### Constants
readonly GIT_BRANCH_PRODUCTION="master"
readonly GIT_BRANCH_DEVELOPMENT="development"
readonly GIT_BRANCH_RELEASE="release/"
readonly GIT_BRANCH_FEATURE="feature/"
readonly GIT_BRANCH_BUGFIX="bugfix/"
readonly GIT_BRANCH_HOTFIX="hotfix/"

readonly VERSION_TYPE_RELEASE="release"
readonly VERSION_TYPE_PRERELEASE="pre-release"
readonly VERSION_TYPE_SNAPSHOT="snapshot"

#### Functions
function log_info() {
	echo "[INFO] ${*}"
}

function log_error() {
	local -r exitCode="${1}"
	shift

	echo "[ERROR] ${*}" 1>&2

	exit "${exitCode}"
}

function return_string() {
	printf "%s" "${*}"
}

function check_semver() {
	local -r mavenVersion="${1}"

	if [[ "${mavenVersion}" =~ ^([0-9]+)"."([0-9]+)"."([0-9]+)$ ]]; then
		local -r versionType="${VERSION_TYPE_RELEASE}"
	elif [[ "${mavenVersion}" =~ ^([0-9]+)"."([0-9]+)"."([0-9]+)"-SNAPSHOT"$ ]]; then
		local -r versionType="${VERSION_TYPE_SNAPSHOT}"
	elif [[ "${mavenVersion}" =~ ^([0-9]+)"."([0-9]+)"."([0-9]+)"-"("alpha"|"beta"|"rc")"."([0-9]+)$ ]]; then
		local -r versionType="${VERSION_TYPE_PRERELEASE}"
	else
		log_error 5 "Maven version must be compliant with Semantic Versioning | Maven version: ${mavenVersion}"
	fi

	return_string "${versionType}"
}

function check_tag() {
	local -r gitTag="${1}"
	local -r mavenVersion="${2}"
	local -r versionType="${3}"

	# Check for release version on Git tags
	if ! [ "${versionType}" = "${VERSION_TYPE_RELEASE}" ] && ! [ "${versionType}" = "${VERSION_TYPE_PRERELEASE}" ]; then
		log_error 6 "Maven version on a Git tag must be a SemVer release or pre-release version | Maven version: ${mavenVersion}"
	fi

	# Check that Maven version and Git tag name match
	if [ "${gitTag}" != "$mavenVersion" ]; then
		log_error 4 "Maven version and Git tag name must match | Maven version: ${mavenVersion} | Git tag: ${gitTag}"
	fi
}

function check_branch() {
	local -r gitBranch="${1}"
	local -r mavenVersion="${2}"
	local -r versionType="${3}"

	if [[ "${gitBranch}" == "${GIT_BRANCH_DEVELOPMENT}" ]] || [[ "${gitBranch}" =~ ^"${GIT_BRANCH_FEATURE}"(.+)$ ]]; then
		# Check for snapshot version on the Git development and feature branches
		[ "${versionType}" = "${VERSION_TYPE_SNAPSHOT}" ] ||
			log_error 7 "Maven version on the Git development or a feature branch must be a SemVer snapshot version | Maven version: ${mavenVersion}"
	elif [[ "${gitBranch}" == "${GIT_BRANCH_PRODUCTION}" ]] || [[ "${gitBranch}" =~ ^"${GIT_BRANCH_HOTFIX}"(.+)$ ]]; then
		# Check for release version on the Git production and hotfix branches
		[ "${versionType}" = "${VERSION_TYPE_RELEASE}" ] ||
			log_error 8 "Maven version on the Git production or a hotfix branch must be a SemVer release version | Maven version: ${mavenVersion}"
	elif [[ "${gitBranch}" =~ ^"${GIT_BRANCH_RELEASE}"([0-9]+"."[0-9]+".0")$ ]] || [[ "${gitBranch}" =~ ^"${GIT_BRANCH_BUGFIX}"(.+)$ ]]; then
		# Check for either release or pre-release version on Git release and bugfix branches
		[ "${versionType}" = "${VERSION_TYPE_RELEASE}" ] || [ "${versionType}" = "${VERSION_TYPE_PRERELEASE}" ] ||
			log_error 9 "Maven version on a Git release or bugfix branch must be a SemVer (pre-)release version | Maven version: ${mavenVersion}"
	else
		log_error 10 "Git branch not permitted | Git branch: ${gitBranch}"
	fi
}

function check_git_reference() {
	local -r gitRef=${1}
	local -r mavenVersion=${2}
	local -r versionType=${3}
	local -r gitTargetRef=${4}

	if [[ "${gitRef}" =~ ^"refs/tags/"(.*)$ ]]; then
		# Check Maven version against Git tag
		local -r gitTag="${BASH_REMATCH[1]}"
		check_tag "${gitTag}" "${mavenVersion}" "${versionType}"
	elif [[ "${gitRef}" =~ ^"refs/heads/"(.*)$ ]]; then
		# Check Maven version against Git branch
		local -r gitBranch="${BASH_REMATCH[1]}"
		check_branch "${gitBranch}" "${mavenVersion}" "${versionType}"
	elif [[ "${gitRef}" =~ ^"refs/pull/" ]]; then
		# Check Maven version against Git target branch in Pull Request
		[ -n "${gitTargetRef}" ] || log_error 11 "Git target reference must be given for pull requests"

		check_branch "${gitTargetRef}" "${mavenVersion}" "${versionType}"
	else
		log_error 3 "Git reference not permitted | Git reference: ${gitRef}"
	fi
}

#### Program start
function main() {
	if [ $# -ne 3 ]; then
		log_error 2 "Usage: ${0} [gitRef] [mavenVersion] [gitTargetRef]"
	fi

	local -r gitRef="${1}"
	local -r mavenVersion="${2}"
	local -r gitTargetRef="${3}"

	# Check that Maven version is compliant with SemVer
	local versionType
	versionType="$(check_semver "${mavenVersion}")" # Prevent masking of return value
	readonly versionType

	# Check Maven version against Git reference
	check_git_reference "${gitRef}" "${mavenVersion}" "${versionType}" "${gitTargetRef}"
}

main "${@}"
