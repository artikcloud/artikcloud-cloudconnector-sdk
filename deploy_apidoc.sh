#!/bin/bash
#
# Script for travis-ci to sync apidoc dir from master branch to gh-pages branch
# based on :
# * https://github.com/steveklabnik/automatically_update_github_pages_with_travis_example
# * https://github.com/gemini-testing/gemini/pull/352/files

set -o errexit -o nounset

if [ "$TRAVIS_BRANCH" != "master" ] ; then
  echo "This commit was made against the $TRAVIS_BRANCH and not the master! No deploy!"
  exit 0
fi


rev=$(git rev-parse --short HEAD)
repo=$(git config --local --get-all remote.origin.url | cut -d ':' -f2)

echo -e "Starting to update gh-pages of ${repo} at ${rev}\n"

# instead of cloning a remote repo, clone itself and define remote as upstream
rm -Rf gh-pages
git clone -b gh-pages . gh-pages
cd gh-pages
git remote add upstream https://${GH_TOKEN}@github.com/${repo}
git fetch upstream
git reset upstream/gh-pages

rsync -avz --stats ../../apidoc/ apidoc/

git config --local user.email "travis@travis-ci.org"
git config --local user.name "Travis"
git add -A .
git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed to gh-pages at ${rev}"
git push upstream gh-pages
cd ..
rm -Rf gh-pages

