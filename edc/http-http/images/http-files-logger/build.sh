## build http-logger
docker build -t ac3-http-request-logger -f http-files-logger.dockerfile .
docker tag ac3-http-request-logger sparkworks/ac3-http-request-logger:latest
docker push sparkworks/ac3-http-request-logger:latest
