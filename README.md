# Dynatrace Axway APIM Integration 

Axway APIM Integration with Dynatrace using Dynatrace One Agent

## API Management Version Compatibility
This artefact was successfully tested for the following versions:
- V7.6.2
- V7.7 and above

## Compile/Build

In `build.gradle` file, update dependencies location:

- Set the variable `apim_folder` to you API-Gateway installation folder (e.g. `opt/Axway/APIM/apigateway`)


```
gradlew clean jar
```


## Setup 

- Copy dynatrace-aspectj-x.x.x.jar file to  apigateway/ext/lib and restart API Gateway instances
- Copy Dynatrace one agent library - https://repo1.maven.org/maven2/com/dynatrace/oneagent/sdk/java/oneagent-sdk/1.8.0/oneagent-sdk-1.8.0.jar to  apigateway/ext/lib
- Copy Aspectj weaver - https://repo1.maven.org/maven2/org/aspectj/aspectjweaver/1.9.6/aspectjweaver-1.9.6.jar to  apigateway/ext/lib
- Restart API Gateway instances
- Create a file named jvm.xml under APIGATEWAY_INSTALL_DIR/apigateway/conf/
## API Gateway without API Manager
```xml
<ConfigurationFragment>
    <VMArg name="-javaagent:/home/axway/Axway-7.7.0-Aug2021/apigateway/ext/lib/aspectjweaver-1.9.6.jar"/>
    <SystemProperty name="apimanager" value="false" />
</ConfigurationFragment>
```
## API Gateway and API Manager
```xml
<ConfigurationFragment>
    <VMArg name="-javaagent:/home/axway/Axway-7.7.0-Aug2021/apigateway/ext/lib/aspectjweaver-1.9.6.jar"/>
</ConfigurationFragment>
```

## Contributing

Please read [Contributing.md](https://github.com/Axway-API-Management-Plus/Common/blob/master/Contributing.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Team

![alt text][Axwaylogo] Axway Team

[Axwaylogo]: https://github.com/Axway-API-Management/Common/blob/master/img/AxwayLogoSmall.png  "Axway logo"

## License
[Apache License 2.0](LICENSE)

