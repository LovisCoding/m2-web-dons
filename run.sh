#!/bin/sh
set -eu
case "${1:-run}" in
  test) mvn test ;;
  run) mvn spring-boot:run ;;
  build) mvn package ;;
  *) echo "Usage: ./run.sh [run|test|build]" >&2; exit 2 ;;
esac
