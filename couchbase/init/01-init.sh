#!/bin/bash
set -e

CB_HOST=127.0.0.1
CB_USER="${CB_USERNAME:-Administrator}"
CB_PASS="${CB_PASSWORD:-password}"
CB_BUCKET="mybucket"
CB_SCOPE="_default"
CB_COLLECTION="my_users"
CB_CLUSTER_NAME="mycluster"
DATASET_PATH="/opt/couchbase/init/test-data.json"
ERROR_LOG="/opt/couchbase/var/lib/couchbase/logs/cbimport-my_users-errors.log"

echo ">>> Starting Couchbase Server..."
/entrypoint.sh couchbase-server &

echo ">>> Waiting for Couchbase to be ready on :8091..."
until curl -s "http://${CB_HOST}:8091/ui/index.html" >/dev/null 2>&1; do
  echo "   Couchbase not ready yet, retrying in 2s..."
  sleep 2
done

echo ">>> Checking if cluster is already initialized..."
if /opt/couchbase/bin/couchbase-cli server-list \
      --cluster "http://${CB_HOST}:8091" \
      --username "${CB_USER}" \
      --password "${CB_PASS}" >/dev/null 2>&1; then
  echo ">>> Cluster already initialized, skipping cluster-init."
else
  echo ">>> Initializing new cluster '${CB_CLUSTER_NAME}'..."
  /opt/couchbase/bin/couchbase-cli cluster-init \
    --cluster "http://${CB_HOST}:8091" \
    --cluster-name "${CB_CLUSTER_NAME}" \
    --cluster-username "${CB_USER}" \
    --cluster-password "${CB_PASS}" \
    --cluster-ramsize 2048 \
    --services data,index,query \
    --index-storage-setting default
fi

echo ">>> Ensuring bucket '${CB_BUCKET}' exists..."
if ! /opt/couchbase/bin/couchbase-cli bucket-list \
        --cluster "http://${CB_HOST}:8091" \
        --username "${CB_USER}" \
        --password "${CB_PASS}" | grep -q "${CB_BUCKET}"; then

  /opt/couchbase/bin/couchbase-cli bucket-create \
    --cluster "http://${CB_HOST}:8091" \
    --username "${CB_USER}" \
    --password "${CB_PASS}" \
    --bucket "${CB_BUCKET}" \
    --bucket-type couchbase \
    --bucket-ramsize 1024 \
    --enable-flush 1 \
    --wait
else
  echo "   Bucket ${CB_BUCKET} already exists."
fi

echo ">>> Ensuring collection '${CB_SCOPE}.${CB_COLLECTION}' exists..."
# _default scope exists automatically for Couchbase buckets in 7.x
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST -u "${CB_USER}:${CB_PASS}" \
  "http://${CB_HOST}:8091/pools/default/buckets/${CB_BUCKET}/scopes/${CB_SCOPE}/collections" \
  -d "name=${CB_COLLECTION}")

if [ "${HTTP_STATUS}" = "200" ] || [ "${HTTP_STATUS}" = "202" ]; then
  echo "   Collection created."
else
  echo "   Collection may already exist or another condition occurred (HTTP ${HTTP_STATUS}), continuing..."
fi
echo ">>> Waiting for Query Service (:8093) to be ready..."
until curl -s "http://${CB_HOST}:8093/admin/ping" >/dev/null 2>&1; do
  echo "   Query service not ready yet, retrying in 2s..."
  sleep 2
done

echo ">>> Creating primary index on ${CB_BUCKET}.${CB_SCOPE}.${CB_COLLECTION}..."

INDEX_QUERY="CREATE PRIMARY INDEX idx_primary_my_users ON \`${CB_BUCKET}\`.\`${CB_SCOPE}\`.\`${CB_COLLECTION}\`;"

INDEX_OUTPUT=$(
  /opt/couchbase/bin/cbq \
    -e "http://${CB_HOST}:8093" \
    -u "${CB_USER}" -p "${CB_PASS}" \
    -s "${INDEX_QUERY}" 2>&1
)

if echo "$INDEX_OUTPUT" | grep -q "already exists"; then
  echo "   Primary index already exists. Continuing..."
elif echo "$INDEX_OUTPUT" | grep -q "\"status\": \"fatal\""; then
  echo "   Error creating index:"
  echo "$INDEX_OUTPUT"
  exit 1
else
  echo "   Primary index created."
fi

if [ -f "${DATASET_PATH}" ]; then
  echo ">>> Importing JSON data from ${DATASET_PATH} into ${CB_BUCKET}.${CB_SCOPE}.${CB_COLLECTION}..."
  /opt/couchbase/bin/cbimport json \
    --cluster "couchbase://${CB_HOST}" \
    --username "${CB_USER}" \
    --password "${CB_PASS}" \
    --bucket "${CB_BUCKET}" \
    --dataset "file://${DATASET_PATH}" \
    --format list \
    --generate-key "user::#UUID#" \
    --scope-collection-exp "${CB_SCOPE}.${CB_COLLECTION}" \
    --threads 2 \
    --errors-log "${ERROR_LOG}"

  echo ">>> cbimport completed. Any skipped/invalid docs are logged in:"
  echo "    ${ERROR_LOG}"
else
  echo ">>> WARNING: Dataset file ${DATASET_PATH} not found. Skipping cbimport."
fi

echo ">>> Couchbase initialization finished. Attaching to server process..."
wait