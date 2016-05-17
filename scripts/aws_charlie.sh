#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol rtv -eddie_ip 52.32.4.179 -debbie_ip 52.40.35.23 charlie
