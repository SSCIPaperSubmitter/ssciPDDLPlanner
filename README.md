### 1. Introduction

This software is used for generating PDDL files out of model descriptions. PDDL is a well-known artificial intelligence planning language. Please note that even though is application generates PDDL, it is not used to interpret PDDL. Users of this software are referred to open-source PDDL planners such as OPTIC planner for this task (see [link](https://github.com/Dunes/janitor/tree/master/optic)).

### 2. Compiling and Using the Software

#### 2.1 Requirements

To install the software, you need a reasonably modern JDK/Maven setup. The following versions of JDK and Maven have been tested with this software, but it should work with newer versions also:

* Oracle JDK build 1.8.0_11-b12 (currently available at http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* Apache Maven 3.1.1 (currently available at http://maven.apache.org/download.cgi)

#### 2.2 Installation and Use

To build the project, issue the following command from the terminal:

`mvn clean package`

To run the project, once maven finished building it:

`java -jar target/pddlgenerator-1.0-SNAPSHOT.jar`

To test the generator with the sample model files, you can use the curl command-line utility (available in MAC OS and most linux distributions). The following example comes prebuilt with the bus model (included under 'resources' folder). Also note that this example uses the TransportPlanner transportation rules file, which contains PDDL-specific terminology for the transportation planning domain. This file is also provided, under the 'resources' folder

`curl -F 'transitionsFile=@resources/BusTransitions.ttl' -F 'statesFile=@resources/BusStates.ttl' -F 'TRFile=@resources/TransportPlannerTR.ttl' -X POST localhost:8080/submitInput`

#### 2.3 API

The software has an API which it uses to load the models and generate the PDDL files. The API is simple, and consists of a single multipart HTTP POST request. Therefore, the content type must be multipart/form-data. The service point is submitInput, prefixed by the URL of the hosting server, for example localhost:8080/submitInput. The names to be used are transitionsFile for the transition file model, statesFile for the states file model and TRFile for the transformation rules model. The reply is a JSON file with two entries, one for the problem file (key problemFile) and one for the domain file (key domainFile).  

#### 2.4 Editing the models

In order to edit the model files, any standard editor will suffice, however Protege is recommended. Protege is available at http://protege.stanford.edu/. The sample BusStates/BusTransitions/TransportPlannerTR model files where created using Protege 5.0.0 (build beta-23).

#### 2.5 Model Structure

##### 2.5.1 Introduction

The model files are based on an ontology with multiple layers of abstraction. We design our ontology by combining high level concepts and cross-domain relationships borrowed from three areas: CPS; Agent-Based Model (ABM); and Systems-of-Systems (SoS). The proposed ontology consists of an Upper Ontology, which contains the CPS, ABM, and SoS concepts and relations and a general ITS Domain Ontology. The general ITS Domain Ontology can be further referenced from ontologies that instantiate transport-domain specific transitions and states. It is of course possible to extend the upper ontology with ontologies describing other domains than ITS, for example healthcare, energy and utilities, agriculture, etc.

The objective of breaking the ontology into multiple levels is twofold. First, this approach allows to capture and isolate different levels of properties, attributes and relationships. Higher layers provide broader definitions and more abstract concepts, while lower layers are less abstract and can support specific domains and applications with concepts and relations which might not be present in the upper levels.

Second, ontologies are expected to change, grow and evolve as new domains and techniques are contemplated in them (Davies et al., 2006). Leaving the more abstract and general concepts in an upper layer, and the more specific ones in lower layers, reinforces the idea that altering the most general concepts should be avoided, making them less likely to suffer constant modifications that could lead to unnecessary changes throughout the ontology. This is important because ontologies often reuse and extend other ontologies. Updating an ontology without proper care can potentially corrupt the others depending on it and consequently all the systems that use it.

##### 2.5.2 Upper ontology design principles

Upper ontologies should be designed to describe general concepts that can be used across all domains. They have a central role in facilitating interoperability among domain specific ontologies, which are built hierarchically underneath the upper and generic layers, and therefore can be seen as specialization of the more abstract concepts.

![Figure 1](/images/fig1.png)

Figure above presents a subset of the proposed upper ontology. Its development was prompted by our use cases in management and control of complex systems-of-systems, and was inspired by other ontologies such as SUMO (Suggested Upper Mergerd Ontology) (Niles and Pease, 2001), and W3C SSN (Semantic Sensor Network Ontology) (Compton et al., 2012).

Some important concepts defined on the proposed general ontology include System, Cyber-Physical System, Agent and CPS Agent. A System is a set of connected parts forming a complex whole that can also be used as a resource by other systems. A Cyber-Physical System is a system with both physical and computational components. They deeply integrate computation, communication and control into physical systems. An Agent is a system that can act on its own, sense the environment and produce changes in the world. When an agent is embedded into a cyberphysical system it is called a CPS Agent, or cyberphysical
agent.

Important for mathematical desciptions of interrelations between systems are the elements Arc, Node and Graph. Where an Arc is any element of a graph that connects two Nodes, while a Graph is a set of Nodes connected by Arcs.

The concept of System can be further expanded by a number of attributes, such as Capacity, Role and Capability that can also have relationships among them. The System itself is represented within the Declarative Knowledge as an Object. Affordance is a property the defines the tasks that can be done on a specific System, while Capability defines the set of tasks the system can perform. Systems can also have Constraints, which in turn are related to KPIs that are used to measure whether such constraints are satisfied. The higher level of the proposed ontology also provides definitions and relationships between the main Knowlegde Base concepts, the Declarative and Procedural Knowledge. 

In our knowledge model, a Transition is a Procedural Knowledge concept that determines how to achieve a certain state (Action) given that an agent observes a particular state (Precondition) as being true in the world and there is an ordered list of effect free function calls in that state (Computation). Meanwhile, both Precondition and Action have a Predicate Set that is directly related to the concept of State from the Declarative Knowledge. The Goal State, which is an specification of State, is related to the concepts of Task and Workflow from the Procedural Knowledge. Where a Workflow is defined as sequence of Tasks, which in turn is defined by a sequence of Goal States assigned to a single Agent. 

![Figure 2](/images/fig2.png)

Figure above presents the main elements of the knowledge base modeling.

##### 2.5.3 ITS Domain ontology design principles

![Figure 3](/images/fig3.png)

With the support of the presented upper ontology model, in this section we propose an ITS domain specific ontology, as depicted in Figure above. One of the central concepts within the ITS domain is the Transport Agent, that extends Agent from the upper ontology. The Transport Agent encompasses agents that are capable of transporting some entity, ranging from physical goods to virtual data. Some important concepts from the upper layers that apply to the Transport Agent include Dynamics and Capacity, among others. Transport Agents in turn are strongly related to the Abstract concept of Transportation Mode which defines the type of transportion scenario (e.g., Roads, Rail, Telco).

Another important concept is the Transportation Infrastructure which encompasses all elements required by a Transportation Mode, such as Routes, Tracks and Transportation Networks. Most elements within the Transportation Infrastructure are extensions of Graph, Arc and Node, abstract concepts from the Upper ontology. Therefore, by using high level graph definitions it is possible to define most of the transportation infrastructure in an ITS Domain. A node inside the transportation infrastructure is referred to as a POI (Point of Interest) and it can be any desired location within the Transportation Network (e.g., a crossing, a specific point in the route, coordinate, a warehouse, a bus stop). A Traffic Semaphore is modeled as a generic Actuator that is used to control and regulate traffic and it can be applied in any transportation scenario. A Transportable Entity encompasses any element that can be transported by a Transport Agent, such as regular Cargo or network Data. A typical Passenger is also a Transportable Entity and extends the upper ontology concept of Human.

#### 3. References

Davies, J., Studer, R., and Warren, P. (2006). Semantic Web technologies: trends and research in ontology-based systems. JohnWiley & Sons, Chichester,West Sussex, PO19 8SQ, England.

Niles, I. and Pease, A. (2001). Towards a Standard Upper Ontology. In Proceedings of the International Conference on Formal Ontology in Information Systems - Volume 2001, FOIS ’01, pages 2–9, New York, NY, USA. ACM.

Compton, M., Barnaghi, P., Bermudez, L., Garcia-Castro, R., Corcho, O., Cox, S., Graybeal, J., Hauswirth, M., Henson, C., Herzog, A., Huang, V., Janowicz, K., Kelsey, W. D., Phuoc, D. L., Lefort, L., Leggieri, M., Neuhaus, H., Nikolov, A., Page, K., Passant, A., Sheth, A., and Taylor, K. (2012). The SSN ontology of the W3C semantic sensor network incubator group. Web Semantics: Science, Services and Agents on the World Wide Web, 17:25 – 32.
