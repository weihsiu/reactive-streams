# JCConf 2016 presentation

The slides for the presentation can be found [here](https://docs.google.com/presentation/d/16jM9lxqN0nCStjp0lUbk4JQWISzVvePDcUqH-7X1agk/edit?usp=sharing). The video of this talk can be found [here](https://www.youtube.com/watch?v=j3blqi3g_lA).

## Examples in The Talk

### Akka Stream to demo backpressure
1.
    ```bash
    bash> watch -d -n 1 netstat -n -p tcp
    ```
2. 
    ```sbt
    sbt> reactiveStreamsJVM/runMain reactivestreams.Firehose
    ```
3.
    ```sbt
    sbt> reactiveStreamsJVM/runMain reactivestreams.DeepWell
    ```
4. Suspend `reactivestreams.DeepWell` (Ctrl-z)

### Monte Carlo with Akka Stream
```sbt
sbt> reactiveStreamsJVM/runMain reactivestreams.akkastreams.Examples
```

### Bitconins Transaction
1. Compile Scala to JavaScript
    ```sbt
    sbt> reactiveStreamsJS/fastOptJS
    ```
2. Run Akka-HTTP
    ```sbt
    sbt> reactiveStreamsJVM/runMain reactivestreams.akkastreams.BitcoinTransactions
    ```
3. Open [bitcoin-transactions.html](js/src/main/resources/bitcoin-transactions.html). After compiling Scala to JavaScript, it will be located at `{root}/js/target/scala-2.12/classes/bitcoin-transactions.html`.

### Monix
```sbt
sbt> reactiveStreamsJVM/runMain reactivestreams.monixs.Examples
```
Although Google's API is not working now, you can still learn how to use Monix.