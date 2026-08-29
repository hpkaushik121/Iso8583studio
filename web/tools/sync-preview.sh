#!/bin/sh
# Copies the build into the session scratchpad, which is where the sandboxed
# preview server can read it from.
set -e
DEST="/private/tmp/claude-501/-Users-mark-2-Desktop-Fintech-ISO8583Studio/f8548348-e06d-4ba3-98cf-15b6b80ad436/scratchpad/preview/site"
rm -rf "$DEST"
cp -R "$(dirname "$0")/../dist/iso8583-studio/browser" "$DEST"
echo "preview synced -> $DEST"
