docker buildx build --platform linux/amd64,linux/arm/v7 . -f Dockerfile -t sparkworks/sw-mapper-ac3:0.15 --push
sleep 30
