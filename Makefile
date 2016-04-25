.PHONY: compile test unit-test integration-test integration-test-timeseries protogen

RIAK_PORT ?= 8087

compile:
	mvn clean compile

test: unit-test integration-test

unit-test:
	mvn test

integration-test:
	mvn -Pitest,default -Dcom.basho.riak.2i=true -Dcom.basho.riak.yokozuna=true -Dcom.basho.riak.buckettype=true -Dcom.basho.riak.crdt=true -Dcom.basho.riak.lifecycle=true -Dcom.basho.riak.pbcport=$(RIAK_PORT) verify

integration-test-timeseries:
	mvn -Pitest,default -Dcom.basho.riak.buckettype=true -Dcom.basho.riak.crdt=true -Dcom.basho.riak.lifecycle=true -Dcom.basho.riak.timeseries=true -Dcom.basho.riak.pbcport=$(RIAK_PORT) verify

protogen:
	mvn -Pprotobuf-generate generate-sources
