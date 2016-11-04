#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol rtv -eddie_ip 35.161.54.54 -debbie_ip 35.162.185.55 charlie
