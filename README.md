
# Examples
This section highlights example implementations for various programming languages and tools how to communicate with the Wikitude Cloud Recognition service. The examples are just meant to assist you getting started with the Manager API of the Cloud Recognition service in your project. 
Every example is structured in the following way:

* `ManagerAPI.[extension]` wraps the interaction between the client and the Wikitude Manager API.
* `ManagerAPITest.[extension]` configures the ManagerAPI object (version, token etc.) and triggers the conversion.

In addition, the following applies to all examples:

* To run the samples, the Manager API token string in `ManagerAPITest.[extension]` (currently set to `"<enter-your-token-here>"`) must be changed to a valid token. To generate a token, please check [on the license page](http://www.wikitude.com/developer/licenses).


## Java
<div class="githubnote">
view source code on <a target="_blank" href="https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/tree/master/Java">GitHub</a>
</div>

## Node.js
<div class="githubnote">
view source code on <a target="_blank" href="https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/tree/master/NodeJs">GitHub</a>
</div>
## PHP
<div class="githubnote">
view source code on <a target="_blank" href="https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/tree/master/PHP">GitHub</a>
</div>
## Python
<div class="githubnote">
view source code on <a target="_blank" href="https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/tree/master/Python">GitHub</a>
</div>
## Ruby
<div class="githubnote">
view source code on <a target="_blank" href="https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/tree/master/Ruby">GitHub</a>
</div>

# Change Log
All notable changes to this project will be documented in this file.

## [2.1.0]
 * compatible with API version 2
 * an example for polling the status of asynchronous operations was
   added
 * addTargets api method was added
 * generateTargetCollection now waits until the operations finishes
 * Java
    * APIException gets thrown if the service responds with an error
 * NodeJs
    * APIError gets thrown if the service responds with an error
    * api methods now return a Promises
 * PHP
    * APIException gets thrown if the service responds with an error
 * Python
    * APIException gets thrown if the service responds with an error
 * Ruby
    * APIError gets thrown if the service responds with an error

## 2.0.0
 * initial release version of examples
 * compatible with API version 2

[2.1.0]: https://github.com/Wikitude/wikitude-cloud-recognition-api-samples/compare/v2.0.0...v2.1.0