# Profile document
- dev-executor: used to run executor instances (ones executing the tasks) outside the container environment with direct IP (ex: 192.168.100.1)  
- dev-scheduler: used to run scheduler instances (ones scheduling the tasks) outside the container environment with direct IP (ex: 192.168.100.1)
- prd-executor: used to run executor instances (ones executing the tasks) outside the container environment using container hostname rather than ip (ex: mysql)
- prd-scheduler:  used to run scheduler instances (ones scheduling the tasks) outside the container environment with direct IP (ex: mysql)

# Common tasks
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

# Run on dev pofile - local
Run executor  from java command line
```
java '-Dspring.profiles.active=dev-executor' -jar ./build/libs/network-statistic-0.0.1-SNAPSHOT.jar
```

Run dev scheduler from java cmd
```
java '-Dspring.profiles.active=dev-scheduler' -jar ./build/libs/network-statistic-0.0.1-SNAPSHOT.jar
```

# Run inside container
Run executor  from java command line
```
java '-Dspring.profiles.active=prd-executor' -jar ./build/libs/network-statistic-0.0.1-SNAPSHOT.jar
```

Run scheduler from java cmd
```
java '-Dspring.profiles.active=prd-scheduler' -jar ./build/libs/network-statistic-0.0.1-SNAPSHOT.jar
```
