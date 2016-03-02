# How to compile and execute examples


## Compilation

 $ _mvn clean package dependency:copy-dependencies_

## Configuration
 * Edit _example.properties_ in client module to suit your needs, file is located in directory: _\<module\>/target/classes/com/deutscheboerse/amqp/examples_, where \<module\> should be replaced by one of:
     * qpid-amqp-0.10-jms
     * qpid-jms
 * Change connection string and queue names according to the documentation.
 * Configure certificates to maintain secure connection to broker
  * Set passwords to trust store and key store in connection string in file _example.properties_ (default password is '123456')
  * Set full path to trust store and key store in connection string in file _example.properties_ (default values 'truststore.jks' and 'ABCFR_ABCFRALMMACC1.keystore' will **NOT work**)

## Execution

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
