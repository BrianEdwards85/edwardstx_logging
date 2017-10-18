#!/bin/bash

/usr/local/bin/lein uberjar

/usr/bin/docker build -t edwardstx/edwardstx_logging .

/usr/bin/docker run -d --restart always -v /etc/service:/etc/service --name edwardstx_logging edwardstx/edwardstx_logging
