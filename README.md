# Dynatrace Axway APIM Integration 

Axway APIM Integration with Dynatrace using Dynatrace One Agent

## API Management Version Compatibility

This artefact tested with following versions:

- V7.7 November 2022 release and above use main brach
- V7.7 November 2021 release use Nov2021 branch


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
    ### API Gateway without API Manager
    ```xml
    <ConfigurationFragment>
        <VMArg name="-javaagent:/home/axway/Axway-7.7.0-Aug2021/apigateway/ext/lib/aspectjweaver-1.9.6.jar"/>
        <SystemProperty name="apimanager" value="false" />
    </ConfigurationFragment>
    ```
    ### API Gateway and API Manager
    ```xml
    <ConfigurationFragment>
        <VMArg name="-javaagent:/home/axway/Axway-7.7.0-Aug2021/apigateway/ext/lib/aspectjweaver-1.9.6.jar"/>
    </ConfigurationFragment>
    ```

- Create Request Attributes in Dynatrace.
    - AxwayAppId
    - AxwayAppName
    - AxwayCorrelationId
    - AxwayOrgName
     
     Go to Settings > Server-side service monitoring > Request Attributes > Define a new request attribute.  Create a Request Attribute for each property with a datasource configured to use an "SDK custom attribute" as in the following screenshot.
     
    ![image](https://user-images.githubusercontent.com/58127265/234663741-32b38f29-371a-4413-9c1a-5b81b6a56af8.png)


## Contributing

Please read [Contributing.md](https://github.com/Axway-API-Management-Plus/Common/blob/master/Contributing.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Team

![alt text][Axwaylogo] Axway Team

[Axwaylogo]: https://github.com/Axway-API-Management/Common/blob/master/img/AxwayLogoSmall.png  "Axway logo"

## License
[Apache License 2.0](LICENSE)
