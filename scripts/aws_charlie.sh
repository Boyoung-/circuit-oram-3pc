#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol rtv -eddie_ip 52.40.162.90 -debbie_ip 52.36.163.127 charlie -pipeline
