## build amqp-http-logger
docker buildx build --platform linux/arm64/v8,linux/arm64,linux/amd64 . -f http-amqp-logger.dockerfile -t sparkworks/ac3-amqp-http-request-logger:latest --push
