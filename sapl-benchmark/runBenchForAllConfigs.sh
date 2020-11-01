#!/bin/bash

java --version

  JAR_NAME=sapl-benchmark-2.0.0-SNAPSHOT.jar
	SEED=3242
	RUNS=100
	ITER=1
	INDEX=SIMPLE

	echo "IMPROVED - 5k policies"
	time java -Xms4096m -Xmx14000m -Dspring.profiles.active=5k -jar target/$JAR_NAME \
		-index $INDEX -iter $ITER --sapl.random.seed=$SEED --sapl.number.of.benchmark.runs=$RUNS

	echo "IMPROVED - 15k policies"
	time java -Xms4096m -Xmx14000m -Dspring.profiles.active=15k -jar target/$JAR_NAME \
		-index $INDEX -iter $ITER --sapl.random.seed=$SEED --sapl.number.of.benchmark.runs=$RUNS

	echo "IMPROVED - 25k policies"
	time java -Xms4096m -Xmx14000m -Dspring.profiles.active=25k -jar target/$JAR_NAME \
		-index $INDEX -iter $ITER --sapl.random.seed=$SEED --sapl.number.of.benchmark.runs=$RUNS

	echo "IMPROVED - 30k policies"
	time java -Xms4096m -Xmx14000m -Dspring.profiles.active=30k -jar target/$JAR_NAME \
		-index $INDEX -iter $ITER --sapl.random.seed=$SEED --sapl.number.of.benchmark.runs=$RUNS

	echo "IMPROVED - 40k policies"
	time java -Xms4096m -Xmx14000m -Dspring.profiles.active=40k -jar target/$JAR_NAME \
		-index $INDEX -iter $ITER --sapl.random.seed=$SEED --sapl.number.of.benchmark.runs=$RUNS

	
