body='{
"request": {
  "branch":"externalCI",
  "config": {
    "env": {
      "AKKA_VERSION": "v2.5.23",
      "TRAVIS_NODE_VERSION": "10.16.0"
    }
  }
}}'

REQUEST=$(curl -s -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $(cat travis.token)" \
   -d "$body" \
   'https://api.travis-ci.org/repo/akka-js%2Fakka.js/requests')

echo "Request result: $REQUEST"

PIPELINE_ID=$(echo $REQUEST | jq -r '.request.id')

echo "PIPELINE ID: $PIPELINE_ID"

function pipeline_status() {
  PIPELINE_STATUS=$(curl -s -X GET \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -H "Travis-API-Version: 3" \
    -H "Authorization: token $(cat travis.token)" \
    "https://api.travis-ci.org/repo/akka-js%2Fakka.js/request/$PIPELINE_ID" | jq -r '.builds[].state')

  echo $PIPELINE_STATUS
}

pipeline_status

while [ "$PIPELINE_STATUS" == "" ] || [ "$PIPELINE_STATUS" == "created" ] || [ "$PIPELINE_STATUS" == "started" ]; do
  sleep 60;
  pipeline_status;
done;

if [ "$PIPELINE_STATUS" == "passed" ]; then
  echo "SUCCESS"
  exit 0
else
  echo "FAILURE"
  exit 1
fi
