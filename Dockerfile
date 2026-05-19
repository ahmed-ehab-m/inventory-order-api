# first we get our java template
# temurin => different order of chars of runtime word
# AdoptOpenJdk is open source project to provice free and stable java versions
# so eclipse take it and rename it to eclipse-temurin
FROM  eclipse-temurin:21-jdk

# for more orgnaization
# create new folder app and go into
# and copy our jar into this folder
WORKDIR /app

# copy our java into root folder of container and name it demo-docker.jar
COPY  target/inventory-order-api.jar inventory-order-api.jar

# meta data about the author of this image
LABEL authors="HP"

# when any one run docker run this command automatic
# this defaule command to run spring boot application
# use java command with -jar to run demo-docker.jar file
ENTRYPOINT ["java","-jar","inventory-order-api.jar"]