## build consumer
docker build -t ac3-connector-http-http-consumer -f consumer.dockerfile .
docker tag ac3-connector-http-http-consumer sparkworks/ac3-connector-http-http-consumer:latest
docker push sparkworks/ac3-connector-http-http-consumer:latest
