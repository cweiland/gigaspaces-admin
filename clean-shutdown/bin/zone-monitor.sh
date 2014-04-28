#!/bin/bash

# Wait for the GigaSpaces infrastructure associated with a particular
# zone to be available.

. `dirname $0`/gs-setup.sh

java -jar `dirname $0`/../lib/zone-monitor.jar $1 $2

