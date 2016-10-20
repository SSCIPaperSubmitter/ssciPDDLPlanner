### 1. Introduction

This software is used for generating PDDL files out of model descriptions. PDDL is a well-known artificial intelligence planning language. Please note that even though is application generates PDDL, it is not used to interpret PDDL. Users of this software are referred to open-source PDDL planners such as OPTIC planner for this task (see [link](https://github.com/Dunes/janitor/tree/master/optic)).

### 2. Compiling and Using the Software

#### 2.1 Requirements

To install the software, you need a reasonably modern JDK/Maven setup. The following versions of JDK and Maven have been tested with this software, but it should work with newer versions also:

* Oracle JDK build 1.8.0_11-b12
* Apache Maven 3.1.1 

#### 2.2 Installation and Use

To build the project, issue the following command from the terminal:

`mvn clean package`

To run the project, once maven finished building it:

`java -jar target/pddlgenerator-1.0-SNAPSHOT.jar`

To test the generator with the sample model files, you can use the curl command-line utility (available in MAC OS and most linux distributions). The following example comes prebuilt with the bus model (included under 'resources' folder). Also note that this example uses the TransportPlanner transportation rules file, which contains PDDL-specific terminology for the transportation planning domain. This file is also provided, under the 'resources' folder

`curl -F 'transitionsFile=@resources/BusTransitions.ttl' -F 'statesFile=@resources/busLogistics.ttl' -F 'TRFile=@resources/TransportPlannerTR.ttl' -X POST localhost:8080/submitInput`

#### 2.3 API

The software has an API which it uses to load the models and generate the PDDL files. The API is simple, and consists of a single multipart HTTP POST request.
 
