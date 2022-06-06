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
java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" options show
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

answer=$(ONECLI_ITEM_ANONYMOUS_COWARD="I was never here" ONECLI_LIST_CONFIG_FILES="./a.json,./b.json" java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" options show --add-config-files '-' --json-fart '123' << ALSO
{
    "ifihadtodoitagain": "i would",
    "again": "andagain\nandagain\nandagain"
}
ALSO
)

expected='{"one":{"two":238,"three":543},"anonymous-coward":"I was never here","println":"setup","bfound":true,"output-format":"json","filename":null,"commands":["options","show"],"fart":123,"again":"andagain\nandagain\nandagain","ifihadtodoitagain":"i would","zed":{"a":true,"b":false},"afound":true}'

if [ "${answer}" != "${expected}" ]
then
    echo "Failed: first test, normal case."
    exit 1
fi

answer=$(ONECLI_ITEM_ANONYMOUS_COWARD="I was never here" ONECLI_LIST_CONFIG_FILES="./a.json,./b.json" java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" options show --set-output-format yaml --add-config-files '-' --yaml-fart '123' << ALSO
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
    echo "Failed: second test, YAML output"
    exit 1
fi

answer=$(java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" exc -o yaml || :)
expected='given-options:
  output-format: yaml
  one:
    two: 238
    three: 543
  zed:
    a: true
    b: false
  commands:
   - exc
e: 1538
g: 15.38
c: why not?
h: |-
  multi
  line
  string
b: false
stacktrace: |
  clojure.lang.ExceptionInfo: This is an exception. {:e 1538, :g 15.38, :c "why not?", :h "multi\nline\nstring", :b false, :d 1538N, :f 15.38M, :i "multi\n\tline\n\tstring\n\twith\n\ttabs", :a 1}
      at onecli.cli$exc.invokeStatic(cli.clj:7)
      at onecli.cli$exc.invoke(cli.clj:7)
      at clojure.lang.Var.invoke(Var.java:384)
      at onecli.core$go_BANG_.invokeStatic(core.clj:671)
      at onecli.cli$_main.invokeStatic(cli.clj:71)
      at onecli.cli$_main.doInvoke(cli.clj:46)
      at clojure.lang.RestFn.applyTo(RestFn.java:137)
      at onecli.cli.main(Unknown Source)
d: !!float '\''1538'\''
f: 15.38
error: '\''clojure.lang.ExceptionInfo: This is an exception. {:e 1538, :g 15.38, :c "why not?", :h "multi\nline\nstring", :b false, :d 1538N, :f 15.38M, :i "multi\n\tline\n\tstring\n\twith\n\ttabs", :a 1}'\''
i: "multi\n\tline\n\tstring\n\twith\n\ttabs"
a: 1'
if [ "${answer}" != "${expected}" ]
then
    echo "Failed: third test, YAML output of an exception"
    exit 1
fi


# Test that json multiple key last-wins behavior is preserved with the new yaml
# update
answer=$(java -jar "${root_path}/target/uberjar/${name}-${version}-standalone.jar" --json-fire '{ "a": true, "a": 13, "a": false }' options show | jq -c .fire)
expected='{"a":false}'

if [ "${answer}" != "${expected}" ]
then
    echo "Failed: fourth test, JSON multiple key behavior is preserved"
    exit 1
fi
