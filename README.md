# Getting Started

сборка
docker build -t akpeisov/mqttdb:v1 .
docker push akpeisov/mqttdb:v1

на сервере
docker pull akpeisov/mqttdb:v1
docker logs container  покажет все логи в реальном времени


Чтобы собрать JAR без запуска, т.к. при запуске он не найдет енвы и не соберет jar
& "D:\spring\apache-maven-3.8.6\bin\mvn.cmd" `-Dmaven.test.skip=true package -f "d:\projects\mqttdb\pom.xml"