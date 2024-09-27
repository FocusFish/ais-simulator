# AIS Simulator

A simple AIS server simulator that replays old AIS data in either `.csv` or `.zip` formats.

Data can be found at https://web.ais.dk/aisdata/. Note that old data is aggregated per month, so if a "day" file is used
it might disappear over time.

## Build

Build with `mvn clean install`, this creates a fat jar.

Build Docker image with: `docker build -t hav/ais-simulator:latest .`. Requires the build above to have successfully
run.

## Run

Run with `java -jar target/ais-simulator.jar`.

Server listens on ports 8040 (ais data) and 8041 (configuration channel).

Additional options can be supplied with `-D` flags, e.g. `java -Dais_nth_pos=3 -jar target/ais-simulator.jar`. See
Options chapter for all options.

Run Docker container with `docker run -d --name ais-simulator -p 8040:8040 -p 8041:8041  hav/ais-simulator:latest`.

### Options

| option                | default value      | comment                                                |
|-----------------------|--------------------|--------------------------------------------------------|
| ais_nth_pos           | 1                  | Reads every nth position in the sim file               |
| sim_file              | aisdk_20190513.csv | The simulation file to replay                          |
| simulate_stuck_socket | false              | Can be set using the config channel, see example below |


## Configuration example

There's a configuration channel running on port 8041. Connect with e.g. `nc`.

```bash
> nc localhost 8041
Welcome to the config channel. Type 'help' for available commands.
help
You can turn on/off simulating stuck socket with sim_stuck

sim_stuck
will enable socket stuck simulation

sim_stuck
will disable socket stuck simulation
```
