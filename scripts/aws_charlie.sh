#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" -protocol rtv -eddie_ip 52.11.141.98 -debbie_ip 52.38.128.79 eddie