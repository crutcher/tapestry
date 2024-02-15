#!/bin/bash

SCRIPT_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd)"

find "$SCRIPT_DIR" -name '*.dot' | while read f; do
  echo $f;
  dot -O -Tpng -Tsvg $f;
done

