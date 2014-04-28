#!/bin/bash

# Shutdown the GigaSpaces infrastructure

. `dirname $0`/gs-setup.sh

java -jar `dirname $0`/../lib/shutdown.jar `dirname $0`/shutdown.properties

