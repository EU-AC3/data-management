## build provider_hot
docker build -t ac3-connector-iot-http-http-provider -f provider_hot.dockerfile .
docker tag ac3-connector-iot-http-http-provider sparkworks/ac3-connector-iot-http-http-provider:latest
docker push sparkworks/ac3-connector-iot-http-http-provider:latest
