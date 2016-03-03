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

## Configuration (all modules except swiftmq)
 * Edit _example.properties_ in client module to suit your needs, file is located in directory: _\<module\>/target/classes/com/deutscheboerse/amqp/examples_, where \<module\> should be replaced by one of:
     * qpid-amqp-0.10-jms
     * qpid-jms
 * Change connection string and queue names according to the documentation.
 * Configure certificates to maintain secure connection to broker
  * Set passwords to trust store and key store in connection string in file _example.properties_ (default password is '123456')
  * Set full path to trust store and key store in connection string in file _example.properties_ (default values 'truststore.jks' and 'ABCFR_ABCFRALMMACC1.keystore' will **NOT work**)

## Configuration (module swiftmq)
 * Edit creating of connection and consumer/producer to suit your needs in files _BroadcastReceiver.java_ and _RequestResponse.java._
     * How to change connection string and queue names is described in documentation.
 * Configure certificates to maintain secure connection to broker
     * Set passwords to trust store and key store in _BroadcastReceiver.java_, _RequestResponse.java_ files (default password is '123456')
     * Set full path to trust store and key store in _BroadcastReceiver.java_, _RequestResponse.java_ files (default values 'truststore' and 'ABCFR_ABCFRALMMACC1.keystore' will **NOT work**)
 * Proceed with compilation

## Execution (all modules)

### Broadcast Receiver
  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" com.deutscheboerse.amqp.examples.BroadcastReceiver

where \<module\> should be replaced again by one of the modules as in configuration. Example how to start client using AMQP 1.0
protocol with Qpid JMS client:

  $ java -cp "qpid-jms/target/classes/:qpid-jms/target/dependency/*" com.deutscheboerse.amqp.examples.BroadcastReceiver


### Request Response

  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" com.deutscheboerse.amqp.examples.RequestResponse

where \<module\> should be replaced again by one of the modules as in configuration. Example how to start client using AMQP 0.10
protocol with Qpid JMS client:

  $ java -cp "qpid-amqp-0.10-jms/target/classes/:qpid-amqp-0.10-jms/target/dependency/*" com.deutscheboerse.amqp.examples.RequestResponse

## Stop example

Any running example can be stopped by pressing _Ctrl + C_

# Documentation

More details about Java APIs and code examples can be found in the Volume B of Eurex Clearing Messaging Interfaces documentation on http://www.eurexclearing.com/clearing-en/technology/eurex-release14/system-documentation/system-documentation/861464?frag=861450
