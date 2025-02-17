## build amqp-http-logger
docker build -t ac3-amqp-http-request-logger -f http-amqp-logger.dockerfile .
docker tag ac3-amqp-http-request-logger sparkworks/ac3-amqp-http-request-logger:latest
docker push sparkworks/ac3-amqp-http-request-logger:latest
