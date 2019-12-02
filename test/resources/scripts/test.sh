#!/bin/sh
set -ex


if [ ! -f './project.clj' -a ! -f './build.boot' ]
then
    echo "This script must be run from the root of the project."
fi
root_path=${PWD}
test_home="${root_path}/test/resources/data/all"
name=$(lein print :name | sed 's|"||g')
version=$(lein print :version | sed 's|"||g')
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

java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" options show
cat > "${test_home}/onecli.json" << ONECLI
{
"one": {
"two": 238,
"three": 543
},
"zed": {
"a": true,
"b": false
}
}
ONECLI
cat > "${test_home}/a.json" << A
{
"afound": true
}
A
cat > "${test_home}/b.json" << B
{
"bfound": true
}
B
ls ./onecli.json

answer=$(ONECLI_ITEM_ANONYMOUS_COWARD="I was never here" ONECLI_LIST_CONFIG_FILES="./a.json,./b.json" java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" options show --add-config-files '-' --json-fart '123' << ALSO
{
    "ifihadtodoitagain": "i would"
}
ALSO
)

if [ ! "${answer}" = '{"one":{"two":238,"three":543},"anonymous-coward":"I was never here","bfound":true,"commands":["options","show"],"fart":123,"ifihadtodoitagain":"i would","zed":{"a":true,"b":false},"afound":true,"andidont":"causealliwantisyou"}' ]
then
    echo "AAAAH"
    exit 1
fi

