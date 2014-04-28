#!/bin/bash

# Shutdown the GigaSpaces infrastructure associated with a particular zone

. `dirname $0`/gs-setup.sh

java -jar `dirname $0`/../lib/zone-shutdown.jar $1

