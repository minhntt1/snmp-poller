# Run on local (local profile by default)
Init environment: grafana, etc.
```
cd docker_dev
docker compose up -d
```

Run spring application from IntellJ
```
gradle bootRun
```

Build and skip test
```
gradle clean build -x test
```
# Run on server (dev profile)
