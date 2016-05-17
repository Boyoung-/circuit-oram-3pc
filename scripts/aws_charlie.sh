#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol rtv -eddie_ip 52.38.200.122 -debbie_ip 52.40.34.110 charlie
