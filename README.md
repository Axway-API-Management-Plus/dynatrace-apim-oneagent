# Dynatrace Axway APIM Integration 

Axway APIM Integration with Dynatrace using Dynatrace One Agent

## API Management Version Compatibility

This artefact tested with following versions:

- V7.7 November 2022 release and above use main branch
- V7.7 November 2021 release use Nov2021 branch


## Compile/Build

In `build.gradle` file, update dependencies location:

- Set the variable `apim_folder` to you API-Gateway installation folder (e.g. `opt/Axway/APIM/apigateway/system`)


```
gradlew clean jar
```

## Setup 

- Copy dynatrace-aspectj-x.x.x.jar file to  apigateway/ext/lib
- Copy Dynatrace one agent library - https://repo1.maven.org/maven2/com/dynatrace/oneagent/sdk/java/oneagent-sdk/1.8.0/oneagent-sdk-1.8.0.jar to  apigateway/ext/lib
- Copy Aspectj weaver - https://repo1.maven.org/maven2/org/aspectj/aspectjweaver/1.9.6/aspectjweaver-1.9.6.jar to  apigateway/ext/lib
- Create a file named jvm.xml under APIGATEWAY_INSTALL_DIR/apigateway/conf/
    ```xml
    <ConfigurationFragment>
        <VMArg name="-javaagent:/home/axway/Axway-7.7.0-Aug2021/apigateway/ext/lib/aspectjweaver-1.9.6.jar"/>
    </ConfigurationFragment>
    ```
- Restart API Gateway instances

- Create Request Attributes in Dynatrace.
    - AxwayAppId
    - AxwayAppName
    - AxwayCorrelationId
    - AxwayOrgName
     
     Go to Settings > Server-side service monitoring > Request Attributes > Define a new request attribute.  Create a Request Attribute for each property with a datasource configured to use an "SDK custom attribute" as in the following screenshot.
     
    ![image](https://user-images.githubusercontent.com/58127265/234663741-32b38f29-371a-4413-9c1a-5b81b6a56af8.png)
## Requests captured in Dynatrace
- Policy exposed as Endpoint. 
- API manager Traffic
- API Repository defined in Policystudio
- API Manager UI traffic

## Requests not captured in Dynatrace
- API Manager REST API call.
- Servlet defined in Policystudio.

# Dynatrace FAQ
## Dynatrace is grouping services as volatile WebRequest
- as on Axway SDK, we are dividing the services by URI, specifically on front-end axway servers, Dynatrace is grouping services as volatile webrequests.
https://community.dynatrace.com/t5/Open-Q-A/volatile-WebRequest/td-p/180705
to resolve this issue, increasing the bucket size of dynatrace managed environment via support ticket.


## Contributing

Please read [Contributing.md](https://github.com/Axway-API-Management-Plus/Common/blob/master/Contributing.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Team

![alt text][Axwaylogo] Axway Team

[Axwaylogo]: https://github.com/Axway-API-Management/Common/blob/master/img/AxwayLogoSmall.png  "Axway logo"

## License
[Apache License 2.0](LICENSE)
