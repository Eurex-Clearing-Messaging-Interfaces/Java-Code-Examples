[![CircleCI](https://circleci.com/gh/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples.svg?style=shield)](https://circleci.com/gh/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples)

# How to compile and execute examples


## Compilation (all modules except swiftmq)

 $ _mvn clean package dependency:copy-dependencies_

## Compilation (module swiftmq)

 * Download swift mq jar files and add them to local repository
     * Download client libraries (swiftmq_*_client.tar.gz) from [here](http://www.swiftmq.com/downloads/index.html)
     * Put files _amqp.jar_ and _swiftmq.jar_ to project root directory (where top level file _pom.xml_ is located)
 * Add _amqp.jar_ and _swiftmq.jar_ files to local repository by executing commands below

 $ _mvn install:install-file -DlocalRepositoryPath=repo-local -DcreateChecksum=true -Dpackaging=jar -Dfile=swiftmq.jar -DgroupId=com -DartifactId=swiftmq -Dversion=1.0_

 $ _mvn install:install-file -DlocalRepositoryPath=repo-local -DcreateChecksum=true -Dpackaging=jar -Dfile=amqp.jar -DgroupId=com.swiftmq -DartifactId=amqp -Dversion=1.0_

 * Compile

 $ _mvn -Pswiftmq clean package dependency:copy-dependencies_

## Configuration (all modules)
 * Edit _BroadcastReceiver.java_ and/or _RequestResponse.java_ in modules you want to compile and change connection options as described in the
documentation:

             Options options = new Options.OptionsBuilder()
                .accountName("ABCFR_ABCFRALMMACC1")
                .hostname("ecag-macal-sim1")
                .port(35671)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("12345678")
                .truststoreFilename("ecag-macal-sim1.truststore")
                .truststorePassword("12345678")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();

## Execution (all modules)

### Broadcast Receiver
  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" \<class\>

where \<module\> should be replaced again by one of the modules as in configuration and \<class\> with the class which should run. Example how to start client with Qpid JMS client:

  $ java -cp "qpid-jms/target/classes/:qpid-jms/target/dependency/*" com.deutscheboerse.amqp_1_0.examples.BroadcastReceiver


### Request Response

  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" \<class\>

where \<module\> should be replaced again by one of the modules as in configuration and \<class\> with the class which should run. Example how to start client with Qpid JMS client:

  $ java -cp "qpid-jms/target/classes/:qpid-jms/target/dependency/*" com.deutscheboerse.amqp_1_0.examples.RequestResponse

## Stop example

Any running example can be stopped by pressing _Ctrl + C_

# Integration tests

The project is using Circle CI to run its own integration tests. The tests are executed against Docker images which contain the AMQP broker with configuration corresponding to Eurex Clearing MACAL Interface. The details of the integration tests can be found in the .circleci/config.yml file.

# Documentation

More details about Java APIs and code examples can be found in the Volume B of Eurex Clearing Messaging Interfaces documentation on [Eurex Clearing website](http://www.eurexclearing.com/clearing-en/technology/eurex-release14/system-documentation/system-documentation/861464?frag=861450)

