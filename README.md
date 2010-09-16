# pfus-464

A monitor to detect in real time newly orphaned CompletedQuestionnaireLinks to
help narrow down the defact causing bug-464

## Usage

There are 2 options

1. Use the standalone jar

    java -jar pfus-464-1.0.0-SNAPSHOT-standalone.jar

2. (recommended) Start the REPL using leiningen with `lein repl`

    pti@pti-laptop:~/playpen/pfus-464$ lein repl
    "REPL started; server listening on localhost:49710."
    pfus-464.core=> (start)
    nil
    [2010-09-16 23:14:38,357] INFO     0[Monitor] - Orphan CQN Monitor started
    pfus-464.core=> (stop)
    [2010-09-16 23:14:42,275] INFO  3918[Monitor] - Monitor interrupted
    [2010-09-16 23:14:42,276] INFO  3919[Monitor] - Orphan CQN monitor stopped
    nil

This starts the interactive environment and allows to start and stop the
monitor with the command `(start)` respectively `(stop)`.

The standalone jar starts immediately up and can be shut down using Ctrl-C.



## Installation

1. Install leiningen

2. Get the code using `git clone http://github.com/melexis/pfus-464.git`

3. Download the dependencies with `lein deps`

4. (Optional) Build the standalone executable jar with `lein uberjar`


## License

Copyright (C) 2010 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
