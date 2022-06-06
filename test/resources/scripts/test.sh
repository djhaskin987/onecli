#!/bin/sh
set -ex

export ONECLI_ITEM_OUTPUT_FORMAT="json"

if [ ! -f './project.clj' ]
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
jarfile="${root_path}/target/uberjar/${name}-${version}-standalone.jar"

java -jar "${jarfile}" options show
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
java -jar "${jarfile}" options show
cat > "${test_home}/onecli.yaml" << ONECLI
one:
  two: 238
  three: 543
zed:
  a: true
  b: false
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

answer=$(ONECLI_ITEM_ANONYMOUS_COWARD="I was never here" ONECLI_LIST_CONFIG_FILES="./a.json,./b.json" java -jar "${jarfile}" options show --add-config-files '-' --json-fart '123' << ALSO
{
    "ifihadtodoitagain": "i would",
    "again": "andagain\nandagain\nandagain"
}
ALSO
)

expected='{"one":{"two":238,"three":543},"anonymous-coward":"I was never here","println":"setup","bfound":true,"output-format":"json","filename":null,"commands":["options","show"],"fart":123,"again":"andagain\nandagain\nandagain","ifihadtodoitagain":"i would","zed":{"a":true,"b":false},"afound":true}'

if [ "${answer}" != "${expected}" ]
then
    echo "Failed conditional test, normal case."
    exit 1
fi

answer=$(ONECLI_ITEM_ANONYMOUS_COWARD="I was never here" ONECLI_LIST_CONFIG_FILES="./a.json,./b.json" java -jar "${jarfile}" options show --set-output-format yaml --add-config-files '-' --yaml-fart '123' << ALSO
ifihadtodoitagain: i would
again: "andagain\nandagain\nandagain"
ALSO
)

expected='one:
  two: 238
  three: 543
anonymous-coward: I was never here
println: setup
bfound: true
output-format: yaml
filename: null
commands:
 - options
 - show
fart: 123
again: |-
  andagain
  andagain
  andagain
ifihadtodoitagain: i would
zed:
  a: true
  b: false
afound: true'

if [ "${answer}" != "${expected}" ]
then
    echo "Failed conditional test, YAML output"
    exit 1
fi

# There better still be tabs in JSON stacktrace output
tabs_in_json=$(
(java -jar "${jarfile}" exc -o json || :) |
    jq -r .stacktrace |
    sed -n '/\t/p')
if [ -z "${tabs_in_json}" ]
then
    echo "Failed conditional test, tabs are in JSON stacktraces"
	exit 1
fi

# There better not be any tabs in YAML stacktrace output
tabs_in_yaml=$(
(java -jar "${jarfile}" exc -o yaml || :) |
    yq .stacktrace |
    sed -n '/\t/p')
if [ -n "${tabs_in_yaml}" ]
then
    echo "Failed test, tabs are in JSON stacktraces"
	exit 1
fi

# Test that json multiple key last-wins behavior is preserved with the new yaml
# update
answer=$(java -jar "${jarfile}" --json-fire '{ "a": true, "a": 13, "a": false }' options show | jq -c .fire)
expected='{"a":false}'

if [ "${answer}" != "${expected}" ]
then
    echo "Failed conditional test, JSON multiple key behavior is preserved"
    exit 1
fi
