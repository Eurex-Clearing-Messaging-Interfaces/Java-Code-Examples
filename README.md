[![Build Status](https://travis-ci.org/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples.svg?branch=master)](https://travis-ci.org/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples)
[![CircleCI](https://circleci.com/gh/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples.svg?style=shield)](https://circleci.com/gh/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples)
[![Coverage Status](https://coveralls.io/repos/github/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples/badge.svg?branch=master)](https://coveralls.io/github/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples?branch=master)

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
                .hostname("ecag-fixml-simu1.deutsche-boerse.com")
                .port(10170)
                .keystoreFilename("ABCFR_ABCFRALMMACC1.keystore")
                .keystorePassword("123456")
                .truststoreFilename("truststore")
                .truststorePassword("123456")
                .certificateAlias("abcfr_abcfralmmacc1")
                .build();

## Execution (all modules)

### Broadcast Receiver
  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" \<class\>

where \<module\> should be replaced again by one of the modules as in configuration and \<class\> with the class which should run. Example how to start client using AMQP 1.0
protocol with Qpid JMS client:

  $ java -cp "qpid-jms/target/classes/:qpid-jms/target/dependency/*" com.deutscheboerse.amqp_1_0.examples.BroadcastReceiver


### Request Response

  $ java -cp "\<module\>/target/classes/:\<module\>target/dependency/*" \<class\>

where \<module\> should be replaced again by one of the modules as in configuration and \<class\> with the class which should run. Example how to start client using AMQP 0.10
protocol with Qpid JMS client:

  $ java -cp "qpid-amqp-0.10-jms/target/classes/:qpid-amqp-0.10-jms/target/dependency/*" com.deutscheboerse.amqp_0_10.examples.RequestResponse

## Stop example

Any running example can be stopped by pressing _Ctrl + C_

# Integration tests

The project is using Travis-CI and Circle CI to run its own integration tests. The tests are executed against Docker images which contain the AMQP broker with configuration corresponding to Eurex Clearing FIXML Interface. The details of the integration tests can be found in the .travis.yml and circle.yml files.

# Documentation

More details about Java APIs and code examples can be found in the Volume B of Eurex Clearing Messaging Interfaces documentation on [Eurex Clearing website](http://www.eurexclearing.com/clearing-en/technology/eurex-release14/system-documentation/system-documentation/861464?frag=861450)

