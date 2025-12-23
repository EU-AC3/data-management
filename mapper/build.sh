docker buildx build --platform linux/amd64 . -f Dockerfile -t sparkworks/sw-mapper-ac3:0.14 --push

sleep 30
