#!/bin/bash

VERSION=$1

kubectl pipelines deploy gcr.io/gsa-pipeliners/lightbend/fraud-detection-trevor:"$VERSION" \
transaction-generator.influxdb-host="influxdb.influxdb" \
transaction-generator.influxdb-port="8086" \
log-transactions.influxdb-host="influxdb.influxdb" \
log-transactions.influxdb-port="8086" \
-u oauth2accesstoken -p $(gcloud auth print-access-token)
