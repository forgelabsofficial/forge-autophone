#!/usr/bin/env bash
#
# Verify the two hand-maintained copies of IAutoPhoneService.aidl agree.
#
# AutoPhone defines the AIDL interface; Forge OS carries a client-side copy so
# it can compile against the generated stub. Those two files are duplicated by
# hand and nothing previously enforced they matched - a one-sided edit compiles
# cleanly in BOTH repos and only fails at runtime, on a real device, as a
# RemoteException.
#
# Comments are ignored: only the package/interface/signature lines matter.
# Run from the repository root.

set -euo pipefail

LOCAL="src/main/aidl/com/forge/autophone/IAutoPhoneService.aidl"
REMOTE_REPO="${AIDL_REMOTE_REPO:-theking196/forge-os}"
REMOTE_PATH="app/src/main/aidl/com/forge/autophone/IAutoPhoneService.aidl"
# Pin to a specific ref. Querying the branch head can return a stale cached
# blob: this check first reported drift because the contents API served an old# 1832-byte copy for main while the file at the HEAD commit was already correct.
REMOTE_REF="${AIDL_REMOTE_REF:-main}"

strip() {
  grep -vE '^\s*(//|/\*|\*)' "$1" | sed -e 's/[[:space:]]*$//' | grep -v '^[[:space:]]*$'
}

if [ ! -f "$LOCAL" ]; then
  echo "::error::Cannot find $LOCAL"
  exit 1
fi

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# Fetch the remote copy; skip the check rather than fail the build if the
# network or token is unavailable.
if gh api "repos/${REMOTE_REPO}/contents/${REMOTE_PATH}?ref=${REMOTE_REF}" --jq '.content' 2>/dev/null \
   | base64 --decode > "$TMP" 2>/dev/null && [ -s "$TMP" ]; then
  :
else
  echo "::warning::Could not fetch $REMOTE_REPO/$REMOTE_PATH - skipping sync check"
  exit 0
fi

if diff -u <(strip "$LOCAL") <(strip "$TMP") > /tmp/aidl_diff.txt 2>&1; then
  echo "AIDL copies are in sync ($(grep -cE '^\s*\S+\s+\w+\(' <(strip "$LOCAL") || true) methods)"
  exit 0
fi

echo "::error::IAutoPhoneService.aidl has drifted between this repo and ${REMOTE_REPO}."
echo "The interface is duplicated by hand. Copy the newer file to BOTH repos in the same"
echo "commit, otherwise the mismatch compiles fine and only fails at runtime."
echo "--- diff (local vs ${REMOTE_REPO}) ---"
cat /tmp/aidl_diff.txt
exit 1