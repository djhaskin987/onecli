#!/bin/sh
set -ex


if [ ! -f './project.clj' -a ! -f './build.boot' ]
then
    echo "This script must be run from the root of the project."
fi
root_path=${PWD}
test_home=test/resources/data/all
rm -rf "${test_home}"
mkdir -p "${test_home}"
cd "${test_home}"

lein uberjar

# Script tests
#for i in test/resources/scripts/test-*
#do
#    if [ ${i} != test/resources/scripts/test-all ]
#    then
#        ./${i}
#    fi
#done

java -jar "${root_path}/target/uberjar/onecli-0.1.0-SNAPSHOT-standalone.jar" options show
cat > "${test_home}/onecli.json" << ONECLI
{
"one": {
"two": 238,
"three": 543
},
"zed": {
1: "a",
2: "b"
}
}
ONECLI

java -jar "${root_path}/target/uberjar/onecli-0.1.0-SNAPSHOT-standalone.jar" options show | jq '.one.two' | grep -q '^238$'
