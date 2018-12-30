This is a composition system for web services
----------------------------------------------

===============in order to undersand the algorithm used in the composition, take a look in the joint pdf.

===============in order to use this system, you should take in consideration those things :

------------------------------------
----> The registry
------------------------------------

- run the registry with a specific port number for request listening (java MyRegistry 5000) [there is 8 ports numbers to use if we want to run more than one registry (5000 ... 5007)]
- in the Properties/conf.properties you *should* modify the path of the folder where the WSDL files exist !! [Folder name : WSDLs]

------------------------------------
----> The composition engine
------------------------------------

- run the composition engine (java ComEngMain), note that the number of the registries and their port number used is specified in the configuration file [think to modify it]
- in the Properties/conf.properties you *should* modify the path of the folder where the WSDL files will be stocked !! [Folder name : RetrievedWSDLs]




------------------------------------
----> The clients
------------------------------------

- run the clients (java MyClient *number_of_clients_to_generate*) [think about choosing the number of clients]
- in the Properties/conf.properties you *should* modify the path of the folder where the WSDL files will be stocked !! [Folder name : CompositionWSDLs]
