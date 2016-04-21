ARTIK Cloud Cloud Connector SDK
========================

[![Build Status](https://travis-ci.org/artikcloud/artikcloud-cloudconnector-sdk.svg?branch=master)](https://travis-ci.org/artikcloud/artikcloud-cloudconnector-sdk)

Background
-----------------------

A device can send its data directly to ARTIK Cloud via [API calls](https://developer.artik.cloud/documentation/connect-the-data/rest-and-websockets.html). However, some devices already send data to a third-party cloud. In this case, ARTIK Cloud can use the device's cloud, rather than the device, as the data source. You can build what we call a Cloud Connector to bridge ARTIK Cloud to the third-party cloud. ARTIK Cloud can then retrieve the device's data from that cloud.

This repository hosts the Cloud Connector SDK. You can write Cloud Connector Groovy code using this SDK.

 * [Using Cloud Connectors](https://developer.artik.cloud/documentation/connect-the-data/using-cloud-connectors.html) gives the overview of the Cloud Connector concept.
 * [Your first Cloud Connector](https://developer.artik.cloud/documentation/tutorials/your-first-cloud-connector.html) explains the developer workflow and Cloud Connector code, using Moves as an example.
 * [CloudConnector API Doc](http://artikcloud.github.io/artikcloud-cloudconnector-sdk/apidoc/) lists functions and structures, and explains goals and usages.
 * [CloudConnector Samples](https://github.com/artikcloud/artikcloud-cloudconnector-samples), the examples have been tested and work in production.

What is included in this repository?
-----------------------

 1. libs: SDK libraries
 2. apidoc: Cloud Connector SDK API documentation
 3. template: A template project. You can build and test your own Cloud Connector code based on it.
