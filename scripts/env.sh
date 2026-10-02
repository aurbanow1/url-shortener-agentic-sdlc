# Source this before building or launching seats:   source scripts/env.sh
# - JDK 21: Gradle 9 and Spring Boot 4 need >= 17; this machine's default is 11.
# - Repo-local Gradle home (.gradle-home/ in the MAIN checkout, also when sourced
#   from a slice worktree) so sandboxed seats can build --offline from a warm cache.
_main_git_dir="$(git rev-parse --git-common-dir 2>/dev/null || echo .git)"
ROOT="$(cd "$(dirname "$_main_git_dir")" 2>/dev/null && pwd || pwd)"
export JAVA_HOME="${URLSHORT_JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME="$ROOT/.gradle-home"
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.daemon=true"
java -version 2>&1 | head -1
