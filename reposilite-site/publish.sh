#!/usr/bin/env bash

cd website || exit
yarn build

rm -rf ../../docs
mkdir ../../docs
mv ./build/reposilite-site/* ../../docs

git add ../../.