#!/usr/bin/env bash
# Apply pub-cache build.gradle patches required to build SQuartor on AGP 9.
#
# Why this exists:
#   * file_picker 11.0.2 skips applying the Kotlin plugin when
#     `android.builtInKotlin` is false on AGP 9+, which breaks Java compilation
#     against its Kotlin sources.
#   * flutter_inappwebview_android 1.1.3 references
#     `proguard-android.txt`, which AGP 9 removed.
#
# Until both upstreams release fixed versions, we keep our patched
# `build.gradle` files in `tooling/pub-cache-patches/` and copy them over the
# resolved versions in pub-cache. Re-run this script after every
# `flutter pub get` (or after `flutter pub cache repair`).
#
# Override the cache location with $PUB_CACHE if needed; otherwise we try the
# common Windows / *nix defaults.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
patches_dir="$repo_root/tooling/pub-cache-patches"

candidates=()
if [[ -n "${PUB_CACHE:-}" ]]; then
  candidates+=("$PUB_CACHE")
fi
case "$(uname -s 2>/dev/null || echo Windows)" in
  *NT*|MINGW*|MSYS*|CYGWIN*|Windows)
    candidates+=("${LOCALAPPDATA:-$HOME/AppData/Local}/Pub/Cache")
    candidates+=("$HOME/AppData/Local/Pub/Cache")
    ;;
  *)
    candidates+=("$HOME/.pub-cache")
    ;;
esac

cache_root=""
for c in "${candidates[@]}"; do
  if [[ -d "$c/hosted/pub.dev" ]]; then
    cache_root="$c"
    break
  fi
done

if [[ -z "$cache_root" ]]; then
  echo "error: cannot locate pub-cache. Tried: ${candidates[*]}" >&2
  echo "Set PUB_CACHE explicitly and retry." >&2
  exit 1
fi

echo "Using pub-cache at: $cache_root"

apply_one() {
  local pkg_subpath="$1"
  local src="$patches_dir/$pkg_subpath"
  local dest="$cache_root/hosted/pub.dev/$pkg_subpath"
  if [[ ! -f "$src" ]]; then
    echo "  skip $pkg_subpath (patch missing)"; return 0
  fi
  if [[ ! -d "$(dirname "$dest")" ]]; then
    echo "  skip $pkg_subpath (package not in cache yet — run flutter pub get first)"; return 0
  fi
  if [[ -f "$dest" ]] && cmp -s "$src" "$dest"; then
    echo "  ok   $pkg_subpath (already patched)"; return 0
  fi
  cp "$src" "$dest"
  echo "  wrote $pkg_subpath"
}

apply_one "file_picker-11.0.2/android/build.gradle"
apply_one "flutter_inappwebview_android-1.1.3/android/build.gradle"

echo "Done."
