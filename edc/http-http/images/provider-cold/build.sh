## build provider_cold
docker build -t ac3-connector-http-http-provider -f provider_cold.dockerfile .
docker tag ac3-connector-http-http-provider sparkworks/ac3-connector-http-http-provider:latest
docker push sparkworks/ac3-connector-http-http-provider:latest
