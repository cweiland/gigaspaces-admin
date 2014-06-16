#!/bin/bash

# Monitor the specified space.  SpaceMonitor uses the LOOKUPLOCATORS and
# LOOKUPGROUPS environment variables, if set.

. `dirname $0`/gs-setup.sh

java -cp ${REQUIRED_DIR}/*:`dirname $0`/../lib/monitor.jar \
     com.gigaspaces.examples.monitor.SpaceMonitor \
     ${MONITOR_LOG_FILE}
